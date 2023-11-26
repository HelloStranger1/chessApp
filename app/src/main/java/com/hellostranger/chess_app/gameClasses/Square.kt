package com.hellostranger.chess_app.gameClasses

import com.hellostranger.chess_app.gameClasses.pieces.Piece

data class Square(
    val colIndex: Int,
    var piece: Piece?,
    val rowIndex: Int
){
    override fun toString(): String {
        return "($colIndex, $rowIndex). Piece: $piece"
    }
}