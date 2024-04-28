package com.hellostranger.chess_app.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.GameState
import com.hellostranger.chess_app.utils.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val GameVMTAG = "GameViewModel"
class GameViewModel(private var currentGame : Game) : ViewModel() {

    val currentPlayerEmail = MyApp.tokenManager.getUserEmail()

    private val _socketStatus = MutableLiveData(false)
    val socketStatus : LiveData<Boolean> = _socketStatus

    private val _gameStatus = MutableLiveData<GameState>()
    val gameStatus : LiveData<GameState> = _gameStatus

    private val _currentBoard = MutableLiveData(currentGame.board)
    val currentBoard : LiveData<Board> = _currentBoard

    private val _startMessageData = MutableLiveData<GameStartMessage>()
    val startMessageData : LiveData<GameStartMessage> = _startMessageData

    private val boardsHistory = ArrayList<Board>()

    private val _drawOffer = MutableLiveData<DrawOfferMessage>()
    val drawOffer : LiveData<DrawOfferMessage> = _drawOffer

    private var currentMoveShown = 0
    private var isOurTurn = false
    var ourElo = -1;
    var isWhite = false
    var gameOverDescription = ""

    fun setStatus(status : Boolean) = viewModelScope.launch(Dispatchers.Main) {
        _socketStatus.value = status
    }

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

    fun showPreviousBoard() = viewModelScope.launch(Dispatchers.Main){
        if(currentMoveShown > 0){
            currentMoveShown--
            _currentBoard.value = boardsHistory[currentMoveShown]
        }
    }

    fun getNextBoard() : Board?{
        if(currentMoveShown < boardsHistory.size - 1){
            return boardsHistory[currentMoveShown + 1]
        }
        return null
    }

    fun showNextBoard() = viewModelScope.launch(Dispatchers.Main){
        if(currentMoveShown < boardsHistory.size - 1){
            currentMoveShown++
            _currentBoard.value = boardsHistory[currentMoveShown]
        }
    }

    fun undoMove() = viewModelScope.launch(Dispatchers.Main){
        _currentBoard.value = boardsHistory.last()
    }



    fun startGame(startMessage: GameStartMessage) = viewModelScope.launch(Dispatchers.Main){

        isWhite = (startMessage.whiteEmail == currentPlayerEmail)
        isOurTurn = isWhite
        if(currentGame.gameState == GameState.WAITING){
            _gameStatus.value = GameState.ACTIVE
        }else{
            _gameStatus.value = currentGame.gameState
        }
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        ourElo = if(isWhite) startMessage.whiteElo else startMessage.blackElo
        _startMessageData.value = startMessage

    }
    fun updateDrawOffer(drawOfferMessage: DrawOfferMessage) {
        _drawOffer.value = drawOfferMessage
    }

    fun onGameEnding(result : GameState, whiteElo : Int, blackElo : Int, description : String) = viewModelScope.launch(Dispatchers.Main){
        gameOverDescription = description
        _gameStatus.value = result
        ourElo = if(isWhite) whiteElo else blackElo
        _socketStatus.value = false

    }
    fun playMoveFromServer(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }

        val newBoard = boardsHistory.last().clone().movePiece(moveMessage)
        if(newBoard != _currentBoard.value){
            newBoard.previousMove = moveMessage
            _currentBoard.value = newBoard
        }

        boardsHistory.add(_currentBoard.value!!)
        currentMoveShown = boardsHistory.size - 1
        isOurTurn = (moveMessage.playerEmail != currentPlayerEmail)
        currentGame.isP1Turn = !currentGame.isP1Turn
    }

    fun temporaryPlayMove(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        if(validateMove(moveMessage)){
            _currentBoard.value = boardsHistory.last().clone().movePiece(moveMessage)
            _currentBoard.value!!.previousMove = moveMessage
            currentGame.isP1Turn = !currentGame.isP1Turn
        } else{
            Log.e(GameVMTAG, "Temp-move is invalid.")
        }

    }
    fun isCastlingMove(moveMessage: MoveMessage): Boolean {
        val startSquare = _currentBoard.value!!.getSquareAt(moveMessage.startCol, moveMessage.startRow)
        val endSquare   = _currentBoard.value!!.getSquareAt(moveMessage.endCol, moveMessage.endRow)
        if (startSquare == null || endSquare == null) {
            return false
        }
        return _currentBoard.value!!.isCastlingMove(startSquare, endSquare)
    }
    fun goToLatestMove() = viewModelScope.launch(Dispatchers.Main){
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        _currentBoard.value = boardsHistory.last()
        currentMoveShown = boardsHistory.size - 1
    }
    fun isOnLastMove() : Boolean{
        return currentMoveShown == boardsHistory.size -1
    }

}