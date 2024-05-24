package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.dto.enums.WebsocketMessageType

class MoveMessage(
     var playerEmail : String,
     var move : Int
/*
     val startCol : Int,
     val startRow: Int,
     val endCol : Int,
     val endRow: Int,
     var moveType: MoveType
*/
     ) : WebSocketMessage(WebsocketMessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage"
//          return "MoveMessage. Name = $playerEmail, startCol = $startCol, startRow = $startRow, endCol = $endCol, endRow = $endRow, moveType: $moveType"
     }
}
