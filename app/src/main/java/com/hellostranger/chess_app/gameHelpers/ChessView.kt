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
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.models.gameModels.Square
import com.hellostranger.chess_app.models.gameModels.enums.MoveType
import com.hellostranger.chess_app.models.gameModels.pieces.Piece
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.Constants.imgResIDs
import com.hellostranger.chess_app.utils.Constants.scaleFactor

class ChessView(context : Context?, attrs : AttributeSet?) : View(context, attrs) {
    private var lightColor = ContextCompat.getColor(context!!, R.color.lightSquare)
    private var darkColor = ContextCompat.getColor(context!!, R.color.darkSquare)
    private var tintedLightColor = ContextCompat.getColor(context!!, R.color.tintedLightSquare)
    private var tintedDarkColor = ContextCompat.getColor(context!!, R.color.tintedDarkSquare)

    private val paint = Paint()
    private val pieceBitmaps = mutableMapOf<Int, Bitmap>()

    private var movingPieceBitmap : Bitmap? = null
    private var movingPiece : Piece? = null

    private var fromCol : Int = -1
    private var fromRow : Int = -1
    private var movingPieceX : Float = -1f
    private var movingPieceY : Float = -1f
    private var movingPieceMoves : ArrayList<Square>? = null

    var isFlipped : Boolean = false
    private var originX = 20f
    private var originY = 200f
    private var cellSide : Float = 130f
    var gameMode : String =""
    var chessGameInterface : ChessGameInterface? = null

    init {
        loadBitmaps()
    }

    fun flipBoard(){
        isFlipped = !isFlipped
    }

    override fun onDraw(canvas: Canvas?){
        canvas ?: return

        val chessBoardSide = Integer.min(width, height) * scaleFactor
        cellSide = chessBoardSide / 8f
        originX= (width - chessBoardSide) / 2f
        originY = (height - chessBoardSide) / 2f

        drawChessBoard(canvas)
        drawPieces(canvas)

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(gameMode == Constants.ANALYSIS_MODE){
            return false
        }
        if(!chessGameInterface!!.isOnLastMove()){
            chessGameInterface!!.goToLastMove()
            Log.e("TAG", "Not on last move.")
            this.invalidate()
            return false
        }
        when (event?.action){
            MotionEvent.ACTION_DOWN ->{
                fromCol = ((event.x - originX) / cellSide).toInt()
                fromRow = 7 - ((event.y - originY) / cellSide).toInt()
                chessGameInterface?.pieceAt(fromCol, fromRow, isFlipped)?.let{
                    movingPiece = it
                    movingPieceBitmap = pieceBitmaps[it.resID]
                    movingPieceMoves=chessGameInterface?.getPiecesMoves(it)
                }
            }
            MotionEvent.ACTION_UP ->{
                val col = ((event.x - originX) / cellSide).toInt()
                val row = 7 - ((event.y - originY) / cellSide).toInt()
                if(fromCol != col || fromRow != row){
                    val moveMessage = MoveMessage("", fromCol, fromRow, col, row, MoveType.REGULAR)
                    chessGameInterface?.playMove(moveMessage, isFlipped)
                }
                fromCol = -1
                fromRow = -1
                movingPieceBitmap = null
                movingPiece = null
                movingPieceMoves = null
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

    private fun loadBitmaps(){
        imgResIDs.forEach{
            pieceBitmaps[it] = BitmapFactory.decodeResource(resources, it)
        }
    }

    private fun drawPieces(canvas: Canvas?){
        for(row in 0..7){
            for(col in 0..7){
                chessGameInterface?.pieceAt(col, row, isFlipped)?.let{
                    if(row != fromRow || col != fromCol || it != movingPiece){
                        drawPieceAt(canvas, col, row, it.resID)
                    }
                }

            }
        }

        movingPieceBitmap?.let {
            canvas?.drawBitmap(it,
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

    private fun drawPieceAt(canvas: Canvas?, col : Int, row : Int, resID : Int){
        canvas ?: return

        val pieceBitmap = pieceBitmaps[resID]!!
        canvas.drawBitmap(pieceBitmap,
            null,
            RectF(originX + col * cellSide,
                originY + (7-row) * cellSide,
                originX + (col+1) * cellSide,
                originY + ((7-row) + 1) * cellSide),
            paint
        )


    }

    private fun drawChessBoard(canvas: Canvas?){
        for(row in 0..7){
            for (col in 0..7) {
                drawSquareAt(canvas, col, row, (col+row) % 2 == 0)
            }
        }
    }

    private fun drawSquareAt(canvas: Canvas?, col : Int,row : Int, isDark : Boolean){
        canvas ?: return
        val isChosenSquare : Boolean = (col == fromCol && row == (7-fromRow))
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
        canvas.drawRect(
            originX + col * cellSide,
            originY + row * cellSide,
            originX + (col + 1)*cellSide,
            originY + (row + 1) * cellSide,
            paint
        )

        if(isSquareInMovesList(col, 7-row)){
            val targetedPaint = Paint()
            targetedPaint.color = Color.GRAY
            if(chessGameInterface!!.pieceAt(col,7-row,isFlipped) == null){

                canvas.drawCircle(
                    originX + (col + 0.5f)*cellSide,
                    originY + ((row) + 0.5f)*cellSide,
                    20f,
                    targetedPaint
                )
            } else{
                targetedPaint.style = Paint.Style.STROKE
                targetedPaint.strokeWidth = 6f
                canvas.drawCircle(
                    originX + (col + 0.5f)*cellSide,
                    originY + ((row) + 0.5f)*cellSide,
                    (cellSide-targetedPaint.strokeWidth)/2,
                    targetedPaint
                )
            }
        }

    }
    private fun isSquareInMovesList(col : Int, row : Int) : Boolean{
        movingPieceMoves ?: return false
        if(isFlipped){
            for (square : Square in movingPieceMoves!!){
                if(square.colIndex == (7-col) && square.rowIndex == (7-row)){
                    return true
                }
            }
        }else{
            for (square : Square in movingPieceMoves!!){
                if(square.colIndex == col && square.rowIndex == row){
                    return true
                }
            }
        }

        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val smaller = Integer.min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(smaller, smaller)
    }

}