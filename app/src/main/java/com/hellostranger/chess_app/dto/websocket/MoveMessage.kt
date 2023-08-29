package com.hellostranger.chess_app.dto.websocket

class MoveMessage(
     var playerEmail : String,
     val startCol : Int,
     val startRow: Int,
     val endCol : Int,
     val endRow: Int
     ) : WebSocketMessage(MessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage. Name = $playerEmail, startCol = $startCol, startRow = $startRow, endCol = $endCol, endRow = $endRow"
     }
}
