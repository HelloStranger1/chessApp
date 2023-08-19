package com.hellostranger.chess_app.chess_models

data class Piece(
    val color: Color,
    var hasMoved: Boolean,
    val pieceType: PieceType,
    var resID : Int = -1
)