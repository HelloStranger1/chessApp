package com.hellostranger.chess_app.dto.auth

data class AuthenticationResponse(

    val accessToken : String,
    val accessExpiresIn : Long,
    val refreshToken : String,
    val refreshExpiresIn : Long
)