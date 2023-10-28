package com.hellostranger.chess_app.models.gameModels.pieces

import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.PieceType


class Queen(color: Color, hasMoved: Boolean, colIndex : Int, rowIndex : Int, resID : Int = -1) :
    Piece(color, hasMoved, PieceType.QUEEN, colIndex, rowIndex, resID) {
    constructor(color: Color, hasMoved: Boolean, square: Square, resID: Int = -1) : this(color, hasMoved, square.colIndex, square.rowIndex, resID)
   companion object{
       val BEAM_INCREMENTS = arrayOf(
           intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1), intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 0), intArrayOf(-1, 0)
       )
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