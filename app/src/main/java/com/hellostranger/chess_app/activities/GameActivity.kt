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
import com.hellostranger.chess_app.core.helpers.Arbiter
import com.hellostranger.chess_app.core.players.Bot
import com.hellostranger.chess_app.core.Game
import com.hellostranger.chess_app.core.board.GameResult
import com.hellostranger.chess_app.core.players.PlayerInfo
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Coord
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import com.hellostranger.chess_app.core.players.Player
import com.hellostranger.chess_app.databinding.ActivityGameViewBinding
import com.hellostranger.chess_app.dto.websocket.ConcedeGameMessage
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.dto.websocket.WebSocketMessage
import com.hellostranger.chess_app.gameHelpers.ChessGameInterface
import com.hellostranger.chess_app.dto.websocket.DrawOfferMessage
import com.hellostranger.chess_app.models.entities.User
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

    /*              Websocket variables              */
    private var chessWebSocket: WebSocket? = null
    private var chessWebSocketListener : ChessWebSocketListener? = null

    /*              Bot (AI) variables              */
    private var botPlayer : Bot? = null

    private lateinit var viewModel: GameViewModel
    private lateinit var gameMode : String

    private var heldMoveMessage : Move = Move.NullMove //To hold the move message while waiting for the player to chose promotion
    private var isDrawOffered : Boolean = false // Holds whether or not the opponent offered a draw. Reset when we make a move

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

        binding.chessView.chessGameInterface = this

        initGame()

        viewModel.gameResult.observe(this) {
            Log.e(TAG, "gameStatus changed to: $it")
            if (savedInstanceState == null && Arbiter.isDrawResult(it) || Arbiter.isWinResult(it) && gameMode != Constants.ANALYSIS_MODE) {
                onGameOver(it)
            }
        }

        viewModel.hasGameStarted.observe(this) {
            if (it == true) {
                startGame(viewModel.isWhite, viewModel.whitePlayerInfo!!, viewModel.blackPlayerInfo!!)
            }
        }

        viewModel.drawOffer.observe(this) {
            if (it.isWhite != viewModel.isWhite) {
                Toast.makeText(this, "Player ${it.playerEmail} Offered a draw. ", Toast.LENGTH_LONG).show()
                isDrawOffered = true
            }
        }

        binding.ibArrowBack.setOnClickListener{
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
            if(gameMode == Constants.ONLINE_MODE || gameMode == Constants.AI_MODE){
                showGameOptionsMenu()
            }
        }
    }

    /**
     * Called when the game finishes. Creates the Game result fragment.
     */
    private fun onGameOver(result: GameResult) {
        val ourElo =
            if (isPlayingWhite) viewModel.whitePlayerInfo!!.elo else viewModel.blackPlayerInfo!!.elo
        val whitePlayerInfo = viewModel.whitePlayerInfo!!
        val blackPlayerInfo = viewModel.blackPlayerInfo!!
        val fragment = GameResultFragment.newInstance(
            whitePlayerInfo.name,
            blackPlayerInfo.name,
            whitePlayerInfo.image,
            blackPlayerInfo.image,
            ourElo,
            result
        )
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.fragment_container_view, fragment)
        }
        binding.shadow.visibility = View.VISIBLE
    }

    /**
     * Initializes everything depending on the game mode. for example, with a bot, create the bot.
     */
    private fun initGame() {
        when (gameMode) {
            Constants.ANALYSIS_MODE -> {
                binding.chessView.isLocked = true
                loadGameForAnalysis()
            }
            Constants.ONLINE_MODE -> {
                initializeSocket()
            }
            Constants.AI_MODE -> {
                loadGameAgainstBot(intent)
            }
        }
    }


    /**
     * Returns the enemy player according to the game mode.
     */
    private fun getEnemyPlayer() : Player {
        return when (gameMode) {
            Constants.ONLINE_MODE -> {
                chessWebSocketListener as Player
            }
            Constants.ANALYSIS_MODE -> {
                this
            }
            Constants.AI_MODE -> {
                this
            }
            else -> {
                this
            }
        }
    }

    /**
     * Initializes the info of the players in the UI and in the view model.
     */
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

    /**
     * Displays The options menu in a game.
     * Changes the text according to the draw offer status from the opponent.
     * bot cannot accept draw, so it is ignored.
     */
    private fun showGameOptionsMenu(){
        val popupMenu = PopupMenu(this@GameActivity, binding.ibExtraSettings)
        popupMenu.menuInflater.inflate(R.menu.popup_game_options_menu, popupMenu.menu)
        if (isDrawOffered) {
            popupMenu.menu.getItem(R.id.draw).setTitle(R.string.accept_draw)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId){
                R.id.resign -> {
                    showConcedeDialog()
                    return@setOnMenuItemClickListener true
                }
                R.id.draw -> {
                    sendDrawOffer() // Does nothing against a bot.
                    return@setOnMenuItemClickListener true
                }
                else -> {return@setOnMenuItemClickListener true}
            }
        }
        popupMenu.show()
    }

    /**
     * Sends the draw offer to the server.
     */
    private fun sendDrawOffer() {
        if (viewModel.socketStatus.value == true) {
            sendMessageToServer(DrawOfferMessage(currentPlayerEmail, viewModel.isWhite))
        }
    }

    /**
     * Flips the board and user data.
     */
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

    /**
     * Initializes the websocket
     */
    private fun initializeSocket(){
        chessWebSocketListener = ChessWebSocketListener(viewModel, currentPlayerEmail)
        chessWebSocketListener!!.connectWebSocket(
            Game.getInstance()!!.id,
            tokenManager.getAccessToken()
        )
        chessWebSocket = chessWebSocketListener!!.getWebSocketInstance()
    }

    /**
     * Loads the game for analysis by playing all of the moves.
     */
    private fun loadGameForAnalysis(){
        // Get start data
        val startData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.START_DATA, GameStartMessage::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Constants.START_DATA)!!
        }

        viewModel.startGame(startData)
        viewModel.whitePlayer = this
        viewModel.blackPlayer = this

        val movesString : String = intent.getStringExtra(Constants.MOVES_LIST)!!
        // Iterate over the moves and play them
        val movesList : List<String> = movesString.split(",")
        for (moveStr in movesList) {
            if (moveStr.length >= 4) {
                viewModel.onMoveChosen(MoveUtility.getMoveFromUCIName(moveStr, viewModel.board))
            }
        }
        binding.chessView.invalidate()
    }

    /**
     * Loads a game against a bot. Also updates the user information in the UI.
     */
    private fun loadGameAgainstBot(intent: Intent) {
        val isWhite : Boolean = Random.nextBoolean()
        val currentUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.USER, User::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Constants.USER)!!
        }
        val startData = if (isWhite) {
            GameStartMessage(currentUser.name, "BOT", currentUser.email, "", currentUser.email, "", currentUser.elo, 1800)
        } else {
            GameStartMessage("BOT", currentUser.name, "", currentUser.email, "", currentUser.image, 1800, currentUser.elo)
        }
        botPlayer = Bot(5_000, 1_00, viewModel)
        if (!isWhite) {
            botPlayer!!.onOpponentMoveChosen()
        }
        if (isWhite) {
            viewModel.whitePlayerInfo = PlayerInfo(currentUser.name, currentUser.email, currentUser.image, currentUser.elo)
            viewModel.blackPlayerInfo = PlayerInfo("BOT", "", "", 1800)
            viewModel.whitePlayer = this
            viewModel.blackPlayer = this
        } else {
            viewModel.whitePlayerInfo = PlayerInfo("BOT", "", "", 1800)
            viewModel.blackPlayerInfo = PlayerInfo(currentUser.name, currentUser.email, currentUser.image, currentUser.elo)
            viewModel.blackPlayer = this
            viewModel.whitePlayer = this
        }
        viewModel.startGame(startData)
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
                viewModel.onMoveChosen(chosenMove)
            }
        }
        isDrawOffered = false

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
        viewModel.onMoveChosen(chosenMove)
        binding.chessView.invalidate()
        return true
    }

    override fun getBoard(): Board = viewModel.board

    override fun onOpponentMoveChosen() {
        binding.chessView.invalidate()
    }


}
