package com.hellostranger.chess_app.gameHelpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator

import com.hellostranger.chess_app.utils.Constants.scaleFactor
import com.hellostranger.chess_app.utils.MyApp

@ExperimentalUnsignedTypes
class ChessView(context : Context?, attrs : AttributeSet?) : View(context, attrs) {
    private val lightColor       : Int = ContextCompat.getColor(context!!, R.color.lightSquare)
    private val darkColor        : Int = ContextCompat.getColor(context!!, R.color.darkSquare)
    private val tintedLightColor : Int = ContextCompat.getColor(context!!, R.color.tintedLightSquare)
    private val tintedDarkColor  : Int = ContextCompat.getColor(context!!, R.color.tintedDarkSquare)

    private val pieceSprites = PieceResIDs(MyApp.pieceTheme)

    private val paint = Paint()
    private val pieceBitmaps = mutableMapOf<Int, Bitmap>()

    private var movingPieceBitmap : Bitmap? = null

    private var movingPieceStartCoord : Coord = Coord(-1, -1)
    private var lastMoveMade : Move = Move.NullMove

/*
    private var lastMoveStartCol : Int = -1
    private var lastMoveStartRow : Int = -1
    private var lastMoveEndCol : Int = -1
    private var lastMoveEndRow : Int = -1
*/

    private var movingPieceX : Float = -1f
    private var movingPieceY : Float = -1f

    var isWhiteOnBottom : Boolean = true
        private set
    private var moveGenerator = MoveGenerator()
    private val board : Board?
        get() = chessGameInterface?.getBoard()
    var isLocked : Boolean = false

    private var originX = 20f
    private var originY = 200f
    private var cellSide : Float = 130f
//    var gameMode : String =""
    var chessGameInterface : ChessGameInterface? = null

    init {
        loadBitmaps()
    }

    fun flipBoard(){
        isWhiteOnBottom = !isWhiteOnBottom
    }

    override fun onDraw(canvas: Canvas) {
        val chessBoardSide = Integer.min(width, height) * scaleFactor
        cellSide = chessBoardSide / 8f
        originX= (width - chessBoardSide) / 2f
        originY = (height - chessBoardSide) / 2f
        board?.let {
            if (it.allGameMoves.isNotEmpty()) {
                lastMoveMade = it.allGameMoves.last()
            }
        }

        drawChessBoard(canvas)
        drawPieces(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isLocked) {
            return false
        }
        when (event?.action){
            MotionEvent.ACTION_DOWN ->{
                val coord = getCoordFromEvent(event.x, event.y)
                Log.e("ChessView","Touching rank: ${coord.rankIndex} and file: ${coord.fileIndex}")
                if (!coord.isValidSquare) {
                    return false
                }
                board?.let {
                    val piece = it.square[BoardHelper.indexFromCoord(coord)]
                    if (piece != Piece.NONE) {
                        movingPieceStartCoord = coord
                        movingPieceBitmap = pieceBitmaps[piece]
                    }
                }
//                chessGameInterface?.pieceAt(tempFromCol,  tempFromRow, isFlipped)?.let{
//                    movingPiece = it
//                    movingPieceBitmap = pieceBitmaps[it.resID]
//                    movingPieceMoves = chessGameInterface?.getPiecesMoves(it)
//                    fromRow = tempFromRow
//                    fromCol = tempFromCol
//                }
            }
            MotionEvent.ACTION_UP ->{
                val targetCoord = getCoordFromEvent(event.x, event.y)
                if (!targetCoord.isValidSquare || targetCoord == movingPieceStartCoord) {
                    resetMovingPiece()
                    invalidate()
                    return false
                }
                if (!movingPieceStartCoord.isValidSquare) {
                    Log.e("ChessView", "How tf is the movingPieceStartCoord not valid?? it is (file, rank): ${movingPieceStartCoord.fileIndex}, ${movingPieceStartCoord.rankIndex}")
                }
                chessGameInterface?.playMove(movingPieceStartCoord, targetCoord)
                resetMovingPiece()


//                if((fromcol != col || fromrow != row) && fromcol != -1){
//                    val movemessage = movemessage("", fromcol, fromrow, col, row, movetype.regular)
//                    chessgameinterface?.playmove(movemessage, isflipped)
//                }
//                fromcol = -1
//                fromrow = -1
//                movingpiecebitmap = null
//                movingpiece = null
//                movingpiecemoves = null
                invalidate()
            }
            MotionEvent.ACTION_MOVE ->{
                movingPieceX = event.x
                movingPieceY = event.y
                invalidate()
            }
        }
        return true
    }

    private fun resetMovingPiece() {
        movingPieceBitmap = null
        movingPieceStartCoord = Coord(-1, -1)
    }
    private fun getCoordFromEvent(x : Float, y : Float) : Coord {
        val file = ((x - originX) / cellSide).toInt()
        val rank = ((y - originY) / cellSide).toInt()
        return if (!isWhiteOnBottom) {
            Coord(7- file, rank)
        } else {
            Coord(file, 7 - rank)
        }
    }

    private fun loadBitmaps(){
        Piece.pieceIndices.forEach {
            pieceBitmaps[it] = BitmapFactory.decodeResource(resources, pieceSprites.getPieceSprite(it))
        }
    }

    private fun drawPieces(canvas: Canvas){
        for (i in 0 until 64) {
            val coord = Coord(i)
            board?.let{
                val piece = it.square[BoardHelper.indexFromCoord(coord)]
                if (coord != movingPieceStartCoord && piece != Piece.NONE) {
                    drawPieceAt(canvas, coord, piece)
                }
            }
        }
//        for(rank in 0..7){
//            for(file in 0..7){
//                val coord = Coord(file, rank)
//                 board?.let{
//                    val piece = it.square[BoardHelper.indexFromCoord(coord.fileIndex, coord.rankIndex)]
//                    if (coord != movingPieceStartCoord && piece != Piece.NONE) {
//                        drawPieceAt(canvas, coord, piece)
//                    }
//                }
///*
//                chessGameInterface?.pieceAt(col, row, isFlipped)?.let{
//                    if(row != fromRow || col != fromCol || it != movingPiece){
//                        drawPieceAt(canvas, col, row, it.resID)
//                    }
//                }
//*/
//
//            }
//        }

        movingPieceBitmap?.let {
            canvas.drawBitmap(it,
                null,
                RectF(
                    movingPieceX - cellSide / 2,
                    movingPieceY - cellSide / 2,
                    movingPieceX + cellSide / 2,
                    movingPieceY + cellSide / 2),
                paint
            )
        }
    }

    private fun drawPieceAt(canvas: Canvas, coord: Coord, piece : Int){


        val pieceBitmap = pieceBitmaps[piece]!!

        canvas.drawBitmap(pieceBitmap, null, squareFromCoord(coord), paint)
/*
        canvas.drawBitmap(pieceBitmap,
            null,
            RectF(originX + col * cellSide,
                originY + (7-row) * cellSide,
                originX + (col+1) * cellSide,
                originY + ((7-row) + 1) * cellSide),
            paint
        )
*/


    }

    private fun drawChessBoard(canvas: Canvas){

/*
        if(lastMove != null){
            if(isFlipped){
                lastMoveStartCol = 7 - lastMove.startCol
                lastMoveStartRow = 7 - lastMove.startRow
                lastMoveEndCol = 7 - lastMove.endCol
                lastMoveEndRow = 7 - lastMove.endRow
            } else{
                lastMoveStartCol = lastMove.startCol
                lastMoveStartRow = lastMove.startRow
                lastMoveEndCol = lastMove.endCol
                lastMoveEndRow = lastMove.endRow
            }
        }
*/
//        for(row in 0..7){
//            for (col in 0..7) {
//                val isDark = if(isFlipped) {
//                    (col+row) % 2 == 0
//                } else {
//                    (col+row) % 2 != 0
//                }
//
//                drawSquareAt(canvas, col, row, isDark)
//            }
//        }
        for (i in 0 until 64) {
            drawSquareAt(canvas, Coord(i))
        }
        drawLegalMoves(canvas)

    }
    private fun drawLegalMoves(canvas: Canvas) {
        board ?: return
        if (!movingPieceStartCoord.isValidSquare) {
            return
        }
        val moves : Array<Move> = moveGenerator.generateMoves(board!!)
        for (i in moves.indices) {
            val move = moves[i]
            if (move.startSquare != BoardHelper.indexFromCoord(movingPieceStartCoord)) {
                continue
            }
            val targetedPaint = Paint()
            targetedPaint.color = Color.GRAY
            var radius = 20f
            if (board!!.square[move.targetSquare] != Piece.NONE) {
                targetedPaint.style = Paint.Style.STROKE
                targetedPaint.strokeWidth = 6f
                radius = (cellSide - targetedPaint.strokeWidth) / 2
            }
            val fileIndex = if (isWhiteOnBottom) BoardHelper.fileIndex(move.targetSquare) else 7 - BoardHelper.fileIndex(move.targetSquare)
            val rankIndex = if (isWhiteOnBottom) 7 - BoardHelper.rankIndex(move.targetSquare) else BoardHelper.rankIndex(move.targetSquare)
            canvas.drawCircle(
                originX + (fileIndex + 0.5f) * cellSide,
                originY + (rankIndex + 0.5f) * cellSide,
                radius,
                targetedPaint
            )
        }
    }
    private fun getSquareColour(coord : Coord) : Int{
        val coordIndex : Int = BoardHelper.indexFromCoord(coord)
        return if (coord.isLightSquare) {
            if ((!lastMoveMade.isNull && (coordIndex == lastMoveMade.startSquare || coordIndex == lastMoveMade.targetSquare)) || coord == movingPieceStartCoord) {
                tintedLightColor
            } else {
                lightColor
            }
        } else {
            if ((!lastMoveMade.isNull && (coordIndex == lastMoveMade.startSquare || coordIndex == lastMoveMade.targetSquare)) || coord == movingPieceStartCoord) {
                tintedDarkColor
            } else {
                darkColor
            }
        }
    }

    private fun drawSquareAt(canvas: Canvas, coord: Coord){
        paint.color = getSquareColour(coord)
        canvas.drawRect(squareFromCoord(coord), paint)
/*
        val isChosenSquare : Boolean = (col == fromCol && row == (7-fromRow)) ||
                (col == lastMoveStartCol && row == (7-lastMoveStartRow)) ||
                (col == lastMoveEndCol && row == (7-lastMoveEndRow))
        if(isDark){
            if(isFlipped){
                paint.color = if(isChosenSquare) tintedLightColor else lightColor
            } else{
                paint.color = if(isChosenSquare) tintedDarkColor else darkColor
            }
        }else{
            if(isFlipped){
                paint.color = if(isChosenSquare) tintedDarkColor else darkColor
            } else{
                paint.color = if(isChosenSquare) tintedLightColor else lightColor
            }
        }
*/
//        canvas.drawRect(
//            originX + col * cellSide,
//            originY + row * cellSide,
//            originX + (col + 1)*cellSide,
//            originY + (row + 1) * cellSide,
//            paint
//        )

//        if(isSquareInMovesList(col, 7-row)){
//            val targetedPaint = Paint()
//            targetedPaint.color = Color.GRAY
//            if(chessGameInterface!!.pieceAt(col,7-row,isFlipped) == null){
//
//                canvas.drawCircle(
//                    originX + (col + 0.5f)*cellSide,
//                    originY + ((row) + 0.5f)*cellSide,
//                    20f,
//                    targetedPaint
//                )
//            } else{
//                targetedPaint.style = Paint.Style.STROKE
//                targetedPaint.strokeWidth = 6f
//                canvas.drawCircle(
//                    originX + (col + 0.5f)*cellSide,
//                    originY + ((row) + 0.5f)*cellSide,
//                    (cellSide-targetedPaint.strokeWidth)/2,
//                    targetedPaint
//                )
//            }
//        }

    }
//    private fun isSquareInMovesList(col : Int, row : Int) : Boolean{
//        movingPieceMoves ?: return false
//        if(isFlipped){
//            for (square : Square in movingPieceMoves!!){
//                if(square.colIndex == (7-col) && square.rowIndex == (7-row)){
//                    return true
//                }
//            }
//        }else{
//            for (square : Square in movingPieceMoves!!){
//                if(square.colIndex == col && square.rowIndex == row){
//                    return true
//                }
//            }
//        }
//
//        return false
//    }

    private fun squareFromCoord(rank : Int, file : Int) : RectF{
        return RectF(
            originX + file * cellSide,
            originY + rank * cellSide,
            originX + (file + 1) * cellSide,
            originY + (rank + 1) * cellSide
        )
    }
    private fun squareFromCoord(coord: Coord) : RectF {
        return if (!isWhiteOnBottom) {
            squareFromCoord(coord.rankIndex, 7 - coord.fileIndex)
        } else {
            squareFromCoord(7 - coord.rankIndex, coord.fileIndex)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val smaller = Integer.min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(smaller, smaller)
    }

}