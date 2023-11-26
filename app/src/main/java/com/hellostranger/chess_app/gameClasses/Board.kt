package com.hellostranger.chess_app.gameClasses

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.dto.enums.MoveType
import com.hellostranger.chess_app.gameClasses.enums.PieceType
import com.hellostranger.chess_app.gameClasses.pieces.King
import com.hellostranger.chess_app.gameClasses.pieces.Piece
import com.hellostranger.chess_app.gameClasses.pieces.PieceJsonDeserializer
import kotlin.math.abs


private const val BoardTAG = "BoardClass"
data class Board(
    var squaresArray: Array<Array<Square>>,
    var whiteKing : King? = null,
    var blackKing : King? = null,
    var phantomPawnSquare : Square? = null,
    @Transient
    var previousMove : MoveMessage? = null
) : Cloneable{


    companion object{
        val gson: Gson = GsonBuilder()
            .setLenient()
            .registerTypeAdapter(Piece::class.java, PieceJsonDeserializer())
            .create()
    }



    init {
        for (squaresRow in squaresArray){
            for(square in squaresRow){
                if(square.piece != null && square.piece!!.pieceType == PieceType.KING){
                    if(square.piece!!.color == Color.WHITE){
                        whiteKing = square.piece as King?
                    } else{
                        blackKing = square.piece as King?
                    }
                }
            }
        }
    }
    fun getSquareAt(col: Int, row: Int): Square? {
        if (row >= 8 || row < 0 || col >= 8 || col < 0) {
            Log.e(BoardTAG, "There is no square at col $col and row $row")
            return null
        }
        return squaresArray[row][col]
    }
    fun movePiece(move : MoveMessage) : Board {
        val startSquare = squaresArray[move.startRow][move.startCol]
        val endSquare = squaresArray[move.endRow][move.endCol]

        val movingPiece = startSquare.piece
        if(movingPiece == null){
            Log.e(BoardTAG, "You can't move this!! No Piece to move.")
            return this
        }

        if(!isValidMove(startSquare, endSquare)){
            Log.e(BoardTAG, "Move is invalid. Can't play it.")
            return this
        }
        if(move.moveType == MoveType.REGULAR){
            movePiece(startSquare, endSquare)
        }
        else if(move.moveType == MoveType.CASTLE){
            makeCastlingMove(startSquare, endSquare)
        }
        if(move.moveType != MoveType.CASTLE && move.moveType != MoveType.REGULAR){
            movingPiece.pieceType = when(move.moveType){
                MoveType.PROMOTION_QUEEN -> {
                    PieceType.QUEEN}
                MoveType.PROMOTION_ROOK -> {
                    PieceType.ROOK}
                MoveType.PROMOTION_BISHOP -> {
                    PieceType.BISHOP}
                MoveType.PROMOTION_KNIGHT -> {
                    PieceType.KNIGHT}
                else -> {
                    PieceType.PAWN}
            }
            if(movingPiece.color == Color.WHITE){
                when(movingPiece.pieceType){
                    PieceType.QUEEN ->{
                        movingPiece.resID = R.drawable.ic_white_queen
                    }
                    PieceType.ROOK ->{
                        movingPiece.resID = R.drawable.ic_white_rook
                    }
                    PieceType.BISHOP ->{
                        movingPiece.resID = R.drawable.ic_white_bishop
                    }
                    PieceType.KNIGHT ->{
                        movingPiece.resID = R.drawable.ic_white_knight
                    }else ->{
                        Log.e(BoardTAG, "Tried to promote to: ${movingPiece.pieceType} but you can't promote to that.")
                    }
                }
            }else{
                when(movingPiece.pieceType){
                    PieceType.QUEEN ->{
                        movingPiece.resID = R.drawable.ic_black_queen
                    }
                    PieceType.ROOK ->{
                        movingPiece.resID = R.drawable.ic_black_rook
                    }
                    PieceType.BISHOP ->{
                        movingPiece.resID = R.drawable.ic_black_bishop
                    }
                    PieceType.KNIGHT ->{
                        movingPiece.resID = R.drawable.ic_black_knight
                    }else ->{
                        Log.e(BoardTAG, "Tried to promote to: ${movingPiece.pieceType} but you can't promote to that.")
                    }
                }
            }
            movePiece(startSquare, endSquare)
        }

        return this

    }
    fun promotePawnAt(pawnSquare: Square, promotionType: PieceType){
        pawnSquare.piece?.pieceType = promotionType
        if(squaresArray[pawnSquare.rowIndex][pawnSquare.colIndex].piece?.pieceType != promotionType){
            squaresArray[pawnSquare.rowIndex][pawnSquare.colIndex].piece?.pieceType = promotionType
        }
    }
    fun isCastlingMove(start: Square, end: Square): Boolean {
        val startPiece = start.piece
        val endPiece = end.piece
        if (startPiece == null || endPiece == null) {
            return false
        }
        if (startPiece.hasMoved || endPiece.hasMoved) {
            return false
        }
        if (endPiece.pieceType != PieceType.ROOK || startPiece.pieceType != PieceType.KING) {
            return false
        }
        if (endPiece.color !== startPiece.color) {
            return false
        }
        Log.i(BoardTAG,"move from square: " + start + "to " + end + "is a castling move")
        return true
    }
    fun isValidMove(start: Square, end: Square): Boolean {
        val movingPiece = start.piece
        if(movingPiece==null){
            Log.e(BoardTAG, "Moving piece is null so the move is invalid")
            return false
        }
        if (!canPieceMoveTo(movingPiece, end)) {
            Log.e(BoardTAG,"Move is invalid because the piece can't move to there")
            return false
        }
        val isCastlingMove = isCastlingMove(start, end)
        val isFirstMove = !movingPiece.hasMoved
        val capturedPiece = end.piece
        if (isCastlingMove) {
            makeCastlingMove(start, end)
        } else {
            movePieceTemp(start, end)
        }

        val isLegalMove = !isKingInCheck(movingPiece.color === Color.WHITE)

        if (isCastlingMove) {
            undoCastlingMove(start, end)
        } else {
            movePieceTemp(end, start)
            end.piece = capturedPiece
        }
        if (isFirstMove) {
            movingPiece.hasMoved = false
        }
        if(!isLegalMove){
            Log.e(BoardTAG, "Move is not legal")
        }
        return isLegalMove
    }

    private fun canPieceMoveTo(piece: Piece, square: Square): Boolean {
        var result = false
        for (movableSquare in piece.getMovableSquares(this)) {
            if (movableSquare == square) {
                result = true
                break
            }
        }
        return result
    }

    private fun canPieceThreatenSquare(piece: Piece, square: Square): Boolean {
        var result = false
        for (movableSquare in piece.getThreatenedSquares(this)) {
            if (movableSquare == square) {
                result = true
                break
            }
        }
        return result
    }
    fun canPlayerPlay(playerColor: Color): Boolean {
        Log.i(BoardTAG, "checking if player: " + playerColor + "can play")
        for (row in 0..7) {
            for (col in 0..7) {
                val currentSquare = squaresArray[row][col]
                if (currentSquare.piece != null && currentSquare.piece!!.color === playerColor) {
                    if (doesPieceHaveAMove(currentSquare.piece!!, currentSquare)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    private fun doesPieceHaveAMove(piece: Piece, startSquare: Square): Boolean {
        for (targetSquare in piece.getMovableSquares(this)) {
            if (isValidMove(startSquare, targetSquare)) {
                Log.e(BoardTAG,"The piece on square: $startSquare  can move to the square: $targetSquare")
                return true
            }
        }
        return false
    }

    fun isKingInCheck(isWhite: Boolean): Boolean {
        if(whiteKing == null || blackKing == null){
            Log.e(BoardTAG, "isKingInCheck won't work cause one of the kings is null. black: $blackKing, white: $whiteKing")
        }
        val kingSquare: Square = if (isWhite) {
            whiteKing?.let { getSquareAt(it.colIndex, it.rowIndex) }!!
        } else {
            blackKing?.let { getSquareAt(it.colIndex, it.rowIndex) }!!
        }
        for (row in 0..7) {
            for (col in 0..7) {
                val (_, piece) = squaresArray[row][col]
                if (piece != null && isWhite != (piece.color === Color.WHITE)) {
                    if (canPieceThreatenSquare(piece, kingSquare)) {
                        Log.e(BoardTAG,"\n \n isKingInCheck king is in check. by piece: \n$piece\n \n")
                        return true
                    }
                }
            }
        }
        return false
    }

    fun setPieceAt(col: Int, row: Int, piece: Piece?) {
        val square = squaresArray[row][col]
        square.piece = piece
    }
    private fun movePiece(start: Square, end: Square) {
        //this function does not care if the move is legal. just makes it (assumes piece at start isn't null).
        val movingPiece = start.piece
        if (movingPiece!!.pieceType === PieceType.PAWN && start.colIndex != end.colIndex) {
            if (end.piece == null) {
                squaresArray[start.rowIndex][end.colIndex].piece = null
            }
        }
        movePieceTemp(start, end)
        phantomPawnSquare =
            if (movingPiece!!.pieceType === PieceType.PAWN && abs(start.rowIndex - end.rowIndex) == 2) {
                squaresArray[(start.rowIndex + end.rowIndex) / 2][start.colIndex]
            } else {
                null
            }
    }

    private fun movePieceTemp(start: Square, end: Square) {
        //this function does not care if the move is legal. just makes it (assumes piece at start isn't null).

        val movingPiece = start.piece
        movingPiece!!.hasMoved = true

        start.piece = null
        end.piece = movingPiece
        movingPiece.move(end)
        if(movingPiece.pieceType == PieceType.KING){
            if(movingPiece.color == Color.WHITE){
                whiteKing = movingPiece as King
            }else{
                blackKing = movingPiece as King
            }
        }
    }
    private fun makeCastlingMove(start: Square, end: Square) {
        //doesn't care if the move is legal. start -> king, end -> rook
        if (end.colIndex > start.colIndex) {
            //O-O
            movePiece(start, squaresArray[end.rowIndex][end.colIndex - 1])
            movePiece(end, squaresArray[start.rowIndex][start.colIndex + 1])
        } else {
            //O-O-O
            movePiece(start, squaresArray[start.rowIndex][start.colIndex - 2])
            movePiece(end, squaresArray[start.rowIndex][start.colIndex - 1])
        }
    }

    private fun undoCastlingMove(start: Square, end: Square) {
        //doesn't care if the move is legal. start -> king, end -> rook
        if (end.colIndex > start.colIndex) {
            //O-O
            movePiece(squaresArray[end.rowIndex][end.colIndex - 1], start)
            movePiece(squaresArray[start.rowIndex][start.colIndex + 1], end)
        } else {
            //O-O-O
            movePiece(squaresArray[start.rowIndex][start.colIndex - 2], start)
            movePiece(squaresArray[start.rowIndex][start.colIndex - 1], end)
        }
        squaresArray[start.rowIndex][start.colIndex].piece!!.hasMoved = false
        squaresArray[end.rowIndex][end.colIndex].piece!!.hasMoved = false
    }

    fun beamSearchThreat(startRow: Int, startCol: Int, color: Color, incrementCol: Int, incrementRow: Int): Array<Square> {
        val threatenedSquares = ArrayList<Square>()
        var curRow = startRow + incrementRow
        var curCol = startCol + incrementCol
        while (curCol >= 0 && curRow >= 0 && curCol <= 7 && curRow <= 7) {
            val curSquare = squaresArray[curRow][curCol]
            val curPiece = curSquare.piece
            if (curPiece != null) {
                if (curPiece.color !== color) {
                    threatenedSquares.add(curSquare)
                }
                break
            }
            threatenedSquares.add(curSquare)
            curCol += incrementCol
            curRow += incrementRow
        }
        val sqrArr = arrayOfNulls<Square>(threatenedSquares.size)
        return threatenedSquares.toArray(sqrArr)
    }

    fun pawnSpotSearchThreat(startRow: Int, startCol: Int, color: Color, incrementCol: Int, incrementRow: Int, isAttacking: Boolean): Square? {
        val curRow = startRow + incrementRow
        val curCol = startCol + incrementCol
        if (curRow >= 8 || curCol >= 8 || curRow < 0 || curCol < 0) {
            return null
        }
        val curSquare = squaresArray[curRow][curCol]
        val curPiece = curSquare.piece
        if (isAttacking && curPiece == null && phantomPawnSquare == curSquare) {
            return curSquare
        }
        if (curPiece != null) {
            if (!isAttacking) {
                return null
            }
            return if (curPiece.color !== color) {
                curSquare
            } else {
                null
            }
        }
        return if (!isAttacking) {
            curSquare
        } else null
    }

    fun spotSearchThreat(startRow: Int, startCol: Int, color: Color, incrementCol: Int, incrementRow: Int): Square? {
        val curRow = startRow + incrementRow
        val curCol = startCol + incrementCol
        if (curRow >= 8 || curCol >= 8 || curRow < 0 || curCol < 0) {
            return null
        }
        val curSquare = squaresArray[curRow][curCol]
        val curPiece = curSquare.piece
        return if (curPiece != null) {
            if (curPiece.color !== color) {
                curSquare
            } else {
                null
            }
        } else curSquare
    }

    override fun toString(): String {
        var desc = " \n"
        for (i in 7 downTo -1 + 1) {
            desc += i
            desc += " "
            for (j in 0..7) {
                val pieceAt: Piece? = squaresArray[i][j].piece
                if (pieceAt == null) {
                    desc +=". "
                } else {
                    val isWhite: Boolean = pieceAt.color == Color.WHITE
                    if (pieceAt.pieceType === PieceType.KING) {
                        desc += if (isWhite) {
                            "k "
                        } else {
                            "K "
                        }
                    } else if (pieceAt.pieceType === PieceType.QUEEN) {
                        desc += if (isWhite) {
                            "q "
                        } else {
                            "Q "
                        }
                    } else if (pieceAt.pieceType === PieceType.ROOK) {
                        desc += if (isWhite) {
                            "r "
                        } else {
                            "R "
                        }
                    } else if (pieceAt.pieceType === PieceType.BISHOP) {
                        desc += if (isWhite) {
                            "b "
                        } else {
                            "B "
                        }
                    } else if (pieceAt.pieceType === PieceType.KNIGHT) {
                        desc += if (isWhite) {
                            "n "
                        } else {
                            "N "
                        }
                    } else if (pieceAt.pieceType === PieceType.PAWN) {
                        desc += if (isWhite) {
                            "p "
                        } else {
                            "P "
                        }
                    }
                }
            }
            desc +="\n"
        }
        desc += "\n And phantom square is: ${phantomPawnSquare.toString()}"
        return desc
    }

    public override fun clone(): Board {


        val tempJsonBoard = gson.toJson(this)

        return gson.fromJson(tempJsonBoard, Board::class.java)

    }

    override fun equals(other: Any?): Boolean {
        return if(other != null && other is Board){
            val thisJson = gson.toJson(this)
            val otherJson = gson.toJson(other)
            thisJson.equals(otherJson)
        }else{
            super.equals(other)
        }

    }

    override fun hashCode(): Int {
        var result = squaresArray.hashCode()
        result = 31 * result + (whiteKing?.hashCode() ?: 0)
        result = 31 * result + (blackKing?.hashCode() ?: 0)
        result = 31 * result + (phantomPawnSquare?.hashCode() ?: 0)
        return result
    }


}
