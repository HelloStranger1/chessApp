package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.dto.enums.WebsocketMessageType
import com.hellostranger.chess_app.gameClasses.enums.GameState

class GameEndMessage(
    val message : String,
    val state : GameState
) : WebSocketMessage(WebsocketMessageType.END) {
    override fun toString(): String {
        return "GameEnd. result: $state msg: $message"
    }
}
