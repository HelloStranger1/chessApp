package com.hellostranger.chess_app.network.websocket

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.viewModels.GameViewModel
import com.hellostranger.chess_app.dto.websocket.GameEndMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.enums.WebsocketMessageType
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "ChessWebSocketListener"
class ChessWebSocketListener(private val viewModel: GameViewModel) : WebSocketListener() {
    private lateinit var webSocket: WebSocket


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
        Log.e(TAG, "onMessage, text is: $text. msgType is: ${message.websocketMessageType}")

        when( message.websocketMessageType){
            WebsocketMessageType.MOVE ->{
                val moveMessage : MoveMessage = gson.fromJson(text, MoveMessage::class.java)
                viewModel.playMoveFromServer(moveMessage)
            }
            WebsocketMessageType.START ->{
                val startMessage : GameStartMessage = gson.fromJson(text, GameStartMessage::class.java)
                Log.e(TAG, "Start Message. text: $text. startMessage: $startMessage")
                viewModel.startGame(startMessage)
            }
            WebsocketMessageType.END ->{
                val endMessage : GameEndMessage = gson.fromJson(text, GameEndMessage::class.java)
                viewModel.onGameEnding(endMessage.state)
            }
            WebsocketMessageType.INVALID_MOVE -> {
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
        viewModel.setStatus(false)
        Log.e(TAG, "Websocket failed. msg: $response and throwable: $t")
        Log.e(TAG, t.message ?: "Unknown error")
    }
}
