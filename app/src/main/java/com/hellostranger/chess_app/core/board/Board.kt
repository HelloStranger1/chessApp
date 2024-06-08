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
        const val WHITE_INDEX: Int = 0 // Index for white pieces
        const val BLACK_INDEX: Int = 1 // Index for black pieces

        /**
         * Creates a board and loads into it the fen position.
         * @param fen: The fen position to be loaded in. if empty, is the starting board
         * @return A board object with the position loaded in.
         */
        fun createBoard(fen: String = FenUtility.START_POSITION_FEN): Board {
            val board = Board() // Create a new Board instance
            board.loadPosition(fen) // Load the position from the FEN string
            return board // Return the initialized board
        }

        /**
         * Creates a copy of an existing board.
         * @param source The board to copy.
         * @return A new board object that is a copy of the source board.
         */
        fun createBoard(source: Board): Board {
            val board = Board() // Create a new Board instance
            board.loadPosition(source.startPositionInfo) // Load the position from the source board's starting position

            // Replay all moves from the source board
            for (i in 0 until source.allGameMoves.count()) {
                board.makeMove(source.allGameMoves[i])
            }
            return board // Return the copied board
        }
    }

    val square: IntArray = IntArray(64) // Array to store the piece code for each square on the board

    lateinit var kingSquare: IntArray // Array to store the index of the white and black king

    /* Bitboards */
    var pieceBitboards: ULongArray? = null // Bitboard for each piece type and color
    var colourBitboards: ULongArray? = null // Bitboard for all pieces of each color
    var allPiecesBitboard: ULong = 0UL // Bitboard for all pieces on the board
    var friendlyOrthogonalSliders: ULong = 0UL // Bitboard for all orthogonal sliding pieces (rooks and queens) of the current player
    var friendlyDiagonalSliders: ULong = 0UL // Bitboard for all diagonal sliding pieces (bishops and queens) of the current player
    var enemyOrthogonalSliders: ULong = 0UL // Bitboard for all orthogonal sliding pieces of the opponent
    var enemyDiagonalSliders: ULong = 0UL // Bitboard for all diagonal sliding pieces of the opponent

    private var totalPieceCountWithoutPawnsAndKings: Int = 0 // Count of all pieces except pawns and kings

    /* Piece lists */
    lateinit var rooks: Array<PieceList> // Array to store lists of rooks for each color
    lateinit var bishops: Array<PieceList> // Array to store lists of bishops for each color
    lateinit var queens: Array<PieceList> // Array to store lists of queens for each color
    lateinit var knights: Array<PieceList> // Array to store lists of knights for each color
    lateinit var pawns: Array<PieceList> // Array to store lists of pawns for each color

    /* Side to move info */
    var isWhiteToMove: Boolean = false // Flag indicating if it's white's turn to move
    val moveColour: Int
        get() = if (isWhiteToMove) Piece.WHITE else Piece.BLACK // The color of the player to move
    val opponentColour: Int
        get() = if (isWhiteToMove) Piece.BLACK else Piece.WHITE // The color of the opponent
    val moveColourIndex: Int
        get() = if (isWhiteToMove) WHITE_INDEX else BLACK_INDEX // The index for the player to move
    private val opponentColourIndex: Int
        get() = if (isWhiteToMove) BLACK_INDEX else WHITE_INDEX // The index for the opponent

    var repetitionPositionHistory: Stack<ULong>? = null // Stack to store hashed positions for repetition checks

    var plyCount: Int = 0 // Total half-moves played
    val fiftyMoveCount: Int
        get() = currentGameState.fiftyMoveCounter // Counter for the fifty-move rule
    lateinit var currentGameState: GameState // The current game state
    val zobristKey: ULong
        get() = currentGameState.zobristKey // The Zobrist key for the current position

    lateinit var allGameMoves: MutableList<Move> // List of all moves played in the game

    private lateinit var allPieceLists: Array<PieceList?> // Array of piece lists for all types and colors
    private lateinit var gameStateHistory: Stack<GameState> // Stack to store game state history for undo operations
    lateinit var startPositionInfo: PositionInfo // Information about the starting position
    private var cachedInCheckValue: Boolean = false // Cached value indicating if the current player is in check
    private var hasCachedInCheckValue: Boolean = false // Flag indicating if the in-check value is cached

    /**
     * Make a move on the board.
     * @param move: The move to play
     * @param inSearch: controls whether this move should be recorded in game history
     */
    fun makeMove(move: Move, inSearch: Boolean = false) {
        val startSquare = move.startSquare // The starting square of the move
        val targetSquare = move.targetSquare // The target square of the move
        val moveFlag = move.moveFlag // The flag associated with the move
        val isPromotion = move.isPromotion // Indicates if the move is a promotion
        val isEnPassant = moveFlag == Move.EN_PASSANT_CAPTURE_FLAG // Indicates if the move is an en passant capture

        val movedPiece = square[startSquare] // The piece being moved
        val movedPieceType = Piece.pieceType(movedPiece) // The type of the piece being moved
        val capturedPiece = if (isEnPassant) Piece.makePiece(Piece.PAWN, opponentColour) else square[targetSquare] // The piece being captured, if any
        val capturedPieceType = Piece.pieceType(capturedPiece) // The type of the captured piece

        val prevCastleState = currentGameState.castlingRights // The previous castling rights
        val prevEnPassantFile = currentGameState.enPassantFile // The previous en passant file
        var newZobristKey = currentGameState.zobristKey // The new Zobrist key after the move
        var newCastleRights = prevCastleState // The new castling rights after the move
        var newEnPassantFile = 0 // The new en passant file after the move

        // Update bitboard of moved piece
        movePiece(movedPiece, startSquare, targetSquare)

        // Handle Captures
        if (capturedPieceType != Piece.NONE) {
            if (capturedPieceType != Piece.PAWN) {
                totalPieceCountWithoutPawnsAndKings-- // Decrement the piece count if the captured piece is not a pawn
            }
            newZobristKey = handleCaptures(targetSquare, isEnPassant, capturedPiece, newZobristKey) // Update the Zobrist key and handle the capture
        }

        // Handle King
        if (movedPieceType == Piece.KING) {
            kingSquare[moveColourIndex] = targetSquare // Update the king's position
            newCastleRights = newCastleRights and if (isWhiteToMove) 0b1100 else 0b0011 // Update castling rights

            // Handle castling
            if (moveFlag == Move.CASTLE_FLAG) {
                newZobristKey = handleCastling(targetSquare, newZobristKey) // Update the Zobrist key for castling
            }
        }

        // Handle promotion
        if (isPromotion) {
            handlePromotion(move, movedPiece, targetSquare) // Handle the promotion
        }

        // Pawn moved 2 forward, mark this file with en passant flag
        if (moveFlag == Move.PAWN_TWO_UP_FLAG) {
            val file: Int = BoardHelper.fileIndex(startSquare) + 1 // Calculate the en passant file
            newEnPassantFile = file // Set the new en passant file
            newZobristKey = newZobristKey xor Zobrist.enPassantFile[file] // Update the Zobrist key for en passant
        }

        // Update castling rights
        if (prevCastleState != 0) {
            newCastleRights = updateCastlingRights(targetSquare, startSquare, newCastleRights) // Update castling rights based on the move
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

        plyCount++ // Increment the ply count
        var newFiftyMoveCounter = currentGameState.fiftyMoveCounter + 1 // Update the fifty-move counter

        // Update extra bitboards
        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX]
        updateSliderBitboards()

        // Pawn moves and captures reset the fifty move counter and clear 3-fold history
        if (movedPieceType == Piece.PAWN || capturedPieceType != Piece.NONE) {
            if (!inSearch) {
                repetitionPositionHistory!!.clear() // Clear the repetition history if it's not a search move
            }
            newFiftyMoveCounter = 0 // Reset the fifty-move counter
        }

        // Updates the game state
        val newState = GameState(capturedPieceType, newEnPassantFile, newCastleRights, newFiftyMoveCounter, newZobristKey)
        gameStateHistory.push(newState) // Push the new state onto the game state history stack
        currentGameState = newState // Update the current game state
        hasCachedInCheckValue = false // Invalidate the cached in-check value

        if (!inSearch) {
            repetitionPositionHistory!!.push(newState.zobristKey) // Update the repetition history
            allGameMoves.add(move) // Add the move to the list of all game moves
        }
    }

    /**
     * Returns updated castling rights.
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
        var newCastleRights = castleRights
        // Any piece moving to/from rook square removes castling right for that side
        if (targetSquare == BoardHelper.H_1 || startSquare == BoardHelper.H_1) {
            newCastleRights = newCastleRights and GameState.CLEAR_WHITE_KINGSIDE_MASK // Clear white kingside castling rights
        } else if (targetSquare == BoardHelper.A_1 || startSquare == BoardHelper.A_1) {
            newCastleRights = newCastleRights and GameState.CLEAR_WHITE_QUEENSIDE_MASK // Clear white queenside castling rights
        }
        if (targetSquare == BoardHelper.H_8 || startSquare == BoardHelper.H_8) {
            newCastleRights = newCastleRights and GameState.CLEAR_BLACK_KINGSIDE_MASK // Clear black kingside castling rights
        } else if (targetSquare == BoardHelper.A_8 || startSquare == BoardHelper.A_8) {
            newCastleRights = newCastleRights and GameState.CLEAR_BLACK_QUEENSIDE_MASK // Clear black queenside castling rights
        }
        return newCastleRights
    }

    /**
     * Handles promoting a pawn.
     * @param move: The move just played
     * @param movedPiece: The old piece
     * @param targetSquare: The target Square
     */
    private fun handlePromotion(
        move: Move,
        movedPiece: Int,
        targetSquare: Int
    ) {
        totalPieceCountWithoutPawnsAndKings++ // Increment the piece count for the new promoted piece
        val promotionPieceType: Int = move.promotionPieceType // Get the promotion piece type from the move
        val promotionPiece = Piece.makePiece(promotionPieceType, moveColour) // Create the promotion piece
        allPieceLists[movedPiece]!!.removePieceAtSquare(targetSquare) // Remove the old pawn piece from the target square
        allPieceLists[promotionPiece]!!.addPieceAtSquare(targetSquare) // Add the new promotion piece to the target square
        square[targetSquare] = promotionPiece // Update the board square with the new promotion piece

        // Update bitboards for the promotion
        pieceBitboards!![movedPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![movedPiece], targetSquare)
        pieceBitboards!![promotionPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![promotionPiece], targetSquare)
    }

    /**
     * Handles the remaining parts of castling after moving the king.
     * Updates relevant bitboards, castling rights, and the zobrist key.
     * @param targetSquare: The targetSquare of the rook
     * @param zobristKey: The old zobrist key
     * @return The new zobrist key
     */
    private fun handleCastling(targetSquare: Int, zobristKey: ULong): ULong {
        var newZobristKey = zobristKey
        val rookPiece: Int = Piece.makePiece(Piece.ROOK, moveColour) // Create the rook piece
        val kingside: Boolean = targetSquare == BoardHelper.G_1 || targetSquare == BoardHelper.G_8 // Check if castling kingside
        val castlingRookFromIndex = if (kingside) targetSquare + 1 else targetSquare - 2 // Determine the rook's starting square
        val castlingRookToIndex = if (kingside) targetSquare - 1 else targetSquare + 1 // Determine the rook's ending square

        // Update Rook Position in bitboards and lists
        pieceBitboards!![rookPiece] = BitBoardUtility.toggleSquares(pieceBitboards!![rookPiece], castlingRookFromIndex, castlingRookToIndex)
        colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], castlingRookFromIndex, castlingRookToIndex)
        allPieceLists[rookPiece]!!.movePiece(castlingRookFromIndex, castlingRookToIndex)
        square[castlingRookFromIndex] = Piece.NONE // Clear the rook's old square
        square[castlingRookToIndex] = Piece.ROOK or moveColour // Set the rook's new square

        // Update the Zobrist key for the rook's move
        newZobristKey = newZobristKey xor Zobrist.piecesArray[rookPiece][castlingRookFromIndex]
        newZobristKey = newZobristKey xor Zobrist.piecesArray[rookPiece][castlingRookToIndex]

        return newZobristKey // Return the new Zobrist key
    }

    /**
     * Handles the remaining parts of a capture after moving piece.
     * Updates relevant bitboards, removes the captured piece, and updates the zobrist key.
     * @param targetSquare: The targetSquare of the captured piece
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
        var captureSquare = targetSquare // The square of the captured piece
        if (isEnPassant) {
            captureSquare = targetSquare + (if (isWhiteToMove) -8 else 8) // Adjust the square for en passant captures
            square[captureSquare] = Piece.NONE // Clear the captured pawn's square for en passant
        }

        // Remove captured piece from bitboards and piece list
        allPieceLists[capturedPiece]!!.removePieceAtSquare(captureSquare)
        pieceBitboards!![capturedPiece] =
            BitBoardUtility.toggleSquare(pieceBitboards!![capturedPiece], captureSquare)
        colourBitboards!![opponentColourIndex] =
            BitBoardUtility.toggleSquare(colourBitboards!![opponentColourIndex], captureSquare)
        return zobristKey xor Zobrist.piecesArray[capturedPiece][captureSquare] // Update the Zobrist key and return it
    }

    /**
     * Undo a move that was just played on the board.
     * @param move: The move to undo
     * @param inSearch: controls whether this move should be recorded in game history
     */
    fun unmakeMove(move: Move, inSearch: Boolean = false) {
        // Swap colours to move
        isWhiteToMove = !isWhiteToMove

        val undoingWhiteMove: Boolean = isWhiteToMove

        // Get Move info
        val movedFrom = move.startSquare // The starting square of the move to undo
        val movedTo = move.targetSquare // The target square of the move to undo
        val moveFlag = move.moveFlag // The flag associated with the move

        val undoingEnPassant = moveFlag == Move.EN_PASSANT_CAPTURE_FLAG // Check if undoing an en passant capture
        val undoingPromotion = move.isPromotion // Check if undoing a promotion
        val undoingCapture = currentGameState.capturedPieceType != Piece.NONE // Check if undoing a capture

        val movedPiece = if (undoingPromotion) Piece.makePiece(Piece.PAWN, moveColour) else square[movedTo] // Determine the moved piece
        val movedPieceType = Piece.pieceType(movedPiece) // Determine the type of the moved piece
        val capturedPieceType = currentGameState.capturedPieceType // Determine the type of the captured piece

        // If undoing promotion, then remove piece from promotion square and replace with pawn
        if (undoingPromotion) {
            val promotedPiece = square[movedTo] // The promoted piece
            val pawnPiece = Piece.makePiece(Piece.PAWN, moveColour) // Create a pawn piece
            totalPieceCountWithoutPawnsAndKings-- // Decrement the piece count for the promoted piece

            // Update piece lists and bitboards for the promotion
            allPieceLists[promotedPiece]!!.removePieceAtSquare(movedTo)
            allPieceLists[movedPiece]!!.addPieceAtSquare(movedTo)
            pieceBitboards!![promotedPiece] = BitBoardUtility.toggleSquare(pieceBitboards!![promotedPiece], movedTo)
            pieceBitboards!![pawnPiece] = BitBoardUtility.toggleSquare(pieceBitboards!![pawnPiece], movedTo)
        }

        movePiece(movedPiece, movedTo, movedFrom) // Move the piece back to its original square

        // Undo capture
        if (undoingCapture) {
            var captureSquare = movedTo // The square of the captured piece
            val capturedPiece = Piece.makePiece(capturedPieceType, opponentColour) // Create the captured piece

            if (undoingEnPassant) {
                captureSquare = movedTo + if (undoingWhiteMove) -8 else 8 // Adjust the square for en passant captures
            }
            if (capturedPieceType != Piece.PAWN) {
                totalPieceCountWithoutPawnsAndKings++ // Increment the piece count for the captured piece
            }

            // Update bitboards and piece lists for the captured piece
            pieceBitboards!![capturedPiece] = BitBoardUtility.toggleSquare(pieceBitboards!![capturedPiece], captureSquare)
            colourBitboards!![opponentColourIndex] = BitBoardUtility.toggleSquare(colourBitboards!![opponentColourIndex], captureSquare)
            allPieceLists[capturedPiece]!!.addPieceAtSquare(captureSquare)
            square[captureSquare] = capturedPiece // Restore the captured piece on the board
        }

        // Update King
        if (movedPieceType == Piece.KING) {
            kingSquare[moveColourIndex] = movedFrom // Restore the king's position

            // Undo castle
            if (moveFlag == Move.CASTLE_FLAG) {
                val rookPiece = Piece.makePiece(Piece.ROOK, moveColour) // Create the rook piece
                val kingside = movedTo == BoardHelper.G_1 || movedTo == BoardHelper.G_8 // Check if undoing a kingside castle
                val rookSquareBeforeCastling = if (kingside) movedTo + 1 else movedTo - 2 // Determine the rook's original square
                val rookSquareAfterCastling = if (kingside) movedTo - 1 else movedTo + 1 // Determine the rook's castling square

                // Update bitboards and piece lists for the rook
                pieceBitboards!![rookPiece] = BitBoardUtility.toggleSquares(pieceBitboards!![rookPiece], rookSquareAfterCastling, rookSquareBeforeCastling)
                colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], rookSquareAfterCastling, rookSquareBeforeCastling)
                square[rookSquareAfterCastling] = Piece.NONE // Clear the rook's castling square
                square[rookSquareBeforeCastling] = rookPiece // Restore the rook's original square
                allPieceLists[rookPiece]!!.movePiece(rookSquareAfterCastling, rookSquareBeforeCastling)
            }
        }

        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX] // Update the bitboard for all pieces
        updateSliderBitboards() // Update bitboards for all sliders (rooks, bishops, and queens)

        if (!inSearch && repetitionPositionHistory!!.isNotEmpty()) {
            repetitionPositionHistory!!.pop() // Remove the last position from the repetition history
        }
        if (!inSearch) {
            allGameMoves.removeLast() // Remove the last move from the list of all game moves
        }

        // Go back to previous state
        gameStateHistory.pop() // Pop the last game state from the history stack
        currentGameState = gameStateHistory.peek() // Restore the previous game state
        plyCount-- // Decrement the ply count
        hasCachedInCheckValue = false // Invalidate the cached in-check value
    }

    /**
     * Used to switch the side to play without making a move.
     */
    fun makeNullMove() {
        isWhiteToMove = !isWhiteToMove // Switch the side to move

        plyCount++ // Increment the ply count

        var newZobristKey = currentGameState.zobristKey // Start with the current Zobrist key
        newZobristKey = newZobristKey xor Zobrist.sideToMove // Update the Zobrist key for the side to move
        newZobristKey = newZobristKey xor Zobrist.enPassantFile[currentGameState.enPassantFile] // Update the Zobrist key for the en passant file

        // Create a new game state for the null move
        val newState = GameState(Piece.NONE, 0, currentGameState.castlingRights, currentGameState.fiftyMoveCounter + 1, newZobristKey)
        currentGameState = newState // Update the current game state
        gameStateHistory.push(currentGameState) // Push the new state onto the game state history stack
        updateSliderBitboards() // Update bitboards for all sliders (rooks, bishops, and queens)
        hasCachedInCheckValue = true // Cache the in-check value
        cachedInCheckValue = false // Set the in-check value to false
    }

    /**
     * Unmake a null move
     */
    fun unmakeNullMove() {
        isWhiteToMove = !isWhiteToMove // Switch the side to move back
        plyCount-- // Decrement the ply count
        gameStateHistory.pop() // Pop the last game state from the history stack
        currentGameState = gameStateHistory.peek() // Restore the previous game state
        updateSliderBitboards() // Update bitboards for all sliders (rooks, bishops, and queens)
        hasCachedInCheckValue = true // Cache the in-check value
        cachedInCheckValue = false // Set the in-check value to false
    }

    /**
     * Checks if the current player is in check.
     * Note: caches check value so calling multiple times does not require recalculating
     */
    fun isInCheck(): Boolean {
        if (hasCachedInCheckValue) {
            return cachedInCheckValue // Return the cached in-check value if available
        }
        cachedInCheckValue = calculateInCheckState() // Calculate and cache the in-check value
        hasCachedInCheckValue = true // Mark the in-check value as cached

        return cachedInCheckValue // Return the cached in-check value
    }

    /**
     * Calculate in check value.
     * Call isInCheck instead for automatic caching of value.
     */
    fun calculateInCheckState(): Boolean {
        val kingSquare = kingSquare[moveColourIndex] // Get the square of the current player's king
        val blockers: ULong = allPiecesBitboard // Get the bitboard of all pieces

        // Check for orthogonal slider attacks (rooks and queens)
        if (enemyOrthogonalSliders != 0UL) {
            val rookAttacks: ULong = BitBoardUtility.getRookAttacks(kingSquare, blockers) // Get the bitboard of rook attacks on the king's square
            if ((rookAttacks and enemyOrthogonalSliders) != 0UL) {
                return true // The king is in check by an orthogonal slider
            }
        }
        // Check for diagonal slider attacks (bishops and queens)
        if (enemyDiagonalSliders != 0UL) {
            val bishopAttacks: ULong = BitBoardUtility.getBishopAttacks(kingSquare, blockers) // Get the bitboard of bishop attacks on the king's square
            if ((bishopAttacks and enemyDiagonalSliders) != 0UL) {
                return true // The king is in check by a diagonal slider
            }
        }
        // Check for knight attacks
        val enemyKnights = pieceBitboards!![Piece.makePiece(Piece.KNIGHT, opponentColour)]
        if ((BitBoardUtility.knightAttacks[kingSquare] and enemyKnights) != 0UL) {
            return true // The king is in check by a knight
        }

        // Check for pawn attacks
        val enemyPawns = pieceBitboards!![Piece.makePiece(Piece.PAWN, opponentColour)]
        val pawnAttackMasks: ULong = if (isWhiteToMove) BitBoardUtility.whitePawnAttacks[kingSquare] else BitBoardUtility.blackPawnAttacks[kingSquare]
        return (pawnAttackMasks and enemyPawns) != 0UL // The king is in check by a pawn
    }

    /**
     * Loads a position from a FEN string.
     * @param fen: The FEN string representing the position
     */
    fun loadPosition(fen: String) {
        val posInfo = FenUtility.positionFromFen(fen) // Convert the FEN string to PositionInfo
        loadPosition(posInfo) // Load the position from the PositionInfo
    }

    /**
     * Loads a position into the board from PositionInfo.
     * @param posInfo: The PositionInfo object representing the position
     */
    fun loadPosition(posInfo: PositionInfo) {
        startPositionInfo = posInfo // Set the starting position information
        initialize() // Initialize the board

        // Load each square with the piece from the position info
        for (squareIndex in 0 until 64) {
            val piece = posInfo.squares[squareIndex] // Get the piece on the square
            val pieceType = Piece.pieceType(piece) // Get the type of the piece
            val colourIndex = if (Piece.isWhite(piece)) WHITE_INDEX else BLACK_INDEX // Determine the color index of the piece
            square[squareIndex] = piece // Set the piece on the board

            if (piece != Piece.NONE) {
                // Update bitboards and piece lists for the piece
                pieceBitboards!![piece] = BitBoardUtility.setSquare(pieceBitboards!![piece], squareIndex)
                colourBitboards!![colourIndex] = BitBoardUtility.setSquare(colourBitboards!![colourIndex], squareIndex)

                if (pieceType == Piece.KING) {
                    kingSquare[colourIndex] = squareIndex // Set the king's position
                } else {
                    allPieceLists[piece]!!.addPieceAtSquare(squareIndex) // Add the piece to the piece list
                }
                if (pieceType != Piece.PAWN && pieceType != Piece.KING) {
                    totalPieceCountWithoutPawnsAndKings++ // Increment the piece count for non-pawns and non-kings
                }
            }
        }

        // Set the side to move
        isWhiteToMove = posInfo.whiteToMove

        // Set extra bitboards
        allPiecesBitboard = colourBitboards!![WHITE_INDEX] or colourBitboards!![BLACK_INDEX]
        updateSliderBitboards() // Update bitboards for all sliders (rooks, bishops, and queens)

        // Calculate the castling rights
        val whiteCastle = (if ((posInfo.whiteCastleKingside)) 1 shl 0 else 0) or (if ((posInfo.whiteCastleQueenside)) 1 shl 1 else 0)
        val blackCastle = (if ((posInfo.blackCastleKingside)) 1 shl 2 else 0) or (if ((posInfo.blackCastleQueenside)) 1 shl 3 else 0)
        val castlingRights = whiteCastle or blackCastle

        plyCount = (posInfo.moveCount - 1) * 2 + if (isWhiteToMove) 0 else 1 // Calculate the ply count

        // Set the initial game state
        currentGameState = GameState(Piece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, 0UL)
        val newZobristKey = Zobrist.calculateZobristKey(this) // Calculate the Zobrist key for the initial position
        currentGameState = GameState(Piece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, newZobristKey)

        repetitionPositionHistory!!.push(newZobristKey) // Add the initial position to the repetition history

        gameStateHistory.push(currentGameState) // Push the initial game state onto the history stack
    }

    /**
     * Update piece lists and bitboards based on given move info.
     * Doesn't account for the following things:
     * 1. Removal of captured piece
     * 2. Moving the rook when castling
     * 3. Removal of pawn from 1st/8th rank on promotion
     * 4. Addition of promoted piece on promotion
     *
     * @param piece: The piece to move
     * @param startSquare: The start square
     * @param targetSquare: The target square
     */
    private fun movePiece(piece: Int, startSquare: Int, targetSquare: Int) {
        // Update bitboards for the piece's move
        pieceBitboards!![piece] = BitBoardUtility.toggleSquares(pieceBitboards!![piece], startSquare, targetSquare)
        colourBitboards!![moveColourIndex] = BitBoardUtility.toggleSquares(colourBitboards!![moveColourIndex], startSquare, targetSquare)

        // Check if the piece list for the piece is null (error handling)
        if (allPieceLists[piece] == null) {
            Log.e("BOARD", "WTF. the piece is: ${Piece.getSymbol(piece)}.")
        }
        allPieceLists[piece]!!.movePiece(startSquare, targetSquare) // Move the piece in the piece list
        square[startSquare] = Piece.NONE // Clear the piece from the starting square
        square[targetSquare] = piece // Set the piece on the target square
    }

    // Updates bitboards for all slider pieces (queens, rooks, and bishops)
    private fun updateSliderBitboards() {
        val friendlyRook = Piece.makePiece(Piece.ROOK, moveColour) // Create the friendly rook piece
        val friendlyQueen = Piece.makePiece(Piece.QUEEN, moveColour) // Create the friendly queen piece
        val friendlyBishop = Piece.makePiece(Piece.BISHOP, moveColour) // Create the friendly bishop piece
        friendlyOrthogonalSliders = pieceBitboards!![friendlyRook] or pieceBitboards!![friendlyQueen] // Update the bitboard for friendly orthogonal sliders
        friendlyDiagonalSliders = pieceBitboards!![friendlyBishop] or pieceBitboards!![friendlyQueen] // Update the bitboard for friendly diagonal sliders

        val enemyRook = Piece.makePiece(Piece.ROOK, opponentColour) // Create the enemy rook piece
        val enemyQueen = Piece.makePiece(Piece.QUEEN, opponentColour) // Create the enemy queen piece
        val enemyBishop = Piece.makePiece(Piece.BISHOP, opponentColour) // Create the enemy bishop piece
        enemyOrthogonalSliders = pieceBitboards!![enemyRook] or pieceBitboards!![enemyQueen] // Update the bitboard for enemy orthogonal sliders
        enemyDiagonalSliders = pieceBitboards!![enemyBishop] or pieceBitboards!![enemyQueen] // Update the bitboard for enemy diagonal sliders
    }

    /**
     * Initializes many values.
     */
    private fun initialize() {
        allGameMoves = mutableListOf() // Initialize the list of all game moves
        kingSquare = IntArray(2) // Initialize the array for king positions
        square.fill(0) // Clear the board squares

        repetitionPositionHistory = Stack() // Initialize the stack for repetition history
        gameStateHistory = Stack() // Initialize the stack for game state history

        currentGameState = GameState() // Initialize the current game state
        plyCount = 0 // Reset the ply count

        // Initialize piece lists for all piece types and colors
        knights = arrayOf(PieceList(10), PieceList(10))
        rooks = arrayOf(PieceList(10), PieceList(10))
        pawns = arrayOf(PieceList(8), PieceList(8))
        bishops = arrayOf(PieceList(10), PieceList(10))
        queens = arrayOf(PieceList(9), PieceList(9))

        // Initialize all piece lists array
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

        pieceBitboards = ULongArray(Piece.MaxPieceIndex + 1) // Initialize bitboards for all piece types
        colourBitboards = ULongArray(2) // Initialize bitboards for both colors
        allPiecesBitboard = 0UL // Clear the bitboard for all pieces
    }
}

