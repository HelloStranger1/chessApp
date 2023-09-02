package com.hellostranger.chess_app.dto.websocket

class InvalidMoveMessage(val playerEmail : String) : WebSocketMessage(MessageType.INVALID_MOVE) {

}