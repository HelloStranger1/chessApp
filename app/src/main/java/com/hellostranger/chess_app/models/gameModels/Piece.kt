package com.hellostranger.chess_app.models.gameModels

import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.PieceType

data class Piece(
    val color: Color,
    var hasMoved: Boolean,
    val pieceType: PieceType,
    var resID : Int = -1
)