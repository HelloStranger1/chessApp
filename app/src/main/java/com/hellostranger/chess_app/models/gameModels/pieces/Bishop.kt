package com.hellostranger.chess_app.models.gameModels.pieces

import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.PieceType

class Bishop(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.BISHOP, colIndex, rowIndex, resID) {
    companion object {
        val BEAM_INCREMENTS =
            arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
    }
    override fun getThreatenedSquares(board: Board): ArrayList<Square> {
        val positions = ArrayList<Square>()
        for (increment in BEAM_INCREMENTS) {
            val squares: Array<Square> = board.beamSearchThreat(
                rowIndex, colIndex, color,
                increment[0], increment[1]
            )
            positions.addAll(listOf(*squares))
        }
        return positions
    }

    override fun getMovableSquares(board: Board): ArrayList<Square> {
        return getThreatenedSquares(board)
    }
}