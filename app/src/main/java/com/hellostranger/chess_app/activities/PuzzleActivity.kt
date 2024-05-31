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

        showProgressDialog("Please wait while we fetch new puzzles...", false)
        CoroutineScope(Dispatchers.IO).launch {
            handleResponse(
                request = {PuzzleRetrofitClient.instance.getRandomPuzzle(4)},
                errorMessage = "Couldn't fetch puzzles."
            )?.let {
                PuzzlesList.instance.addPuzzles(it)
            }
            withContext(Dispatchers.Main){
                goToNextPuzzle()
            }
            runOnUiThread {
                hideProgressDialog()
            }
        }
    }

    private fun showCorrectMoveUi(isLastMove : Boolean){
        binding.ivLogo.setImageResource(R.drawable.ic_green_checkmark)
        if(isLastMove){
            binding.tv1.text = getString(R.string.success)
            binding.tv2.visibility = View.INVISIBLE
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

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if(heldMoveMessage.isNull){
            Log.e(TAG, "held Move message is null")
            return false
        }
        val chosenMove : Move = when (menuItem.itemId){
            R.id.queen -> {
                Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_QUEEN_FLAG)
            }

            R.id.rook -> {
                Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_ROOK_FLAG)
            }

            R.id.bishop -> {
                Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_BISHOP_FLAG)
            }

            R.id.knight -> {
                Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_KNIGHT_FLAG)
            }

            else -> {return false}
        }
        onMoveChosen(chosenMove)
        binding.chessView.invalidate()
        return true
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