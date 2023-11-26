package com.hellostranger.chess_app.gameClasses.pieces

import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.PieceType


class King(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.KING, colIndex, rowIndex, resID) {
    constructor(color: Color, hasMoved: Boolean, square: Square, resID: Int = -1) : this(color, hasMoved, square.colIndex, square.rowIndex, resID)
    companion object{
        val SPOT_INCREMENTS = arrayOf(
            intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1)
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
        val movableSquares = getThreatenedSquares(board)
        if (hasMoved) {
            return movableSquares
        }
        val squares: Array<Array<Square>> = board.squaresArray

        if (squares[rowIndex][0].piece != null && !squares[rowIndex][0].piece!!.hasMoved) {
            //King can long castle
            movableSquares.add(squares[rowIndex][0])
        }
        if (squares[rowIndex][7].piece != null && !squares[rowIndex][7].piece!!.hasMoved) {
            //King can long castle
            movableSquares.add(squares[rowIndex][7])
        }
        return movableSquares
    }
}