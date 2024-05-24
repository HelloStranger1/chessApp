package com.hellostranger.chess_app.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import com.hellostranger.chess_app.databinding.ActivityPuzzleBinding
import com.hellostranger.chess_app.models.entities.Puzzle
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.gameHelpers.PuzzlesList
import com.hellostranger.chess_app.network.retrofit.puzzleApi.PuzzleRetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos

private const val TAG = "PuzzleActivityTag"
@ExperimentalUnsignedTypes
class PuzzleActivity : BaseActivity(), ChessGameInterface, PopupMenu.OnMenuItemClickListener {
    private lateinit var binding : ActivityPuzzleBinding
    private lateinit var board : Board
    private lateinit var currentPuzzle : Puzzle
    private var currentMoveShown = 0
    private var isWhite : Boolean = true
    private var heldMoveMessage : Move = Move.NullMove //To hold the move message while waiting for the player to chose promotion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuzzleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(PuzzlesList.instance.getCurrentPuzzle() == null){
            Log.e(TAG, "We don't have any puzzle")
        }
        binding.chessView.chessGameInterface = this

        updateCurrentPuzzle()
        startCurrentPuzzle()
        updateUIToStartPuzzle()


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
        board = Board.createBoard(currentPuzzle.fen)
        board.makeMove(MoveUtility.getMoveFromUCIName(currentPuzzle.moves[0], board))
        isWhite = board.isWhiteToMove
        currentMoveShown++
    }
    private fun updateCurrentPuzzle(){
        currentPuzzle = PuzzlesList.instance.getCurrentPuzzle()!!
        currentMoveShown = 0
        // updatePiecesResId()

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
//    private fun convertMoveToMoveMessage(move : String) : MoveMessage{
//        val moveMessage = MoveMessage(
//            "",
//            startCol = (move[0] - 'a'),
//            startRow = (move[1]).digitToInt() -1,
//            endCol = (move[2] - 'a'),
//            endRow = (move[3]).digitToInt() -1,
//            MoveType.REGULAR
//        )
//        if(move.length > 4){
//            if(move[4] == 'q') {
//                moveMessage.moveType = MoveType.PROMOTION_QUEEN
//            }
//            if(move[4] == 'r'){
//                moveMessage.moveType = MoveType.PROMOTION_ROOK
//            }
//            if(move[4] == 'b') {
//                moveMessage.moveType = MoveType.PROMOTION_BISHOP
//            }
//            if(move[4] == 'n'){
//                moveMessage.moveType = MoveType.PROMOTION_KNIGHT
//            }
//        }
//        if(move == "e1g1" || move == "e1c1" || move == "e8g8" || move == "e8c8"){
//            moveMessage.moveType = MoveType.CASTLE
//        }
//        return moveMessage
//        return MoveMessage("", 0)
//    }

//    override fun pieceAt(col: Int, row: Int, isFlipped: Boolean): Piece? {
//        return if(isFlipped){
//           boardHistory[mCurrentBoardShown].squaresArray[7 - row][7 - col].piece
//        } else{
//            boardHistory[mCurrentBoardShown].squaresArray[row][col].piece
//        }
//    }
//    private fun isCastlingMove(moveMessage: MoveMessage): Boolean {
//        val startSquare =
//            boardHistory[mCurrentBoardShown].getSquareAt(moveMessage.startCol, moveMessage.startRow)
//        val endSquare = boardHistory[mCurrentBoardShown].getSquareAt(moveMessage.endCol, moveMessage.endRow)
//
//        if (startSquare == null || endSquare == null) {
//            Log.e(
//                TAG,
//                "Move isn't a castling move because one of the squares are null. moveMessage is: $moveMessage and the squares are: $startSquare, $endSquare"
//            )
//            return false
//        }
//        return boardHistory[mCurrentBoardShown].isCastlingMove(startSquare, endSquare)
//    }

//    override fun getPiecesMoves(piece: Piece): ArrayList<Square> {
//        val movableSquares : ArrayList<Square> = ArrayList()
//        val startSquare : Square = boardHistory[mCurrentBoardShown].getSquareAt(piece.colIndex, piece.rowIndex)!!
//        for(square : Square in piece.getMovableSquares(boardHistory[mCurrentBoardShown])){
//            if(boardHistory[mCurrentBoardShown].isValidMove(startSquare, square)){
//                movableSquares.add(square)
//            }
//        }
//        Log.i(TAG, "the move's of piece: $piece are: $movableSquares")
//        return movableSquares
//    }

//    fun validateMove(moveMessage: MoveMessage) : Boolean {
//        val board = boardHistory[mCurrentBoardShown]
//        val startSquare = board.getSquareAt(moveMessage.startCol, moveMessage.startRow)!!
//        val endSquare = board.getSquareAt(moveMessage.endCol, moveMessage.endRow)!!
//
//        if(startSquare.piece == null || isWhite != (startSquare.piece!!.color == Color.WHITE)) {
//            return false
//        }
//        if(!board.isValidMove(startSquare, endSquare)){
//            return false
//        }
//        return true
//    }
//    override fun playMove(moveMessage: MoveMessage, isFlipped: Boolean) {
//        var updatedMoveMessage = moveMessage
//        if(isFlipped){
//            updatedMoveMessage = MoveMessage(
//                moveMessage.playerEmail, 7 - moveMessage.startCol, 7 - moveMessage.startRow, 7 - moveMessage.endCol, 7 - moveMessage.endRow, moveMessage.moveType
//            )
//        }
//        if(isWhite){
//            if(updatedMoveMessage.endRow == 7 &&
//                boardHistory[mCurrentBoardShown].squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
//                heldMoveMessage == null) {
//                heldMoveMessage = updatedMoveMessage
//                setPiecePromotionMenu()
//                return
//            }
//        } else{
//            if(updatedMoveMessage.endRow == 0 &&
//                boardHistory[mCurrentBoardShown].squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
//                heldMoveMessage == null) {
//                heldMoveMessage = updatedMoveMessage
//                setPiecePromotionMenu()
//                return
//            }
//        }
//        if(isCastlingMove(updatedMoveMessage)){
//            updatedMoveMessage.moveType = MoveType.CASTLE
//        }
//        if(!validateMove(updatedMoveMessage)) {
//            return
//        }
//
//        val correctMove = convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown])
//        if(
//            updatedMoveMessage.startCol == correctMove.startCol &&
//            updatedMoveMessage.startRow == correctMove.startRow &&
//            updatedMoveMessage.endCol == correctMove.endCol &&
//            updatedMoveMessage.endRow == correctMove.endRow &&
//            updatedMoveMessage.moveType == correctMove.moveType
//        ){
//            val isOnLastMove = mCurrentBoardShown == currentPuzzle.moves.size - 1
//            showCorrectMoveUi(isOnLastMove);
//            playMoveForPuzzle(updatedMoveMessage)
//            if(!isOnLastMove){
//                CoroutineScope(Dispatchers.Main).launch {
//                    delay(100)
//                    playMoveForPuzzle(convertMoveToMoveMessage(currentPuzzle.moves[mCurrentBoardShown]))
//                }
//
//            } else{
//                binding.llNextPuzzle.visibility = View.VISIBLE
//                Toast.makeText(this, "Finished The puzzle!", Toast.LENGTH_LONG).show()
//
//            }
//
//        } else{
//            Log.e(TAG, "MovePlayed: $updatedMoveMessage. the move we were expecting: $correctMove")
//            showWrongMoveUi()
//        }
//    }
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
//    private fun playMoveForPuzzle(updatedMoveMessage: MoveMessage){
//        val newBoard = boardHistory[mCurrentBoardShown].clone().movePiece(updatedMoveMessage)
//        newBoard.previousMove = updatedMoveMessage
//        boardHistory.add(newBoard)
//        mCurrentBoardShown += 1
//        binding.chessView.postInvalidate()
//    }

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


//    override fun isOnLastMove(): Boolean {
//       return mCurrentBoardShown == boardHistory.size -1
//    }
//
//    override fun goToLastMove() {
//        mCurrentBoardShown = boardHistory.size - 1
//        binding.chessView.invalidate()
//    }
//
//    override fun getLastMovePlayed(): MoveMessage? {
//        return boardHistory[mCurrentBoardShown].previousMove
//    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if(heldMoveMessage.isNull){
            Log.e(TAG, "held Move message is null")
            return false
        }
        when (menuItem.itemId){
            R.id.queen -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_QUEEN_FLAG)
                onMoveChosen(chosenMove)
                binding.chessView.invalidate()
                return true
            }
            R.id.rook -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_ROOK_FLAG)
                onMoveChosen(chosenMove)
                binding.chessView.invalidate()
                return true
            }
            R.id.bishop -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_BISHOP_FLAG)
                onMoveChosen(chosenMove)
                binding.chessView.invalidate()
                return true
            }
            R.id.knight -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_KNIGHT_FLAG)
                onMoveChosen(chosenMove)
                binding.chessView.invalidate()
                return true
            }
            else -> {return false}
        }
    }

    override fun playMove(startCoord: Coord, endCoord: Coord) {
        val startIndex  = BoardHelper.indexFromCoord(startCoord)
        val targetIndex = BoardHelper.indexFromCoord(endCoord)
        var isPromotion = false
        var isLegal = false
        var chosenMove = Move.NullMove
        val moveGenerator = MoveGenerator()

        for (legalMove in moveGenerator.generateMoves(board)) {
            if (legalMove.startSquare == startIndex && legalMove.targetSquare == targetIndex) {
                if (legalMove.isPromotion) {
                    isPromotion = true
                }
                isLegal = true
                chosenMove = legalMove
                break
            }
        }
        if (isLegal) {
            if (isPromotion) {
                heldMoveMessage = Move(chosenMove.startSquare, chosenMove.targetSquare)
                Log.i(TAG, "Held move message is now (in UCE): ${MoveUtility.getMoveNameUCI(heldMoveMessage)}")
                setPiecePromotionMenu()
            } else {
                onMoveChosen(chosenMove)
            }
        }

    }
    private fun playMoveForPuzzle(move : Move) {
        board.makeMove(move)
        currentMoveShown++
        binding.chessView.postInvalidate()
    }
    private fun onMoveChosen(move : Move) {
        if (MoveUtility.getMoveNameUCI(move) == currentPuzzle.moves[currentMoveShown]) {
            val isOnLastMove = currentMoveShown == currentPuzzle.moves.size - 1
            showCorrectMoveUi(isOnLastMove)
            playMoveForPuzzle(move)
            if(!isOnLastMove){
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    playMoveForPuzzle(MoveUtility.getMoveFromUCIName(currentPuzzle.moves[currentMoveShown], board))
                }

            } else{
                binding.llNextPuzzle.visibility = View.VISIBLE
                Toast.makeText(this, "Finished The puzzle!", Toast.LENGTH_LONG).show()

            }

        } else{
            Log.e(TAG, "MovePlayed: ${MoveUtility.getMoveNameUCI(move)}. the move we were expecting: ${currentPuzzle.moves[currentMoveShown]}")
            showWrongMoveUi()
        }
    }

    override fun getBoard(): Board {
        return board
    }


}