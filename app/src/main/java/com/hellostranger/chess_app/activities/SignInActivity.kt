package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.dto.auth.AuthenticateRequest
import com.hellostranger.chess_app.dto.auth.AuthenticationResponse
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
class SignInActivity : BaseActivity() {


    private var etEmail : EditText? = null
    private var etPass : EditText? = null
    private var btnSignIn : AppCompatButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        etEmail = findViewById(R.id.et_email_sign_in)
        etPass = findViewById(R.id.et_password_sign_in)
        btnSignIn = findViewById(R.id.btn_sign_in)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
        }

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

        toolbarSignIn.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun signInRegisteredUser(){
        val email: String = etEmail!!.text.toString().trim{ it <= ' ' }
        val password: String = etPass!!.text.toString().trim{it <= ' '}

        if(validateForm(email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            CoroutineScope(Dispatchers.IO).launch {
                val response = handleResponse(
                    {AuthRetrofitClient.instance.authenticate(AuthenticateRequest(email, password))},
                    "Couldn't authorize user"
                )
                if (response == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@SignInActivity,
                            "Couldn't sign in.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                MyApp.tokenManager.saveAccessToken(response.accessToken, response.accessExpiresIn)
                MyApp.tokenManager.saveRefreshToken(response.refreshToken, response.refreshExpiresIn)
                MyApp.tokenManager.saveUserEmail(email)
                runOnUiThread {
                    hideProgressDialog()
                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                }
            }
        }


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