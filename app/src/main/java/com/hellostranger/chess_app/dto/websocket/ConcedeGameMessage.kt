package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.dto.enums.WebsocketMessageType

class ConcedeGameMessage(val playerEmail : String) : WebSocketMessage(WebsocketMessageType.CONCEDE)
