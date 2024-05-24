package com.hellostranger.chess_app.gameHelpers

import android.util.Log
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.utils.MyApp

class PieceResIDs(private val pieceTheme: MyApp.PieceTheme) {

    private fun getWhitePieces() = if (pieceTheme == MyApp.PieceTheme.PLANT) whitePlantPieces else whiteRegularPieces
    private fun getBlackPieces() = if (pieceTheme == MyApp.PieceTheme.PLANT) blackPlantPieces else blackRegularPieces
    fun getPieceSprite(piece : Int) : Int {
        val pieceSprites = if (Piece.isWhite(piece)) getWhitePieces() else getBlackPieces()
        return when (Piece.pieceType(piece)) {
            Piece.PAWN   -> pieceSprites[5]
            Piece.BISHOP -> pieceSprites[4]
            Piece.KNIGHT -> pieceSprites[3]
            Piece.ROOK   -> pieceSprites[2]
            Piece.QUEEN  -> pieceSprites[1]
            Piece.KING   -> pieceSprites[0]
            else -> {
                Log.e("PIECE_SPRITES", "Invalid piece type")
                return -1
            }
        }

    }



    companion object {
        val whitePlantPieces = arrayOf(
            R.drawable.ic_white_king_plant,
            R.drawable.ic_white_queen,
            R.drawable.ic_white_rook_plant,
            R.drawable.ic_white_knight_plant,
            R.drawable.ic_white_bishop_plant,
            R.drawable.ic_white_pawn_plant,
        )
        val blackPlantPieces = arrayOf(
            R.drawable.ic_black_king_plant,
            R.drawable.ic_black_queen,
            R.drawable.ic_black_rook_plant,
            R.drawable.ic_black_knight_plant,
            R.drawable.ic_black_bishop_plant,
            R.drawable.ic_black_pawn_plant,
        )
        val whiteRegularPieces = arrayOf(
            R.drawable.ic_white_king,
            R.drawable.ic_white_queen,
            R.drawable.ic_white_rook,
            R.drawable.ic_white_knight,
            R.drawable.ic_white_bishop,
            R.drawable.ic_white_pawn,
        )
        val blackRegularPieces = arrayOf(
            R.drawable.ic_black_king,
            R.drawable.ic_black_queen,
            R.drawable.ic_black_rook,
            R.drawable.ic_black_knight,
            R.drawable.ic_black_bishop,
            R.drawable.ic_black_pawn,
        )
    }

}