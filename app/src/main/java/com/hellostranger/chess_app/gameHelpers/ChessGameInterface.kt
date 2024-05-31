package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord


interface ChessGameInterface {
    fun playMove(startCoord: Coord, endCoord: Coord)
    fun getBoard() : Board?
}