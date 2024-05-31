package com.hellostranger.chess_app.core.evaluation

import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper


@ExperimentalUnsignedTypes
/**
 * Holds position-based square tables for each piece.
 */
object PieceSquareTable {
    private val tables: Array<IntArray> = Array(Piece.MaxPieceIndex + 1) { IntArray(0) }

    fun read(table: IntArray, square: Int, isWhite: Boolean): Int {
        var sq = square
        if (isWhite) {
            val col = BoardHelper.fileIndex(sq)
            var row = BoardHelper.rankIndex(sq)
            row = 7 - row
            sq = BoardHelper.indexFromCoord(col, row)
        }
        return table[sq]
    }

    fun read(piece: Int, square: Int): Int {
        return tables[piece][square]
    }

    val pawns = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )

    val pawnsEnd = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        80, 80, 80, 80, 80, 80, 80, 80,
        50, 50, 50, 50, 50, 50, 50, 50,
        30, 30, 30, 30, 30, 30, 30, 30,
        20, 20, 20, 20, 20, 20, 20, 20,
        10, 10, 10, 10, 10, 10, 10, 10,
        10, 10, 10, 10, 10, 10, 10, 10,
        0, 0, 0, 0, 0, 0, 0, 0
    )

    val rooks = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, 10, 10, 10, 10, 5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        0, 0, 0, 5, 5, 0, 0, 0
    )

    val knights = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    val bishops = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    val queens = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20
    )

    val kingStart = intArrayOf(
        -80, -70, -70, -70, -70, -70, -70, -80,
        -60, -60, -60, -60, -60, -60, -60, -60,
        -40, -50, -50, -60, -60, -50, -50, -40,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        20, 20, -5, -5, -5, -5, 20, 20,
        20, 30, 10, 0, 0, 10, 30, 20
    )

    val kingEnd = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -10, -5, 20, 30, 30, 20, -5, -10,
        -15, -10, 35, 45, 45, 35, -10, -15,
        -20, -15, 30, 40, 40, 30, -15, -20,
        -25, -20, 20, 25, 25, 20, -20, -25,
        -30, -25, 0, 0, 0, 0, -25, -30,
        -50, -30, -30, -30, -30, -30, -30, -50
    )


    init {
        tables[Piece.makePiece(Piece.PAWN, Piece.WHITE)] = pawns
        tables[Piece.makePiece(Piece.ROOK, Piece.WHITE)] = rooks
        tables[Piece.makePiece(Piece.KNIGHT, Piece.WHITE)] = knights
        tables[Piece.makePiece(Piece.BISHOP, Piece.WHITE)] = bishops
        tables[Piece.makePiece(Piece.QUEEN, Piece.WHITE)] = queens

        tables[Piece.makePiece(Piece.PAWN, Piece.BLACK)] = getFlippedTable(pawns)
        tables[Piece.makePiece(Piece.ROOK, Piece.BLACK)] = getFlippedTable(rooks)
        tables[Piece.makePiece(Piece.KNIGHT, Piece.BLACK)] = getFlippedTable(knights)
        tables[Piece.makePiece(Piece.BISHOP, Piece.BLACK)] = getFlippedTable(bishops)
        tables[Piece.makePiece(Piece.QUEEN, Piece.BLACK)] = getFlippedTable(queens)
    }

    private fun getFlippedTable(table: IntArray): IntArray {
        val flippedTable = IntArray(table.size)
        for (i in table.indices) {
            val coord = Coord(i)
            val flippedCoord = Coord(coord.fileIndex, 7 - coord.rankIndex)
            flippedTable[flippedCoord.squareIndex] = table[i]
        }
        return flippedTable
    }
}
