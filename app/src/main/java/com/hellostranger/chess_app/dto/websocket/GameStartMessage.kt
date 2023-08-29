package com.hellostranger.chess_app.dto.websocket

import com.google.gson.annotations.SerializedName


class GameStartMessage (
    @SerializedName("whitePlayerName")
    val whiteName : String,

    @SerializedName("blackPlayerName")
    val blackName : String,

    @SerializedName("whitePlayerEmail")
    val whiteEmail : String,

    @SerializedName("blackPlayerEmail")
    val blackEmail : String,

    @SerializedName("whiteElo")
    val whiteElo : Int,

    @SerializedName("blackElo")
    val blackElo : Int,

    ) : WebSocketMessage(MessageType.START){
    override fun toString(): String {
        return "Game Start. white name: $whiteName. black name: $blackName. white email: $whiteEmail. black email: $blackEmail"
    }

}