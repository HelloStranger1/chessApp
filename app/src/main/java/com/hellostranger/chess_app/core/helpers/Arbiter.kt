package com.hellostranger.chess_app.core.helpers

import com.hellostranger.chess_app.core.board.GameResult
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import java.util.Stack


@ExperimentalUnsignedTypes
object Arbiter {
    fun isDrawResult(result: GameResult) : Boolean {
        return result == GameResult.DrawByArbiter || result == GameResult.FiftyMoveRule ||
                result == GameResult.Repetition || result == GameResult.Stalemate || result == GameResult.InsufficientMaterial || result == GameResult.DrawByAgreement
    }

    fun isWinResult(result : GameResult) : Boolean {
        return isWhiteWinResult(result) || isBlackWinResult(result)
    }

    fun isWhiteWinResult(result: GameResult) : Boolean {
        return result == GameResult.BlackIsMated || result == GameResult.BlackResigned
    }
    fun isBlackWinResult(result : GameResult) : Boolean {
        return result == GameResult.WhiteIsMated || result == GameResult.WhiteResigned
    }

    fun getResultDescription(result: GameResult) : String {
        return when (result) {
            GameResult.DrawByArbiter -> "Draw by arbiter"
            GameResult.FiftyMoveRule -> "Draw by fifty move rule"
            GameResult.Repetition -> "Draw by repetition"
            GameResult.Stalemate -> "Draw by stalemate"
            GameResult.InsufficientMaterial -> "Draw due to insufficient material"
            GameResult.DrawByAgreement -> "Draw by agreement"
            GameResult.BlackResigned -> "White won by resignation"
            GameResult.BlackIsMated -> "White won by checkmate"
            GameResult.WhiteResigned -> "Black won by resignation"
            GameResult.WhiteIsMated -> "Black won by checkmate"
            else -> ""
        }
    }


    /**
     * Calculates the game state.
     * Checks for checkmate, fifty move rule, insufficient material and repetition.
     */
    fun getGameState(board: Board) : GameResult {
        val moveGenerator = MoveGenerator()
        val moves = moveGenerator.generateMoves(board)

        if (moves.isEmpty()) {
            if (moveGenerator.inCheck()) {
                return if (board.isWhiteToMove) GameResult.WhiteIsMated else GameResult.BlackIsMated
            }
            return GameResult.Stalemate
        }

        // Fifty Move Rule
        if (board.fiftyMoveCount >= 100) {
            return GameResult.FiftyMoveRule
        }

        if (insufficientMaterial(board)) {
            return GameResult.InsufficientMaterial
        }
        if (repetition(board)) {
            return GameResult.Repetition
        }
        return GameResult.InProgress
    }

    /**
     * Checks for repetition
     */
    private fun repetition(board: Board) : Boolean {
        val repetitionCount = HashMap<ULong, Int>()
        val history = Array(board.repetitionPositionHistory!!.size + 1) {0UL}
        board.repetitionPositionHistory!!.copyInto(history) // We copy the stack into an array
        for (key in history) {
            repetitionCount[key] = repetitionCount.getOrDefault(key, 0) + 1
            if (repetitionCount[key] == 3) {
                return true
            }
        }
        return false
    }

    // Test for insufficient material (Note: not all cases are implemented)
    private fun insufficientMaterial(board: Board): Boolean {
        if (board.pawns[Board.WHITE_INDEX].count > 0 || board.pawns[Board.BLACK_INDEX].count > 0) {
            return false
        }

        if (board.friendlyOrthogonalSliders != 0UL || board.enemyOrthogonalSliders != 0UL) {
            return false
        }


        // If no pawns, queens, or rooks on the board, then consider knight and bishop cases
        val numWhiteBishops: Int = board.bishops[Board.WHITE_INDEX].count
        val numBlackBishops: Int = board.bishops[Board.BLACK_INDEX].count
        val numWhiteKnights: Int = board.knights[Board.WHITE_INDEX].count
        val numBlackKnights: Int = board.knights[Board.BLACK_INDEX].count
        val numWhiteMinors = numWhiteBishops + numWhiteKnights
        val numBlackMinors = numBlackBishops + numBlackKnights
        val numMinors = numWhiteMinors + numBlackMinors

        // Lone kings or King vs King + single minor: is insufficient
        if (numMinors <= 1) {
            return true
        }

        // Bishop vs bishop: is insufficient when bishops are same colour complex
        if (numMinors == 2 && numWhiteBishops == 1 && numBlackBishops == 1) {
            val whiteBishopIsLightSquare: Boolean = BoardHelper.isLightSquare(board.bishops[Board.WHITE_INDEX][0])
            val blackBishopIsLightSquare: Boolean = BoardHelper.isLightSquare(board.bishops[Board.BLACK_INDEX][0])
            return whiteBishopIsLightSquare == blackBishopIsLightSquare
        }
        return false

    }
}