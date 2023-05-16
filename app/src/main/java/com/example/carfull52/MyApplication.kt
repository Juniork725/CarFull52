package com.juniork.carfull52

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.jakewharton.threetenabp.AndroidThreeTen
import java.io.Serializable

class MyApplication: MultiDexApplication() {
    companion object {
        lateinit var auth: FirebaseAuth
        lateinit var db: FirebaseFirestore
        var email: String? = null
        var ID: String? = null
        fun checkAuth(): Boolean {
            val currentUser = auth.currentUser

            return currentUser?.let {
                email = currentUser.email
                ID = currentUser.displayName
                currentUser.isEmailVerified
            } ?: let {
                false
            }
        }
        class Reservation: Serializable {
            var email: String? = null
            lateinit var userID: String
            lateinit var start: String
            lateinit var end: String
            var date: String = ""
            var time: String? = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        AndroidThreeTen.init(this)
    }
}