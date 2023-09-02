package com.hellostranger.chess_app.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.R

import com.hellostranger.chess_app.databinding.ActivityGameAnalysisBinding
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.gameHelpers.BaseChessGame
import com.hellostranger.chess_app.models.gameModels.Piece

class GameAnalysisActivity : BaseActivity()/*, ChessGameInterface {
    private lateinit var binding : ActivityGameAnalysisBinding
    private lateinit var gameService: BaseChessGame

    private lateinit var gameStartData : GameStartMessage

    private val TAG = "GameAnalysisActivity"
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameStartData = intent.getParcelableExtra("START", GameStartMessage::class.java)!!

        gameService = BaseChessGame(true)
        binding.chessView.chessGameInterface = this
        setUpActionBar()

        binding.ibArrowBack.setOnClickListener{
            if(gameService.showPreviousBoard()){
                binding.chessView.invalidate()
            }
        }
        binding.ibArrowForward.setOnClickListener{
            if(gameService.showNextBoard()){
                binding.chessView.invalidate()
            }
        }

        binding.ibFlipBoard.setOnClickListener{
            binding.chessView.flipBoard()
            binding.chessView.invalidate()
            if(binding.chessView.isFlipped){
                setPlayerOneData(gameStartData.whiteName, gameStartData.whiteEmail, gameStartData.whiteImage, gameStartData.whiteElo)
                setPlayerTwoData(gameStartData.blackName, gameStartData.blackEmail, gameStartData.blackImage, gameStartData.blackElo)
            } else{
                setPlayerOneData(gameStartData.blackName, gameStartData.blackEmail, gameStartData.blackImage, gameStartData.blackElo)
                setPlayerTwoData(gameStartData.whiteName, gameStartData.whiteEmail, gameStartData.whiteImage, gameStartData.whiteElo)
            }
        }
    }

    private fun setUpActionBar(){
        val toolbarGameAnalysis = binding.toolbarGameAnalysis
        setSupportActionBar(toolbarGameAnalysis)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_arrow_back)
        }

        toolbarGameAnalysis.setNavigationOnClickListener {
            finish()
        }
    }
    private fun setPlayerOneData(name: String, email: String, image : String, elo : Int) {
        val fullName = "$name (White) ($elo)"
        binding.tvP1Name.text = fullName
        Glide
            .with(this@GameAnalysisActivity)
            .load(image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP1Image)


    }

    *//*
    Sets information for the player at The Top
     *//*
    private fun setPlayerTwoData(name: String, email: String, image: String, elo : Int) {
        val fullName = "$name (Black) ($elo)"
        binding.tvP2Name.text = fullName
        Glide
            .with(this@GameAnalysisActivity)
            .load(image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP2Image)

    }

    override fun pieceAt(col: Int, row: Int, isFlipped: Boolean): Piece? {
        return if(isFlipped){
            gameService.getPieceAt(7 - col, 7 - row)
        } else{
            gameService.getPieceAt(col, row)
        }
    }

    override fun playMove(moveMessage: MoveMessage, isFlipped: Boolean) {
        Log.e(TAG, "Error. Can't play move in analysis")
    }

    override fun isOnLastMove(): Boolean {
        return gameService.isOnLastMove()
    }

    override fun goToLastMove() {
        gameService.goToLastMove()
        binding.chessView.invalidate()
    }
}*/