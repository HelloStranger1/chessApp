package com.hellostranger.chess_app.dto.websocket

import com.google.gson.annotations.SerializedName
import com.hellostranger.chess_app.dto.enums.WebsocketMessageType

open class WebSocketMessage(
    @SerializedName("messageType")
    var websocketMessageType : WebsocketMessageType
)