package com.hellostranger.chess_app.dto.requests

data class RegisterRequest(
    val name : String,
    val email : String,
    val password : String
)