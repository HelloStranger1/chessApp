package com.hellostranger.chess_app.gameHelpers

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import com.hellostranger.chess_app.models.gameModels.Game
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState
import com.hellostranger.chess_app.models.gameModels.enums.PieceType
import com.hellostranger.chess_app.network.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.network.websocket.MoveListener
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import okhttp3.WebSocket

class OnlineChessGame(
    currentGame : Game,
    isWhite : Boolean,
    currentPlayerEmail: String,
    val chessView: ChessView
){ /*: BaseChessGame(currentGame, isWhite, currentPlayerEmail), MoveListener {

    private var tokenManager : TokenManager = MyApp.tokenManager
    private var chessWebSocket : WebSocket
    private var chessWebSocketListener: ChessWebSocketListener = ChessWebSocketListener(this)
    init {
        chessWebSocketListener.connectWebSocket(Game.getInstance()!!.id, tokenManager.getAccessToken())
        chessWebSocket = chessWebSocketListener.getWebSocketInstance()
    }
    private val TAG = "OnlineChessGameClass"
    override fun validateMove(moveMessage: MoveMessage) : Boolean {
        if(
            currentGame.getCurrentBoard()!!.squaresArray[moveMessage.startRow][moveMessage.startCol].piece == null
            || isWhite != (currentGame.getCurrentBoard()!!.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.color == Color.WHITE)
            || currentGame.isP1turn != isWhite
        ){
            return false
        }
        return true;
    }

    fun sendMove(moveMessage: MoveMessage, isFlipped: Boolean) {
        var updatedMoveMessage: MoveMessage = moveMessage
        if(isFlipped){
            updatedMoveMessage = MoveMessage(
                moveMessage.playerEmail,
                7 - moveMessage.startCol,
                7 - moveMessage.startRow,
                7 - moveMessage.endCol,
                7 - moveMessage.endRow
            )
            Log.e(TAG, "PlayMove, Flipped. flipped move msg is: $updatedMoveMessage")
        }

        if(!validateMove(updatedMoveMessage)) return

        temporaryMakeMove(updatedMoveMessage)
        sendMessageToServer(updatedMoveMessage)
        chessView.invalidate()
    }

    override fun onMoveReceived(moveMessage: MoveMessage) {
        super.playMove(moveMessage)
        chessView.invalidate()
    }

    override fun startGame(startMessage: GameStartMessage) {
        TODO("Not yet implemented")
    }

    override fun onGameEnding(result: GameState) {
        TODO("Not yet implemented")
    }

    override fun undoLastMove() {
        TODO("Not yet implemented")
    }

    private fun sendMessageToServer(message: WebSocketMessage) {
        val gson = Gson()
        val messageJson = gson.toJson(message)
        if (chessWebSocketListener.isWebSocketOpen()) {
            chessWebSocket.send(messageJson)
        }

    }

    fun temporaryMakeMove(moveMessage: MoveMessage){
        if(!currentGame.isCurrentMoveLast()){
            //we arent on the latest move
            Log.e(TAG, "(temporaryMakeMove) We arent on the last move. current move show: ${currentGame.currentMove} and size is: ${currentGame.boards_history!!.size - 1}")
            currentGame.goToLatestMove()
        }
        currentGame.temporaryBoard = currentGame.getCurrentBoard()!!.clone()
        currentGame.currentMove = - 1
        val currentBoard = currentGame.getCurrentBoard()!!
        if(isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 0 &&
            currentGame.getCurrentBoard()!!.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.pieceType == PieceType.KING
        ){
            //White might be trying to castle. Checking for that.
            if(moveMessage.endCol == 7 && moveMessage.endRow == 0){ //Checking for short castle (O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol+2, 0))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 0))
            }else if(moveMessage.endCol == 0 && moveMessage.endRow == 0){ //Checking for long castle (O-O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol-2, 0))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol-1, 0))
            } else{
                currentBoard.movePiece(moveMessage)
            }
        }else if(!isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 7 &&
            currentBoard.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.pieceType == PieceType.KING
        ){
            //Black might be trying to castle. Checking for that.
            if(moveMessage.endCol == 7 && moveMessage.endRow == 7){ //Checking for short castle (O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol+2, 7))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 7))
            }else if(moveMessage.endCol == 0 && moveMessage.endRow == 7){ //Checking for long castle (O-O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol-2, 7))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 7, moveMessage.startCol-1, 7))
            } else{
                currentBoard.movePiece(moveMessage)
            }
        }else{
            currentBoard.movePiece(moveMessage)
        }
        Log.e(TAG, "Copied board to prev_board and played the move")

    }*/
}