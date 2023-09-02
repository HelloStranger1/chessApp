package com.hellostranger.chess_app.models.gameModels

data class Square(
    val colIndex: Int,
    var piece: Piece?,
    val rowIndex: Int
)