package com.hellostranger.chess_app.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.core.helpers.Arbiter
import com.hellostranger.chess_app.core.board.GameResult
import com.hellostranger.chess_app.core.players.PlayerInfo
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.players.Player
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.utils.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Stack

@ExperimentalUnsignedTypes
class GameViewModel : ViewModel() {

    /* Websocket variables */
    private val _socketStatus = MutableLiveData(false)
    val socketStatus : LiveData<Boolean> = _socketStatus


    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult : LiveData<GameResult> = _gameResult

    var whitePlayerInfo : PlayerInfo? = null
    var blackPlayerInfo : PlayerInfo? = null

    private val _hasGameStarted = MutableLiveData(false)
    val hasGameStarted : LiveData<Boolean> = _hasGameStarted

    lateinit var whitePlayer : Player
    lateinit var blackPlayer : Player
    private val currentPlayer : Player
        get() = if (board.isWhiteToMove) whitePlayer else blackPlayer

    var board : Board = Board.createBoard()
    private val movesToPlay = Stack<Move>()

    private val _drawOffer = MutableLiveData<DrawOfferMessage>()
    val drawOffer : LiveData<DrawOfferMessage> = _drawOffer

    private var isOurTurn = false
    var isWhite = false
    var gameOverDescription = ""


    fun setStatus(status : Boolean) = viewModelScope.launch(Dispatchers.Main) {
        _socketStatus.value = status
    }
    fun onMoveChosen(move: Move) {
        if (!isOnLastMove()) {
            while (movesToPlay.isNotEmpty()) {
                board.makeMove(movesToPlay.pop())
            }
        }
        board.makeMove(move)
        if (socketStatus.value == false) {
            val gameState = Arbiter.getGameState(board)
            if (Arbiter.isWinResult(gameState) || Arbiter.isDrawResult(gameState)) {
                onGameEnding(gameState, whitePlayerInfo?.elo ?: 800, blackPlayerInfo?.elo ?: 800)
                return
            }

        }
        currentPlayer.onOpponentMoveChosen()
    }

    fun showPreviousBoard() {
        if (board.allGameMoves.isNotEmpty()) {
            movesToPlay.push(board.allGameMoves.last())
            board.unmakeMove(board.allGameMoves.last())
        }
    }

    fun showNextBoard() {
        if (movesToPlay.isNotEmpty()) {
            board.makeMove(movesToPlay.pop())
        }
    }

    fun getLastMove() = board.allGameMoves.last()

    fun startGame(startMessage: GameStartMessage) {
        isWhite = (startMessage.whiteEmail == MyApp.tokenManager.getUserEmail())
        isOurTurn = isWhite
        if (_gameResult.value == GameResult.NotStarted || _gameResult.value == GameResult.Waiting) {
            _gameResult.postValue(GameResult.InProgress)
        }

        whitePlayerInfo = PlayerInfo(startMessage.whiteName, startMessage.whiteEmail, startMessage.whiteImage, startMessage.whiteElo)
        blackPlayerInfo = PlayerInfo(startMessage.blackName, startMessage.blackEmail, startMessage.blackImage, startMessage.blackElo)
        _hasGameStarted.postValue(true )


    }
    fun updateDrawOffer(drawOfferMessage: DrawOfferMessage) = viewModelScope.launch(Dispatchers.Main) {
        _drawOffer.value = drawOfferMessage
    }

    fun onGameEnding(result : GameResult, whiteElo : Int, blackElo : Int) = viewModelScope.launch(Dispatchers.Main) {
        if (_socketStatus.value == true) {
            _socketStatus.value = false
        }
        whitePlayerInfo?.elo ?: whiteElo
        blackPlayerInfo?.elo ?: blackElo
        _gameResult.value = result
    }
    fun isOnLastMove() = movesToPlay.isEmpty()

}