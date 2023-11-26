package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.dto.enums.WebsocketMessageType
import com.hellostranger.chess_app.dto.enums.MoveType

class MoveMessage(
     var playerEmail : String,
     val startCol : Int,
     val startRow: Int,
     val endCol : Int,
     val endRow: Int,
     var moveType: MoveType
     ) : WebSocketMessage(WebsocketMessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage. Name = $playerEmail, startCol = $startCol, startRow = $startRow, endCol = $endCol, endRow = $endRow, moveType: $moveType"
     }
}
