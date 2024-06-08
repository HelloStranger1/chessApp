package com.hellostranger.chess_app.core.players

import android.util.Log
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.players.Player
import com.hellostranger.chess_app.core.search.Searcher
import com.hellostranger.chess_app.viewModels.GameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@ExperimentalUnsignedTypes
class Bot(private val baseThinkTimeMs : Long, private val extraThinkTimeMs : Long, private val viewModel: GameViewModel) : Player {
    private var searcher : Searcher = Searcher(viewModel.board)
    private var isMoveFound = false
    private var move : Move = Move.NullMove



    override fun onOpponentMoveChosen() {
        Log.i("TAG", "Starting to think")
        searcher = Searcher(Board.createBoard(viewModel.board))

        searcher.onSearchComplete = {
            isMoveFound = true
            move = it
            Log.i("TAG", "Best move is: ${MoveUtility.getMoveNameUCI(move)}")
            viewModel.onMoveChosen(it)
        }
        isMoveFound = false
        val timeToThink : Long = (baseThinkTimeMs + extraThinkTimeMs * Random.nextDouble()).toLong()
        val searchThread = Thread {
            searcher.startSearch()
        }
        searchThread.start()
        val delayThread = Thread {
            try {
                Thread.sleep(timeToThink)
            } catch (e : InterruptedException) {
                e.printStackTrace()
            }
            searcher.endSearch()
            Log.i("TAG", "Ending search.")
        }
        delayThread.start()
    }


}