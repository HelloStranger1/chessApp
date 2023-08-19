package com.hellostranger.chess_app.chess_models

data class Player(
    val uid : String,
    val name : String,
    val color : Color? = null
)
