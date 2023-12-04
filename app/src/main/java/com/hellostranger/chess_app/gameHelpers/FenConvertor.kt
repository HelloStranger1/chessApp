package com.hellostranger.chess_app.gameHelpers

import android.util.Log
import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.GameState
import com.hellostranger.chess_app.gameClasses.enums.PieceType
import com.hellostranger.chess_app.gameClasses.pieces.King
import com.hellostranger.chess_app.gameClasses.pieces.Piece
import com.hellostranger.chess_app.gameClasses.pieces.PieceFactory
import java.util.UUID


class FenConvertor {
    fun convertFENToGame(fen1: String): Game {
        var fen = fen1
        val pieceFactory = PieceFactory()
        val squaresArray : Array<Array<Square>> = Array(8) { row ->
            Array(8) {col ->
                Square(col, null, row)
            } }
        val board = Board(squaresArray)
        val game = Game(board, "", true, GameState.ACTIVE)
        game.id = (UUID.randomUUID().toString())
        var col = 0
        var row = 7
        while (row >= 0) {
            if (row == 0 && col == 8) {
                row--
                continue
            }
            val currentChar = fen[0]
            fen = fen.substring(1)
            if (currentChar == '/') {
                row--
                col = 0
                continue
            }
            if (currentChar == ' ') {
                continue
            }
            try {
                col += currentChar.toString().toInt()
                continue
            } catch (ignored: NumberFormatException) {
            }
            //currentChar is a piece
            val pieceColor: Color = if (Character.isUpperCase(currentChar)) {
                Color.WHITE
            } else {
                Color.BLACK
            }
            var pieceType: PieceType? = null
            var hasMoved = true
            when (currentChar) {
                'Q', 'q' -> pieceType = PieceType.QUEEN
                'R', 'r' -> pieceType = PieceType.ROOK
                'B', 'b' -> pieceType = PieceType.BISHOP
                'N', 'n' -> pieceType = PieceType.KNIGHT
                'K', 'k' -> pieceType = PieceType.KING
                'P', 'p' -> {
                    pieceType = PieceType.PAWN
                    if (pieceColor === Color.WHITE && row == 1 || pieceColor === Color.BLACK && row == 6) {
                        hasMoved = false
                    }
                }

                else -> Log.e("FenConvertor","\n convertFromFen failed since the char: $currentChar  isn't actually a piece")
            }

            val curPiece: Piece =
                pieceFactory.getPiece(pieceType!!, pieceColor, board.getSquareAt(col, row)!!, hasMoved)
            if (curPiece.pieceType === PieceType.KING) {
                if (curPiece.color === Color.WHITE) {
                    board.whiteKing = curPiece as King
                } else {
                    board.blackKing = curPiece as King
                }
            }
            board.setPieceAt(col, row, curPiece)
            col++
        }
        //We should have finished all the pieces.

        var curChar = fen[1]
        fen = fen.substring(2)
        game.isP1Turn = (curChar == 'w')
        curChar = fen[1]
        fen = fen.substring(1)
        Log.e("TAG","fen before checking castle is: $fen and cur char is: $curChar")
        if (curChar != '-') {
            while (curChar != ' ') {
                when (curChar) {
                    'Q' -> {
                        board.getSquareAt(4, 0)!!.piece!!.hasMoved = false //White King
                        board.getSquareAt(0, 0)!!.piece!!.hasMoved = false //White Rook on a1
                    }
                    'q' -> {
                        board.getSquareAt(4, 7)!!.piece!!.hasMoved = false //Black King
                        board.getSquareAt(0, 7)!!.piece!!.hasMoved = false //Black Rook on a8
                    }
                    'K' -> {
                        board.getSquareAt(4, 0)!!.piece!!.hasMoved = false //White King
                        board.getSquareAt(7, 0)!!.piece!!.hasMoved = false //White Rook on h1
                    }
                    'k' -> {
                        board.getSquareAt(4, 7)!!.piece!!.hasMoved = false //Black King
                        board.getSquareAt(7, 7)!!.piece!!.hasMoved = false //Black Rook on h8
                    }
                }
                curChar = fen[1]
                fen = fen.substring(1)
                Log.e("TAG","FEN $fen, crchar $curChar")
            }
        } else{
            fen = fen.substring(1)
        }
        //En-peasant

        Log.e("TAG", "FEN before en passant is: $fen")
        fen = if (fen[1] == '-') {
            board.phantomPawnSquare = (null)
            fen.substring(3)
        } else {
            val phantomPawnCol = when (fen[1]) {
                'a' -> 0
                'b' -> 1
                'c' -> 2
                'd' -> 3
                'e' -> 4
                'f' -> 5
                'g' -> 6
                'h' -> 7
                else -> -1
            }
            if (phantomPawnCol == -1) {
                Log.e("FenConvertor","convertFromFen failed since the col: ${fen[1]}  isn't a col in algebraic notation")
            }
            val phantomPawnRow = fen[2].toString().toInt()
            board.phantomPawnSquare = (board.getSquareAt(phantomPawnCol, phantomPawnRow))
            fen.substring(4)
        }
        //Only thing left is half-moves and full-moves.

        return game
    }


}