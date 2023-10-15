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
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameViewModel(private val currentGame : Game) : ViewModel() {

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
        Log.e("TAG", "moveMessage is: $moveMessage")
        if(!isOurTurn
            || board.squaresArray[moveMessage.startRow][moveMessage.startCol].piece == null
            || isWhite !=(board.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.color == Color.WHITE)){
                return false;
            }
        Log.e("TAG", "Move is valid. it is our turn: $isOurTurn the moving piece isn't null: " +
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
        _startMessageData.value = startMessage
        isWhite = startMessage.whiteEmail == tokenManager.getUserEmail()
        isOurTurn = isWhite
        if(currentGame.gameState == GameState.WAITING){
            _gameStatus.value = GameState.ACTIVE
        }else{
            _gameStatus.value = currentGame.gameState
        }
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }

    }

    fun onGameEnding(result : GameState) = viewModelScope.launch(Dispatchers.Main){
        _gameStatus.value = result
        _socketStatus.value = false

    }
    fun playMoveFromServer(moveMessage: MoveMessage) = viewModelScope.launch(Dispatchers.Main){
        if(boardsHistory.isEmpty()){
            boardsHistory.add(_currentBoard.value!!)
        }
        _currentBoard.value = boardsHistory.last().clone().movePiece(moveMessage)
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
        }
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