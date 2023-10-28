package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.pieces.Piece


interface ChessGameInterface {
    fun pieceAt(col : Int, row : Int, isFlipped : Boolean) : Piece?

    fun getPiecesMoves(piece : Piece) : ArrayList<Square>

    fun playMove(moveMessage : MoveMessage, isFlipped: Boolean)

    fun isOnLastMove() : Boolean

    fun goToLastMove()

}