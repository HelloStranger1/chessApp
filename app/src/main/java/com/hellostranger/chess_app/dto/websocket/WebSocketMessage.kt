package com.hellostranger.chess_app.dto.websocket

import com.google.gson.annotations.SerializedName

open class WebSocketMessage(
    @SerializedName("messageType")
    var messageType : MessageType
)