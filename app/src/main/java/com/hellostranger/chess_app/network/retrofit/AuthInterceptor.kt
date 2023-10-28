package com.hellostranger.chess_app.network.retrofit

import android.util.Log
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.network.retrofit.auth.AuthApiService
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val authApiService: AuthApiService) : Interceptor {
    private val tokenManager : TokenManager = MyApp.tokenManager

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenManager.getAccessToken()
        Log.e("AuthInterceptor", "is access expired? ${tokenManager.isAccessTokenExpired()} the token is: ${tokenManager.getAccessToken()}")
        if (accessToken != "" && tokenManager.isAccessTokenExpired()) {
            val refreshToken = tokenManager.getRefreshToken()

            // Make the token refresh request
            val refreshedToken = runBlocking {
                val response = authApiService.refreshToken("Bearer $refreshToken")
                if(response.isSuccessful){
                    tokenManager.saveAccessToken(response.body()!!.accessToken, response.body()!!.accessExpiresIn)
                    response.body()!!.accessToken
                }else{
                    tokenManager.clearSession()
                    //TODO: If user's refresh token is invalid, send him back to the intro screen.
                }
                // Update the refreshed access token and its expiration time in the session

            }

            // Create a new request with the refreshed access token
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $refreshedToken")
                .build()

            // Retry the request with the new access token
            return chain.proceed(newRequest)
        }

        // Add the access token to the request header
        val authorizedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        Log.e("TAG", "Intercepted and corrected. authorizedRequest is: $authorizedRequest ")

        return chain.proceed(authorizedRequest)
    }
}