package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.models.gameModels.Piece


interface ChessGameInterface {
    fun pieceAt(col : Int, row : Int, isFlipped : Boolean) : Piece?

    fun playMove(moveMessage : MoveMessage, isFlipped: Boolean)

    fun isOnLastMove() : Boolean

    fun goToLastMove()

    /*fun showPreviousBoard() : Boolean

    fun showNextBoard() : Boolean*/
}