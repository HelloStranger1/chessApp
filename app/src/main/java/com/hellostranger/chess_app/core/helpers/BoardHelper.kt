package com.hellostranger.chess_app.core.helpers

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.moveGeneration.bitboards.BitBoardUtility

@ExperimentalUnsignedTypes
object BoardHelper {
    const val A_1: Int = 0
    const val B_1: Int = 1
    const val C_1: Int = 2
    const val D_1: Int = 3
    const val E_1: Int = 4
    const val F_1: Int = 5
    const val G_1: Int = 6
    const val H_1: Int = 7

    const val A_8: Int = 56
    const val B_8: Int = 57
    const val C_8: Int = 58
    const val D_8: Int = 59
    const val E_8: Int = 60
    const val F_8: Int = 61
    const val G_8: Int = 62
    const val H_8: Int = 63

    const val FILE_NAMES: String = "abcdefgh"

    const val RANK_NAMES: String = "12345678"

    fun rankIndex(squareIndex: Int): Int {
        return squareIndex shr 3
    }

    fun fileIndex(squareIndex: Int): Int {
        return squareIndex and 7
    }

    fun indexFromCoord(rankIndex: Int, fileIndex: Int): Int {
        return rankIndex * 8 + fileIndex
    }

    fun indexFromCoord(coord: Coord): Int {
        return indexFromCoord(coord.rankIndex, coord.fileIndex)
    }

    private fun isLightSquare(fileIndex: Int, rankIndex: Int): Boolean {
        return (rankIndex + fileIndex) % 2 != 0
    }

    fun isLightSquare(squareIndex: Int) : Boolean {
        return isLightSquare(rankIndex(squareIndex), fileIndex(squareIndex))
    }

    fun squareNameFromCoord(fileIndex: Int, rankIndex: Int): String {
        return FILE_NAMES[fileIndex].toString() + "" + (rankIndex + 1)
    }

    fun squareNameFromIndex(squareIndex: Int): String {
        return squareNameFromCoord(Coord(squareIndex))
    }

    private fun squareNameFromCoord(coord: Coord): String {
        return squareNameFromCoord(coord.fileIndex, coord.rankIndex)
    }

    fun squareIndexFromName(name: String): Int {
        val fileName = name[0]
        val rankName = name[1]
        val fileIndex = FILE_NAMES.indexOf(fileName)
        val rankIndex = RANK_NAMES.indexOf(rankName)
        return indexFromCoord(rankIndex, fileIndex)
    }

    // Creates an ascii diagram of the bitboard
    fun createBitboard(bitboard : ULong) : String {
        val result : StringBuilder = StringBuilder()
        for (y in 0 until 8) {
            val rankIndex = 7 - y
            result.appendLine("+---+---+---+---+---+---+---+---+")

            for (fileIndex in 0 until 8) {
                val squareIndex = indexFromCoord(rankIndex, fileIndex)

                if (BitBoardUtility.containsSquare(bitboard, squareIndex)) {
                    result.append("| P ")
                } else {
                    result.append("| _ ")
                }

                if (fileIndex == 7) {
                    result.appendLine("| ${rankIndex + 1}")
                }
            }

            if (y == 7) {
                result.appendLine("+---+---+---+---+---+---+---+---+")
                val fileNames = "  a   b   c   d   e   f   g   h  "
                result.appendLine(fileNames)
                result.appendLine()
            }
        }
        return result.toString()
    }

    /**
     * Creates an ASCII diagram of the current board.
     *
     */
    fun createDiagram(
        board: Board,
        blackAtTop: Boolean = true,
        includeFen: Boolean = true,
        includeZobristKey: Boolean = true
    ): String {
        val result : StringBuilder = StringBuilder()
        val lastMoveSquare = if (board.allGameMoves.isNotEmpty()) board.allGameMoves.last().targetSquare else -1

        for (y in 0 until 8) {
            val rankIndex = if (blackAtTop) 7 - y else y
            result.appendLine("+---+---+---+---+---+---+---+---+")

            for (x in 0 until 8) {
                val fileIndex = if (blackAtTop) x else 7 - x
                val squareIndex = indexFromCoord(rankIndex, fileIndex)
                val highlight : Boolean = squareIndex == lastMoveSquare
                val piece = board.square[squareIndex]

                if (highlight) {
                    result.append("|(${Piece.getSymbol(piece)})")
                } else {
                    result.append("| ${Piece.getSymbol(piece)} ")
                }

                if (x == 7) {
                    result.appendLine("| ${rankIndex + 1}")
                }
            }

            if (y == 7) {
                result.appendLine("+---+---+---+---+---+---+---+---+")
                val fileNames = "  a   b   c   d   e   f   g   h  "
                val fileNamesReversed = "  h   g   f   e   d   c   b   a  "
                if (blackAtTop) {
                    result.appendLine(fileNames)
                } else {
                    result.appendLine(fileNamesReversed)
                }
                result.appendLine()
                if (includeFen) {
                    result.appendLine("Fen         : ${FenUtility.currentFen(board)}")
                }
                if (includeZobristKey) {
                    result.appendLine("Zobrist Key : ${board.zobristKey}")
                }
            }
        }

        return result.toString()
    }
}
