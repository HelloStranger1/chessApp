package com.hellostranger.chess_app

import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.chess_models.Piece


interface ChessDelegate {
    fun pieceAt(col : Int, row : Int) : Piece?

    fun playMove(moveMessage : MoveMessage)
}