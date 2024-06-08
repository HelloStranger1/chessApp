package com.hellostranger.chess_app.core.helpers

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece

@ExperimentalUnsignedTypes
/**
 * Utility object for handling operations related to the FEN (Forsyth-Edwards Notation) representation
 * of a chess board state.
 */
object FenUtility {

    // Constant representing the FEN string for the initial position in a standard chess game.
    const val START_POSITION_FEN: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    /**
     * Converts a FEN string to a PositionInfo object.
     * @param fen The FEN string to convert.
     * @return A PositionInfo object representing the board state described by the FEN string.
     */
    fun positionFromFen(fen: String): PositionInfo {
        return PositionInfo(fen)
    }

    /**
     * Generates a FEN string representing the current state of a given board.
     * @param board The board object representing the current game state.
     * @param alwaysIncludeEPSquare Flag to determine whether to always include the en passant square.
     * @return The FEN string representing the current board state.
     */
    @JvmStatic
    fun currentFen(board: Board, alwaysIncludeEPSquare: Boolean = true): String {
        val fen = StringBuilder()

        // Loop through each rank from 8 (index 7) to 1 (index 0).
        for (rank in 7 downTo 0) {
            var numEmptyTiles = 0  // Counter for consecutive empty tiles.
            for (file in 0..7) {   // Loop through each file from 'a' (index 0) to 'h' (index 7).
                val i = rank * 8 + file  // Calculate the index in the board's square array.
                val piece = board.square[i]  // Get the piece at the current index.

                if (piece != 0) {  // If there's a piece on the current square.
                    if (numEmptyTiles != 0) {  // If there were empty tiles before this piece.
                        fen.append(numEmptyTiles)  // Append the number of empty tiles to the FEN string.
                        numEmptyTiles = 0  // Reset the empty tiles counter.
                    }
                    val isBlack = Piece.isColour(piece, Piece.BLACK)  // Check if the piece is black.
                    val pieceType = Piece.pieceType(piece)  // Get the type of the piece (pawn, knight, etc.).
                    val pieceChar = Piece.getSymbol(pieceType)  // Get the symbol for the piece type.
                    fen.append(if (isBlack) pieceChar.lowercaseChar() else pieceChar)  // Append the piece symbol, in lowercase if black.
                } else {
                    numEmptyTiles++  // Increment the empty tiles counter.
                }
            }
            if (numEmptyTiles != 0) {
                fen.append(numEmptyTiles)  // Append the remaining empty tiles count at the end of the rank.
            }
            if (rank != 0) {
                fen.append('/')  // Add a rank separator if not the last rank.
            }
        }

        // Side to move
        fen.append(' ')
        fen.append(if (board.isWhiteToMove) 'w' else 'b')  // 'w' if it's white's turn, 'b' if black's.

        // Castling rights
        fen.append(' ')
        val whiteKingside = board.currentGameState.hasKingSideCastleRights(true)  // White kingside castling rights.
        val whiteQueenside = board.currentGameState.hasQueenSideCastleRights(true)  // White queenside castling rights.
        val blackKingside = board.currentGameState.hasKingSideCastleRights(false)  // Black kingside castling rights.
        val blackQueenside = board.currentGameState.hasQueenSideCastleRights(false)  // Black queenside castling rights.
        fen.append(if (whiteKingside) 'K' else "")  // Append 'K' if white can castle kingside.
        fen.append(if (whiteQueenside) 'Q' else "")  // Append 'Q' if white can castle queenside.
        fen.append(if (blackKingside) 'k' else "")  // Append 'k' if black can castle kingside.
        fen.append(if (blackQueenside) 'q' else "")  // Append 'q' if black can castle queenside.
        fen.append(if ((board.currentGameState.castlingRights == 0)) "-" else "")  // Append '-' if no castling rights.

        // En passant square
        fen.append(" ")
        val epFileIndex = board.currentGameState.enPassantFile  // Get the file index for en passant.
        val epRankIndex = if ((board.isWhiteToMove)) 5 else 2  // Determine the rank index for en passant based on whose turn it is.
        val isEnPassant = epFileIndex != -1  // Check if en passant is possible.
        val includeEP = alwaysIncludeEPSquare || enPassantCanBeCaptured(epFileIndex, epRankIndex, board)  // Decide whether to include the en passant square in the FEN.

        if (isEnPassant && includeEP) {
            fen.append(BoardHelper.squareNameFromCoord(epFileIndex, epRankIndex))  // Append the en passant square name.
        } else {
            fen.append('-')  // Append '-' if no en passant square.
        }

        // 50-move rule counter
        fen.append(' ')
        fen.append(board.currentGameState.fiftyMoveCounter)  // Append the fifty-move counter.

        // Full move number
        fen.append(' ')
        fen.append((board.plyCount / 2) + 1)  // Calculate and append the full move number.

        return fen.toString()  // Return the complete FEN string.
    }

    /**
     * Determines if an en passant capture can be made from the given en passant square.
     * @param epFileIndex The file index of the en passant square.
     * @param epRankIndex The rank index of the en passant square.
     * @param board The board object representing the current game state.
     * @return True if the en passant capture is possible, false otherwise.
     */
    private fun enPassantCanBeCaptured(epFileIndex: Int, epRankIndex: Int, board: Board): Boolean {
        val captureFromA = Coord(epFileIndex - 1, epRankIndex + if(board.isWhiteToMove) -1 else 1)  // Coordinate for capture from the left.
        val captureFromB = Coord(epFileIndex + 1, epRankIndex + if(board.isWhiteToMove) -1 else 1)  // Coordinate for capture from the right.
        val epCaptureSquare = Coord(epFileIndex, epRankIndex).squareIndex  // Calculate the square index for en passant capture.
        val friendlyPawn = Piece.makePiece(Piece.PAWN, board.moveColour)  // Create a pawn of the current moving color.

        // Check capture from A
        var isPawnOnSquare = board.square[captureFromA.squareIndex] == friendlyPawn  // Check if there's a friendly pawn at the left capture square.
        if (captureFromA.isValidSquare && isPawnOnSquare) {  // If valid capture square and friendly pawn exists.
            val move = Move(captureFromA.squareIndex, epCaptureSquare, Move.EN_PASSANT_CAPTURE_FLAG)  // Create an en passant capture move.
            board.makeMove(move)  // Make the en passant capture move.
            board.makeNullMove()  // Make a null move (to switch the side to move).
            val wasLegalMove = !board.calculateInCheckState()  // Check if the move results in a legal board state.

            board.unmakeNullMove()  // Unmake the null move.
            board.unmakeMove(move)  // Unmake the en passant move.
            if (wasLegalMove) return true  // If the move was legal, en passant capture is possible.
        }

        // Check capture from B
        isPawnOnSquare = board.square[captureFromB.squareIndex] == friendlyPawn  // Check if there's a friendly pawn at the right capture square.
        if (captureFromB.isValidSquare && isPawnOnSquare) {  // If valid capture square and friendly pawn exists.
            val move = Move(captureFromB.squareIndex, epCaptureSquare, Move.EN_PASSANT_CAPTURE_FLAG)  // Create an en passant capture move.
            board.makeMove(move)  // Make the en passant capture move.
            board.makeNullMove()  // Make a null move (to switch the side to move).
            val canCapture = !board.calculateInCheckState()  // Check if the move results in a legal board state.

            board.unmakeNullMove()  // Unmake the null move.
            board.unmakeMove(move)  // Unmake the en passant move.
            return canCapture  // Return the result of the capture check.
        }

        return false  // Return false if no valid en passant capture was found.
    }

    /**
     * Class representing information parsed from a FEN string.
     */
    class PositionInfo(fen: String) {
        private val fen: String  // Store the FEN string.
        val squares: IntArray  // Array representing the pieces on the board.

        // Castling rights
        val whiteCastleKingside: Boolean  // White kingside castling rights.
        val whiteCastleQueenside: Boolean  // White queenside castling rights.
        val blackCastleKingside: Boolean  // Black kingside castling rights.
        val blackCastleQueenside: Boolean  // Black queenside castling rights.

        // En passant file (1 is file a)
        val epFile: Int  // File index for en passant.
        val whiteToMove: Boolean  // Flag indicating if it's white's turn to move.
        val fiftyMovePlyCount: Int  // Counter for the fifty-move rule.
        val moveCount: Int  // The full move number.

        init {
            this.fen = fen  // Store the FEN string.
            val squarePieces = IntArray(64)  // Array to store the pieces on the board.

            val sections = fen.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()  // Split the FEN string into sections.

            var file = 0  // Initial file index.
            var rank = 7  // Initial rank index.

            for (i in 0 until sections[0].length) {  // Iterate through the board representation section.
                val symbol = sections[0][i]  // Get the current symbol.

                if (symbol == '/') {  // If the symbol is a rank separator.
                    file = 0  // Reset the file index.
                    rank--  // Move to the next lower rank.
                } else {
                    if (Character.isDigit(symbol)) {  // If the symbol is a digit.
                        file += Character.getNumericValue(symbol)  // Skip the number of empty squares.
                    } else {  // If the symbol is a piece.
                        val pieceColour = if ((Character.isUpperCase(symbol))) Piece.WHITE else Piece.BLACK  // Determine the piece color.
                        val pieceType = Piece.getPieceFromSymbol(symbol)  // Get the piece type.

                        squarePieces[rank * 8 + file] = Piece.makePiece(pieceType, pieceColour)  // Store the piece in the board array.
                        file++  // Move to the next file.
                    }
                }
            }

            this.squares = squarePieces  // Store the board array.

            this.whiteToMove = (sections[1] == "w")  // Determine the side to move from the FEN string.

            val castlingRights = sections[2]  // Get the castling rights section.
            whiteCastleKingside = castlingRights.contains("K")  // Check for white kingside castling rights.
            whiteCastleQueenside = castlingRights.contains("Q")  // Check for white queenside castling rights.
            blackCastleKingside = castlingRights.contains("k")  // Check for black kingside castling rights.
            blackCastleQueenside = castlingRights.contains("q")  // Check for black queenside castling rights.

            // Default values
            var epFile = 0
            var fiftyMovePlyCount = 0
            var moveCount = 0

            if (sections.size > 3) {  // If the en passant section exists.
                val enPassantFileName = sections[3][0].toString()  // Get the en passant file name.

                if (BoardHelper.FILE_NAMES.contains(enPassantFileName)) {  // Check if it's a valid file name.
                    epFile = BoardHelper.FILE_NAMES.indexOf(enPassantFileName) + 1  // Convert the file name to an index.
                }
            }

            if (sections.size > 4) {  // If the fifty-move rule counter exists.
                try {
                    fiftyMovePlyCount = sections[4].toInt()  // Parse the fifty-move counter.
                } catch (ignored: NumberFormatException) {
                }
            }

            if (sections.size > 5) {  // If the move counter exists.
                try {
                    moveCount = sections[5].toInt()  // Parse the move counter.
                } catch (ignored: NumberFormatException) {
                }
            }

            this.epFile = epFile  // Store the en passant file index.
            this.fiftyMovePlyCount = fiftyMovePlyCount  // Store the fifty-move counter.
            this.moveCount = moveCount  // Store the full move number.
        }
    }
}
