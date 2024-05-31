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
import com.hellostranger.chess_app.dto.requests.RegisterRequest
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
class SignUpActivity : BaseActivity() {


    private var binding: ActivitySignUpBinding? = null

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
    private fun setUpActionBar(){

        setSupportActionBar(binding?.toolbarSignUpActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)

        binding?.toolbarSignUpActivity?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun registerUser(){
        val name: String = binding?.etNameSignUp?.text.toString().trim{ it <= ' ' }
        val email: String = binding?.etEmailSignUp?.text.toString().trim{ it <= ' ' }
        val password: String = binding?.etPasswordSignUp?.text.toString().trim{ it <= ' ' }

        if(validateForm(name, email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))

            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val response =
                    AuthRetrofitClient.instance.register(RegisterRequest(name, email, password))
                if(response.isSuccessful && response.body() != null){
                    Log.e("TAG", "registered the user. response: " + response.body())
                    MyApp.tokenManager.saveAccessToken(response.body()!!.accessToken, response.body()!!.accessExpiresIn)
                    MyApp.tokenManager.saveRefreshToken(response.body()!!.refreshToken, response.body()!!.refreshExpiresIn)
                    MyApp.tokenManager.saveUserEmail(email)
                    runOnUiThread {
                        hideProgressDialog()
                        val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                        startActivity(intent)
                    }
                } else if(!response.isSuccessful){
                    runOnUiThread{
                        Toast.makeText(
                            this@SignUpActivity,
                            "Response failed, it is: ${response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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

}