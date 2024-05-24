package com.hellostranger.chess_app.core.board

class Move(var moveValue: UShort) {

    companion object {
        // Flags
        const val NO_FLAG: Int = 0b0000
        const val EN_PASSANT_CAPTURE_FLAG: Int = 0b0001
        const val CASTLE_FLAG: Int = 0b0010
        const val PAWN_TWO_UP_FLAG: Int = 0b0011

        const val PROMOTE_TO_QUEEN_FLAG: Int = 0b0100
        const val PROMOTE_TO_KNIGHT_FLAG: Int = 0b0101
        const val PROMOTE_TO_ROOK_FLAG: Int = 0b0110
        const val PROMOTE_TO_BISHOP_FLAG: Int = 0b0111

        private const val START_SQUARE_MASK : UShort = 0b0000000000111111u
        private const val TARGET_SQUARE_MASK : UShort = 0b0000111111000000u
        private const val FLAG_MASK : UShort = 0b1111000000000000u

        val NullMove: Move
            get() = Move(0u)

        fun sameMove(a: Move, b: Move): Boolean {
            return a.moveValue == b.moveValue
        }


    }


    // Masks


    constructor(startSquare: Int, targetSquare: Int) : this((startSquare or (targetSquare shl 6)).toUShort())

    constructor(startSquare: Int, targetSquare: Int, flag: Int) : this((startSquare or (targetSquare shl 6) or (flag shl 12)).toUShort())

    val startSquare: Int
        get() = (moveValue and START_SQUARE_MASK).toInt()
    val targetSquare: Int
        get() = (moveValue and TARGET_SQUARE_MASK) shr 6

    var moveFlag: Int
        get() = moveValue shr 12
        set(flag) {
            moveValue = (moveValue and FLAG_MASK) or (flag shl 12).toUShort()
        }

    val isPromotion: Boolean
        get() = moveFlag >= PROMOTE_TO_QUEEN_FLAG

    val promotionPieceType: Int
        get() = when (moveFlag) {
            PROMOTE_TO_QUEEN_FLAG -> {
                Piece.QUEEN
            }

            PROMOTE_TO_ROOK_FLAG -> {
                Piece.ROOK
            }

            PROMOTE_TO_BISHOP_FLAG -> {
                Piece.BISHOP
            }

            PROMOTE_TO_KNIGHT_FLAG -> {
                Piece.KNIGHT
            }

            else -> {
                Piece.NONE
            }
        }

    val isNull: Boolean
        get() = this.moveValue == 0.toUShort()


    private infix fun UShort.shr(that : Int) = this.toInt().shr(that)

}
