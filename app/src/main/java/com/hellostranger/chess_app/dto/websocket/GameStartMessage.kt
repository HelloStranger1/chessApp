package com.hellostranger.chess_app.dto.websocket

import com.google.gson.annotations.SerializedName


class GameStartMessage (
    @SerializedName("whitePlayerName")
    val whiteName : String,

    @SerializedName("blackPlayerName")
    val blackName : String,

    @SerializedName("whitePlayerUid")
    val whiteUid : String,

    @SerializedName("blackPlayerUid")
    val blackUid : String,
    ) : WebSocketMessage(MessageType.START){
    override fun toString(): String {
        return "Game Start. white name: $whiteName. black name: $blackName. white uid: $whiteUid. blackUid: $blackUid"
    }

}