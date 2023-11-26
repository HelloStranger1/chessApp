package com.hellostranger.chess_app.gameClasses.pieces

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.PieceType
import java.lang.reflect.Type


abstract class Piece(
    val color: Color,
    var hasMoved: Boolean,
    var pieceType: PieceType,
    var colIndex : Int,
    var rowIndex : Int,
    var resID : Int = -1
){
    fun move(target: Square) {
        colIndex = target.colIndex
        rowIndex = target.rowIndex
    }
    abstract fun getThreatenedSquares(board: Board) : ArrayList<Square>
    abstract fun getMovableSquares(board: Board) : ArrayList<Square>

}

class PieceJsonDeserializer : JsonDeserializer<Piece> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Piece {
        val jsonObject  = json?.asJsonObject

        return when(val pieceType : String = jsonObject?.get("pieceType")!!.asString){
            "KING" -> context!!.deserialize(jsonObject, King::class.java)
            "QUEEN" -> context!!.deserialize(jsonObject, Queen::class.java)
            "ROOK" -> context!!.deserialize(jsonObject, Rook::class.java)
            "BISHOP" -> context!!.deserialize(jsonObject, Bishop::class.java)
            "KNIGHT" -> context!!.deserialize(jsonObject, Knight::class.java)
            "PAWN" -> context!!.deserialize(jsonObject, Pawn::class.java)
            else -> {throw JsonParseException("Unknown piece type: $pieceType")
            }
        }

    }
}