package com.hellostranger.chess_app.dto.websocket

class DrawOfferMessage( val playerEmail : String, val isWhite : Boolean) : WebSocketMessage(
    WebsocketMessageType.DRAW_OFFER) {

    override fun toString(): String {
        return "Player $playerEmail offered a draw"
    }

}