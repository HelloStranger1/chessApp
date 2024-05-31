package com.hellostranger.chess_app.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context : Context){

    companion object{
        private const val PREF_NAME = "TokenPrefs"
        private const val KEY_ACCESS_TOKEN = "jwtAccessToken"
        private const val KEY_ACCESS_EXPIRATION = "accessExpirationDate"
        private const val KEY_REFRESH_TOKEN = "jwtRefreshToken"
        private const val KEY_REFRESH_EXPIRATION = "accessRefreshDate"
        private const val KEY_USER_EMAIL = "userEmail"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveRefreshToken(token: String, expirationDate: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_REFRESH_TOKEN, token)
            putLong(KEY_REFRESH_EXPIRATION, expirationDate)
            apply()
        }
    }

    fun saveAccessToken(token: String, expirationDate: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            putLong(KEY_ACCESS_EXPIRATION, expirationDate)
            apply()
        }
    }

    fun saveUserEmail(email : String){
        sharedPreferences.edit().apply{
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }


    fun getUserEmail() : String{
        return sharedPreferences.getString(KEY_USER_EMAIL, null) ?: return ""
    }

    fun getAccessToken(): String {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return ""
    }
    fun getRefreshToken(): String {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null) ?: return ""
    }

    fun shouldRefreshAccessToken() : Boolean{
        val currentTimeMillis = System.currentTimeMillis()
        val expiry = getAccessExpiration()
        return currentTimeMillis + 60_000 >= expiry && currentTimeMillis < expiry
    }
    fun isAccessTokenExpired() : Boolean {
        return System.currentTimeMillis() >= getAccessExpiration()
    }


    private fun getAccessExpiration() : Long{
        return sharedPreferences.getLong(KEY_ACCESS_EXPIRATION, 0)
    }
    private fun getRefreshExpiration() : Long{
        return sharedPreferences.getLong(KEY_REFRESH_EXPIRATION, 0)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}