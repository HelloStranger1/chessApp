package com.hellostranger.chess_app.activities

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hellostranger.chess_app.GameViewModel
import com.hellostranger.chess_app.GameViewModelFactory
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.gameHelpers.BaseChessGame
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.Game
import com.hellostranger.chess_app.models.gameModels.enums.GameState
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.models.gameModels.Piece
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding
import com.hellostranger.chess_app.dto.websocket.ConcedeGameMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.enums.PieceType
import com.hellostranger.chess_app.network.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.network.websocket.MoveListener
import com.hellostranger.chess_app.utils.Constants
import okhttp3.WebSocket


class GameActivity : BaseActivity(), ChessGameInterface {

    private lateinit var binding : ActivityGameViewBinding
    private var tokenManager : TokenManager = MyApp.tokenManager

    private var chessWebSocket: WebSocket? = null
    private var chessWebSocketListener : ChessWebSocketListener? = null

    private lateinit var currentPlayerEmail : String

    private lateinit var viewModel: GameViewModel
    private lateinit var gameMode : String
    private val TAG = "GameActivity"
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressDialog("Waiting for game to start...")
        setUpActionBar()

        viewModel = ViewModelProvider(this, GameViewModelFactory(Game.getInstance()!!))[GameViewModel::class.java]
        gameMode = intent.getStringExtra("MODE")!!
        binding.chessView.chessGameInterface = this
        binding.chessView.gameMode = gameMode
        currentPlayerEmail = tokenManager.getUserEmail()
        if(gameMode == Constants.ANALYSIS_MODE){
            val startData = intent.getParcelableExtra("START", GameStartMessage::class.java)!!
            val moves : String = intent.getStringExtra("MOVES")!!

            for(i in 0 until moves.length step 4){
                val move : String = moves.substring(i, i + 4)
                val moveMessage : MoveMessage = MoveMessage(
                    "",
                    startCol = move[0].digitToInt(),
                    startRow = move[1].digitToInt(),
                    endCol = move[2].digitToInt(),
                    endRow = move[3].digitToInt()
                )

                viewModel.playMoveFromServer(moveMessage)
            }
            startGame(startData)
        } else if(gameMode == Constants.ONLINE_MODE){
            //Connecting to websocket
            chessWebSocketListener = ChessWebSocketListener(viewModel)
            chessWebSocketListener!!.connectWebSocket(Game.getInstance()!!.id, tokenManager.getAccessToken())
            chessWebSocket = chessWebSocketListener!!.getWebSocketInstance()
        }

        viewModel.socketStatus.observe(this){
            Log.e(TAG, "socketStatus changed to: $it")
        }
        viewModel.gameStatus.observe(this){
            Toast.makeText(this@GameActivity, "GameStatus changed to: $it", Toast.LENGTH_LONG).show()
            if(it != GameState.NEW && it != GameState.WAITING){

            }
            Log.e(TAG, "gameStatus changed to: $it")
        }
        viewModel.startMessageData.observe(this){
            startGame(it)
        }
        viewModel.currentBoard.observe(this){
            binding.chessView.invalidate()
        }
        binding.ibArrowBack.setOnClickListener{
            viewModel.showPreviousBoard()
        }
        binding.ibArrowForward.setOnClickListener{
            viewModel.showNextBoard()
        }

        binding.ibFlipBoard.setOnClickListener{
            binding.chessView.flipBoard()
            binding.chessView.invalidate()
            val gameStartData = viewModel.startMessageData.value!!
            if(binding.chessView.isFlipped){
                setPlayerOneData(gameStartData.whiteName, gameStartData.whiteEmail, gameStartData.whiteImage, gameStartData.whiteElo)
                setPlayerTwoData(gameStartData.blackName, gameStartData.blackEmail, gameStartData.blackImage, gameStartData.blackElo)
            } else{
                setPlayerOneData(gameStartData.blackName, gameStartData.blackEmail, gameStartData.blackImage, gameStartData.blackElo)
                setPlayerTwoData(gameStartData.whiteName, gameStartData.whiteEmail, gameStartData.whiteImage, gameStartData.whiteElo)
            }
        }

        binding.ibExtraSettings.setOnClickListener {
            if(gameMode == Constants.ONLINE_MODE){
                val popupMenu = PopupMenu(this@GameActivity, binding.ibExtraSettings)
                popupMenu.menuInflater.inflate(R.menu.popup_game_options_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId){
                        R.id.resign -> {
                            showConcedeDialog()
                            return@setOnMenuItemClickListener true
                        }
                        else -> {return@setOnMenuItemClickListener true}
                    }
                }
                popupMenu.show()
            } else{
                //TODO: Add extra settings for analysis, like adding a comment
            }

        }
    }

    private fun showConcedeDialog(){
        val builder = AlertDialog.Builder(this@GameActivity)
        builder.setTitle("Resign Game")
        builder.setMessage("Do you want to resign?")
        builder.setPositiveButton("Yes"){ dialog, _ ->
            sendMessageToServer(ConcedeGameMessage(currentPlayerEmail))
            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton("No"){
                dialog, _ -> dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }


    private fun setUpActionBar(){
        val toolbarUpdateProfileActivity = binding.toolbarGameActivity
        setSupportActionBar(toolbarUpdateProfileActivity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_arrow_back)
        }

        toolbarUpdateProfileActivity.setNavigationOnClickListener {
            if(gameMode == Constants.ONLINE_MODE && viewModel.socketStatus.value == true){
                showConcedeDialog()
            } else{
                finish()
            }

        }
    }
    override fun pieceAt(col : Int, row : Int, isFlipped : Boolean): Piece? {
        return if(isFlipped){
            viewModel.currentBoard.value!!.squaresArray[7 - row][7 - col].piece
        } else{
            viewModel.currentBoard.value!!.squaresArray[row][col].piece
        }

    }

    /*
    Called by ChessView when the player makes a move. Takes in a MoveMessage object.
    Performs a basic check that the move is valid (actually moving a piece, piece is owned by the player, etc). Backend server will validate the move.
     */
    override fun playMove(moveMessage: MoveMessage, isFlipped: Boolean) {
        moveMessage.playerEmail = viewModel.currentPlayerEmail //Chess View is unaware of the playerEmail, adding it to the message.
        var updatedMoveMessage = moveMessage
        if(isFlipped){
            updatedMoveMessage = MoveMessage(
                moveMessage.playerEmail,
                7 - moveMessage.startCol,
                7 - moveMessage.startRow,
                7 - moveMessage.endCol,
                7 - moveMessage.endRow
                )
            Log.e(TAG, "PlayMove, Flipped. flipped move msg is: $updatedMoveMessage")
        }
        var chosenPromotion : PieceType? = null
        if(viewModel.isWhite){
            if(updatedMoveMessage.endRow == 7 && viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN) {
                chosenPromotion = setPiecePromotionMenu()
            }
        } else{
            if(updatedMoveMessage.endRow == 0 && viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN) {
                chosenPromotion = setPiecePromotionMenu()
            }
        }
        if(chosenPromotion == null) {
            viewModel.temporaryPlayMove(updatedMoveMessage)
        }else{
            updatedMoveMessage.promotionType = chosenPromotion
            viewModel.temporaryPlayMove(updatedMoveMessage)
        }


       /* if(!gameService.validateMove(updatedMoveMessage)) return

        gameService.temporaryMakeMove(updatedMoveMessage)*/ //Instead of waiting for the server to validate the move, we play it temporarily and undo it if the server says it is invalid.
        if(gameMode == Constants.ONLINE_MODE){
            sendMessageToServer(updatedMoveMessage)
        }
    }

    private fun setPiecePromotionMenu() : PieceType?{
        val popupMenu = PopupMenu(this@GameActivity, binding.llPlayer2)
        var chosenPromotion : PieceType? = null
        if(viewModel.isWhite){
            popupMenu.menuInflater.inflate(R.menu.popup_white_promotion_options, popupMenu.menu)
        } else{
            popupMenu.menuInflater.inflate(R.menu.popup_black_promotion_options, popupMenu.menu)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId){
                R.id.queen -> {
                    chosenPromotion = PieceType.QUEEN
                    return@setOnMenuItemClickListener true
                }
                R.id.rook -> {
                    chosenPromotion = PieceType.ROOK
                    return@setOnMenuItemClickListener true
                }
                R.id.bishop -> {
                    chosenPromotion = PieceType.BISHOP
                    return@setOnMenuItemClickListener true
                }R.id.knight -> {
                chosenPromotion = PieceType.KNIGHT
                return@setOnMenuItemClickListener true
            }
                else -> {return@setOnMenuItemClickListener false}
            }
        }
        popupMenu.show()
        Log.e(TAG, "Chosen promotion is: $chosenPromotion")
        return chosenPromotion
    }



    override fun isOnLastMove(): Boolean {
        return viewModel.isOnLastMove()
    }

    private fun sendMessageToServer(message: WebSocketMessage) {
        val gson = Gson()
        val messageJson = gson.toJson(message)
        if (viewModel.socketStatus.value!! && chessWebSocketListener!!.isWebSocketOpen()) {
            chessWebSocket!!.send(messageJson)
        }

    }


    // Close the WebSocket connection when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Disconnect WebSocket when the activity is destroyed
        if(gameMode == Constants.ONLINE_MODE){
            chessWebSocketListener!!.disconnectWebSocket()
        }

    }

    /*
    Plays a move that is received from the server.
    The move might be from us, since the server echos a move if it is legal, or it might be from the opponent.
     */
    /*override fun onMoveReceived(moveMessage: MoveMessage) {
        gameService.playMove(moveMessage,currentPlayerEmail)
        binding.chessView.invalidate()
    }*/

    /*
    Sets information for the player at The Bottom
     */
    private fun setPlayerOneData(name: String, email: String, image : String, elo : Int) {
        var fullName = "";
        if(viewModel.isWhite){
            if(email == currentPlayerEmail){
                fullName = "$name (White) ($elo)"
            } else{
                fullName = "$name (Black) ($elo)"
            }
        } else{
            if(email == currentPlayerEmail){
                fullName = "$name (Black) ($elo)"
            } else{
                fullName = "$name (White) ($elo)"
            }
        }
        binding.tvP1Name.text = fullName

        Glide
            .with(this@GameActivity)
            .load(image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP1Image)


    }

    /*
    Sets information for the player at The Top
     */
    private fun setPlayerTwoData(name: String, email: String, image: String, elo : Int) {
        var fullName = ""
        if(!viewModel.isWhite){
            if(email == currentPlayerEmail){
                fullName = "$name (White) ($elo)"
            } else{
                fullName = "$name (Black) ($elo)"
            }
        } else {
            if (email == currentPlayerEmail) {
                fullName = "$name (Black) ($elo)"
            } else {
                fullName = "$name (White) ($elo)"
            }
        }
        binding.tvP2Name.text = fullName
        Glide
            .with(this@GameActivity)
            .load(image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP2Image)

    }

    /*
    Runs when 2 players joined into the game.
    Sets the Data in the ui and informs the user the game has started.
     */
    fun startGame(startMessage: GameStartMessage) {
        hideProgressDialog()
        val playerColor : Color = if(viewModel.isWhite){
            Color.WHITE
        }else{
            Color.BLACK
        }
        runOnUiThread{
            setPlayerOneData(startMessage.whiteName, startMessage.whiteEmail, startMessage.whiteImage, startMessage.whiteElo)
            setPlayerTwoData(startMessage.blackName, startMessage.blackEmail, startMessage.blackImage, startMessage.blackElo)
            if(gameMode != Constants.ANALYSIS_MODE){
                Toast.makeText(this@GameActivity, "Game started! You are playing $playerColor", Toast.LENGTH_SHORT).show()
            }
        }


    }


     fun onGameEnding(result: GameState) {
        chessWebSocketListener!!.disconnectWebSocket()
        runOnUiThread {
            Toast.makeText(this@GameActivity, "Game ended. result is: $result", Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Game ended. result is: $result")

    }

    /*
    Gets called when the player tried to play an illegal move and it was invalidated by the server.
     */
   /* override fun undoLastMove() {
        gameService.undoMove()
        binding.chessView.invalidate()
    }*/

    /*private fun checkIfPlayingWhite(whiteEmail: String) {
        isWhite = whiteEmail == tokenManager.getUserEmail()
    }*/

    override fun goToLastMove(){
        viewModel.goToLatestMove()
    }


}