package com.hellostranger.chess_app

import android.app.Application

class MyApp : Application() {
    companion object{
        lateinit var tokenManager: TokenManager
    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
    }
}