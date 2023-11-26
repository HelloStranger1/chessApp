package com.hellostranger.chess_app.gameClasses.pieces

import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.PieceType


class Knight(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.KNIGHT, colIndex, rowIndex, resID) {
    constructor(color: Color, hasMoved: Boolean, square: Square, resID: Int = -1) : this(color, hasMoved, square.colIndex, square.rowIndex, resID)
    companion object{
        val SPOT_INCREMENTS = arrayOf(
            intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(-2, 1), intArrayOf(-2, -1), intArrayOf(1, 2), intArrayOf(1, -2), intArrayOf(-1, 2), intArrayOf(-1, -2)
        )
    }
    override fun getThreatenedSquares(board: Board): ArrayList<Square> {
        val positions = ArrayList<Square>()
        for (increment in SPOT_INCREMENTS) {
            val target: Square? = board.spotSearchThreat(
                rowIndex, colIndex, color,
                increment[0], increment[1]
            )
            if (target != null) {
                positions.add(target)
            }
        }
        return positions
    }

    override fun getMovableSquares(board: Board): ArrayList<Square> {
        return getThreatenedSquares(board)
    }
}