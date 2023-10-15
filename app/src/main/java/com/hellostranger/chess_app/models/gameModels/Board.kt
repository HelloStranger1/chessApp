package com.hellostranger.chess_app.models.gameModels

import android.util.Log
import com.google.gson.Gson
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.models.gameModels.enums.PieceType
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.utils.Constants

data class Board(
    val squaresArray: List<List<Square>>,
) : Cloneable{

    fun movePiece(move : MoveMessage) : Board {
        val startSquare = squaresArray[move.startRow][move.startCol]
        val endSquare = squaresArray[move.endRow][move.endCol]

        val movingPiece = startSquare.piece
        if(movingPiece == null){
            Log.e("TAG", "You can't move this!! No Piece to move.")
            return this
        }

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
        if(move.promotionType != null){
            movingPiece.pieceType = move.promotionType!!
            if(movingPiece.color == Color.WHITE){
                when(move.promotionType){
                    PieceType.QUEEN ->{
                        movingPiece.resID = R.drawable.ic_white_queen
                    }
                    PieceType.ROOK ->{
                        movingPiece.resID = R.drawable.ic_white_rook
                    }
                    PieceType.BISHOP ->{
                        movingPiece.resID = R.drawable.ic_white_bishop
                    }
                    PieceType.KNIGHT ->{
                        movingPiece.resID = R.drawable.ic_white_knight
                    }else ->{
                        Log.e("TAG", "Tried to promote to: ${move.promotionType} but you can't promote to that.")
                    }
                }
            }else{
                when(move.promotionType){
                    PieceType.QUEEN ->{
                        movingPiece.resID = R.drawable.ic_black_queen
                    }
                    PieceType.ROOK ->{
                        movingPiece.resID = R.drawable.ic_black_rook
                    }
                    PieceType.BISHOP ->{
                        movingPiece.resID = R.drawable.ic_black_bishop
                    }
                    PieceType.KNIGHT ->{
                        movingPiece.resID = R.drawable.ic_black_knight
                    }else ->{
                    Log.e("TAG", "Tried to promote to: ${move.promotionType} but you can't promote to that.")
                }
                }
            }
        }
        endSquare.piece = movingPiece
        startSquare.piece = null
        movingPiece.hasMoved = true
        return this

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
