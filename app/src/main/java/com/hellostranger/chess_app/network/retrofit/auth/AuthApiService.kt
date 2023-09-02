package com.hellostranger.chess_app.network.retrofit.auth

import com.hellostranger.chess_app.dto.AuthenticateRequest
import com.hellostranger.chess_app.dto.AuthenticationResponse
import com.hellostranger.chess_app.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("/api/auth/register")
    suspend fun register(@Body request : RegisterRequest) : Response<AuthenticationResponse>

    @POST("/api/auth/authenticate")
    suspend fun authenticate(@Body request : AuthenticateRequest) : Response<AuthenticationResponse>

    @POST("/api/auth/refresh-token")
    suspend fun refreshToken(@Header("Authorization") refreshToken : String) : Response<AuthenticationResponse>

    @POST("/api/auth/logout")
    suspend fun logout()
}