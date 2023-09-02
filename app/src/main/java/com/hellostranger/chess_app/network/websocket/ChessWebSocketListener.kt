package com.hellostranger.chess_app.network.websocket

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.GameViewModel
import com.hellostranger.chess_app.models.gameModels.enums.GameState
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

class ChessWebSocketListener(private val viewModel: GameViewModel) : WebSocketListener() {
    private lateinit var webSocket: WebSocket
    private val TAG = "ChessWebSocketListener"

    fun connectWebSocket(path : String, token : String) {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/chess/$path")
            .addHeader("Authorization", "Bearer $token")
            .build()
        Log.e(TAG, "connectToWebSocket")

        webSocket = client.newWebSocket(request, this)
    }

    fun disconnectWebSocket() {
        webSocket.cancel()
        viewModel.setStatus(false)

    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        viewModel.setStatus(true)
        Log.e("TAG", "Open, $response")

    }

    fun getWebSocketInstance(): WebSocket {
        return webSocket
    }

    fun isWebSocketOpen(): Boolean {
        return viewModel.socketStatus.value!!
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val gson = Gson()
        val message : WebSocketMessage = gson.fromJson(text, WebSocketMessage::class.java)
        Log.e(TAG, "onMessage, text is: $text. msgType is: ${message.messageType}")

        when( message.messageType){
            MessageType.MOVE ->{
                val moveMessage : MoveMessage = gson.fromJson(text, MoveMessage::class.java)
                viewModel.playMoveFromServer(moveMessage)
            }
            MessageType.START ->{
                val startMessage : GameStartMessage = gson.fromJson(text, GameStartMessage::class.java)
                Log.e(TAG, "Start Message. text: $text. startMessage: $startMessage")
                viewModel.startGame(startMessage)
            }
            MessageType.END ->{
                val endMessage : GameEndMessage = gson.fromJson(text, GameEndMessage::class.java)
                viewModel.onGameEnding(endMessage.state)
            }
            MessageType.INVALID_MOVE -> {
                Log.e(TAG, "Invalid Move, undoing it")
                viewModel.undoMove()
            }

            else -> {
                Log.e(TAG, "I have no idea what happened")
            }
        }

    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        viewModel.setStatus(false)
        Log.e(TAG, "CLOSED")

    }


    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, t.message ?: "Unknown error")
    }
}

interface MoveListener {
    fun onMoveReceived(moveMessage: MoveMessage)

    fun startGame(startMessage: GameStartMessage)

    fun onGameEnding(result : GameState)

    fun undoLastMove()

}