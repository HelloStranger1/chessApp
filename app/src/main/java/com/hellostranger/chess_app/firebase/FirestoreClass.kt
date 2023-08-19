package com.hellostranger.chess_app.firebase

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.hellostranger.chess_app.activities.MainActivity
import com.hellostranger.chess_app.activities.SignInActivity
import com.hellostranger.chess_app.activities.SignUpActivity
import com.hellostranger.chess_app.models.User
import com.hellostranger.chess_app.utils.Constants

class FirestoreClass {

    private val mFirestore = Firebase.firestore

    fun registerUser(activity : SignUpActivity, userInfo : User){

        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error Writing Document",
                    e
                )
            }


    }

    fun signInUser(activity : Activity){
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener {document ->
                val loggedInUser = document.toObject(User::class.java)
                Log.e("TAG", "Sign in user. document: ${document.toString()}, loggedInUser: $loggedInUser")
                when(activity){
                    is SignInActivity ->{
                        activity.signInSuccess(loggedInUser!!)
                    }
                    is MainActivity ->{
                        activity.updateNavigationUserDetails(loggedInUser!!)
                    }
                }

            }.addOnFailureListener{
                when(activity){
                    is SignInActivity ->{
                        activity.hideProgressDialog()
                    }
                    is MainActivity ->{
                        activity.hideProgressDialog()
                    }
                }
                Log.e("Firestore SignIn", "Error writing document $it")

            }

    }
    fun getCurrentUserId() : String{
        val currentUser =  FirebaseAuth.getInstance().currentUser
        var currentUserId = ""
        if (currentUser != null){
            currentUserId = currentUser.uid
        }
        return currentUserId

    }

}