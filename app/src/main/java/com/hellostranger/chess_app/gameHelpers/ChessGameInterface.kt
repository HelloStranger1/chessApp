package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.pieces.Piece


interface ChessGameInterface {
    fun pieceAt(col : Int, row : Int, isFlipped : Boolean) : Piece?

    fun getPiecesMoves(piece : Piece) : ArrayList<Square>

    fun playMove(moveMessage : MoveMessage, isFlipped: Boolean)

    fun isOnLastMove() : Boolean

    fun goToLastMove()

    fun getLastMovePlayed() : MoveMessage?



}