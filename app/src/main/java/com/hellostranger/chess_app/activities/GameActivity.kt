package com.hellostranger.chess_app.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.hellostranger.chess_app.ChessDelegate
import com.hellostranger.chess_app.MyApp
import com.hellostranger.chess_app.TokenManager
import com.hellostranger.chess_app.chess_models.Color
import com.hellostranger.chess_app.chess_models.Game
import com.hellostranger.chess_app.chess_models.GameState
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.chess_models.Piece
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding
import com.hellostranger.chess_app.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.websocket.MoveListener
import okhttp3.WebSocket


class GameActivity : BaseActivity(), ChessDelegate, MoveListener {

    private lateinit var binding : ActivityGameViewBinding

    private lateinit var chessWebSocket: WebSocket
    private lateinit var chessWebSocketListener :ChessWebSocketListener

    private lateinit var currentPlayerEmail : String

    private var tokenManager : TokenManager = MyApp.tokenManager

    private var currentMoveDisplayed : Int = 0

    private var isWhite : Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressDialog("Waiting for game to start...")


        chessWebSocketListener = ChessWebSocketListener(this)
        chessWebSocketListener.connectWebSocket(Game.getInstance()!!.id, tokenManager.getAccessToken())

        chessWebSocket = chessWebSocketListener.getWebSocketInstance()

        binding.chessView.chessDelegate = this

        binding.ibArrowBack.setOnClickListener{
            if(currentMoveDisplayed > 0){
                Game.getInstance()!!.board = Game.getInstance()!!.boards_history!![currentMoveDisplayed-1]
                currentMoveDisplayed--
                binding.chessView.invalidate()
            }
        }
        binding.ibArrowForward.setOnClickListener{
            if(currentMoveDisplayed < Game.getInstance()!!.boards_history!!.size){
                Game.getInstance()!!.board = Game.getInstance()!!.boards_history!![currentMoveDisplayed+1]
                currentMoveDisplayed++
                binding.chessView.invalidate()
            }
        }

    }



    override fun pieceAt(col : Int, row : Int): Piece? {
        val currentGame : Game = Game.getInstance()!!
        return currentGame.board.squaresArray[row][col].piece
    }

    override fun playMove(moveMessage: MoveMessage) {
        val currentGame = Game.getInstance()!!
        Log.e("TAG", "PlayMove. move msg is: $moveMessage")
        if(moveMessage.playerEmail.isEmpty()){
            if(currentGame.board.squaresArray[moveMessage.startRow][moveMessage.startCol].piece == null){
                return
            }
            if(isWhite != (currentGame.board.squaresArray
                        [moveMessage.startRow]
                        [moveMessage.startCol]
                        .piece!!.color == Color.WHITE)){
                return
            }
            if(currentGame.isP1turn != isWhite){
                return
            }

            moveMessage.playerEmail = currentPlayerEmail
            temporaryMakeMove(moveMessage)
            sendMessageToServer(moveMessage)
            Log.e("TAG", "sendingMove. move is: $moveMessage")
            binding.chessView.invalidate()
        }else{
            if(currentMoveDisplayed != currentGame.boards_history!!.size){
                //we arent on the latest move
                currentGame.board = currentGame.boards_history!!.last()
            }
            currentGame.boards_history!!.add(currentGame.board.clone())
            currentGame.board.movePiece(moveMessage)

            Log.e("TAG", "received move from socket. move is: $moveMessage")
            binding.chessView.invalidate()
            currentMoveDisplayed = currentGame.board.halfMoveCount
            if(isWhite){
                Game.getInstance()!!.isP1turn = moveMessage.playerEmail != currentPlayerEmail
            }else{
                Game.getInstance()!!.isP1turn = moveMessage.playerEmail == currentPlayerEmail
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
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol+2, 0))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 0))
            }else{
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol-2, 0))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol-1, 0))
            }
        }else if(!isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 7
        ){
            if(moveMessage.endCol == 7 && moveMessage.endRow == 7){
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol+2, 7))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 7))
            }else{
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol-2, 7))
                currentGame.board.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 7, moveMessage.startCol-1, 7))
            }
        }else{
            currentGame.board.movePiece(moveMessage)
        }
        currentGame.boards_history!!.add(currentGame.board)
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

    private fun updatePlayerOne(name: String, email: String, elo : Int) {
        val fullName = "$name (White) ($elo)"
        if(isWhite){
            currentPlayerEmail = email
        }
        binding.tvP1Name.text = fullName



    }

    private fun updatePlayerTwo(name: String, email: String, elo : Int) {
        var fullName = "$name (Black) ($elo)"
        if(!isWhite){
            fullName = "$fullName (You)"
            currentPlayerEmail = email
        }
        binding.tvP2Name.text = fullName




    }

    override fun startGame(whiteName : String, blackName : String, whiteEmail: String, blackEmail : String, whiteElo : Int, blackElo : Int) {
        hideProgressDialog()
        checkIfPlayingWhite(whiteEmail)
        val playerColor : Color = if(isWhite){
            Color.WHITE
        }else{
            Color.BLACK
        }
        runOnUiThread{
            updatePlayerOne(whiteName, whiteEmail, whiteElo)
            updatePlayerTwo(blackName, blackEmail, blackElo)
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
        currentGame.boards_history!!.removeAt(currentGame.board.halfMoveCount)
        currentGame.board = currentGame.prev_board!!
        currentMoveDisplayed = currentGame.board.halfMoveCount
        binding.chessView.invalidate()
    }

    private fun checkIfPlayingWhite(whiteEmail: String) {
        isWhite = whiteEmail == tokenManager.getUserEmail()
    }


}