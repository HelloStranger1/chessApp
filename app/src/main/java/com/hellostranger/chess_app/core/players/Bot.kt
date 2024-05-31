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
class Bot(private val baseThinkTimeMs : Long, private val extraThinkTimeMs : Long, private val viewModel: GameViewModel) :
    Player {
    private var searcher : Searcher = Searcher(viewModel.board)
    private var isMoveFound = false
    private var move : Move = Move.NullMove

    init {
        searcher.onSearchComplete = {
            isMoveFound = true
            move = it
        }
    }

    override fun onOpponentMoveChosen() {
        searcher = Searcher(Board.createBoard(viewModel.board))
        isMoveFound = false
        val timeToThink : Long = (baseThinkTimeMs + extraThinkTimeMs * Random.nextDouble()).toLong()
        CoroutineScope(Dispatchers.Default).launch {
            searcher.startSearch()
        }
        CoroutineScope(Dispatchers.Default).launch {
            delay(timeToThink)
            searcher.endSearch()
            val searchResults = searcher.getSearchResult()
            Log.i("TAG", "Best move is: ${MoveUtility.getMoveNameUCI(searchResults.move)} and the eval is: ${searchResults.eval}")
            viewModel.onMoveChosen(searchResults.move)
        }
    }


}