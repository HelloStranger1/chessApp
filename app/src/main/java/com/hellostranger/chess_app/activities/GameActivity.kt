package com.hellostranger.chess_app.activities

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem

import android.widget.PopupMenu
import android.widget.PopupMenu.OnMenuItemClickListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hellostranger.chess_app.viewModels.GameViewModel
import com.hellostranger.chess_app.viewModels.factories.GameViewModelFactory
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding
import com.hellostranger.chess_app.dto.websocket.ConcedeGameMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.MoveMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.gameClasses.Square
import com.hellostranger.chess_app.dto.enums.MoveType
import com.hellostranger.chess_app.gameClasses.enums.PieceType
import com.hellostranger.chess_app.gameClasses.pieces.Piece
import com.hellostranger.chess_app.network.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import okhttp3.WebSocket

private const val TAG = "GameActivity"
class GameActivity : BaseActivity(), ChessGameInterface, OnMenuItemClickListener {

    private lateinit var binding : ActivityGameViewBinding
    private var tokenManager : TokenManager = MyApp.tokenManager

    private var chessWebSocket: WebSocket? = null
    private var chessWebSocketListener : ChessWebSocketListener? = null

    private lateinit var viewModel: GameViewModel
    private lateinit var gameMode : String
    private var heldMoveMessage : MoveMessage? = null //To hold the move message while waiting for the player to chose promotion

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressDialog("Waiting for game to start...")
        setUpActionBar()

        viewModel = ViewModelProvider(this, GameViewModelFactory(Game.getInstance()!!))[GameViewModel::class.java]
        gameMode = intent.getStringExtra(Constants.MODE)!!

        setUpChessView()

        if(gameMode == Constants.ANALYSIS_MODE){
            loadGameForAnalysis()
        } else if(gameMode == Constants.ONLINE_MODE) {
            initializeSocket()
        }

        viewModel.socketStatus.observe(this){
            Log.e(TAG, "socketStatus changed to: $it")
        }

        viewModel.gameStatus.observe(this){
            Toast.makeText(this@GameActivity, "GameStatus changed to: $it", Toast.LENGTH_LONG).show()
            Log.e(TAG, "gameStatus changed to: $it")
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
            flipUserData()
        }

        binding.ibExtraSettings.setOnClickListener {
            if(gameMode == Constants.ONLINE_MODE){
                showOnlineOptionsMenu()
            } else{
                //TODO: Add extra settings for analysis, like adding a comment
            }

        }
    }
    private fun showOnlineOptionsMenu(){
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
    }
    private fun flipUserData(){
        binding.chessView.flipBoard()
        binding.chessView.invalidate()
        val startData = viewModel.startMessageData.value!!
        if(!binding.chessView.isFlipped){
            setPlayerOneData(startData.whiteName, startData.whiteEmail, startData.whiteImage, startData.whiteElo)
            setPlayerTwoData(startData.blackName, startData.blackEmail, startData.blackImage, startData.blackElo)
        } else{
            setPlayerOneData(startData.blackName, startData.blackEmail, startData.blackImage, startData.blackElo)
            setPlayerTwoData(startData.whiteName, startData.whiteEmail, startData.whiteImage, startData.whiteElo)
        }

    }
    private fun setUpChessView(){
        binding.chessView.chessGameInterface = this
        binding.chessView.gameMode = gameMode
    }
    private fun initializeSocket(){
        chessWebSocketListener = ChessWebSocketListener(viewModel)
        chessWebSocketListener!!.connectWebSocket(
            Game.getInstance()!!.id,
            tokenManager.getAccessToken()
        )
        chessWebSocket = chessWebSocketListener!!.getWebSocketInstance()
    }
    private fun loadGameForAnalysis(){
        val startData = intent.getParcelableExtra(Constants.START_DATA, GameStartMessage::class.java)!!
        val moves : String = intent.getStringExtra(Constants.MOVES_LIST)!!

        viewModel.startGame(startData)

        for(i in moves.indices step 5){
            val move : String = moves.substring(i, i + 5)
            val moveMessage = MoveMessage(
                "",
                startCol = move[0].digitToInt(),
                startRow = move[1].digitToInt(),
                endCol = move[2].digitToInt(),
                endRow = move[3].digitToInt(),
                moveType = matchCharToMoveType(move[4])
            )
            viewModel.playMoveFromServer(moveMessage)
        }
    }

    private fun matchCharToMoveType(char : Char): MoveType {
        return when (char) {
            'O' -> MoveType.REGULAR
            'C' -> MoveType.CASTLE
            'Q' -> MoveType.PROMOTION_QUEEN
            'R' -> MoveType.PROMOTION_ROOK
            'B' -> MoveType.PROMOTION_BISHOP
            'K' -> MoveType.PROMOTION_KNIGHT
            else -> {
                MoveType.REGULAR}
        }
    }

    private fun showConcedeDialog(){
        val builder = AlertDialog.Builder(this@GameActivity)
        builder.setTitle("Resign Game")
        builder.setMessage("Do you want to resign?")
        builder.setPositiveButton("Yes"){ dialog, _ ->
            sendMessageToServer(ConcedeGameMessage(viewModel.currentPlayerEmail))
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

    override fun getPiecesMoves(piece: Piece): ArrayList<Square> {
        val movableSquares : ArrayList<Square> = ArrayList()
        val startSquare : Square = viewModel.currentBoard.value!!.getSquareAt(piece.colIndex, piece.rowIndex)!!
        for(square : Square in piece.getMovableSquares(viewModel.currentBoard.value!!)){
            if(viewModel.currentBoard.value!!.isValidMove(startSquare, square)){
                movableSquares.add(square)
            }
        }
        Log.i(TAG, "the move's of piece: $piece are: $movableSquares")
        return movableSquares
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
                moveMessage.playerEmail, 7 - moveMessage.startCol, 7 - moveMessage.startRow, 7 - moveMessage.endCol, 7 - moveMessage.endRow, moveMessage.moveType
            )
        }
        if(viewModel.isWhite){
            if(updatedMoveMessage.endRow == 7 &&
                viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
                heldMoveMessage == null) {
                heldMoveMessage = updatedMoveMessage
                setPiecePromotionMenu()
                return
            }
        } else{
            if(updatedMoveMessage.endRow == 0 &&
                viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
                heldMoveMessage == null) {
                heldMoveMessage = updatedMoveMessage
                setPiecePromotionMenu()
                return
            }
        }
        if(viewModel.isCastlingMove(updatedMoveMessage)){
            updatedMoveMessage.moveType = MoveType.CASTLE
        }
        val isValidMove : Boolean = viewModel.validateMove(updatedMoveMessage)
        viewModel.temporaryPlayMove(updatedMoveMessage)
        if(isValidMove){
            binding.chessView.invalidate()
            if(gameMode == Constants.ONLINE_MODE){
                sendMessageToServer(updatedMoveMessage)
            }
        }
       /* if(!gameService.validateMove(updatedMoveMessage)) return

        gameService.temporaryMakeMove(updatedMoveMessage)*/ //Instead of waiting for the server to validate the move, we play it temporarily and undo it if the server says it is invalid.

    }

    override fun getLastMovePlayed() : MoveMessage? {
        return viewModel.currentBoard.value?.previousMove
    }

    private fun setPiecePromotionMenu(){
        val popupMenu = PopupMenu(this@GameActivity, binding.llPlayer2)
        if(viewModel.isWhite){
            popupMenu.menuInflater.inflate(R.menu.popup_white_promotion_options, popupMenu.menu)
        } else{
            popupMenu.menuInflater.inflate(R.menu.popup_black_promotion_options, popupMenu.menu)
        }
        popupMenu.setOnMenuItemClickListener(this@GameActivity)
        popupMenu.show()
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
        val fullName : String = if(viewModel.isWhite){
            if(email == viewModel.currentPlayerEmail){
                "$name (White) ($elo)"
            } else{
                "$name (Black) ($elo)"
            }
        } else{
            if(email == viewModel.currentPlayerEmail){
                "$name (Black) ($elo)"
            } else{
                "$name (White) ($elo)"
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
        val fullName : String = if(!viewModel.isWhite){
            if(email != viewModel.currentPlayerEmail){
                "$name (White) ($elo)"
            } else{
                "$name (Black) ($elo)"
            }
        } else {
            if (email != viewModel.currentPlayerEmail) {
                "$name (Black) ($elo)"
            } else {
                "$name (White) ($elo)"
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
    override fun goToLastMove(){
        viewModel.goToLatestMove()
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