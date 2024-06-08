package com.hellostranger.chess_app.dto.websocket

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class GameStartMessage (
    @SerializedName("whitePlayerName")
    val whiteName : String,

    @SerializedName("blackPlayerName")
    val blackName : String,

    @SerializedName("whitePlayerEmail")
    val whiteEmail : String,

    @SerializedName("blackPlayerEmail")
    val blackEmail : String,

    @SerializedName("whitePlayerImage")
    val whiteImage : String,

    @SerializedName("blackPlayerImage")
    val blackImage : String,

    @SerializedName("whiteElo")
    val whiteElo : Int,

    @SerializedName("blackElo")
    val blackElo : Int,

    ) : WebSocketMessage(WebsocketMessageType.START), Parcelable{
    override fun toString(): String {
        return "Game Start. white name: $whiteName. black name: $blackName. white email: $whiteEmail. black email: $blackEmail. whiteImage: $whiteImage. blackImage: $blackImage"
    }

}