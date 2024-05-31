package com.hellostranger.chess_app.core.search

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import kotlin.math.sign

// Taken from https://web.archive.org/web/20071031100051/http://www.brucemo.com/compchess/programming/hashing.htm
@ExperimentalUnsignedTypes
class TranspositionTable(private val board: Board) {
    companion object {
        const val LOOKUP_FAILED = -1

        // The value for this position is the exact evaluation
        const val EXACT = 1

        // A move was found during the search that was too good, meaning the opponent will play a different move earlier on,
        // not allowing the position where this move was available to be reached. Because the search cuts off at
        // this point (beta cut-off), an even better move may exist. This means that the evaluation for the
        // position could be even higher, making the stored value the lower bound of the actual value.
        const val LOWER_BOUND = 1

        // No move during the search resulted in a position that was better than the current player could get from playing a
        // different move in an earlier position (i.e eval was <= alpha for all moves in the position).
        // Due to the way alpha-beta search works, the value we get here won't be the exact evaluation of the position,
        // but rather the upper bound of the evaluation. This means that the evaluation is, at most, equal to this value.
        const val UPPER_BOUND = 2
    }

    private val count : Int = 1_000_000
    var entries : Array<Entry> = Array(count) { Entry() }
    private var enabled : Boolean = true

    val index : Int
        get() = (board.currentGameState.zobristKey % count.toUInt()).toInt()

    fun tryToGetStoredMove() : Move? {
        return entries[index].move
    }

    fun lookupEvaluation(depth : Int, plyFromRoot : Int, alpha : Int, beta : Int) : Int {
        if (!enabled) {
            return LOOKUP_FAILED
        }
        val entry : Entry = entries[index]

        if (entry.key != board.currentGameState.zobristKey) {
            return LOOKUP_FAILED
        }
        // Only use stored evaluation if it has been searched to at least the same depth as would be searched now
        if (entry.depth < depth) {
            return LOOKUP_FAILED
        }
        val correctedScore = correctRetrievedMateScore(entry.value, plyFromRoot)
        if (entry.nodeType == EXACT.toByte()) {
            return correctedScore
        }
        if (entry.nodeType == UPPER_BOUND.toByte() && correctedScore <= alpha) {
            return correctedScore
        }
        if (entry.nodeType == LOWER_BOUND.toByte() && correctedScore >= beta) {
            return correctedScore
        }
        return LOOKUP_FAILED
    }

    fun storeEvaluation(depth: Int, numPlySearched: Int, eval : Int, evalType : Int, move: Move) {
        if (!enabled) {
            return
        }
        val index = index
        val entry = Entry(board.currentGameState.zobristKey, correctMateScoreForStorage(eval, numPlySearched), move, depth.toByte(), evalType.toByte())
        entries[index] = entry

    }
    private fun correctMateScoreForStorage(score: Int, numPlySearched: Int) : Int {
        if (Searcher.isMateScore(score)) {
            val sign : Int = score.sign
            return (score * sign + numPlySearched) * sign
        }
        return score
    }

    private fun correctRetrievedMateScore(score : Int, numPlySearched : Int) : Int {
        if (Searcher.isMateScore(score)) {
            val sign : Int = score.sign
            return (score * sign - numPlySearched) * sign
        }
        return score
    }

    data class Entry(
        val key : ULong = 0UL,
        val value : Int = 0,
        val move : Move? = null,
        val depth : Byte = 0, // depth is how many ply were searched ahead from this position
        val nodeType : Byte = 0
    )

}