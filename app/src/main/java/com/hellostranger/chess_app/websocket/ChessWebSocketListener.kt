package com.hellostranger.chess_app.websocket

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.chess_models.GameState
import com.hellostranger.chess_app.dto.websocket.GameEndMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MessageType
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChessWebSocketListener(private val moveListener: MoveListener) : WebSocketListener() {
    private lateinit var webSocket: WebSocket


    fun connectWebSocket(path : String, token : String) {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/chess/$path")
            .addHeader("Authorization", "Bearer $token")
            .build()
        webSocket = client.newWebSocket(request, this)
        Log.e("TAG", "connectToWebSocket")
    }

    fun disconnectWebSocket() {
        webSocket.cancel()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.e("TAG", "Open, $response")
    }

    fun getWebSocketInstance(): WebSocket {
        return webSocket
    }

    fun isWebSocketOpen(): Boolean {
        return true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val gson = Gson()
        val message : WebSocketMessage = gson.fromJson(text, WebSocketMessage::class.java)
        Log.e("TAG", "onMessage, text is: $text. msgType is: ${message.messageType}")

        when( message.messageType){
            MessageType.MOVE ->{
                val moveMessage : MoveMessage = gson.fromJson(text, MoveMessage::class.java)
                moveListener.onMoveReceived(moveMessage)
            }
            MessageType.START ->{
                val startMessage : GameStartMessage = gson.fromJson(text, GameStartMessage::class.java)
                Log.e("TAG", "Start Message. text: $text. startMessage: $startMessage")
                moveListener.startGame(startMessage.whiteName,
                    startMessage.blackName,
                    startMessage.whiteEmail,
                    startMessage.blackEmail,
                    startMessage.whiteElo,
                    startMessage.blackElo)
            }
            MessageType.END ->{
                val endMessage : GameEndMessage = gson.fromJson(text, GameEndMessage::class.java)

                moveListener.onGameEnding(endMessage.state)
            }
            MessageType.INVALID_MOVE -> {
                Log.e("TAG", "Invalid Move, undoing it")
                moveListener.undoLastMove()
            }

        }

    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        // WebSocket connection is closed
        // You can perform any actions needed when the connection is closed
        Log.e("TAG", "CLOSED")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("TAG", t.message ?: "Unknown error")
    }
}

interface MoveListener {
    fun onMoveReceived(moveMessage: MoveMessage)

    fun startGame(whiteName : String, blackName : String, whiteEmail: String, blackEmail : String, whiteElo : Int, blackElo : Int)

    fun onGameEnding(result : GameState)

    fun undoLastMove()

}