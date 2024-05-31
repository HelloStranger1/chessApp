package com.hellostranger.chess_app.network.websocket

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.players.Player
import com.hellostranger.chess_app.viewModels.GameViewModel
import com.hellostranger.chess_app.dto.websocket.GameEndMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.enums.WebsocketMessageType
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "ChessWebSocketListener"
@ExperimentalUnsignedTypes
class ChessWebSocketListener(private val viewModel: GameViewModel, private val currentUserEmail : String) : WebSocketListener(),
    Player {
    private lateinit var webSocket: WebSocket


    val gson = Gson()
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
        val message : WebSocketMessage = gson.fromJson(text, WebSocketMessage::class.java)
        Log.i(TAG, "onMessage, text is: $text. msgType is: ${message.websocketMessageType}")

        when( message.websocketMessageType){
            WebsocketMessageType.MOVE ->{
                val moveMessage : MoveMessage = gson.fromJson(text, MoveMessage::class.java)
                Log.i(TAG, "Move message. move value: ${Move(moveMessage.move.toUShort())}. Move name: ${MoveUtility.getMoveNameUCI(Move(moveMessage.move.toUShort()))}")
                viewModel.onMoveChosen(Move(moveMessage.move.toUShort()))
            }
            WebsocketMessageType.START ->{
                val startMessage : GameStartMessage = gson.fromJson(text, GameStartMessage::class.java)
                Log.i(TAG, "Start Message. text: $text. startMessage: $startMessage")
                viewModel.startGame(startMessage)
            }
            WebsocketMessageType.END ->{
                val endMessage : GameEndMessage = gson.fromJson(text, GameEndMessage::class.java)
                Log.i(TAG, "End Message. state: ${endMessage.state}, message: ${endMessage.message}")
                viewModel.onGameEnding(endMessage.state, endMessage.whiteElo, endMessage.blackElo, endMessage.message)
            }
            WebsocketMessageType.INVALID_MOVE -> {
                Log.e(TAG, "\n")
                Log.e(TAG, "Invalid Move, undoing it")
                Log.e(TAG, "\n")
            }
            WebsocketMessageType.DRAW_OFFER -> {
                viewModel.updateDrawOffer(gson.fromJson(text, DrawOfferMessage::class.java))
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

    override fun onOpponentMoveChosen() {
        val move : Move = viewModel.getLastMove()
        val moveMsg = MoveMessage(currentUserEmail, move.moveValue.toInt())
        val moveJson = gson.toJson(moveMsg)
        Log.e(TAG, "Sending our move to the server. Move uce name: ${MoveUtility.getMoveNameUCI(move)}, move json is: $moveJson")
        webSocket.send(moveJson)

    }
}
