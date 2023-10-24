package com.hellostranger.chess_app

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Game
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState
import com.hellostranger.chess_app.models.gameModels.enums.MoveType
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameViewModel(private val currentGame : Game) : ViewModel() {
    private val TAG = "GameViewModel"

    private val tokenManager = MyApp.tokenManager

    private val _socketStatus = MutableLiveData(false)
    val socketStatus : LiveData<Boolean> = _socketStatus

    private val _gameStatus = MutableLiveData<GameState>()
    val gameStatus : LiveData<GameState> = _gameStatus

    private val _currentBoard = MutableLiveData(currentGame.board)
    val currentBoard : LiveData<Board> = _currentBoard

    private val _startMessageData = MutableLiveData<GameStartMessage>()
    val startMessageData : LiveData<GameStartMessage> = _startMessageData
    private var isOurTurn = false
    var isWhite = false
    private val boardsHistory = ArrayList<Board>()
    val currentPlayerEmail = tokenManager.getUserEmail()

    private var currentMoveShown = 0

    fun setStatus(status : Boolean) = viewModelScope.launch(Dispatchers.Main) {
        _socketStatus.value = status
    }

    private fun validateMove(moveMessage: MoveMessage) : Boolean{
        val board = _currentBoard.value!!
        Log.e(TAG, "moveMessage is: $moveMessage")
        if(!isOurTurn) {
            Log.e(TAG, "Move is invalid because it is not our turn")
            return false;
        }
        val startSquare = board.getSquareAt(moveMessage.startCol, moveMessage.startRow)!!
        if(startSquare.piece == null){
            Log.e(TAG, "Moving piece is null so the move is invalid")
            return false
        }
        if(isWhite !=( startSquare.piece!!.color == Color.WHITE)){
            Log.e(TAG, "Moving piece is not our color so the move is invalid. are we white? $isWhite. and the piece colro is: ${startSquare.piece!!.color}")
            return false
        }
        val endSquare = board.getSquareAt(moveMessage.endCol, moveMessage.endRow)
        if(!board.isValidMove(startSquare, endSquare!!)){
            Log.e(TAG, "Move is invalid")
            return false
        }
        Log.e(TAG, "Move is valid. it is our turn: $isOurTurn the moving piece isn't null: " +
                "${board.squaresArray[moveMessage.startRow][moveMessage.startCol].piece} and our color (are we white? $isWhite) matches the piece color: " +
                "${board.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.color}")
        return true
    }

    fun addBoardToList(board : Board){
        boardsHistory.add(board)
        if(_currentBoard.value == null){
            viewModelScope.launch(Dispatchers.Main){
                _currentBoard.value = board
            }
        }
    }

    fun showPreviousBoard() = viewModelScope.launch(Dispatchers.Main){
        if(currentMoveShown > 0){
            currentMoveShown--
            _currentBoard.value = boardsHistory[currentMoveShown]
        }
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

        isWhite = startMessage.whiteEmail == tokenManager.getUserEmail()
        Log.e(TAG, "StartGame. isWhite: $isWhite. the whiteEmail is: ${startMessage.whiteEmail} and our email is: ${tokenManager.getUserEmail()}")
        isOurTurn = isWhite
        if(currentGame.gameState == GameState.WAITING){
            _gameStatus.value = GameState.ACTIVE
        }else{
            _gameStatus.value = currentGame.gameState
        }
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        _startMessageData.value = startMessage

    }

    fun onGameEnding(result : GameState) = viewModelScope.launch(Dispatchers.Main){
        _gameStatus.value = result
        _socketStatus.value = false

    }
    fun playMoveFromServer(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){

        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        if(moveMessage.playerEmail == currentPlayerEmail){
            val tempBoard = boardsHistory.last().clone().movePiece(moveMessage)
            if(tempBoard != _currentBoard.value){
                Log.e(TAG, "Guess we changed something. tempBoard is: $tempBoard \n \n and currentBoard is: ${_currentBoard.value}. ")
                _currentBoard.value = tempBoard
            }
        }else{
            _currentBoard.value = boardsHistory.last().clone().movePiece(moveMessage)
        }
        boardsHistory.add(_currentBoard.value!!)
        currentMoveShown = boardsHistory.size - 1
        isOurTurn = (moveMessage.playerEmail != tokenManager.getUserEmail())
    }

    fun temporaryPlayMove(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        if(validateMove(moveMessage)){
            _currentBoard.value = boardsHistory.last().clone().movePiece(moveMessage)
            Log.e(TAG, "Temporary play move. board is now: ${_currentBoard.value}")
        } else{
            Log.e(TAG, "Tempmove is invalid.")
        }

    }
    fun isCastlingMove(moveMessage: MoveMessage) : Boolean{
        val startSquare = _currentBoard.value!!.getSquareAt(moveMessage.startCol, moveMessage.startRow)
        val endSquare = _currentBoard.value!!.getSquareAt(moveMessage.endCol, moveMessage.endRow)
        if(startSquare == null && endSquare == null){
            return false
        }
        return _currentBoard.value!!.isCastlingMove(startSquare!!, endSquare!!)
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