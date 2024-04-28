package com.hellostranger.chess_app.activities

import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.ActivityPuzzleBinding
import com.hellostranger.chess_app.models.entities.Puzzle
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.gameHelpers.FenConvertor
import com.hellostranger.chess_app.gameHelpers.PuzzlesList
import com.hellostranger.chess_app.gameClasses.Board
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.dto.enums.MoveType
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.enums.PieceType
import com.hellostranger.chess_app.gameClasses.pieces.Piece
import com.hellostranger.chess_app.network.retrofit.puzzleApi.PuzzleRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.MyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PuzzleActivity : BaseActivity(), ChessGameInterface, PopupMenu.OnMenuItemClickListener {
    private lateinit var binding : ActivityPuzzleBinding
    private var fenConvertor = FenConvertor()
    private var boardHistory = mutableListOf<Board>()
    private lateinit var currentPuzzle : Puzzle
    private var isWhite : Boolean = true
    private var mCurrentBoardShown = 0
    private var heldMoveMessage : MoveMessage? = null //To hold the move message while waiting for the player to chose promotion

    private val TAG = "PuzzleActivityTag"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuzzleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(PuzzlesList.instance.getCurrentPuzzle() == null){
            Log.e(TAG, "We don't have any puzzle")
        }
        binding.chessView.chessGameInterface = this

        updateCurrentPuzzle()
        updateUIToStartPuzzle()

        startCurrentPuzzle()

        binding.btnRetry.setOnClickListener {
            retryPuzzle()
        }
        binding.btnNext.setOnClickListener {
            goToNextPuzzle()
        }


    }

    private fun updateUIToStartPuzzle(){
        binding.llNextPuzzle.visibility = View.GONE
        binding.tv1.text = getString(R.string.your_turn)
        binding.tv2.visibility = View.VISIBLE
        if(isWhite){
            binding.ivLogo.setImageResource(R.drawable.ic_white_king)
            binding.tv2.text = getString(R.string.white_to_play)
        } else{
            binding.ivLogo.setImageResource(R.drawable.ic_black_king)
            binding.tv2.text = getString(R.string.black_to_play)
        }

    }

    private fun startCurrentPuzzle(){
        val newBoard = boardHistory[mCurrentBoardShown].clone().movePiece(convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown]))
        newBoard.previousMove = convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown])
        boardHistory.add(newBoard)
        mCurrentBoardShown += 1
    }
    private fun updateCurrentPuzzle(){
        currentPuzzle = PuzzlesList.instance.getCurrentPuzzle()!!
        Game.setInstance(fenConvertor.convertFENToGame(currentPuzzle.fen))

        isWhite = !Game.getInstance()!!.isP1Turn
        boardHistory.clear()
        mCurrentBoardShown = 0;
        boardHistory.add(Game.getInstance()!!.board)
        updatePiecesResId()

    }
    private fun setPiecePromotionMenu(){
        val popupMenu = PopupMenu(this@PuzzleActivity, binding.llInfo)
        if(isWhite){
            popupMenu.menuInflater.inflate(R.menu.popup_white_promotion_options, popupMenu.menu)
        } else{
            popupMenu.menuInflater.inflate(R.menu.popup_black_promotion_options, popupMenu.menu)
        }
        popupMenu.setOnMenuItemClickListener(this@PuzzleActivity)
        popupMenu.show()
    }
    private fun convertMoveToMoveMessage(move : String) : MoveMessage{
        val moveMessage = MoveMessage(
            "",
            startCol = (move[0] - 'a'),
            startRow = (move[1]).digitToInt() -1,
            endCol = (move[2] - 'a'),
            endRow = (move[3]).digitToInt() -1,
            MoveType.REGULAR
        )
        if(move.length > 4){
            if(move[4] == 'q') {
                moveMessage.moveType = MoveType.PROMOTION_QUEEN
            }
            if(move[4] == 'r'){
                moveMessage.moveType = MoveType.PROMOTION_ROOK
            }
            if(move[4] == 'b') {
                moveMessage.moveType = MoveType.PROMOTION_BISHOP
            }
            if(move[4] == 'n'){
                moveMessage.moveType = MoveType.PROMOTION_KNIGHT
            }
        }
        if(move == "e1g1" || move == "e1c1" || move == "e8g8" || move == "e8c8"){
            moveMessage.moveType = MoveType.CASTLE
        }
        return moveMessage
    }

    override fun pieceAt(col: Int, row: Int, isFlipped: Boolean): Piece? {
        return if(isFlipped){
           boardHistory[mCurrentBoardShown].squaresArray[7 - row][7 - col].piece
        } else{
            boardHistory[mCurrentBoardShown].squaresArray[row][col].piece
        }
    }
    private fun isCastlingMove(moveMessage: MoveMessage): Boolean {
        val startSquare =
            boardHistory[mCurrentBoardShown].getSquareAt(moveMessage.startCol, moveMessage.startRow)
        val endSquare = boardHistory[mCurrentBoardShown].getSquareAt(moveMessage.endCol, moveMessage.endRow)

        if (startSquare == null || endSquare == null) {
            Log.e(
                TAG,
                "Move isn't a castling move because one of the squares are null. moveMessage is: $moveMessage and the squares are: $startSquare, $endSquare"
            )
            return false
        }
        return boardHistory[mCurrentBoardShown].isCastlingMove(startSquare, endSquare)
    }

    override fun getPiecesMoves(piece: Piece): ArrayList<Square> {
        val movableSquares : ArrayList<Square> = ArrayList()
        val startSquare : Square = boardHistory[mCurrentBoardShown].getSquareAt(piece.colIndex, piece.rowIndex)!!
        for(square : Square in piece.getMovableSquares(boardHistory[mCurrentBoardShown])){
            if(boardHistory[mCurrentBoardShown].isValidMove(startSquare, square)){
                movableSquares.add(square)
            }
        }
        Log.i(TAG, "the move's of piece: $piece are: $movableSquares")
        return movableSquares
    }

    fun validateMove(moveMessage: MoveMessage) : Boolean {
        val board = boardHistory[mCurrentBoardShown]
        val startSquare = board.getSquareAt(moveMessage.startCol, moveMessage.startRow)!!
        val endSquare = board.getSquareAt(moveMessage.endCol, moveMessage.endRow)!!

        if(startSquare.piece == null || isWhite != (startSquare.piece!!.color == Color.WHITE)) {
            return false
        }
        if(!board.isValidMove(startSquare, endSquare)){
            return false
        }
        return true
    }
    override fun playMove(moveMessage: MoveMessage, isFlipped: Boolean) {
        var updatedMoveMessage = moveMessage
        if(isFlipped){
            updatedMoveMessage = MoveMessage(
                moveMessage.playerEmail, 7 - moveMessage.startCol, 7 - moveMessage.startRow, 7 - moveMessage.endCol, 7 - moveMessage.endRow, moveMessage.moveType
            )
        }
        if(isWhite){
            if(updatedMoveMessage.endRow == 7 &&
                boardHistory[mCurrentBoardShown].squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
                heldMoveMessage == null) {
                heldMoveMessage = updatedMoveMessage
                setPiecePromotionMenu()
                return
            }
        } else{
            if(updatedMoveMessage.endRow == 0 &&
                boardHistory[mCurrentBoardShown].squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
                heldMoveMessage == null) {
                heldMoveMessage = updatedMoveMessage
                setPiecePromotionMenu()
                return
            }
        }
        if(isCastlingMove(updatedMoveMessage)){
            updatedMoveMessage.moveType = MoveType.CASTLE
        }
        if(!validateMove(updatedMoveMessage)) {
            return
        }

        val correctMove = convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown])
        if(
            updatedMoveMessage.startCol == correctMove.startCol &&
            updatedMoveMessage.startRow == correctMove.startRow &&
            updatedMoveMessage.endCol == correctMove.endCol &&
            updatedMoveMessage.endRow == correctMove.endRow &&
            updatedMoveMessage.moveType == correctMove.moveType
        ){
            val isOnLastMove = mCurrentBoardShown == currentPuzzle.moves.size - 1
            showCorrectMoveUi(isOnLastMove);
            playMoveForPuzzle(updatedMoveMessage)
            if(!isOnLastMove){
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    playMoveForPuzzle(convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown]))
                }

            } else{
                binding.llNextPuzzle.visibility = View.VISIBLE
                Toast.makeText(this, "Finished The puzzle!", Toast.LENGTH_LONG).show()

            }

        } else{
            Log.e(TAG, "MovePlayed: $updatedMoveMessage. the move we were expecting: $correctMove")
            showWrongMoveUi()
        }
    }
    private fun retryPuzzle(){
        updateCurrentPuzzle()
        updateUIToStartPuzzle()
        startCurrentPuzzle()
        CoroutineScope(Dispatchers.Main).launch {
            delay(800)
            binding.chessView.postInvalidate()
        }
    }
    private fun goToNextPuzzle(){
        if(PuzzlesList.instance.goToNextPuzzle() != null){
            updateCurrentPuzzle()
            updateUIToStartPuzzle()

            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                startCurrentPuzzle()
                binding.chessView.postInvalidate()
            }
            return
        }
        //TODO: We ran out of puzzles
        showProgressDialog("Please wait while we fetch new puzzles...", false)
        CoroutineScope(Dispatchers.IO).launch {
            val response =
                PuzzleRetrofitClient.instance.getRandomPuzzle(4)
            if (response.isSuccessful && response.body() != null) {
                Log.e("TAG", "(PuzzleActivity) Got puzzles. response: ${response.body()}")
                PuzzlesList.instance.addPuzzles(response.body()!!)
                Log.e("TAG", "(PuzzleActivity) PuzzleList: ${PuzzlesList.instance}")
            }
            withContext(Dispatchers.Main){
                goToNextPuzzle()
            }
        }
    }
    private fun playMoveForPuzzle(updatedMoveMessage: MoveMessage){
        val newBoard = boardHistory[mCurrentBoardShown].clone().movePiece(updatedMoveMessage)
        newBoard.previousMove = updatedMoveMessage
        boardHistory.add(newBoard)
        mCurrentBoardShown += 1;
        binding.chessView.postInvalidate()
    }

    private fun showCorrectMoveUi(isLastMove : Boolean){
        binding.ivLogo.setImageResource(R.drawable.ic_green_checkmark)
        if(isLastMove){
            binding.tv1.text = getString(R.string.success)
            binding.tv2.visibility = View.INVISIBLE
            //TODO: Add the option to go to next puzzle
        } else{
            binding.tv1.text = getString(R.string.best_move)
            binding.tv2.text = getString(R.string.keep_going)

        }
    }
    private fun showWrongMoveUi(){
        binding.ivLogo.setImageResource(R.drawable.ic_wrong_move)
        binding.tv1.text = getString(R.string.not_the_move)
        binding.tv2.text = getString(R.string.try_again)

    }


    override fun isOnLastMove(): Boolean {
       return mCurrentBoardShown == boardHistory.size -1
    }

    override fun goToLastMove() {
        mCurrentBoardShown = boardHistory.size - 1
        binding.chessView.invalidate()
    }

    override fun getLastMovePlayed(): MoveMessage? {
        return boardHistory[mCurrentBoardShown].previousMove
    }

    override fun getPieceResIds(): Set<Int> {
        if (MyApp.pieceTheme == MyApp.PieceTheme.DEFAULT) {
            return Constants.imgResIDs
        } else if (MyApp.pieceTheme == MyApp.PieceTheme.PLANT) {
            return Constants.plantResIDs
        }

        return Constants.imgResIDs

    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if(heldMoveMessage == null){
            Log.e(TAG, "held Move message is null")
            return false
        }
        when (menuItem.itemId){
            R.id.queen -> {
                heldMoveMessage!!.moveType = MoveType.PROMOTION_QUEEN
                playMove(heldMoveMessage!!, false)
                return true
            }
            R.id.rook -> {
                heldMoveMessage!!.moveType = MoveType.PROMOTION_ROOK
                playMove(heldMoveMessage!!, false)
                return true
            }
            R.id.bishop -> {
                heldMoveMessage!!.moveType = MoveType.PROMOTION_BISHOP
                playMove(heldMoveMessage!!, false)
                return true
            }R.id.knight -> {
            heldMoveMessage!!.moveType = MoveType.PROMOTION_KNIGHT
            playMove(heldMoveMessage!!, false)
            return true
        }
            else -> {return false}
        }
    }


}