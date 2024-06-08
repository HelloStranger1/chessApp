package com.hellostranger.chess_app.network.retrofit.auth

import android.util.Log
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val authApiService: AuthApiService) : Interceptor {
    private val tokenManager : TokenManager = MyApp.tokenManager

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()
        if (accessToken != "" && tokenManager.shouldRefreshAccessToken()) {
            val refreshToken = tokenManager.getRefreshToken()
            // Make the token refresh request
            val refreshedToken = runBlocking {
                val response = authApiService.refreshToken("Bearer $refreshToken")
                if(response.isSuccessful){
                    tokenManager.saveAccessToken(response.body()!!.accessToken, response.body()!!.accessExpiresIn)
                    response.body()!!.accessToken
                }else{
                    tokenManager.clearSession()
                }
            }

            // Create a new request with the refreshed access token
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $refreshedToken")
                .build()

            // Retry the request with the new access token
            return chain.proceed(newRequest)
        } else if (accessToken != "" && tokenManager.isAccessTokenExpired()) {
            tokenManager.clearSession()
        }

        // Add the access token to the request header
        val authorizedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(authorizedRequest)
    }
}