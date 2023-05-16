package com.juniork.carfull52

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.uptimeMillis
import android.service.autofill.Validators.not
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.firebase.auth.UserProfileChangeRequest
import com.juniork.carfull52.MyApplication.Companion.auth
import com.juniork.carfull52.MyApplication.Companion.email
import com.juniork.carfull52.databinding.ActivityJoinBinding

class JoinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinBinding
    private var toast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.join.setOnClickListener {
            var isGoToJoin = true
            val ID = binding.userID.text.toString()
            val email = binding.usermail.text.toString()
            val password = binding.password.text.toString()
            val passwordCheck = binding.passwordCheck.text.toString()

            if (ID.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "ID를 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            } else if (password.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            } else if (passwordCheck.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "비밀번호를 한 번 더 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            } else if (password != passwordCheck) {
                toast?.cancel()
                toast = Toast.makeText(this, "비밀번호를 똑같이 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            } else if (password.length < 6) {
                toast?.cancel()
                toast = Toast.makeText(this, "비밀번호를 6자리 이상으로 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            } else if (email.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "E-mail을 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
                isGoToJoin = false
            }

            if (isGoToJoin) {
                MyApplication.db.collection("ID_List")
                    .get()
                    .addOnSuccessListener { result ->
                        if (result.any { it.id == ID }) {
                            toast?.cancel()
                            toast = Toast.makeText(this, "이미 사용 중인 ID입니다.", Toast.LENGTH_SHORT)
                            toast?.show()
                        } else {
                            MyApplication.auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        binding.join.isEnabled = false
                                        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                        imm.hideSoftInputFromWindow(binding.join.windowToken, 0)

                                        MyApplication.auth.currentUser?.sendEmailVerification()
                                            ?.addOnCompleteListener { sendTask ->
                                                if (sendTask.isSuccessful) {
                                                    MyApplication.db.collection("ID_List")
                                                        .document(ID)
                                                        .set(mapOf(
                                                            "ID" to ID,
                                                            "email" to email
                                                        ))
                                                        .addOnSuccessListener {
                                                            auth.currentUser!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(ID).build())
                                                                .addOnCompleteListener{
                                                                    Log.d("Jun", "JoinActivity) ID set: ${auth.currentUser!!.displayName}")
                                                                    MyApplication.ID = auth.currentUser!!.displayName
                                                                }.addOnFailureListener { e ->
                                                                    Log.w("Jun", "JoinActivity) ID setting error by ",e)
                                                                }
                                                            toast?.cancel()
                                                            toast = Toast.makeText(this, "회원가입에 성공했습니다." +
                                                                    "입력한 이메일로 전송된 메일을 확인해 주세요.", Toast.LENGTH_LONG)
                                                            toast?.show()
                                                        }.addOnFailureListener { e ->
                                                            Log.d("Jun", "JoinActivity) ID updating on DB error by ",e)
                                                        }
                                                } else {
                                                    toast?.cancel()
                                                    toast = Toast.makeText(this, "메일 전송 실패", Toast.LENGTH_LONG)
                                                    toast?.show()
                                                }
                                            }
                                    } else if (task.exception?.message == "The email address is already in use by another account.") {
                                        toast?.cancel()
                                        toast = Toast.makeText(baseContext, "이미 가입 요청된 이메일입니다. 전송된 메일을 확인해주세요.", Toast.LENGTH_LONG)
                                        toast?.show()
                                    } else if (task.exception?.message == "The email address is badly formatted."){
                                        toast?.cancel()
                                        toast = Toast.makeText(baseContext, "유효한 이메일을 입력해주세요.", Toast.LENGTH_LONG)
                                        toast?.show()
                                    } else {
                                        toast?.cancel()
                                        toast = Toast.makeText(baseContext, "회원가입 오류", Toast.LENGTH_LONG)
                                        toast?.show()
                                        Log.d("Account Join Error", task.exception?.message,task.exception)
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("Jun", "Get ID list failed with", e)
                    }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStop() {
        super.onStop()
        toast?.cancel()
    }
}