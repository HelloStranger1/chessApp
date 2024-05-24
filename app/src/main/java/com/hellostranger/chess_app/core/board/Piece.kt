package com.hellostranger.chess_app.core.board

object Piece {
    // Piece types
    const val NONE: Int = 0
    const val PAWN: Int = 1
    const val KNIGHT: Int = 2
    const val BISHOP: Int = 3
    const val ROOK: Int = 4
    const val QUEEN: Int = 5
    const val KING: Int = 6

    // Piece Colours
    const val WHITE: Int = 0
    const val BLACK: Int = 8

    //Pieces
    const val WHITE_PAWN: Int = PAWN or WHITE // = 1
    const val WHITE_KNIGHT: Int = KNIGHT or WHITE // = 2
    const val WHITE_BISHOP: Int = BISHOP or WHITE // = 3
    const val WHITE_ROOK: Int = ROOK or WHITE // = 4
    const val WHITE_QUEEN: Int = QUEEN or WHITE // = 5
    const val WHITE_KING: Int = KING or WHITE // = 6

    const val BLACK_PAWN: Int = PAWN or BLACK // = 9
    const val BLACK_KNIGHT: Int = KNIGHT or BLACK // = 10
    const val BLACK_BISHOP: Int = BISHOP or BLACK // = 11
    const val BLACK_ROOK: Int = ROOK or BLACK // = 12
    const val BLACK_QUEEN: Int = QUEEN or BLACK // = 13
    const val BLACK_KING: Int = KING or BLACK // = 14

    var MaxPieceIndex: Int = BLACK_KING

    val pieceIndices: IntArray = intArrayOf(
        WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING,
        BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING
    )

    // Bit Masks
    private const val TYPE_MASK = 0b0111
    private const val COLOUR_MASK = 0b1000

    fun makePiece(pieceType: Int, pieceColour: Int): Int {
        return pieceType or pieceColour
    }

    fun makePiece(pieceType: Int, isWhite: Boolean): Int {
        return makePiece(pieceType, if (isWhite) WHITE else BLACK)
    }

    // Returns true if the given piece matches the given colour. If the piece is type 'None', result will always be false.
    fun isColour(piece: Int, colour: Int): Boolean {
        return (piece and COLOUR_MASK) == colour && piece != 0
    }

    fun isWhite(piece: Int): Boolean {
        return isColour(piece, WHITE)
    }

    fun pieceColour(piece: Int): Int {
        return piece and COLOUR_MASK
    }

    fun pieceType(piece: Int): Int {
        return piece and TYPE_MASK
    }

    // Rook or Queen
    fun isOrthogonalSlider(piece: Int): Boolean {
        return (piece and QUEEN) == QUEEN || (piece and ROOK) == ROOK
    }

    // Bishop or Queen
    fun isDiagonalSlider(piece: Int): Boolean {
        return (piece and QUEEN) == QUEEN || (piece and BISHOP) == BISHOP
    }

    // Bishop, Rook or Queen
    fun isSlidingPiece(piece: Int): Boolean {
        return (piece and 4) != 0
    }

    fun getSymbol(piece: Int): Char {
        val pieceType = pieceType(piece)
        var symbol = when (pieceType) {
            ROOK -> 'R'
            KNIGHT -> 'N'
            BISHOP -> 'B'
            QUEEN -> 'Q'
            KING -> 'K'
            PAWN -> 'P'
            else -> ' '
        }
        symbol = if (isWhite(piece)) symbol else symbol.lowercaseChar()
        return symbol
    }

    fun getPieceFromSymbol(symbol: Char): Int {
        return when (symbol.uppercaseChar()) {
            'R' -> ROOK
            'N' -> KNIGHT
            'B' -> BISHOP
            'Q' -> QUEEN
            'K' -> KING
            'P' -> PAWN
            else -> NONE
        }
    }
}
