package com.hellostranger.chess_app.chess_models

data class Square(
    val colIndex: Int,
    var piece: Piece?,
    val rowIndex: Int
)