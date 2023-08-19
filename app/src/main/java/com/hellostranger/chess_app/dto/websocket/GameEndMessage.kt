package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.chess_models.GameState

class GameEndMessage(
    val state : GameState
) : WebSocketMessage(MessageType.END) {
    override fun toString(): String {
        return "GameEnd. result: $state"
    }
}
