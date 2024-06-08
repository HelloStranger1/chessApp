package com.hellostranger.chess_app.dto.websocket

class ConcedeGameMessage(val playerEmail : String) : WebSocketMessage(WebsocketMessageType.CONCEDE)
