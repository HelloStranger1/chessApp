package com.hellostranger.chess_app.dto.websocket

class MoveMessage(
     var playerName : String,
     val startCol : Int,
     val startRow: Int,
     val endCol : Int,
     val endRow: Int
     ) : WebSocketMessage(MessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage. Name = $playerName, startCol = $startCol, startRow = $startRow, endCol = $endCol, endRow = $endRow"
     }
}
