package com.hellostranger.chess_app.core.evaluation

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.board.PieceList
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.moveGeneration.PrecomputedMoveData
import com.hellostranger.chess_app.core.moveGeneration.bitboards.Bits
import kotlin.math.min

@ExperimentalUnsignedTypes
/**
 * Used to evaluate a position.
 */
class Evaluation {
    companion object {
        const val PAWN_VALUE = 100
        const val KNIGHT_VALUE = 300
        const val BISHOP_VALUE = 320
        const val ROOK_VALUE = 500
        const val QUEEN_VALUE = 900

        val passedPawnBonuses : IntArray = intArrayOf(0, 120, 80, 50, 30, 15, 15)
        val isolatedPawnPenaltyByCount = intArrayOf( 0, -10, -25, -50, -75, -75, -75, -75, -75 )
        val kingPawnShieldScores = intArrayOf(4, 7, 4, 3, 6, 3)

    }
    private val pawnShieldSquaresWhite : Array<IntArray> = Array(64) {IntArray(0)}
    private val pawnShieldSquaresBlack : Array<IntArray> = Array(64) {IntArray(0)}

    init {
        for (squareIndex in 0 until 64) {
            createPawnShieldSquare(squareIndex)
        }
    }


    lateinit var board: Board
    private lateinit var whiteEval : EvaluationData
    private lateinit var blackEval : EvaluationData

    // Performs static evaluation of the current position.
    // The position is assumed to be 'quiet', i.e no captures are available that could drastically affect the evaluation.
    // The score that's returned is given from the perspective of whoever turn it is to move.
    // So a positive score means the player who's turn it is to move has an advantage, while a negative score indicates a disadvantage.
    fun evaluate(board: Board) : Int {
        this.board = board
        whiteEval = EvaluationData()
        blackEval = EvaluationData()

        val whiteMaterial = getMaterialInfo(Board.WHITE_INDEX)
        val blackMaterial = getMaterialInfo(Board.BLACK_INDEX)

        // Score based on number and type of pieces on board
        whiteEval.materialScore = whiteMaterial.materialScore
        blackEval.materialScore = blackMaterial.materialScore
        // Score based on positions of pieces
        whiteEval.pieceSquareScore = evaluatePieceSquareTables(true, blackMaterial.endgameT)
        blackEval.pieceSquareScore = evaluatePieceSquareTables(false, whiteMaterial.endgameT)

        // Encourage using own king to push enemy king to edge of board in winning endgame
        whiteEval.mopUpScore = mopUpEval(true, whiteMaterial, blackMaterial)
        blackEval.mopUpScore = mopUpEval(false, blackMaterial, whiteMaterial)

        whiteEval.pawnScore = evaluatePawns(Board.WHITE_INDEX)
        blackEval.pawnScore = evaluatePawns(Board.BLACK_INDEX)

        whiteEval.pawnShieldScore = kingPawnShield(Board.WHITE_INDEX, blackMaterial, blackEval.pieceSquareScore.toFloat())
        blackEval.pawnShieldScore = kingPawnShield(Board.BLACK_INDEX, whiteMaterial, whiteEval.pieceSquareScore.toFloat())

        val perspective = if(board.isWhiteToMove) 1 else -1
        val eval = whiteEval.sum() - blackEval.sum()
        return eval * perspective
    }

    private fun kingPawnShield(colourIndex: Int, enemyMaterial: MaterialInfo, enemyPieceSquareScore : Float) : Int {
        if (enemyMaterial.endgameT >= 1) {
            return 0
        }
        var penalty = 0

        val isWhite = colourIndex == Board.WHITE_INDEX
        val friendlyPawn = Piece.makePiece(Piece.PAWN, isWhite)
        val kingSquare = board.kingSquare[colourIndex]
        val kingFile = BoardHelper.fileIndex(kingSquare)

        var uncastledKingPenalty  = 0

        if (kingFile <= 2 || kingFile >= 5) {
            val squares = if(isWhite) pawnShieldSquaresWhite[kingSquare] else pawnShieldSquaresBlack[kingSquare]

            for (i in 0 until squares.size / 2) {
                val shieldSquareIndex = squares[i]
                if (board.square[shieldSquareIndex] != friendlyPawn) {
                    penalty += if (squares.size > 3 && board.square[squares[i + 3]] == friendlyPawn) {
                        kingPawnShieldScores[i+3]
                    } else {
                        kingPawnShieldScores[i]
                    }
                }
            }
            penalty *= penalty
        } else {
            val enemyDevelopmentScore : Float = ((enemyPieceSquareScore + 10) / 130f).coerceIn(0f..1f)
            uncastledKingPenalty = (50 * enemyDevelopmentScore).toInt()
        }

        var openFileAgainstKingPenalty = 0
        if (enemyMaterial.numRooks > 1 || (enemyMaterial.numRooks > 0 && enemyMaterial.numQueens > 0)) {
            val clampedKingFile = kingFile.coerceIn(1..6)
            val myPawns = enemyMaterial.enemyPawns
            for (attackFile in clampedKingFile..clampedKingFile + 1) {
                val fileMask = Bits.fileMasks[attackFile]
                val isKingFile = attackFile == kingFile
                if ((enemyMaterial.pawns and fileMask) == 0UL) {
                    openFileAgainstKingPenalty += if (isKingFile) 25 else 15
                    if ((myPawns and fileMask) == 0UL) {
                        openFileAgainstKingPenalty += if (isKingFile) 15 else 10
                    }
                }
            }
        }
        var pawnShieldWeight : Float = 1 - enemyMaterial.endgameT
        if (board.queens[1 - colourIndex].count == 0) {
            pawnShieldWeight *= 0.6f
        }
        return ((-penalty - uncastledKingPenalty - openFileAgainstKingPenalty) *pawnShieldWeight).toInt()


    }

    private fun evaluatePawns(colourIndex: Int) : Int{
        val pawns : PieceList = board.pawns[colourIndex]
        val isWhite = colourIndex == Board.WHITE_INDEX
        val opponentPawns = board.pieceBitboards!![Piece.makePiece(Piece.PAWN, !isWhite)]
        val friendlyPawns = board.pieceBitboards!![Piece.makePiece(Piece.PAWN, isWhite)]

        val masks : ULongArray = if(isWhite) Bits.whitePassedPawnMasks else Bits.blackPassedPawnMasks
        var bonus = 0
        var numIsolatedPawns = 0

        for (i in 0 until pawns.count) {
            val square = pawns[i]
            val passedMask = masks[square]

            if ((opponentPawns and passedMask) == 0UL) {
                val rank = BoardHelper.rankIndex(square)
                val numSquaresFromPromotion = if (isWhite) 7 - rank else rank
                bonus += passedPawnBonuses[numSquaresFromPromotion]
            }

            if ((friendlyPawns and Bits.adjacentFileMasks[BoardHelper.fileIndex(square)]) == 0UL) {
                numIsolatedPawns++
            }
        }

        return bonus + isolatedPawnPenaltyByCount[numIsolatedPawns]

    }

    private fun mopUpEval(isWhite : Boolean, myMaterial : MaterialInfo, enemyMaterial : MaterialInfo) : Int {
        if (myMaterial.materialScore <= enemyMaterial.materialScore + PAWN_VALUE * 2 || enemyMaterial.endgameT <= 0f) {
            return 0
        }
        val friendlyIndex = if(isWhite) Board.WHITE_INDEX else Board.BLACK_INDEX
        val enemyIndex = if (isWhite) Board.BLACK_INDEX else Board.WHITE_INDEX

        val friendlyKingSquare = board.kingSquare[friendlyIndex]
        val enemyKingSquare = board.kingSquare[enemyIndex]

        var mopUpScore = (14 - PrecomputedMoveData.orthogonalDistance[friendlyKingSquare][enemyKingSquare]) * 4
        mopUpScore += PrecomputedMoveData.centreManhattanDistance[enemyKingSquare] * 10
        return (mopUpScore * enemyMaterial.endgameT).toInt()
    }

    private fun evaluatePieceSquareTables(isWhite: Boolean, endgameT : Float) : Int {
        var value = 0
        val colourIndex = if(isWhite) Board.WHITE_INDEX else Board.BLACK_INDEX
        value += evaluatePieceSquareTable(PieceSquareTable.rooks, board.rooks[colourIndex], isWhite)
        value += evaluatePieceSquareTable(PieceSquareTable.knights, board.knights[colourIndex], isWhite)
        value += evaluatePieceSquareTable(PieceSquareTable.bishops, board.bishops[colourIndex], isWhite)
        value += evaluatePieceSquareTable(PieceSquareTable.queens, board.queens[colourIndex], isWhite)

        val pawnEarly = evaluatePieceSquareTable(PieceSquareTable.pawns, board.pawns[colourIndex], isWhite)
        val pawnsLate = evaluatePieceSquareTable(PieceSquareTable.pawnsEnd, board.pawns[colourIndex], isWhite)
        value += (pawnEarly * (1 - endgameT)).toInt()
        value += (pawnsLate * endgameT).toInt()

        val kingEarlyPhase = PieceSquareTable.read(
            PieceSquareTable.kingStart,
            board.kingSquare[colourIndex],
            isWhite
        )
        val kingLatePhase =
            PieceSquareTable.read(PieceSquareTable.kingEnd, board.kingSquare[colourIndex], isWhite)
        value += (kingEarlyPhase * (1 - endgameT)).toInt()
        value += (kingLatePhase * endgameT).toInt()

        return value
    }

    private fun evaluatePieceSquareTable(table : IntArray, pieceList: PieceList, isWhite: Boolean) : Int {
        var value = 0
        for (i in 0 until pieceList.count) {
            value += PieceSquareTable.read(table, pieceList[i], isWhite)
        }
        return value
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
    private fun addIfValid(coord: Coord, list : MutableList<Int>) {
        if (coord.isValidSquare) {
            list.add(coord.squareIndex)
        }
    }

    data class  EvaluationData(
        var materialScore : Int = 0,
        var mopUpScore : Int = 0,
        var pieceSquareScore : Int = 0,
        var pawnScore : Int = 0,
        var pawnShieldScore : Int = 0
    ) {
        fun sum() : Int {
            return materialScore + mopUpScore + pieceSquareScore + pawnScore + pawnShieldScore
        }
    }

    private fun getMaterialInfo(colourIndex : Int) : MaterialInfo {
        val numPawns: Int = board.pawns[colourIndex].count
        val numKnights: Int = board.knights[colourIndex].count
        val numBishops: Int = board.bishops[colourIndex].count
        val numRooks: Int = board.rooks[colourIndex].count
        val numQueens: Int = board.queens[colourIndex].count

        val isWhite = colourIndex == Board.WHITE_INDEX
        val myPawns = board.pieceBitboards!![Piece.makePiece(Piece.PAWN, isWhite)]
        val enemyPawns = board.pieceBitboards!![Piece.makePiece(Piece.PAWN, !isWhite)]

    return MaterialInfo(numPawns, numKnights, numBishops, numQueens, numRooks, myPawns, enemyPawns)
    }

    class MaterialInfo(
        numPawns: Int, numKnights : Int,
        numBishops: Int, val numQueens: Int, val numRooks: Int, val pawns : ULong,
        val enemyPawns: ULong
    ) {
        var materialScore : Int = 0
        var endgameT : Float


        init {
            materialScore += numPawns * PAWN_VALUE
            materialScore += numKnights * KNIGHT_VALUE
            materialScore += numBishops * BISHOP_VALUE
            materialScore += numRooks * ROOK_VALUE
            materialScore += numQueens * QUEEN_VALUE

            // Endgame Transition (0->1)
            val queenEndgameWeight = 45
            val rookEndgameWeight = 20
            val bishopEndgameWeight = 10
            val knightEndgameWeight = 10

            val endgameStartWeight = 2 * rookEndgameWeight + 2 * bishopEndgameWeight + 2 * knightEndgameWeight + queenEndgameWeight
            val endgameWeightSum = numQueens * queenEndgameWeight + numRooks * rookEndgameWeight + numBishops * bishopEndgameWeight + numKnights * knightEndgameWeight
            endgameT = 1 - min(1f, endgameWeightSum / endgameStartWeight.toFloat())
        }
    }
}