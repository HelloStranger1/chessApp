package com.hellostranger.chess_app.chess_models

import com.google.gson.Gson
import com.hellostranger.chess_app.dto.websocket.MoveMessage

data class Board(
    val squaresArray: List<List<Square>>,
    var halfMoveCount : Int = 0
) : Cloneable{

    fun movePiece(move : MoveMessage){
        val startSquare = squaresArray[move.startRow][move.startCol]
        val endSquare = squaresArray[move.endRow][move.endCol]

        val movingPiece = startSquare.piece ?: return

        if(movingPiece.color == Color.WHITE && movingPiece.pieceType == PieceType.PAWN){
            if(move.endRow - move.startRow == 1 &&
                kotlin.math.abs(move.endCol - move.startCol) == 1 &&
                endSquare.piece == null){
                squaresArray[move.startRow][move.endCol].piece = null
            }
        } else if(movingPiece.color == Color.BLACK && movingPiece.pieceType == PieceType.PAWN) {
            if (move.endRow - move.startRow == -1 &&
                kotlin.math.abs(move.endCol - move.startCol) == 1 &&
                endSquare.piece == null
            ) {
                squaresArray[move.startRow][move.endCol].piece = null
            }
        }
        endSquare.piece = movingPiece
        startSquare.piece = null
        movingPiece.hasMoved = true
        halfMoveCount++
    }

    override fun toString(): String {
        var desc = " \n"
        for (i in 7 downTo -1 + 1) {
            desc += i
            desc += " "
            for (j in 0..7) {
                val pieceAt: Piece? = squaresArray[i][j].piece
                if (pieceAt == null) {
                    desc +=". "
                } else {
                    val isWhite: Boolean = pieceAt.color == Color.WHITE
                    if (pieceAt.pieceType === PieceType.KING) {
                        desc += if (isWhite) {
                            "k "
                        } else {
                            "K "
                        }
                    } else if (pieceAt.pieceType === PieceType.QUEEN) {
                        desc += if (isWhite) {
                            "q "
                        } else {
                            "Q "
                        }
                    } else if (pieceAt.pieceType === PieceType.ROOK) {
                        desc += if (isWhite) {
                            "r "
                        } else {
                            "R "
                        }
                    } else if (pieceAt.pieceType === PieceType.BISHOP) {
                        desc += if (isWhite) {
                            "b "
                        } else {
                            "B "
                        }
                    } else if (pieceAt.pieceType === PieceType.KNIGHT) {
                        desc += if (isWhite) {
                            "n "
                        } else {
                            "N "
                        }
                    } else if (pieceAt.pieceType === PieceType.PAWN) {
                        desc += if (isWhite) {
                            "p "
                        } else {
                            "P "
                        }
                    }
                }
            }
            desc +="\n"
        }
        return desc
    }

    public override fun clone(): Board {
        val json = Gson()

        val tempJsonBoard = json.toJson(this)

        return json.fromJson(tempJsonBoard, Board::class.java)

    }
}
