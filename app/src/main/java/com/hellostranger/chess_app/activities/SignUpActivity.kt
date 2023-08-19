package com.hellostranger.chess_app.activities

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.ActivitySignUpBinding
import com.hellostranger.chess_app.firebase.FirestoreClass
import com.hellostranger.chess_app.models.User

class SignUpActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private var binding: ActivitySignUpBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        auth = Firebase.auth

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
        }

        binding?.btnSignUp?.setOnClickListener{
            registerUser()
        }

        setUpActionBar()

    }
    private fun setUpActionBar(){

        setSupportActionBar(binding?.toolbarSignUpActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)

        binding?.toolbarSignUpActivity?.setNavigationOnClickListener { onBackPressed() }
    }

    private fun registerUser(){
        val name: String = binding?.etNameSignUp?.text.toString().trim{ it <= ' ' }
        val email: String = binding?.etEmailSignUp?.text.toString().trim{ it <= ' ' }
        val password: String = binding?.etPasswordSignUp?.text.toString().trim{ it <= ' ' }

        if(validateForm(name, email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email!!
                        val user = User(firebaseUser.uid, name, registeredEmail)
                        FirestoreClass().registerUser(this, user)
                    } else {
                        Toast.makeText(
                            this,
                            task.exception!!.message, Toast.LENGTH_SHORT
                        ).show()
                    }

                }

        }
    }
    private fun validateForm(name : String, email : String, password : String) : Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter a name")
                false
            }
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
    fun userRegisteredSuccess(){
        Toast.makeText(
            this@SignUpActivity,
            "Successfully registered",
            Toast.LENGTH_LONG
        ).show()

        hideProgressDialog()
        auth.signOut()
        finish()
    }

}