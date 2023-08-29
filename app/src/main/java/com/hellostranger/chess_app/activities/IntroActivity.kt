package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import com.hellostranger.chess_app.R

class IntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView((R.layout.activity_intro))

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
        }

        val btnSignUp : AppCompatButton = findViewById(R.id.btn_sign_up_intro)
        btnSignUp.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val btnSignIn : AppCompatButton = findViewById(R.id.btn_sign_in_intro)
        btnSignIn.setOnClickListener{
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}