package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.models.gameModels.enums.GameState

class GameEndMessage(
    val message : String,
    val state : GameState
) : WebSocketMessage(MessageType.END) {
    override fun toString(): String {
        return "GameEnd. result: $state msg: $message"
    }
}
