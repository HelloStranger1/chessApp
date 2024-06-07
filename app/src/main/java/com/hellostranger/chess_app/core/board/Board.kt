@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package com.hellostranger.chess_app.core.board

import android.util.Log
import com.hellostranger.chess_app.core.moveGeneration.bitboards.BitBoardUtility
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.helpers.FenUtility
import com.hellostranger.chess_app.core.helpers.FenUtility.PositionInfo
import java.util.*


@OptIn(ExperimentalUnsignedTypes::class)
/**
 * Represents a chess board and manages the state of the game.
 * Use createBoard to create an object.
 * @author Eyal Ben Natan
 */
class Board {
    companion object {
        const val WHITE_INDEX: Int = 0
        const val BLACK_INDEX: Int = 1

        /**
         * Creates a board and loads into it the fen position.
         * @param fen: The fen position to be loaded in. if empty, is the starting board
         * @return A board object with the position loaded in.
         */
        fun createBoard(fen : String = FenUtility.START_POSITION_FEN) : Board {
            val board = Board()
            board.loadPosition(fen)
            return board
        }

        /**
         * Creates a copy of an existing board.
         * @param source The board to copy.
         * @return A new board object that is a copy of the source board.
         */
        fun createBoard(source : Board) : Board {
            val board = Board()
            board.loadPosition(source.startPositionInfo)

            for (i in 0 until source.allGameMoves.count()) {
                board.makeMove(source.allGameMoves[i])
            }
            return board
        }
    }

    // Stores piece code for each square on the board
    val square: IntArray = IntArray(64)

    // Stores index of white and black king
    lateinit var kingSquare: IntArray

    /* Bitboards */
    // Bitboard for each piece type anc colour
    var pieceBitboards: ULongArray? = null

    // Bitboard for all pieces of either colour
    var colourBitboards: ULongArray? = null
    var allPiecesBitboard: ULong = 0UL
    var friendlyOrthogonalSliders: ULong = 0UL
    var friendlyDiagonalSliders: ULong = 0UL
    var enemyOrthogonalSliders: ULong = 0UL
    var enemyDiagonalSliders: ULong = 0UL

    // Piece count excluding pawns and kings
    private var totalPieceCountWithoutPawnsAndKings: Int = 0

    /* Piece lists */
    lateinit var rooks: Array<PieceList>
    lateinit var bishops: Array<PieceList>
    lateinit var queens: Array<PieceList>
    lateinit var knights: Array<PieceList>
    lateinit var pawns: Array<PieceList>

    /* Side to move info */
    var isWhiteToMove: Boolean = false
    val moveColour: Int
        get() = if (isWhiteToMove) Piece.WHITE else Piece.BLACK
    val opponentColour: Int
        get() = if (isWhiteToMove) Piece.BLACK else Piece.WHITE
    val moveColourIndex: Int
        get() = if (isWhiteToMove) WHITE_INDEX else BLACK_INDEX
    private val opponentColourIndex : Int
        get() = if (isWhiteToMove) BLACK_INDEX else WHITE_INDEX

    // List of hashed positions since last pawn move or capture (For repetitions)
    var repetitionPositionHistory: Stack<ULong>? = null

    // Total half-moves played
    var plyCount: Int = 0
    val fiftyMoveCount : Int
        get() = currentGameState.fiftyMoveCounter
    lateinit var currentGameState: GameState
    val zobristKey : ULong
        get() = currentGameState.zobristKey

    lateinit var allGameMoves: MutableList<Move>

    private lateinit var allPieceLists: Array<PieceList?>
    private lateinit var gameStateHistory: Stack<GameState>
    lateinit var startPositionInfo : PositionInfo
    private var cachedInCheckValue: Boolean = false
    private var hasCachedInCheckValue: Boolean = false

    /**
     * Make a move on the board
     * @param move: The move to play
     * @param inSearch: controls whether this move should be recorded in game history
     */
    fun makeMove(move: Move, inSearch: Boolean = false) {
        val startSquare = move.startSquare
        val targetSquare = move.targetSquare
        val moveFlag = move.moveFlag
        val isPromotion = move.isPromotion
        val isEnPassant = moveFlag == Move.EN_PASSANT_CAPTURE_FLAG

        val movedPiece = square[startSquare]
        val movedPieceType = Piece.pieceType(movedPiece)
        val capturedPiece = if (isEnPassant) Piece.makePiece(Piece.PAWN, opponentColour) else square[targetSquare]
        val capturedPieceType = Piece.pieceType(capturedPiece)

        val prevCastleState = currentGameState.castlingRights
        val prevEnPassantFile = currentGameState.enPassantFile
        var newZobristKey = currentGameState.zobristKey
        var newCastleRights = prevCastleState
        var newEnPassantFile = 0

        // Update bitboard of moved piece
        movePiece(movedPiece, startSquare, targetSquare)

        // Handle Captures
        if (capturedPieceType != Piece.NONE) {
            if (capturedPieceType != Piece.PAWN) {
                totalPieceCountWithoutPawnsAndKings--
            }
            newZobristKey =
                handleCaptures(targetSquare, isEnPassant, capturedPiece, newZobristKey)
        }

        // Handle King
        if (movedPieceType == Piece.KING) {
            kingSquare[moveColourIndex] = targetSquare
            newCastleRights = newCastleRights and if (isWhiteToMove) 0b1100 else 0b0011

            // Handle castling
            if (moveFlag == Move.CASTLE_FLAG) {
                newZobristKey = handleCastling(targetSquare, newZobristKey)
            }
        }

        // Handle promotion
        if (isPromotion) {
            handlePromotion(move, movedPiece, targetSquare)
        }

        // Pawn moved 2 forward, mark this file with en passant flag
        if (moveFlag == Move.PAWN_TWO_UP_FLAG) {
            val file : Int = BoardHelper.fileIndex(startSquare) + 1
            newEnPassantFile = file
            newZobristKey = newZobristKey xor Zobrist.enPassantFile[file]
        }

        // Update castling rights
        if (prevCastleState != 0) {
            newCastleRights = updateCastlingRights(targetSquare, startSquare, newCastleRights)
        }

        // Update zobrist key with new piece position and side to move
        newZobristKey = newZobristKey xor Zobrist.sideToMove
        newZobristKey = newZobristKey xor Zobrist.piecesArray[movedPiece][startSquare]
        newZobristKey = newZobristKey xor Zobrist.piecesArray[square[targetSquare]][targetSquare]
        newZobristKey = newZobristKey xor Zobrist.enPassantFile[prevEnPassantFile]

        if (newCastleRights != prevCastleState) {
            newZobristKey = newZobristKey xor Zobrist.castlingRights[prevCastleState]
            newZobristKey = newZobristKey xor Zobrist.castlingRights[newCastleRights]
        }

        // Change side to move
        isWhiteToMove = !isWhiteToMove

        plyCount++
        var newFiftyMoveCounter = currentGameState.fiftyMoveCounter + 1

        // Update extra bitboards
        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX]
        updateSliderBitboards()

        // Pawn moves and captures reset the fifty move counter and clear 3-fold history
        if (movedPieceType == Piece.PAWN || capturedPieceType != Piece.NONE) {
            if (!inSearch) {
                repetitionPositionHistory!!.clear()
            }
            newFiftyMoveCounter = 0
        }

        // Updates the game state
        val newState = GameState(capturedPieceType, newEnPassantFile, newCastleRights, newFiftyMoveCounter, newZobristKey)
        gameStateHistory.push(newState)
        currentGameState = newState
        hasCachedInCheckValue = false

        if (!inSearch) {
            repetitionPositionHistory!!.push(newState.zobristKey)
            allGameMoves.add(move)
        }
    }

    /**
     * Returns an updated castling rights.
     * @param targetSquare The target square of the move.
     * @param startSquare The start square of the move.
     * @param castleRights The current castling rights.
     * @return The updated castling rights.
     */
    private fun updateCastlingRights(
        targetSquare: Int,
        startSquare: Int,
        castleRights: Int
    ): Int {
        // Any piece moving to/from rook square removes castling right for that side
        var newCastleRights = castleRights
        if (targetSquare == BoardHelper.H_1 || startSquare == BoardHelper.H_1) {
            newCastleRights = newCastleRights and GameState.CLEAR_WHITE_KINGSIDE_MASK
        } else if (targetSquare == BoardHelper.A_1 || startSquare == BoardHelper.A_1) {
            newCastleRights = newCastleRights and GameState.CLEAR_WHITE_QUEENSIDE_MASK
        }
        if (targetSquare == BoardHelper.H_8 || startSquare == BoardHelper.H_8) {
            newCastleRights = newCastleRights and GameState.CLEAR_BLACK_KINGSIDE_MASK
        } else if (targetSquare == BoardHelper.A_8 || startSquare == BoardHelper.A_8) {
            newCastleRights = newCastleRights and GameState.CLEAR_BLACK_QUEENSIDE_MASK
        }
        return newCastleRights
    }


    /**
     * handles promoting a pawn.
     * @param move: The moved just played
     * @param movedPiece: The old piece
     * @param targetSquare: The target Square
     */
    private fun handlePromotion(
        move: Move,
        movedPiece: Int,
        targetSquare: Int
    ) {
        totalPieceCountWithoutPawnsAndKings++
        val promotionPieceType: Int = move.promotionPieceType
        val promotionPiece = Piece.makePiece(promotionPieceType, moveColour)
        allPieceLists[movedPiece]!!.removePieceAtSquare(targetSquare)
        allPieceLists[promotionPiece]!!.addPieceAtSquare(targetSquare)
        square[targetSquare] = promotionPiece

        pieceBitboards!![movedPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![movedPiece], targetSquare)
        pieceBitboards!![promotionPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![promotionPiece], targetSquare)
    }

    /**
     * Handles the remaining parts of castling after moving the king.
     * updates relevant bitboards, castling rights, and the zobrist key.
     * @param targetSquare: The targetSquare of the rook
     * @param zobristKey: The old zobrist key
     * @return The new zobrist key
     */
    private fun handleCastling(targetSquare: Int, zobristKey: ULong): ULong {
        var newZobristKey = zobristKey
        val rookPiece: Int = Piece.makePiece(Piece.ROOK, moveColour)
        val kingside: Boolean = targetSquare == BoardHelper.G_1 || targetSquare == BoardHelper.G_8
        val castlingRookFromIndex = if (kingside) targetSquare + 1 else targetSquare - 2
        val castlingRookToIndex = if (kingside) targetSquare - 1 else targetSquare + 1

        // Update Rook Position
        pieceBitboards!![rookPiece] = BitBoardUtility.toggleSquares(pieceBitboards!![rookPiece], castlingRookFromIndex, castlingRookToIndex)
        colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], castlingRookFromIndex, castlingRookToIndex)
        allPieceLists[rookPiece]!!.movePiece(castlingRookFromIndex, castlingRookToIndex)
        square[castlingRookFromIndex] = Piece.NONE
        square[castlingRookToIndex] = Piece.ROOK or moveColour

        newZobristKey = newZobristKey xor Zobrist.piecesArray[rookPiece][castlingRookFromIndex]
        newZobristKey = newZobristKey xor Zobrist.piecesArray[rookPiece][castlingRookToIndex]

        return newZobristKey
    }

    /**
     * Handles the remaining parts of a capture after moving piece
     * updates relevant bitboards, removes the captured piece, and updates the zobrist key.
     * @param targetSquare: The targetSquare of the rook
     * @param isEnPassant: States if the move is enPassant
     * @param capturedPiece: The captured piece to be removed.
     * @param zobristKey: The old zobrist key
     * @return The new zobrist key
     */
    private fun handleCaptures(
        targetSquare: Int,
        isEnPassant: Boolean,
        capturedPiece: Int,
        zobristKey: ULong
    ): ULong {
        var captureSquare = targetSquare
        if (isEnPassant) {
            captureSquare = targetSquare + (if (isWhiteToMove) -8 else 8)
            square[captureSquare] = Piece.NONE
        }

        // Remove captured piece from bitboards/piece list
        allPieceLists[capturedPiece]!!.removePieceAtSquare(captureSquare)
        pieceBitboards!![capturedPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![capturedPiece], captureSquare)
        colourBitboards!![opponentColourIndex] =
            BitBoardUtility.toggleSquare(colourBitboards!![opponentColourIndex], captureSquare)
        return zobristKey xor Zobrist.piecesArray[capturedPiece][captureSquare]
    }

    /**
     * Undo a move that was just played on the board.
     * @param move: The move to undo
     * @param inSearch: controls whether this move should be recorded in game history
     */
    fun unmakeMove(move: Move, inSearch: Boolean = false) {
        // Swap colours to move
        isWhiteToMove = !isWhiteToMove

        val undoingWhiteMove : Boolean = isWhiteToMove

        // Get Move info
        val movedFrom = move.startSquare
        val movedTo = move.targetSquare
        val moveFlag = move.moveFlag

        val undoingEnPassant = moveFlag == Move.EN_PASSANT_CAPTURE_FLAG
        val undoingPromotion = move.isPromotion
        val undoingCapture = currentGameState.capturedPieceType != Piece.NONE

        val movedPiece = if (undoingPromotion) Piece.makePiece(Piece.PAWN, moveColour) else square[movedTo]
        val movedPieceType = Piece.pieceType(movedPiece)
        val capturedPieceType = currentGameState.capturedPieceType

        // If undoing promotion, then remove piece from promotion square and replace with pawn
        if (undoingPromotion) {
            val promotedPiece = square[movedTo]
            val pawnPiece = Piece.makePiece(Piece.PAWN, moveColour)
            totalPieceCountWithoutPawnsAndKings--

            allPieceLists[promotedPiece]!!.removePieceAtSquare(movedTo)
            allPieceLists[movedPiece]!!.addPieceAtSquare(movedTo)
            pieceBitboards!![promotedPiece] = BitBoardUtility.toggleSquare(pieceBitboards!![promotedPiece], movedTo)
            pieceBitboards!![pawnPiece] = BitBoardUtility.toggleSquare(pieceBitboards!![pawnPiece], movedTo)
        }

        movePiece(movedPiece, movedTo, movedFrom)

        // Undo capture
        if (undoingCapture) {
            var captureSquare = movedTo
            val capturedPiece = Piece.makePiece(capturedPieceType, opponentColour)

            if (undoingEnPassant) {
                captureSquare = movedTo + if (undoingWhiteMove) -8 else 8
            }
            if (capturedPieceType != Piece.PAWN) {
                totalPieceCountWithoutPawnsAndKings++
            }

            pieceBitboards!![capturedPiece]        = BitBoardUtility.toggleSquare(pieceBitboards!![capturedPiece], captureSquare)
            colourBitboards!![opponentColourIndex] = BitBoardUtility.toggleSquare(colourBitboards!![opponentColourIndex], captureSquare)
            allPieceLists[capturedPiece]!!.addPieceAtSquare(captureSquare)
            square[captureSquare] = capturedPiece
        }

        // Update King
        if (movedPieceType == Piece.KING) {
            kingSquare[moveColourIndex] = movedFrom

            // Undo castle
            if (moveFlag == Move.CASTLE_FLAG) {
                val rookPiece = Piece.makePiece(Piece.ROOK, moveColour)
                val kingside = movedTo == BoardHelper.G_1 || movedTo == BoardHelper.G_8
                val rookSquareBeforeCastling = if (kingside) movedTo + 1 else movedTo - 2
                val rookSquareAfterCastling = if (kingside) movedTo - 1 else movedTo + 1

                pieceBitboards!![rookPiece]        = BitBoardUtility.toggleSquares(pieceBitboards!![rookPiece], rookSquareAfterCastling, rookSquareBeforeCastling)
                colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], rookSquareAfterCastling, rookSquareBeforeCastling)
                square[rookSquareAfterCastling]  = Piece.NONE
                square[rookSquareBeforeCastling] = rookPiece
                allPieceLists[rookPiece]!!.movePiece(rookSquareAfterCastling, rookSquareBeforeCastling)

            }
        }

        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX]
        updateSliderBitboards()

        if (!inSearch && repetitionPositionHistory!!.isNotEmpty()) {
            repetitionPositionHistory!!.pop()
        }
        if (!inSearch) {
            allGameMoves.removeLast()
        }

        // Go back to previous state
        gameStateHistory.pop()
        currentGameState = gameStateHistory.peek()
        plyCount--
        hasCachedInCheckValue = false
    }

    /**
     * Used to switch the side to play without making a move.
     */
    fun makeNullMove() {
        isWhiteToMove = !isWhiteToMove

        plyCount++

        var newZobristKey = currentGameState.zobristKey
        newZobristKey = newZobristKey xor Zobrist.sideToMove
        newZobristKey = newZobristKey xor Zobrist.enPassantFile[currentGameState.enPassantFile]

        val newState = GameState(Piece.NONE, 0, currentGameState.castlingRights, currentGameState.fiftyMoveCounter + 1, newZobristKey)
        currentGameState = newState
        gameStateHistory.push(currentGameState)
        updateSliderBitboards()
        hasCachedInCheckValue = true
        cachedInCheckValue = false
    }


    /**
     * Unmake a null move
     */
    fun unmakeNullMove() {
        isWhiteToMove = !isWhiteToMove
        plyCount--
        gameStateHistory.pop()
        currentGameState = gameStateHistory.peek()
        updateSliderBitboards()
        hasCachedInCheckValue = true
        cachedInCheckValue = false
    }

    /**
     * Checks If current player in check?
     * Note: caches check value so calling multiple times does not require recalculating
     */
    fun isInCheck() : Boolean {
        if (hasCachedInCheckValue) {
            return cachedInCheckValue
        }
        cachedInCheckValue = calculateInCheckState()
        hasCachedInCheckValue = true

        return cachedInCheckValue
    }

    /**
     * Calculate in check value
     * Call IsInCheck instead for automatic caching of value
     */
    fun calculateInCheckState() : Boolean {
        val kingSquare = kingSquare[moveColourIndex]
        val blockers : ULong = allPiecesBitboard

        if (enemyOrthogonalSliders != 0UL) {
            val rookAttacks : ULong = BitBoardUtility.getRookAttacks(kingSquare, blockers)
            if ((rookAttacks and enemyOrthogonalSliders) != 0UL) {
                return true
            }
        }
        if (enemyDiagonalSliders != 0UL) {
            val bishopAttacks : ULong = BitBoardUtility.getBishopAttacks(kingSquare, blockers)
            if ((bishopAttacks and enemyDiagonalSliders) != 0UL) {
                return true
            }
        }
        val enemyKnights = pieceBitboards!![Piece.makePiece(Piece.KNIGHT, opponentColour)]
        if ((BitBoardUtility.knightAttacks[kingSquare] and enemyKnights) != 0UL) {
            return true
        }

        val enemyPawns = pieceBitboards!![Piece.makePiece(Piece.PAWN, opponentColour)]
        val pawnAttackMasks : ULong = if (isWhiteToMove) BitBoardUtility.whitePawnAttacks[kingSquare] else BitBoardUtility.blackPawnAttacks[kingSquare]
        return (pawnAttackMasks and enemyPawns) != 0UL
    }

    /**
     * Loads a position from a fen
     */
    fun loadPosition(fen : String) {
        val posInfo = FenUtility.positionFromFen(fen)
        loadPosition(posInfo)
    }


    /**
     * Loads a position into the board from PositionInfo
     */
    fun loadPosition(posInfo : PositionInfo) {
        startPositionInfo = posInfo
        initialize()

        for (squareIndex in 0 until 64) {
            val piece = posInfo.squares[squareIndex]
            val pieceType = Piece.pieceType(piece)
            val colourIndex = if (Piece.isWhite(piece)) WHITE_INDEX else BLACK_INDEX
            square[squareIndex] = piece

            if (piece != Piece.NONE) {
                pieceBitboards!![piece]        = BitBoardUtility.setSquare(pieceBitboards!![piece], squareIndex)
                colourBitboards!![colourIndex] = BitBoardUtility.setSquare(colourBitboards!![colourIndex], squareIndex)

                if (pieceType == Piece.KING) {
                    kingSquare[colourIndex] = squareIndex
                } else {
                    allPieceLists[piece]!!.addPieceAtSquare(squareIndex)
                }
                if (pieceType != Piece.PAWN && pieceType != Piece.KING) {
                    totalPieceCountWithoutPawnsAndKings++
                }

            }
        }

        // Side to move
        isWhiteToMove = posInfo.whiteToMove

        // Set extra bitboards
        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX]
        updateSliderBitboards()

        // Create game state
        val whiteCastle =
            (if ((posInfo.whiteCastleKingside)) 1 shl 0 else 0) or (if ((posInfo.whiteCastleQueenside)) 1 shl 1 else 0)
        val blackCastle =
            (if ((posInfo.blackCastleKingside)) 1 shl 2 else 0) or (if ((posInfo.blackCastleQueenside)) 1 shl 3 else 0)
        val castlingRights = whiteCastle or blackCastle

        plyCount = (posInfo.moveCount - 1) * 2 + if (isWhiteToMove) 0 else 1

        // Set game state (note: calculating zobrist key relies on current game state)
        currentGameState = GameState(Piece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, 0UL)
        val newZobristKey = Zobrist.calculateZobristKey(this)
        currentGameState = GameState(Piece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, newZobristKey)

        repetitionPositionHistory!!.push(newZobristKey)

        gameStateHistory.push(currentGameState)
    }


    /**
     * Update piece lists / bitboards based on given move info.
     * Doesn't account for the following things:
     * 1. removal of captured piece
     * 2. Moving the rook when castling
     * 3. Removal of pawn from 1st/8th rank on promotion
     * 4. Addition of promoted piece on promotion
     *
     * @param piece: The piece to move
     * @param startSquare: The start square
     * @param targetSquare: The target square
     */
    private fun movePiece(piece: Int, startSquare: Int, targetSquare: Int) {
        pieceBitboards!![piece] = BitBoardUtility.toggleSquares(pieceBitboards!![piece], startSquare, targetSquare)
        colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], startSquare, targetSquare)

        if (allPieceLists[piece] == null) {
            Log.e("BOARD", "WTF. the piece is: ${Piece.getSymbol(piece)}.")
        }
        allPieceLists[piece]!!.movePiece(startSquare, targetSquare)
        square[startSquare] = Piece.NONE
        square[targetSquare] = piece
    }

    // Updates bitboards for all slider pieces (Queens, rooks and bishops)
    private fun updateSliderBitboards() {
        val friendlyRook   = Piece.makePiece(Piece.ROOK, moveColour)
        val friendlyQueen  = Piece.makePiece(Piece.QUEEN, moveColour)
        val friendlyBishop = Piece.makePiece(Piece.BISHOP, moveColour)
        friendlyOrthogonalSliders = pieceBitboards!![friendlyRook] or pieceBitboards!![friendlyQueen]
        friendlyDiagonalSliders = pieceBitboards!![friendlyBishop] or pieceBitboards!![friendlyQueen]


        val enemyRook   = Piece.makePiece(Piece.ROOK, opponentColour)
        val enemyQueen  = Piece.makePiece(Piece.QUEEN, opponentColour)
        val enemyBishop = Piece.makePiece(Piece.BISHOP, opponentColour)
        enemyOrthogonalSliders = pieceBitboards!![enemyRook] or pieceBitboards!![enemyQueen]
        enemyDiagonalSliders = pieceBitboards!![enemyBishop] or pieceBitboards!![enemyQueen]
    }

    /**
     * Initializes many values
     */
    private fun initialize() {
        allGameMoves = mutableListOf()
        kingSquare = IntArray(2)
        square.fill(0)

        repetitionPositionHistory = Stack()
        gameStateHistory = Stack()

        currentGameState = GameState()
        plyCount = 0

        knights = arrayOf(PieceList(10), PieceList(10))
        rooks = arrayOf(PieceList(10), PieceList(10))
        pawns = arrayOf(PieceList(8), PieceList(8))
        bishops = arrayOf(PieceList(10), PieceList(10))
        queens = arrayOf(PieceList(9), PieceList(9))


        allPieceLists = arrayOfNulls(Piece.MaxPieceIndex + 1)
        allPieceLists[Piece.WHITE_PAWN] = pawns[WHITE_INDEX]
        allPieceLists[Piece.WHITE_KNIGHT] = knights[WHITE_INDEX]
        allPieceLists[Piece.WHITE_BISHOP] = bishops[WHITE_INDEX]
        allPieceLists[Piece.WHITE_ROOK] = rooks[WHITE_INDEX]
        allPieceLists[Piece.WHITE_QUEEN] = queens[WHITE_INDEX]
        allPieceLists[Piece.WHITE_KING] = PieceList(1)

        allPieceLists[Piece.BLACK_PAWN] = pawns[BLACK_INDEX]
        allPieceLists[Piece.BLACK_KNIGHT] = knights[BLACK_INDEX]
        allPieceLists[Piece.BLACK_BISHOP] = bishops[BLACK_INDEX]
        allPieceLists[Piece.BLACK_ROOK] = rooks[BLACK_INDEX]
        allPieceLists[Piece.BLACK_QUEEN] = queens[BLACK_INDEX]
        allPieceLists[Piece.BLACK_KING] = PieceList(1)

        pieceBitboards = ULongArray(Piece.MaxPieceIndex + 1)
        colourBitboards = ULongArray(2)
        allPiecesBitboard = 0UL

    }
}

