package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.GameHistoryEvent
import com.hellostranger.chess_app.GameHistoryRepository
import com.hellostranger.chess_app.GamesHistoryAdapter
import com.hellostranger.chess_app.ProfileViewModel
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.ProfileViewModelFactory
import com.hellostranger.chess_app.databinding.ActivityProfileBinding
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.models.entites.User
import com.hellostranger.chess_app.models.gameModels.Board
import com.hellostranger.chess_app.models.gameModels.Game
import com.hellostranger.chess_app.network.retrofit.general.GeneralRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.database.GameHistoryDatabase
import com.hellostranger.chess_app.models.gameModels.pieces.Piece
import com.hellostranger.chess_app.models.gameModels.pieces.PieceJsonDeserializer

private const val TAG = "ProfileActivity"
class ProfileActivity : BaseActivity() {
    private lateinit var binding : ActivityProfileBinding
    private var currentUser : User? = null
    private var tokenManager : TokenManager = MyApp.tokenManager

    private lateinit var viewModel : ProfileViewModel
    private lateinit var adapter : GamesHistoryAdapter

    private var isGuestUser : Boolean = false

    private val gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Piece::class.java, PieceJsonDeserializer())
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize Room DB

        val favoriteGamesDb = Room.databaseBuilder(
            applicationContext, GameHistoryDatabase::class.java, Constants.FAVORITE_GAMES_DB
        ).fallbackToDestructiveMigration().build()

        //Take the userEmail From The intent, Compare it against the logged-in userEmail.
        var userEmail = intent.getStringExtra(Constants.GUEST_EMAIL)
        if(userEmail == null) {
            userEmail = tokenManager.getUserEmail()
            binding.llGuestExtraOptions.visibility = View.GONE
        } else{
            isGuestUser = true
        }

        viewModel = ViewModelProvider(this,
            ProfileViewModelFactory(GameHistoryRepository(GeneralRetrofitClient.instance, tokenManager, favoriteGamesDb.dao))
        )[ProfileViewModel::class.java]

        adapter = initializeGameHistoryAdapter()
        binding.rvRecentGames.adapter = adapter
        binding.rvRecentGames.setHasFixedSize(true)


        viewModel.gameHistoryList.observe(this) {
            Log.d(TAG, "onCreate History: $it")
            adapter.updateGameHistoryList(it)
        }
        viewModel.userDetails.observe(this){
            Log.d(TAG, "onCreate User: $it")
            setDataInUi(it)
        }
        viewModel.isFriendsWithUser.observe(this){
            Log.d(TAG, "isFriendsWithUser changed to: $it")
            if(it){
                binding.tvSendFriendRequest.visibility = View.GONE
                binding.tvUnfriend.visibility = View.VISIBLE
            } else{
                binding.tvSendFriendRequest.visibility = View.VISIBLE
                binding.tvUnfriend.visibility = View.GONE
            }

        }
        viewModel.failed.observe(this) {
            Log.e(TAG, "viewModel failed. it: $it")
        }


        viewModel.getAllGameHistories(userEmail)
        viewModel.getUserDetails(userEmail)
        viewModel.areFriendsWithUser(userEmail)

        viewModel.friendRequestStatus.observe(this){
            Log.e(TAG, "viewModel friendRequestStatus: $it")
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
        binding.tvSendFriendRequest.setOnClickListener {
            viewModel.sendFriendRequest(userEmail)

        }
        binding.tvUnfriend.setOnClickListener {
            viewModel.removeFriend(userEmail)
            binding.tvSendFriendRequest.visibility = View.VISIBLE
            binding.tvUnfriend.visibility = View.GONE

        }
        binding.ivToolbarOptions.setOnClickListener{
            if(currentUser != null && !isGuestUser){
                val intent = Intent(this@ProfileActivity, UpdateProfileActivity::class.java)
                intent.putExtra("USER", currentUser)
                startActivity(intent)
            }
        }

        binding.ivToolbarBack.setOnClickListener { onBackPressed() }
    }
    private fun initializeGameHistoryAdapter() : GamesHistoryAdapter{
        return GamesHistoryAdapter(
            GamesHistoryAdapter.GameHistoryOnClickListener(
                {
                    val startMessage = GameStartMessage(
                        whiteName = it.whiteName, blackName = it.blackName,
                        whiteEmail = "", blackEmail = "",
                        whiteImage = it.whiteImage, blackImage = it.blackImage,
                        whiteElo = it.whiteElo, blackElo = it.blackElo
                    )

                    val game = Game(board = gson.fromJson(it.startBoardJson,  Board::class.java), id = "",gameState= it.result)
                    Game.setInstance(game)
                    updatePiecesResId()
                    val intent = Intent(this@ProfileActivity, GameActivity::class.java)
                    intent.putExtra(Constants.MODE, Constants.ANALYSIS_MODE)
                    intent.putExtra(Constants.MOVES_LIST, it.gameMoves)
                    intent.putExtra(Constants.START_DATA, startMessage)
                    startActivity(intent)

                },
                {
                    if(it.isSaved){
                        Toast.makeText(this@ProfileActivity, "Delete Game from favorites", Toast.LENGTH_LONG).show()
                        viewModel.onEvent(GameHistoryEvent.DeleteGame(it))
                    } else{
                        Toast.makeText(this@ProfileActivity, "Save Game", Toast.LENGTH_LONG).show()
                        viewModel.onEvent(GameHistoryEvent.SaveGame(it))

                    }

                }
            ),
            4
        )
    }
    private fun setDataInUi(user: User) {
        currentUser = user
        binding.tvUsername.text = user.name
        binding.tvElo.text = user.elo.toString()
        binding.tvGamesCount.text = (user.totalGames).toString()
        binding.tvGamesDrawn.text = user.gamesDrawn.toString()
        binding.tvGamesWon.text = user.gamesWon.toString()
        binding.tvGamesLost.text = user.gamesLost.toString()
        binding.tvUserCreation.text = String.format(resources.getString(R.string.joined_at), user.accountCreation)
        Glide
            .with(this@ProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivProfileImage)
    }


}