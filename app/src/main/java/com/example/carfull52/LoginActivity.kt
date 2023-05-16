package com.juniork.carfull52

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.UserProfileChangeRequest
import com.juniork.carfull52.MyApplication.Companion.auth
import com.juniork.carfull52.MyApplication.Companion.db
import com.juniork.carfull52.MyApplication.Companion.email
import com.juniork.carfull52.databinding.ActivityLoginBinding
import com.juniork.carfull52.databinding.FindIdDialogfragmentBinding
import com.juniork.carfull52.databinding.ResetPasswordDialogfragmentBinding
import kotlinx.android.synthetic.main.activity_join.*
import kotlinx.android.synthetic.main.find_id_dialogfragment.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var toast: Toast? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var count = 0
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.setOnClickListener {
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.userID!!.windowToken, 0)
            imm.hideSoftInputFromWindow(binding.password.windowToken, 0)
            binding.login.isEnabled = false
            val ID = binding.userID!!.text.toString()
            val password = binding.password.text.toString()
            if (ID.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "ID를 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
            } else if (password.isEmpty()) {
                toast?.cancel()
                toast = Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT)
                toast?.show()
            } else {
                MyApplication.db.collection("ID_List").get().addOnSuccessListener { result ->
                    if (result.any { it.id == ID }) {
                        MyApplication.db.collection("ID_List").document(ID).get().addOnSuccessListener { document ->
                            val email = document.data!!.get("email").toString()
                            Log.d("Jun", "LoginActivity) E-mail searched: ${email}")

                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    //binding.userID!!.text.clear()
                                    //binding.password.text.clear()
                                    if (MyApplication.checkAuth()) {
                                        toast?.cancel()
                                        toast = Toast.makeText(this@LoginActivity, "개인 로그인 성공",Toast.LENGTH_SHORT)
                                        toast?.show()
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        toast?.cancel()
                                        toast = Toast.makeText(baseContext, "전송된 메일로 이메일 인증이 되지 않았습니다.", Toast.LENGTH_SHORT)
                                        toast?.show()
                                    }
                                }/* else if (task.exception?.message == "The email address is badly formatted.") {
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "E-mail을 다시 확인해주세요.", Toast.LENGTH_SHORT)
                                    toast?.show()
                                }*/ else if (task.exception?.message == "The password is invalid or the user does not have a password.") {
                                    count += 1
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "비밀번호 오류. 5회 틀릴 시 차단됩니다. ($count/5)", Toast.LENGTH_SHORT)
                                    toast?.show()
                                }/* else if (task.exception?.message == "There is no user record corresponding to this identifier. The user may have been deleted.") {
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "등록되지 않은 계정입니다.", Toast.LENGTH_SHORT)
                                    toast?.show()
                                }*/ else if (task.exception?.message == "We have blocked all requests from this device due to unusual activity. Try again later. [ Access to this account has been temporarily disabled due to many failed login attempts. You can immediately restore it by resetting your password or you can try again later. ]") {
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "비밀번호를 5회 틀렸습니다. 비밀번호를 재설정해주세요.", Toast.LENGTH_SHORT)
                                    toast?.show()
                                } else if (task.exception?.message == "There is no user record corresponding to this identifier. The user may have been deleted.") {
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "등록되지 않은 계정입니다.", Toast.LENGTH_SHORT)
                                    toast?.show()
                                } else {
                                    toast?.cancel()
                                    toast = Toast.makeText(this, "로그인 실패", Toast.LENGTH_LONG)
                                    toast?.show()
                                    Log.d("Login Error",task.exception?.message, task.exception)
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.d("Jun", "LoginActivity) E-mail searching failed with ", e)
                        }
                    } else {
                        toast?.cancel()
                        toast = Toast.makeText(this, "등록되지 않은 ID입니다.", Toast.LENGTH_SHORT)
                        toast?.show()
                    }
                }.addOnFailureListener { e ->
                    Log.d("Jun", "LoginActivity) Getting collection failed with ", e)
                }
            }
            binding.login.isEnabled = true
        }

        binding.findID?.setOnClickListener {
            val alertDialog = AlertDialog.Builder(this)
            val findIDBinding = FindIdDialogfragmentBinding.inflate(layoutInflater)
            alertDialog.setTitle("ID 찾기")
            alertDialog.setMessage("인증에 사용한 E-mail을 입력하세요.")
            alertDialog.setView(findIDBinding.root)
            alertDialog.setPositiveButton("찾기"){_,_ -> null}
            alertDialog.setNegativeButton("닫기"){_,_ -> null}

            val AD = alertDialog.create()

            AD.show()

            AD.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = findIDBinding.Emailtext.text.toString()
                db.collection("ID_List")
                    .whereEqualTo("email", text)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty()) {
                            toast?.cancel()
                            toast = Toast.makeText(this, "해당 계정을 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            toast?.show()
                        } else {
                            for (document in documents) {
                                toast?.cancel()
                                toast = Toast.makeText(this, "해당 계정의 ID는 [${document.id}]입니다.", Toast.LENGTH_SHORT)
                                toast?.show()
                                AD.dismiss()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("Jun", "LoginActivity) Finding ID failed with ", e)
                    }
            }
        }

        binding.passwordReset?.setOnClickListener {
            val alertDialog = AlertDialog.Builder(this)
            val resetPasswordBinding = ResetPasswordDialogfragmentBinding.inflate(layoutInflater)
            alertDialog.setTitle("비밀번호 변경")
            alertDialog.setMessage("ID와 인증용 E-mail을 입력하세요.")
            alertDialog.setView(resetPasswordBinding.root)
            alertDialog.setPositiveButton("확인"){_,_ -> null}
            alertDialog.setNegativeButton("닫기"){_,_ -> null}

            val AD = alertDialog.create()

            AD.show()

            AD.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val id = resetPasswordBinding.IDtext.text.toString()
                val email = resetPasswordBinding.Emailtext.text.toString()
                db.collection("ID_List").get().addOnSuccessListener { result ->
                    if (result.any { it.id == id }) {
                        db.collection("ID_List").document(id).get().addOnSuccessListener { document ->
                            if (email == document.data?.get("email").toString() ?:"") {
                                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                    if (task.isSuccessful){
                                        toast?.cancel()
                                        toast = Toast.makeText(this@LoginActivity, "비밀번호 변경 메일을 전송했습니다.", Toast.LENGTH_SHORT)
                                        toast?.show()
                                        AD.dismiss()
                                    } else {
                                        toast?.cancel()
                                        toast = Toast.makeText(this@LoginActivity, "오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT)
                                        toast?.show()
                                        Log.d("Jun", "LoginActivity) Resetting password failed with ", task.exception)
                                        AD.dismiss()
                                    }
                                }
                            } else {
                                toast?.cancel()
                                toast = Toast.makeText(this, "ID와 인증용 계정이 맞지 않습니다. 다시 확인해주세요.", Toast.LENGTH_SHORT)
                                toast?.show()
                            }
                        }.addOnFailureListener { e ->
                            Log.d("Jun", "LoginActivity) Finding ID failed with ", e)
                        }
                    } else {
                        toast?.cancel()
                        toast = Toast.makeText(this, "등록되지 않은 ID입니다.", Toast.LENGTH_SHORT)
                        toast?.show()
                    }
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}