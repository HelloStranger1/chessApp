package com.hellostranger.chess_app.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.core.GameResult
import com.hellostranger.chess_app.core.PlayerInfo
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import com.hellostranger.chess_app.core.Player
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.utils.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Stack

private const val GameVMTAG = "GameViewModel"
@ExperimentalUnsignedTypes
class GameViewModel : ViewModel() {
//    val currentPlayerEmail = MyApp.tokenManager.getUserEmail()

    /* Websocket variables */
    private val _socketStatus = MutableLiveData(false)
    val socketStatus : LiveData<Boolean> = _socketStatus


    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult : LiveData<GameResult> = _gameResult

    var whitePlayerInfo : PlayerInfo? = null
    var blackPlayerInfo : PlayerInfo? = null

//    private val _userPlayerInfo = MutableLiveData<PlayerInfo>()
//    val userPlayerInfo : LiveData<PlayerInfo> = _userPlayerInfo
//
//    private val _opponentPlayerInfo = MutableLiveData<PlayerInfo>()
//    val opponentPlayerInfo : LiveData<PlayerInfo> = _opponentPlayerInfo

    private val _hasGameStarted = MutableLiveData(false)
    val hasGameStarted : LiveData<Boolean> = _hasGameStarted

    lateinit var whitePlayer : Player
    lateinit var blackPlayer : Player
    private val currentPlayer : Player
        get() = if (board.isWhiteToMove) whitePlayer else blackPlayer

    var board : Board = Board.createBoard()
    private var moveGenerator: MoveGenerator = MoveGenerator()


    private val movesToPlay = Stack<Move>()


/*
    private val _currentBoard = MutableLiveData(currentGame.board)
    val currentBoard : LiveData<Board> = _currentBoard
*/

/*
    private val _startMessageData = MutableLiveData<GameStartMessage>()
    val startMessageData : LiveData<GameStartMessage> = _startMessageData
*/

/*
    private val boardsHistory = ArrayList<Board>()
*/

    private val _drawOffer = MutableLiveData<DrawOfferMessage>()
    val drawOffer : LiveData<DrawOfferMessage> = _drawOffer


/*
    private var currentMoveShown = 0
*/
    private var isOurTurn = false
    var isWhite = false
    var gameOverDescription = ""


    fun setStatus(status : Boolean) = viewModelScope.launch(Dispatchers.Main) {
        _socketStatus.value = status
    }
    fun onMoveChosen(move: Move, player : Player) {
/*
        val playerColour = if(player == whitePlayer) Piece.WHITE else Piece.BLACK

        if (!Piece.isColour(board.square[move.startSquare], playerColour) || playerColour != board.moveColour) {
            Log.e(GameVMTAG, "ERROR! Not your colour or not your turn!")
            return
        }
*/
        if (!isOnLastMove()) {
            while (movesToPlay.isNotEmpty()) {
                board.makeMove(movesToPlay.pop())

            }
        }
        board.makeMove(move)
        currentPlayer.onOpponentMoveChosen()
    }


    fun isMoveLegal(move : Move) : Boolean {
        val possibleMoves = moveGenerator.generateMoves(board)
        return possibleMoves.indexOf(move) != -1
    }
/*
    fun validateMove(moveMessage: MoveMessage) : Boolean{
        val board = _currentBoard.value!!
        val startSquare = board.getSquareAt(moveMessage.startCol, moveMessage.startRow)!!
        val endSquare = board.getSquareAt(moveMessage.endCol, moveMessage.endRow)!!

        if(!isOurTurn || startSquare.piece == null || isWhite != (startSquare.piece!!.color == Color.WHITE)) {
            return false
        }
        if(!board.isValidMove(startSquare, endSquare)){
            return false
        }
        return true
    }

*/
    fun showPreviousBoard() {
        if (board.allGameMoves.isNotEmpty()) {
            movesToPlay.push(board.allGameMoves.last())
            board.unmakeMove(board.allGameMoves.last())
        }
}
/*
    fun showPreviousBoard() = viewModelScope.launch(Dispatchers.Main){
        if(currentMoveShown > 0){
            currentMoveShown--
            _currentBoard.value = boardsHistory[currentMoveShown]
        }
    }
*/

    fun showNextBoard() {
        if (movesToPlay.isNotEmpty()) {
            board.makeMove(movesToPlay.pop())
        }
    }
/*
    fun getNextBoard() : Board?{
        if(currentMoveShown < boardsHistory.size - 1){
            return boardsHistory[currentMoveShown + 1]
        }
        return null
    }
*/

/*
    fun showNextBoard() = viewModelScope.launch(Dispatchers.Main){
        if(currentMoveShown < boardsHistory.size - 1){
            currentMoveShown++
            _currentBoard.value = boardsHistory[currentMoveShown]
        }
    }
*/


    fun getLastMove() = board.allGameMoves.last()
    fun undoMove() {
        board.unmakeMove(board.allGameMoves.last())
    }


    fun startGame(startMessage: GameStartMessage) = viewModelScope.launch(Dispatchers.Main){
        isWhite = (startMessage.whiteEmail == MyApp.tokenManager.getUserEmail())
        isOurTurn = isWhite
        if (_gameResult.value == GameResult.NotStarted || _gameResult.value == GameResult.Waiting) {
            _gameResult.value = GameResult.InProgress
        }

        whitePlayerInfo = PlayerInfo(startMessage.whiteName, startMessage.whiteEmail, startMessage.whiteImage, startMessage.whiteElo)
        blackPlayerInfo = PlayerInfo(startMessage.blackName, startMessage.blackEmail, startMessage.blackImage, startMessage.blackElo)
        _hasGameStarted.value = true


    }
    fun updateDrawOffer(drawOfferMessage: DrawOfferMessage) = viewModelScope.launch(Dispatchers.Main) {
        _drawOffer.value = drawOfferMessage
    }

    fun onGameEnding(result : GameResult, whiteElo : Int, blackElo : Int, description : String) = viewModelScope.launch(Dispatchers.Main) {
        gameOverDescription = description
        if (_socketStatus.value == true) {
            _socketStatus.value = false
        }
        whitePlayerInfo?.elo ?: whiteElo
        blackPlayerInfo?.elo ?: blackElo
        _gameResult.value = result
    }

//    fun playMoveFromServer(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
//        if(boardsHistory.isEmpty()){
//            boardsHistory.add(_currentBoard.value!!)
//        }
//
//        val newBoard = boardsHistory.last().clone().movePiece(moveMessage)
//        if(newBoard != _currentBoard.value){
//            newBoard.previousMove = moveMessage
//            _currentBoard.value = newBoard
//        }
//
//        boardsHistory.add(_currentBoard.value!!)
//        currentMoveShown = boardsHistory.size - 1
//        isOurTurn = (moveMessage.playerEmail != currentPlayerEmail)
//        currentGame.isP1Turn = !currentGame.isP1Turn
//    }

//    fun temporaryPlayMove(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
//        if(boardsHistory.isEmpty()){
//            boardsHistory.add(_currentBoard.value!!)
//        }
//        if(validateMove(moveMessage)){
//            _currentBoard.value = boardsHistory.last().clone().movePiece(moveMessage)
//            _currentBoard.value!!.previousMove = moveMessage
//            currentGame.isP1Turn = !currentGame.isP1Turn
//        } else{
//            Log.e(GameVMTAG, "Temp-move is invalid.")
//        }
//
//    }
/*
    fun isCastlingMove(moveMessage: MoveMessage): Boolean {
        val startSquare = _currentBoard.value!!.getSquareAt(moveMessage.startCol, moveMessage.startRow)
        val endSquare   = _currentBoard.value!!.getSquareAt(moveMessage.endCol, moveMessage.endRow)
        if (startSquare == null || endSquare == null) {
            return false
        }
        return _currentBoard.value!!.isCastlingMove(startSquare, endSquare)
    }
*/
    fun goToLatestMove() {
        while (movesToPlay.isNotEmpty()) {
            board.makeMove(movesToPlay.pop())
        }

        currentPlayer.onOpponentMoveChosen()

    }
    fun isOnLastMove() = movesToPlay.isEmpty()

}