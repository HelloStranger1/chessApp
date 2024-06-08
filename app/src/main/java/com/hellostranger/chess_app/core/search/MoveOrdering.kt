package com.hellostranger.chess_app.core.search

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.evaluation.Evaluation
import com.hellostranger.chess_app.core.evaluation.PieceSquareTable
import com.hellostranger.chess_app.core.moveGeneration.bitboards.BitBoardUtility

@ExperimentalUnsignedTypes
class MoveOrdering {

    companion object {
        const val MAX_MOVE_COUNT = 218

        const val MAX_KILLER_MOVE_PLY = 32

        const val HASH_MOVE_SCORE = 1_000_000_000
        const val WINNING_CAPTURE_BIAS = 8_000_000
        const val PROMOTE_BIAS = 6_000_000
        const val KILLER_BIAS = 4_000_000
        const val LOSING_CAPTURE_BIAS = 2_000_000

        @ExperimentalUnsignedTypes
        private fun getPieceValue(pieceType : Int) : Int {
            return when (pieceType) {
                Piece.QUEEN -> {
                    Evaluation.QUEEN_VALUE
                }
                Piece.ROOK -> {
                    Evaluation.ROOK_VALUE
                }
                Piece.BISHOP -> {
                    Evaluation.BISHOP_VALUE
                }
                Piece.KNIGHT -> {
                    Evaluation.KNIGHT_VALUE
                }
                Piece.PAWN -> {
                    Evaluation.PAWN_VALUE
                }
                else -> {
                    0
                }
            }

        }

    }
    private val moveScores : IntArray = IntArray(MAX_MOVE_COUNT)

    var killerMoves : Array<Killers> = Array(MAX_KILLER_MOVE_PLY) { Killers() }
    var history : Array<Array<IntArray>> = Array(2) {Array(64) {IntArray(64)} }

    fun clearHistory() {
        history = Array(2) {Array(64) {IntArray(64)} }
    }

    fun clearKillers() {
        killerMoves = Array(MAX_KILLER_MOVE_PLY) { Killers() }
    }


    fun orderMoves(hashMove: Move, board: Board, moves : Array<Move>, oppAttacks : ULong, oppPawnAttacks : ULong, inQSearch : Boolean, ply : Int) : Array<Move>{
        for (i in moves.indices) {
            val move = moves[i]
            if (Move.sameMove(move, hashMove)) {
                moveScores[i] = HASH_MOVE_SCORE
                continue
            }
            var score = 0
            val startSquare = move.startSquare
            val targetSquare = move.targetSquare

            val movePiece = board.square[startSquare]
            val movePieceType = Piece.pieceType(movePiece)
            val capturePieceType = Piece.pieceType(board.square[targetSquare])
            val isCapture = capturePieceType != Piece.NONE
            val flag = move.moveFlag
            val pieceValue = getPieceValue(movePieceType)

            if (isCapture) {
                // Order moves to try capturing the most valuable opponent piece with least valuable of own pieces first
                val captureMaterialData = getPieceValue(capturePieceType) - pieceValue
                val opponentCanRecapture = BitBoardUtility.containsSquare(oppPawnAttacks or oppAttacks, targetSquare)
                score += if (opponentCanRecapture) {
                    ( if(captureMaterialData >= 0) WINNING_CAPTURE_BIAS else  LOSING_CAPTURE_BIAS) + captureMaterialData
                } else {
                    WINNING_CAPTURE_BIAS + captureMaterialData
                }
            }

            if (movePieceType == Piece.PAWN) {
                if (flag == Move.PROMOTE_TO_QUEEN_FLAG && !isCapture) {
                    score += PROMOTE_BIAS
                }
            } else if (movePieceType != Piece.KING && movePieceType != Piece.NONE) {
                val toScore = PieceSquareTable.read(movePiece, targetSquare)
                val fromScore = PieceSquareTable.read(movePiece, startSquare)
                score += toScore - fromScore

                if (BitBoardUtility.containsSquare(oppPawnAttacks, targetSquare)) {
                    score -= 50
                } else if (BitBoardUtility.containsSquare(oppAttacks, targetSquare)) {
                    score -= 25
                }
            }

            if (!isCapture) {
                val isKiller = !inQSearch && ply < MAX_KILLER_MOVE_PLY && killerMoves[ply].match(move)
                if (isKiller) {
                    score += KILLER_BIAS
                }
                score += history[board.moveColourIndex][move.startSquare][move.targetSquare]
            }
            moveScores[i] = score
        }
        // Sort moves by moveScores
        val scoredMoves = moveScores.take(moves.size).zip(moves)
        val sortedMoveScores = scoredMoves.sortedBy { it.first }
        return sortedMoveScores.map {it.second}.toTypedArray()
    }
    data class Killers(var moveA : Move? = null, var moveB : Move? = null) {

        fun add(move: Move) {
            if (moveA == null || move.moveValue != moveA!!.moveValue) {
                moveB = moveA
                moveA = move
            }
        }

        fun match(move: Move) : Boolean = move.moveValue == moveA?.moveValue || move.moveValue == moveB?.moveValue
    }
}