package com.hellostranger.chess_app.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R

@SuppressLint("CustomSplashScreen")
@ExperimentalUnsignedTypes
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }else{
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
        }

        // Checks if you are logged in and sends you to the appropriate activity.
        Handler(Looper.getMainLooper()).postDelayed({
            if(!MyApp.tokenManager.isAccessTokenExpired()){
                startActivity(Intent(this, MainActivity::class.java))
            }else{
                startActivity(Intent(this, IntroActivity::class.java))
            }

            finish()
        }, 2500)
    }
}