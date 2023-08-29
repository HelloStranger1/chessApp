package com.hellostranger.chess_app.dto

data class AuthenticationResponse(

    val accessToken : String,
    val accessExpiresIn : Long,
    val refreshToken : String,
    val refreshExpiresIn : Long
)