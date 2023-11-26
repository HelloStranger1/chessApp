package com.hellostranger.chess_app.gameClasses.pieces

import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.PieceType


class Pawn(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.PAWN, colIndex, rowIndex, resID) {
    constructor(color: Color, hasMoved: Boolean, square: Square, resID: Int = -1) : this(color, hasMoved, square.colIndex, square.rowIndex, resID)
    companion object{
        val SPOT_INCREMENTS_MOVE = arrayOf(intArrayOf(0, 1))
        val SPOT_INCREMENTS_MOVE_FIRST = arrayOf(intArrayOf(0, 1), intArrayOf(0, 2))
        val SPOT_INCREMENTS_TAKE = arrayOf(intArrayOf(-1, 1), intArrayOf(1, 1))
    }

    override fun getThreatenedSquares(board: Board): ArrayList<Square> {
        val positions = ArrayList<Square>()
        for (increment in SPOT_INCREMENTS_TAKE) {
            val target: Square? = if (color === Color.WHITE) {
                board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], false
                )
            } else {
                board.pawnSpotSearchThreat(
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

        for (increment in increments) {
            val target: Square? = if (color === Color.WHITE) {
                board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], false
                )
            } else {
                board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], -1 * increment[1], false
                )
            }
            if (target != null) {
                positions.add(target)
            }
        }
        for (increment in SPOT_INCREMENTS_TAKE) {
            val target: Square? = if (color === Color.WHITE) {
                board.pawnSpotSearchThreat(
                    rowIndex, colIndex, color,
                    increment[0], increment[1], true
                )
            } else {
                board.pawnSpotSearchThreat(
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