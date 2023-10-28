package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.hellostranger.chess_app.UserRepository
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

    private val gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Piece::class.java, PieceJsonDeserializer())
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val favoriteGamesDb = Room.databaseBuilder(
            applicationContext, GameHistoryDatabase::class.java, Constants.FAVORITE_GAMES_DB
        ).fallbackToDestructiveMigration().build()

        viewModel = ViewModelProvider(this,
            ProfileViewModelFactory(
                GameHistoryRepository(GeneralRetrofitClient.instance, tokenManager),
                UserRepository(GeneralRetrofitClient.instance, tokenManager),
                favoriteGamesDb.dao
                )
        )[ProfileViewModel::class.java]

        adapter = GamesHistoryAdapter(
            GamesHistoryAdapter.OnClickListener(
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
                    intent.putExtra(Constants.MOVES_LIST, startMessage)
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
        binding.rvRecentGames.adapter = adapter
        binding.rvRecentGames.setHasFixedSize(true)
        viewModel.gameHistoryList.observe(this) {
            Log.d(TAG, "onCreate History: $it")
            adapter.updateGameHistoryList(it)
        }
        viewModel.failed.observe(this) { Log.e(TAG, "viewModel failed. it: $it") }
        viewModel.getAllGameHistories()

        viewModel.userDetails.observe(this){
            Log.d(TAG, "onCreate User: $it")
            setDataInUi(it)
        }
        viewModel.getUserDetails()


        binding.ivToolbarOptions.setOnClickListener{
            if(currentUser != null){
                val intent = Intent(this@ProfileActivity, UpdateProfileActivity::class.java)
                intent.putExtra("USER", currentUser)
                startActivity(intent)
            }
        }

        binding.ivToolbarBack.setOnClickListener { onBackPressed() }
    }

    private fun setDataInUi(user: User) {
        currentUser = user
        binding.tvUsername.text = user.name
        binding.tvElo.text = user.elo.toString()
        binding.tvGamesCount.text = (user.totalGames).toString()
        binding.tvGamesDrawn.text = user.gamesDrawn.toString()
        binding.tvGamesWon.text = user.gamesWon.toString()
        binding.tvGamesLost.text = user.gamesLost.toString()
        Glide
            .with(this@ProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivProfileImage)
    }


}