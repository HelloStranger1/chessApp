package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.ActivitySignUpBinding
import com.hellostranger.chess_app.dto.auth.AuthenticateRequest
import com.hellostranger.chess_app.dto.requests.RegisterRequest
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
/**
 * Activity for user sign-up.
 */
class SignUpActivity : BaseActivity() {


    private var binding: ActivitySignUpBinding? = null // Binding for the UI

    /**
     * Called when the activity is first created. Initializes the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

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

    /**
     * Sets up the action bar for the activity.
     */
    private fun setUpActionBar(){

        setSupportActionBar(binding?.toolbarSignUpActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)

        binding?.toolbarSignUpActivity?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Registers a new user by sending a request to the backend.
     */
    private fun registerUser(){
        val name: String = binding?.etNameSignUp?.text.toString().trim{ it <= ' ' }
        val email: String = binding?.etEmailSignUp?.text.toString().trim{ it <= ' ' }
        val password: String = binding?.etPasswordSignUp?.text.toString().trim{ it <= ' ' }

        if (!validateForm(name, email, password)) {
            return
        }
        showProgressDialog(resources.getString(R.string.please_wait))

        CoroutineScope(Dispatchers.IO).launch {
            val response = handleResponse(
                {AuthRetrofitClient.instance.register(RegisterRequest(name, email, password))},
                "Couldn't register user"
            )
            if (response == null) {
                runOnUiThread {
                    Toast.makeText(
                        this@SignUpActivity,
                        "Couldn't sign up.",
                        Toast.LENGTH_LONG
                    ).show()
                    hideProgressDialog()
                }
                return@launch
            }

            MyApp.tokenManager.saveAccessToken(response.accessToken, response.accessExpiresIn)
            MyApp.tokenManager.saveRefreshToken(response.refreshToken, response.refreshExpiresIn)
            MyApp.tokenManager.saveUserEmail(email)
            runOnUiThread {
                hideProgressDialog()
                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
            }
        }


    }
    /**
     * Validates the sign-up form.
     * @param name: String - The entered name.
     * @param email: String - The entered email address.
     * @param password: String - The entered password.
     * @return Boolean - Returns true if the form is valid, false otherwise.
     */
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

}