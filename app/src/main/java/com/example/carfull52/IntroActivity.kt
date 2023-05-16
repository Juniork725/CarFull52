package com.juniork.carfull52

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.juniork.carfull52.MyApplication.Companion.auth
import com.juniork.carfull52.MyApplication.Companion.db
import com.juniork.carfull52.MyApplication.Companion.email
import com.juniork.carfull52.R
import com.juniork.carfull52.databinding.ActivityIntroBinding
import com.juniork.carfull52.databinding.FindIdDialogfragmentBinding
import kotlinx.android.synthetic.main.activity_join.*
import kotlinx.android.synthetic.main.activity_join.view.*
import java.util.stream.DoubleStream.builder

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Firebase.auth.currentUser != null) {
            MyApplication.checkAuth()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        binding.joinButton.setOnClickListener {
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
        }

        val requestLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult())
        {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        db.collection("ID_List").whereEqualTo("email",account.email).get().addOnSuccessListener { documents ->
                            if (documents.isEmpty()) {
                                val alertDialog = AlertDialog.Builder(this)
                                val findIDBinding = FindIdDialogfragmentBinding.inflate(layoutInflater)
                                alertDialog.setTitle("ID 설정")
                                alertDialog.setMessage("다른 사용자들에게 보여질 ID를 설정해주세요.")
                                alertDialog.setView(findIDBinding.root)
                                findIDBinding.Emailtext.hint = "ID"
                                findIDBinding.Emailtext.inputType = InputType.TYPE_CLASS_TEXT
                                alertDialog.setPositiveButton("확인"){_,_ -> null}
                                alertDialog.setNegativeButton("닫기"){_,_ -> null}

                                val AD = alertDialog.create()

                                AD.show()

                                AD.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                    val userID = findIDBinding.Emailtext.text.toString()
                                    MyApplication.db.collection("ID_List")
                                        .get()
                                        .addOnSuccessListener { result ->
                                            if (result.any { it.id == userID }) {
                                                toast?.cancel()
                                                toast = Toast.makeText(this, "이미 사용 중인 ID입니다.", Toast.LENGTH_SHORT)
                                                toast?.show()
                                            } else {
                                                MyApplication.db.collection("ID_List")
                                                    .document(userID)
                                                    .set(mapOf(
                                                        "ID" to userID,
                                                        "email" to account.email
                                                    )).addOnSuccessListener {
                                                        MyApplication.email = account.email
                                                        MyApplication.ID = userID
                                                        auth.currentUser!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(userID).build())
                                                        toast?.cancel()
                                                        toast = Toast.makeText(this, "구글 로그인 성공", Toast.LENGTH_SHORT)
                                                        toast?.show()
                                                        Log.d("Jun", "IntroActivity) Google log in success")
                                                        val intent = Intent(this, MainActivity::class.java)
                                                        startActivity(intent)
                                                        AD.dismiss()
                                                    }.addOnFailureListener { e ->
                                                        Log.d("Jun", "IntroActivity) Setting google user ID failed with ", e)
                                                    }
                                            }
                                        }.addOnFailureListener { e ->
                                            Log.d("Jun", "IntroActivity) Getting user ID failed with ", e)
                                        }
                                }
                            } else {
                                Log.d("Jun", "IntroActivity) Google log in success")
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    } else {
                        toast?.cancel()
                        toast = Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT)
                        toast?.show()
                    }
                }
            } catch (e: ApiException) {
                Log.w("Jun","error",e)
                toast?.cancel()
                Toast.makeText(this, "구글 로그인 오류", Toast.LENGTH_SHORT)
                toast?.show()
            }
        }

        binding.googleLoginButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(
                R.string.default_web_client_id)).requestEmail().build()
            val signInIntent = GoogleSignIn.getClient(this, gso).signInIntent
            requestLauncher.launch(signInIntent)
        }
    }

    override fun onStop() {
        super.onStop()
        toast?.cancel()
    }

    var initTime = 0L
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode === KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - initTime > 3000) {
                toast?.cancel()
                toast = Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
                toast?.show()
                initTime = System.currentTimeMillis()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}