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
import androidx.core.content.ContextCompat
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator

import com.hellostranger.chess_app.utils.Constants.SCALE_FACTOR
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
    var chessGameInterface : ChessGameInterface? = null

    init {
        loadBitmaps()
    }

    fun flipBoard(){
        isWhiteOnBottom = !isWhiteOnBottom
    }

    override fun onDraw(canvas: Canvas) {
        val chessBoardSide = Integer.min(width, height) * SCALE_FACTOR
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
            }
            MotionEvent.ACTION_UP ->{
                val targetCoord = getCoordFromEvent(event.x, event.y)
                if (!targetCoord.isValidSquare || targetCoord == movingPieceStartCoord || !movingPieceStartCoord.isValidSquare) {
                    resetMovingPiece()
                    invalidate()
                    return false
                }
                chessGameInterface?.playMove(movingPieceStartCoord, targetCoord)
                resetMovingPiece()
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
    }

    private fun drawChessBoard(canvas: Canvas){
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
    }

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