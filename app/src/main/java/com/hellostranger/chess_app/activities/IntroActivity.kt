package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import com.hellostranger.chess_app.R

class IntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView((R.layout.activity_intro))

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

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