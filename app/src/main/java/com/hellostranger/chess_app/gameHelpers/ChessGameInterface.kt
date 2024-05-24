package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord


interface ChessGameInterface {
//    fun pieceAt(col : Int, row : Int, isFlipped : Boolean) : Piece?
//
//    fun getPiecesMoves(piece : Piece) : ArrayList<Square>

    fun playMove(startCoord: Coord, endCoord: Coord)
    fun getBoard() : Board?

//    fun isOnLastMove() : Boolean
//
//    fun goToLastMove()

//    fun getLastMovePlayed() : MoveMessage?




}