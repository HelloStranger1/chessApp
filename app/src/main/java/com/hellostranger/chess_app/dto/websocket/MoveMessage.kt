package com.hellostranger.chess_app.dto.websocket

import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.helpers.MoveUtility

@ExperimentalUnsignedTypes
class MoveMessage(
     private var playerEmail : String,
     var move : Int
     ) : WebSocketMessage(WebsocketMessageType.MOVE){

     override fun toString(): String {
          return "MoveMessage. email: $playerEmail. move in UCI: ${MoveUtility.getMoveNameUCI(Move(move.toUShort()))}"
     }
}
