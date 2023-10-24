package com.hellostranger.chess_app.models.gameModels.pieces

import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.PieceType


class King(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.KING, colIndex, rowIndex, resID) {
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
        val squares: List<List<Square>> = board.squaresArray

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