package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.models.User

class SignInActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private var etEmail : EditText? = null
    private var etPass : EditText? = null
    private var btnSignIn : AppCompatButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_in)

        auth = Firebase.auth

        etEmail = findViewById(R.id.et_email_sign_in)
        etPass = findViewById(R.id.et_password_sign_in)
        btnSignIn = findViewById(R.id.btn_sign_in)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        btnSignIn?.setOnClickListener{
            signInRegisteredUser()
        }

        setUpActionBar()

    }

    private fun setUpActionBar(){
        val toolbarSignIn : Toolbar = findViewById(R.id.toolbar_sign_in_activity)
        setSupportActionBar(toolbarSignIn)

        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)

        }

        toolbarSignIn.setNavigationOnClickListener { onBackPressed() }
    }

    private fun signInRegisteredUser(){
        val email: String = etEmail!!.text.toString().trim{ it <= ' ' }
        val password: String = etPass!!.text.toString().trim{it <= ' '}

        if(validateForm(email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        hideProgressDialog()
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Sign In", "signInWithEmail:success")
                        val user = auth.currentUser
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Sign In", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()

                    }
                }

        }


    }

    fun signInSuccess(user : User){
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }

    private fun validateForm(email : String, password : String) : Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter an email address")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter a password")
                Log.e("pass", "Missing password")
                false
            }
            else -> {
                true
            }

        }
    }
}