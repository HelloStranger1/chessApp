package com.hellostranger.chess_app.core.board

/**
 * Represents various relevant parts of a game state
 * @author Eyal Ben Natan
 */
class GameState(
    val capturedPieceType: Int = Piece.NONE,
    val enPassantFile: Int = 0,
    val castlingRights: Int = 0,
    val fiftyMoveCounter: Int = 0,
    val zobristKey: ULong = 0UL
) {
    companion object {

        const val CLEAR_WHITE_KINGSIDE_MASK  : Int = 0b1110
        const val CLEAR_WHITE_QUEENSIDE_MASK : Int = 0b1101
        const val CLEAR_BLACK_KINGSIDE_MASK  : Int = 0b1011
        const val CLEAR_BLACK_QUEENSIDE_MASK : Int = 0b0111
    }

    /**
     * Checks if the specified colour has king side castling rights
     */
    fun hasKingSideCastleRights(isWhite: Boolean): Boolean {
        val mask = if (isWhite) 1 else 4
        return (castlingRights and mask) != 0
    }

    /**
     * Checks if the specified colour has queen side castling rights
     */
    fun hasQueenSideCastleRights(isWhite: Boolean): Boolean {
        val mask = if (isWhite) 2 else 8
        return (castlingRights and mask) != 0
    }
}
