package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.bumptech.glide.Glide
import com.google.gson.Gson
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
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState
import com.hellostranger.chess_app.network.retrofit.general.GeneralRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.database.GameHistoryDatabase

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding : ActivityProfileBinding
    private var currentUser : User? = null
    private var tokenManager : TokenManager = MyApp.tokenManager

    lateinit var viewModel : ProfileViewModel
    lateinit var adapter : GamesHistoryAdapter
    private val TAG = "ProfileActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val favoriteGamesDb = Room.databaseBuilder(
            applicationContext,
            GameHistoryDatabase::class.java, "favorite-games-database"
        ).build()
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
                    var startMessage : GameStartMessage
                    if(it.opponentColor == Color.WHITE){
                        startMessage = GameStartMessage(
                            whiteName = it.opponentName,
                            blackName = currentUser!!.name,
                            "", "",
                            whiteImage = it.opponentImage,
                            blackImage = currentUser!!.image,
                            whiteElo = it.opponentElo,
                            blackElo = currentUser!!.elo)
                    } else{
                        startMessage = GameStartMessage(
                            blackName = it.opponentName,
                            whiteName = currentUser!!.name,
                            whiteEmail = "", blackEmail =  "",
                            blackImage = it.opponentImage,
                            whiteImage = currentUser!!.image,
                            blackElo = it.opponentElo,
                            whiteElo = currentUser!!.elo)
                    }
                    val gson = Gson()
                    val game : Game = Game(currentMove = 0 ,board = gson.fromJson(it.boardsHistoryRepresentation[0],  Board::class.java), id = "",gameState= it.result)
                    Game.setInstance(game)
                    val intent = Intent(this@ProfileActivity, GameActivity::class.java)
                    intent.putExtra("MODE", Constants.ANALYSIS_MODE)
                    intent.putExtra("BOARDS", arrayListOf(it.boardsHistoryRepresentation))
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
        viewModel.gameHistoryList.observe(this, Observer {
            Log.d(TAG, "onCreate History: $it")
            adapter.updateGameHistoryList(it)
        })
        viewModel.failed.observe(this, Observer{Log.e(TAG, "viewModel failed. it: $it")})
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