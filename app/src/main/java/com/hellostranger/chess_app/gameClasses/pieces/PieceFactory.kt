package com.hellostranger.chess_app.gameClasses.pieces

import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color

import com.hellostranger.chess_app.gameClasses.enums.PieceType




class PieceFactory {
    fun getPiece(pieceType: PieceType, color: Color, square: Square): Piece {
        return when (pieceType) {
            PieceType.KING -> King(color, false, square)
            PieceType.QUEEN -> Queen(color, false, square)
            PieceType.ROOK -> Rook(color, false, square)
            PieceType.BISHOP -> Bishop(color, false, square)
            PieceType.KNIGHT -> Knight(color, false, square)
            PieceType.PAWN -> Pawn(color, false, square)
        }
    }

    fun getPiece(pieceType: PieceType, color: Color, square: Square, hasMoved: Boolean): Piece {
        return when (pieceType) {
            PieceType.KING -> King(color, hasMoved, square)
            PieceType.QUEEN -> Queen(color, hasMoved, square)
            PieceType.ROOK -> Rook(color, hasMoved, square)
            PieceType.BISHOP -> Bishop(color, hasMoved, square)
            PieceType.KNIGHT -> Knight(color, hasMoved, square)
            PieceType.PAWN -> Pawn(color, hasMoved, square)
        }
    }
}