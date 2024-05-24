package com.hellostranger.chess_app.core.evaluation

import com.hellostranger.chess_app.core.board.Coord

object PrecomputedEvaluationData {
    val pawnShieldSquaresWhite : Array<IntArray> = Array(64) {IntArray(0)}
    val pawnShieldSquaresBlack : Array<IntArray> = Array(64) {IntArray(0)}

    init {
        for (squareIndex in 0 until 64) {
            createPawnShieldSquare(squareIndex)
        }
    }
    private fun addIfValid(coord: Coord, list : MutableList<Int>) {
        if (coord.isValidSquare) {
            list.add(coord.squareIndex)
        }
    }
    private fun createPawnShieldSquare(squareIndex : Int) {
        val shieldIndicesWhite : MutableList<Int> = mutableListOf()
        val shieldIndicesBlack : MutableList<Int> = mutableListOf()
        val coord = Coord(squareIndex)
        val rank = coord.rankIndex
        val file = coord.fileIndex.coerceIn(1..6)

        for (fileOffset in -1..1) {
            addIfValid(Coord(file + fileOffset, rank + 1), shieldIndicesWhite)
            addIfValid(Coord(file + fileOffset, rank + 2), shieldIndicesWhite)

            addIfValid(Coord(file + fileOffset, rank - 1), shieldIndicesBlack)
            addIfValid(Coord(file + fileOffset, rank - 2), shieldIndicesBlack)
        }

        pawnShieldSquaresWhite[squareIndex] = shieldIndicesWhite.toIntArray()
        pawnShieldSquaresBlack[squareIndex] = shieldIndicesBlack.toIntArray()

    }
}