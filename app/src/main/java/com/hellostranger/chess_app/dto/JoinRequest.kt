package com.hellostranger.chess_app.dto

import com.hellostranger.chess_app.chess_models.Player

data class JoinRequest(
    val gameId : String,
    val player: Player
)
