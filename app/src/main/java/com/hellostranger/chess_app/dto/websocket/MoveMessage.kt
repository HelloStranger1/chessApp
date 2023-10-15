package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.models.gameModels.enums.PieceType

class MoveMessage(
     var playerEmail : String,
     val startCol : Int,
     val startRow: Int,
     val endCol : Int,
     val endRow: Int,
     val isSecondCastleMove : Boolean = false,
     var promotionType : PieceType? = null
     ) : WebSocketMessage(MessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage. Name = $playerEmail, startCol = $startCol, startRow = $startRow, endCol = $endCol, endRow = $endRow, promotion: $promotionType, isSecondCastle: $isSecondCastleMove"
     }
}
