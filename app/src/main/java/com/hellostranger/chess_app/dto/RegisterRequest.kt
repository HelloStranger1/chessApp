package com.hellostranger.chess_app.dto

data class RegisterRequest(
    val firebaseUuid : String,
    val name : String,
    val email : String
)