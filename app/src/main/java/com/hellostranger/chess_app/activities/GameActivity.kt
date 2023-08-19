package com.hellostranger.chess_app.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.hellostranger.chess_app.ChessDelegate
import com.hellostranger.chess_app.chess_models.Color
import com.hellostranger.chess_app.chess_models.Game
import com.hellostranger.chess_app.chess_models.GameState
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.chess_models.Piece
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding

import com.hellostranger.chess_app.retrofit.RetrofitClient
import com.hellostranger.chess_app.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.websocket.MoveListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.WebSocket


class GameActivity : BaseActivity(), ChessDelegate, MoveListener {

    private lateinit var binding : ActivityGameViewBinding

    private lateinit var chessWebSocket: WebSocket
    private lateinit var chessWebSocketListener :ChessWebSocketListener

    private lateinit var currentPlayerName : String

    private var isWhite : Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressDialog("Waiting for game to start...")


        chessWebSocketListener = ChessWebSocketListener(this)
        chessWebSocketListener.connectWebSocket(Game.getInstance()!!.id)

        chessWebSocket = chessWebSocketListener.getWebSocketInstance()

        binding.chessView.chessDelegate = this

    }



    override fun pieceAt(col : Int, row : Int): Piece? {
        val currentGame : Game = Game.getInstance()!!
        return currentGame.board.squaresArray[row][col].piece
    }

    override fun playMove(moveMessage: MoveMessage) {
        val currentGame = Game.getInstance()!!
        if(moveMessage.playerName.isEmpty()){
            if(isWhite != (currentGame.board.squaresArray
                        [moveMessage.startRow]
                        [moveMessage.startCol]
                        .piece!!.color == Color.WHITE)){
                return;
            }
            if(currentGame.isP1turn != isWhite){
                return;
            }

            moveMessage.playerName = currentPlayerName
            temporaryMakeMove(moveMessage)
            sendMessageToServer(moveMessage)
            Log.e("TAG", "sendingMove. move is: $moveMessage")
            binding.chessView.invalidate()
        }else{
            currentGame.board.movePiece(moveMessage)
            Log.e("TAG", "received move from socket. move is: $moveMessage")
            binding.chessView.invalidate()
            if(isWhite){
                Game.getInstance()!!.isP1turn = moveMessage.playerName != currentPlayerName
            }else{
                Game.getInstance()!!.isP1turn = moveMessage.playerName == currentPlayerName
            }
        }



    }

    private fun sendMessageToServer(moveMessage: MoveMessage) {
        val gson = Gson()
        val moveJson = gson.toJson(moveMessage)
        if (::chessWebSocket.isInitialized && chessWebSocketListener.isWebSocketOpen()) {
            chessWebSocket.send(moveJson)
        }

    }

    private fun temporaryMakeMove(moveMessage: MoveMessage){
        val currentGame = Game.getInstance()!!
        currentGame.prev_board = currentGame.board.clone()
        if(isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 0
        ){
            if(moveMessage.endCol == 7 && moveMessage.endRow == 0){
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.startCol, 0, moveMessage.startCol+2, 0))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.endCol, 0, moveMessage.startCol+1, 0))
            }else{
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.startCol, 0, moveMessage.startCol-2, 0))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.endCol, 0, moveMessage.startCol-1, 0))
            }
        }else if(!isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 7
        ){
            if(moveMessage.endCol == 7 && moveMessage.endRow == 7){
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.startCol, 7, moveMessage.startCol+2, 7))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.endCol, 0, moveMessage.startCol+1, 7))
            }else{
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.startCol, 7, moveMessage.startCol-2, 7))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerName, moveMessage.endCol, 7, moveMessage.startCol-1, 7))
            }
        }else{
            currentGame.board.movePiece(moveMessage)
        }
        Log.e("TAG", "Copied board to prev_board and played the move")
    }

    // Close the WebSocket connection when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Disconnect WebSocket when the activity is destroyed
        chessWebSocketListener.disconnectWebSocket()
    }

    override fun onMoveReceived(moveMessage: MoveMessage) {
        playMove(moveMessage)
    }

    private fun updatePlayerOne(name: String, uid: String) {
        var fullName = "$name (White)"
        if(isWhite){
            Log.e("TAG", "fullname before: $fullName")
            fullName = "$fullName (You)"
            Log.e("TAG", "fullname after: $fullName")

            currentPlayerName = name
        }
        binding.tvP1Name.text = fullName




        //TODO: use the uid to update the profile pic
    }

    private fun updatePlayerTwo(name: String, uid: String) {
        var fullName = "$name (Black)"
        if(!isWhite){
            fullName = "$name (You)"
            currentPlayerName = name
        }
        binding.tvP2Name.text = fullName



        //TODO: use the uid to update the profile pic
    }

    override fun startGame(whiteName : String, blackName : String, whiteUid: String, blackUid : String) {
        hideProgressDialog()
        checkIfPlayingWhite(whiteUid)
        val playerColor : Color = if(isWhite){
            Color.WHITE
        }else{
            Color.BLACK
        }
        runOnUiThread{
            updatePlayerOne(whiteName, whiteUid)
            updatePlayerTwo(blackName, blackUid)
            Toast.makeText(this@GameActivity, "Game started! You are playing $playerColor", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onGameEnding(result: GameState) {
        runOnUiThread {
            Toast.makeText(this@GameActivity, "Game ended. result is: $result", Toast.LENGTH_LONG).show()
        }
        Log.e("TAG", "Game ended. result is: $result")
    }

    override fun undoLastMove() {
        val currentGame = Game.getInstance()!!
        Log.e("TAG", "Undid move. prev_board was: ${currentGame.prev_board} \n and the invalid change was to: ${currentGame.board}")
        currentGame.board = currentGame.prev_board!!

        binding.chessView.invalidate()
    }

    private fun checkIfPlayingWhite(whiteUid: String) {
        isWhite = whiteUid == getCurrentUserID()
    }


}