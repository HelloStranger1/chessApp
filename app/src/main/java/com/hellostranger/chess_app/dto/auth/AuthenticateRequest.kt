package com.hellostranger.chess_app.dto.auth

data class AuthenticateRequest(
    val email : String,
    val password : String
)
