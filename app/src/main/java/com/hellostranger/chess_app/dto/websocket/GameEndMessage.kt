package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.core.board.GameResult
import com.hellostranger.chess_app.dto.enums.WebsocketMessageType

class GameEndMessage(
    val message : String,
    val state : GameResult,
    val whiteElo : Int,
    val blackElo : Int
) : WebSocketMessage(WebsocketMessageType.END) {
    override fun toString(): String {
        return "GameEnd. result: $state msg: $message whiteElo: $whiteElo blackElo: $blackElo"
    }
}
