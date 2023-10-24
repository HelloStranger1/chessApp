package com.hellostranger.chess_app.models.gameModels.pieces

import android.util.Log
import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.PieceType
import java.util.Arrays


class Pawn(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.PAWN, colIndex, rowIndex, resID) {
    companion object{
        val SPOT_INCREMENTS_MOVE = arrayOf(intArrayOf(0, 1))
        val SPOT_INCREMENTS_MOVE_FIRST = arrayOf(intArrayOf(0, 1), intArrayOf(0, 2))
        val SPOT_INCREMENTS_TAKE = arrayOf(intArrayOf(-1, 1), intArrayOf(1, 1))
    }

    override fun getThreatenedSquares(board: Board): ArrayList<Square> {
        val positions = ArrayList<Square>()
        for (increment in SPOT_INCREMENTS_TAKE) {
            var target: Square?
            if (color === Color.WHITE) {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], false
                )
            } else {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], -1 * increment[1], false
                )
            }
            if (target != null) {
                positions.add(target)
            }
        }
        return positions
    }

    override fun getMovableSquares(board: Board): ArrayList<Square> {
        val positions = ArrayList<Square>()
        val increments: Array<IntArray> = if (hasMoved) {
            SPOT_INCREMENTS_MOVE
        } else {
            SPOT_INCREMENTS_MOVE_FIRST
        }
        Log.e("PawnTag", "Increments is: ${increments.contentDeepToString()}. hasMoved is: $hasMoved, move_first is: ${
            SPOT_INCREMENTS_MOVE_FIRST.contentDeepToString()} and move: ${SPOT_INCREMENTS_MOVE.contentDeepToString()}")
        for (increment in increments) {
            var target: Square?
            if (color === Color.WHITE) {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], false
                )
            } else {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], -1 * increment[1], false
                )
            }
            if (target != null) {
                positions.add(target)
            }
        }
        for (increment in SPOT_INCREMENTS_TAKE) {
            var target: Square?
            if (color === Color.WHITE) {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], true
                )
            } else {
                target = board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], -1 * increment[1], true
                )
            }
            if (target != null) {
                positions.add(target)
            }
        }
        return positions
    }
}