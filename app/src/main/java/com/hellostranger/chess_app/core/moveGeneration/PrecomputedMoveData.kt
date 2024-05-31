@file:OptIn(ExperimentalUnsignedTypes::class)

package com.hellostranger.chess_app.core.moveGeneration

import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.helpers.BoardHelper


import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

object PrecomputedMoveData {

    val alignMask: Array<ULongArray>
    val dirRayMask: Array<ULongArray>

    // First 4 are orthogonal, last 4 are diagonals (N, S, W, E, NW, SE, NE, SW)
    val directionOffsets = intArrayOf(8, -8, -1, 1, 7, -7, 9, -9)

    private val dirOffsets2D = arrayOf(
        Coord(0, 1),
        Coord(0, -1),
        Coord(-1, 0),
        Coord(1, 0),
        Coord(-1, 1),
        Coord(1, -1),
        Coord(1, 1),
        Coord(-1, -1)
    )

    // Stores number of moves available in each of the 8 directions for every square on the board
    // Order of directions is: N, S, W, E, NW, SE, NE, SW
    // So for example, if availableSquares[0][1] == 7...
    // that means that there are 7 squares to the north of b1 (the square with index 1 in board array)
    val numSquaresToEdge: Array<IntArray> = Array(64) { IntArray(0) }

    // Aka manhattan distance (answers how many moves for a rook to get from square a to square b)
    var orthogonalDistance: Array<IntArray>
    // Aka chebyshev distance (answers how many moves for a king to get from square a to square b)
    var centreManhattanDistance: IntArray

    // Initialize lookup data
    init {

        for (squareIndex in 0 until 64) {
            val y = squareIndex / 8
            val x = squareIndex - y * 8

            val north = 7 - y
            val east = 7 - x
            numSquaresToEdge[squareIndex] = intArrayOf(
                north,
                y,
                x,
                east,
                minOf(north, x),
                minOf(y, east),
                minOf(north, east),
                minOf(y, x)
            )
        }

        // Distance lookup
        orthogonalDistance = Array(64) { IntArray(64) }
        centreManhattanDistance = IntArray(64)
        for (squareA in 0 until 64) {
            val coordA = Coord(squareA)
            val fileDstFromCentre = max(3 - coordA.fileIndex, coordA.fileIndex - 4)
            val rankDstFromCentre = max(3 - coordA.rankIndex, coordA.rankIndex - 4)
            centreManhattanDistance[squareA] = fileDstFromCentre + rankDstFromCentre
            for (squareB in 0 until 64) {
                val coordB = Coord(squareB)
                val rankDistance = abs(coordA.rankIndex - coordB.rankIndex)
                val fileDistance = abs(coordA.fileIndex - coordB.fileIndex)
                orthogonalDistance[squareA][squareB] = fileDistance + rankDistance
            }
        }

        alignMask = Array(64) { ULongArray(64) }
        for (squareA in 0 until 64) {
            for (squareB in 0 until 64) {
                val cA = Coord(squareA)
                val cB = Coord(squareB)
                val delta = cB - cA
                val dir = Coord(delta.fileIndex.sign, delta.rankIndex.sign)
                for (i in -8 until 8) {
                    val coord = Coord(squareA) + dir * i
                    if (coord.isValidSquare) {
                        alignMask[squareA][squareB] = alignMask[squareA][squareB] or (1UL shl (BoardHelper.indexFromCoord(coord)))
                    }
                }
            }
        }

        dirRayMask = Array(8) { ULongArray(64) }
        for (dirIndex in dirOffsets2D.indices) {
            for (squareIndex in 0 until 64) {
                val square = Coord(squareIndex)
                for (i in 0 until 8) {
                    val coord = square + dirOffsets2D[dirIndex] * i
                    if (coord.isValidSquare) {
                        dirRayMask[dirIndex][squareIndex] = dirRayMask[dirIndex][squareIndex] or (1UL shl (BoardHelper.indexFromCoord(coord)))
                    } else {
                        break
                    }
                }
            }
        }
    }
}
