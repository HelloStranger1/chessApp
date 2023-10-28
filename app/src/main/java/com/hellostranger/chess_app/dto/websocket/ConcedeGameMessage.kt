package com.hellostranger.chess_app.dto.websocket

class ConcedeGameMessage(val playerEmail : String) : WebSocketMessage(MessageType.CONCEDE)
