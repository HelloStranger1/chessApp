package com.hellostranger.chess_app.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View

import android.widget.PopupMenu
import android.widget.PopupMenu.OnMenuItemClickListener
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hellostranger.chess_app.GameResultFragment
import com.hellostranger.chess_app.viewModels.GameViewModel
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.Arbiter
import com.hellostranger.chess_app.core.Bot
import com.hellostranger.chess_app.core.Game
import com.hellostranger.chess_app.core.PlayerInfo
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import com.hellostranger.chess_app.core.Player
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding
import com.hellostranger.chess_app.dto.websocket.ConcedeGameMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.dto.enums.MoveType
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.network.websocket.ChessWebSocketListener
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import okhttp3.WebSocket
import kotlin.random.Random

private const val TAG = "GameActivity"

@ExperimentalUnsignedTypes
class GameActivity : BaseActivity(), ChessGameInterface, OnMenuItemClickListener, Player {

    private lateinit var binding : ActivityGameViewBinding
    private var tokenManager : TokenManager = MyApp.tokenManager

    private var chessWebSocket: WebSocket? = null
    private var chessWebSocketListener : ChessWebSocketListener? = null

    private var botPlayer : Bot? = null

    private lateinit var viewModel: GameViewModel
    private lateinit var gameMode : String
    private var heldMoveMessage : Move = Move.NullMove //To hold the move message while waiting for the player to chose promotion
    private var isDrawOffered : Boolean = false

    private val currentPlayerEmail = MyApp.tokenManager.getUserEmail()
    private var isPlayingWhite = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressDialog("Waiting for game to start...", false)
        setUpActionBar()

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        gameMode = intent.getStringExtra(Constants.MODE)!!

        setUpChessView()

        if(gameMode == Constants.ANALYSIS_MODE){
            binding.chessView.isLocked = true
            loadGameForAnalysis()
        } else if(gameMode == Constants.ONLINE_MODE) {
            initializeSocket()
        } else if (gameMode == Constants.AI_MODE) {
            loadGameAgainstBot(intent)
        }

        viewModel.socketStatus.observe(this){
            Log.e(TAG, "socketStatus changed to: $it")
        }

        viewModel.gameResult.observe(this) {
            Toast.makeText(this@GameActivity, "GameStatus changed to: $it", Toast.LENGTH_LONG).show()
            Log.e(TAG, "gameStatus changed to: $it")
            if (savedInstanceState == null && Arbiter.isDrawResult(it) || Arbiter.isWinResult(it)) {
                val ourElo = if (isPlayingWhite) viewModel.whitePlayerInfo!!.elo else viewModel.blackPlayerInfo!!.elo
                val whitePlayerInfo = viewModel.whitePlayerInfo!!
                val blackPlayerInfo = viewModel.blackPlayerInfo!!
                val fragment = GameResultFragment.newInstance(whitePlayerInfo.name, blackPlayerInfo.name, whitePlayerInfo.image, blackPlayerInfo.image, ourElo, it, viewModel.gameOverDescription)
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add(R.id.fragment_container_view, fragment)
                }
                binding.shadow.visibility = View.VISIBLE
            }
        }

//        viewModel.gameStatus.observe(this){
//            val result : Int = when (it) {
//                GameState.WHITE_WIN -> 0
//                GameState.DRAW, GameState.ABORTED -> 1
//                GameState.BLACK_WIN -> 2
//                else -> -1
//            }
//            if(savedInstanceState == null && result != -1){
//                val ourElo = viewModel.ourElo
//
//                viewModel.startMessageData.value?.let { msg ->
//                    val fragment : GameResultFragment = GameResultFragment.newInstance(msg.whiteName, msg.blackName,
//                        msg.whiteImage, msg.blackImage, ourElo, result, viewModel.gameOverDescription)
//                    supportFragmentManager.commit {
//                        setReorderingAllowed(true)
//                        add(R.id.fragment_container_view, fragment)
//                    }
//                    binding.shadow.visibility = View.VISIBLE
//                }
//            }
//        }


        viewModel.hasGameStarted.observe(this) {
            if (it == true) {
                startGame(viewModel.isWhite, viewModel.whitePlayerInfo!!, viewModel.blackPlayerInfo!!)
            }
        }
//        viewModel.currentBoard.observe(this){
//            binding.chessView.invalidate()
//        }
        viewModel.drawOffer.observe(this) {
            if (it.isWhite != viewModel.isWhite) {
                Toast.makeText(this, "Player ${it.playerEmail} Offered a draw. ", Toast.LENGTH_LONG).show()
                isDrawOffered = true
            }
        }

        binding.ibArrowBack.setOnClickListener{
            Log.i(TAG, "Pressed arrow back")
            viewModel.showPreviousBoard()
            binding.chessView.isLocked = true
            binding.chessView.invalidate()
        }
        binding.ibArrowForward.setOnClickListener{
            viewModel.showNextBoard()
            if (viewModel.isOnLastMove()) {
                binding.chessView.isLocked = false
            }
            binding.chessView.invalidate()
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


    private fun getEnemyPlayer() : Player {
        return when (gameMode) {
            Constants.ONLINE_MODE -> {
                chessWebSocketListener as Player
            }
            Constants.ANALYSIS_MODE -> {
                this
            }
            Constants.AI_MODE -> {
                botPlayer!!
            }
            else -> {
                this
            }
        }
    }
    private fun startGame(isWhite : Boolean, whitePlayerInfo : PlayerInfo, blackPlayerInfo : PlayerInfo){
        if (isWhite) {
            viewModel.whitePlayer = this
            viewModel.blackPlayer = getEnemyPlayer()
        } else {
            viewModel.blackPlayer = this
            viewModel.whitePlayer = getEnemyPlayer()
        }
        hideProgressDialog()
        setPlayerOneData(whitePlayerInfo, true)
        setPlayerTwoData(blackPlayerInfo, false)
        isPlayingWhite = isWhite
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
                R.id.draw -> {
                    sendDrawOffer()
                    return@setOnMenuItemClickListener true
                }
                else -> {return@setOnMenuItemClickListener true}
            }
        }
        popupMenu.show()
    }
    private fun sendDrawOffer() {
        if (viewModel.socketStatus.value == true) {
            sendMessageToServer(DrawOfferMessage(currentPlayerEmail, viewModel.isWhite))
        }
    }
    private fun flipUserData(){
        binding.chessView.flipBoard()
        binding.chessView.invalidate()

        if(binding.chessView.isWhiteOnBottom){
            viewModel.whitePlayerInfo?.let { setPlayerOneData(it, true) }
            viewModel.blackPlayerInfo?.let { setPlayerTwoData(it, false) }
        } else{
            viewModel.whitePlayerInfo?.let { setPlayerTwoData(it, true) }
            viewModel.blackPlayerInfo?.let { setPlayerOneData(it, false) }
        }

    }
    private fun setUpChessView(){
        binding.chessView.chessGameInterface = this
    }
    private fun initializeSocket(){
        chessWebSocketListener = ChessWebSocketListener(viewModel, currentPlayerEmail)
        chessWebSocketListener!!.connectWebSocket(
            Game.getInstance()!!.id,
            tokenManager.getAccessToken()
        )
        chessWebSocket = chessWebSocketListener!!.getWebSocketInstance()
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun loadGameForAnalysis(){
        val startData = intent.getParcelableExtra(Constants.START_DATA, GameStartMessage::class.java)!!
        val movesString : String = intent.getStringExtra(Constants.MOVES_LIST)!!

        viewModel.startGame(startData)

        val movesList : List<String> = movesString.split(",")
        for (moveStr in movesList) {
            if (moveStr.length >= 4) {
                viewModel.onMoveChosen(MoveUtility.getMoveFromUCIName(moveStr, viewModel.board), this)
            }
        }
        Log.i(TAG, "Game board has: ${viewModel.board.allGameMoves.size} moves")
        binding.chessView.invalidate()
    }

    private fun loadGameAgainstBot(intent: Intent) {
        val isWhite : Boolean = Random.nextBoolean()
        val ourName = intent.getStringExtra(Constants.USER_NAME)!!
        val ourEmail = intent.getStringExtra(Constants.USER_EMAIL)!!
        val ourElo = intent.getIntExtra(Constants.USER_ELO, 0)
        val ourImage = intent.getStringExtra(Constants.USER_IMAGE)!!
        val startData = if (isWhite) {
            GameStartMessage(ourName, "BOT", ourEmail, "", ourImage, "", ourElo, 99999)
        } else {
            GameStartMessage("BOT", ourName, "", ourEmail, "", ourImage, 99999, ourElo)
        }
        botPlayer = Bot(7_500, 2_500, viewModel)
        viewModel.startGame(startData)
        if (!isWhite) {
            botPlayer!!.onOpponentMoveChosen()
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
            sendMessageToServer(ConcedeGameMessage(currentPlayerEmail))
            dialog.dismiss()

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
//    override fun pieceAt(col : Int, row : Int, isFlipped : Boolean): Piece? {
//        return if(isFlipped){
//            viewModel.currentBoard.value!!.squaresArray[7 - row][7 - col].piece
//        } else{
//            viewModel.currentBoard.value!!.squaresArray[row][col].piece
//        }
//
//    }
//
//    override fun getPiecesMoves(piece: Piece): ArrayList<Square> {
//        val movableSquares : ArrayList<Square> = ArrayList()
//        val startSquare : Square = viewModel.currentBoard.value!!.getSquareAt(piece.colIndex, piece.rowIndex)!!
//        for(square : Square in piece.getMovableSquares(viewModel.currentBoard.value!!)){
//            if(viewModel.currentBoard.value!!.isValidMove(startSquare, square)){
//                movableSquares.add(square)
//            }
//        }
//        Log.i(TAG, "the move's of piece: $piece are: $movableSquares")
//        return movableSquares
//    }

    /*
    Called by ChessView when the player makes a move. Takes in a MoveMessage object.
    Performs a basic check that the move is valid (actually moving a piece, piece is owned by the player, etc). Backend server will validate the move.
     */
//    override fun playMove(moveMessage: MoveMessage, isFlipped: Boolean) {
//
//        moveMessage.playerEmail = viewModel.currentPlayerEmail //Chess View is unaware of the playerEmail, adding it to the message.
//        var updatedMoveMessage = moveMessage
//        if(isFlipped){
//            updatedMoveMessage = MoveMessage(
//                moveMessage.playerEmail, 7 - moveMessage.startCol, 7 - moveMessage.startRow, 7 - moveMessage.endCol, 7 - moveMessage.endRow, moveMessage.moveType
//            )
//        }
//        if(viewModel.isWhite){
//            if(updatedMoveMessage.endRow == 7 &&
//                viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
//                heldMoveMessage == null) {
//                heldMoveMessage = updatedMoveMessage
//                setPiecePromotionMenu()
//                return
//            }
//        } else{
//            if(updatedMoveMessage.endRow == 0 &&
//                viewModel.currentBoard.value!!.squaresArray[updatedMoveMessage.startRow][updatedMoveMessage.startCol].piece!!.pieceType == PieceType.PAWN &&
//                heldMoveMessage == null) {
//                heldMoveMessage = updatedMoveMessage
//                setPiecePromotionMenu()
//                return
//            }
//        }
//        if(viewModel.isCastlingMove(updatedMoveMessage)){
//            updatedMoveMessage.moveType = MoveType.CASTLE
//        }
//        val isValidMove : Boolean = viewModel.validateMove(updatedMoveMessage)
//        viewModel.temporaryPlayMove(updatedMoveMessage)
//        if(isValidMove){
//            binding.chessView.invalidate()
//            if(gameMode == Constants.ONLINE_MODE){
//                sendMessageToServer(updatedMoveMessage)
//            }
//        }
//        isDrawOffered = false
//       /* if(!gameService.validateMove(updatedMoveMessage)) return
//
//        gameService.temporaryMakeMove(updatedMoveMessage)*/ //Instead of waiting for the server to validate the move, we play it temporarily and undo it if the server says it is invalid.
//
//    }
    override fun playMove(startCoord: Coord, endCoord: Coord) {
        val startIndex  = BoardHelper.indexFromCoord(startCoord)
        val targetIndex = BoardHelper.indexFromCoord(endCoord)
        var isPromotion = false
        var isLegal = false
        var chosenMove = Move.NullMove
        val moveGenerator = MoveGenerator()

        if (Piece.isWhite(viewModel.board.square[startIndex]) != isPlayingWhite) {
            return
        }

        for (legalMove in moveGenerator.generateMoves(viewModel.board)) {
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
                viewModel.onMoveChosen(chosenMove, this)
            }
        }

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
    private fun setPlayerOneData(playerInfo: PlayerInfo, isWhite : Boolean) {
        val fullName : String = if (isWhite) {
            "${playerInfo.name} (White) (${playerInfo.elo})"
        } else {
            "${playerInfo.name} (Black) (${playerInfo.elo}"
        }
        binding.tvP1Name.text = fullName

        Glide
            .with(this@GameActivity)
            .load(playerInfo.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP1Image)
    }

    /*
    Sets information for the player at The Top
     */
    private fun setPlayerTwoData(playerInfo: PlayerInfo, isWhite : Boolean) {
        val fullName : String = if (isWhite) {
            "${playerInfo.name} (White) (${playerInfo.elo}"
        } else {
            "${playerInfo.name} (Black) (${playerInfo.elo}"
        }
        binding.tvP2Name.text = fullName

        Glide
            .with(this@GameActivity)
            .load(playerInfo.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivP2Image)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        if(heldMoveMessage.isNull){
            Log.e(TAG, "held Move message is null")
            return false
        }
        when (menuItem.itemId){
            R.id.queen -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_QUEEN_FLAG)
                viewModel.onMoveChosen(chosenMove, this)
                binding.chessView.invalidate()
                return true
            }
            R.id.rook -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_ROOK_FLAG)
                viewModel.onMoveChosen(chosenMove, this)
                binding.chessView.invalidate()
                return true
            }
            R.id.bishop -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_BISHOP_FLAG)
                viewModel.onMoveChosen(chosenMove, this)
                binding.chessView.invalidate()
                return true
            }R.id.knight -> {
                val chosenMove = Move(heldMoveMessage.startSquare, heldMoveMessage.targetSquare, Move.PROMOTE_TO_KNIGHT_FLAG)
                viewModel.onMoveChosen(chosenMove, this)
                binding.chessView.invalidate()
                return true
            }
            else -> {return false}
        }
    }

    override fun getBoard(): Board = viewModel.board

    override fun onOpponentMoveChosen() {

        binding.chessView.invalidate()
    }


}
