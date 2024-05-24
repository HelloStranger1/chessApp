package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.rv.GameHistoryEvent
import com.hellostranger.chess_app.database.UserRepository
import com.hellostranger.chess_app.rv.adapters.GamesHistoryAdapter
import com.hellostranger.chess_app.viewModels.ProfileViewModel
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.Game
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.viewModels.factories.ProfileViewModelFactory
import com.hellostranger.chess_app.databinding.ActivityProfileBinding
import com.hellostranger.chess_app.dto.websocket.GameStartMessage
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.models.rvEntities.Friend
import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.rv.adapters.FriendsAdapter

private const val TAG = "ProfileActivity"
@ExperimentalUnsignedTypes
class ProfileActivity : BaseActivity() {
    private lateinit var binding : ActivityProfileBinding
    private var currentUser : User? = null
    private var tokenManager : TokenManager = MyApp.tokenManager

    private lateinit var viewModel : ProfileViewModel
    private lateinit var gamesHistoryAdapter : GamesHistoryAdapter
    private lateinit var friendsAdapter : FriendsAdapter

    private var isGuestUser : Boolean = false


    override fun onRestart() {
        super.onRestart()
        currentUser?.email?.let { viewModel.getUserDetails(it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize Room DB

        val favoriteGamesDb = MyApp.favoriteGameDB
        //Take the userEmail From The intent, Compare it against the logged-in userEmail.
        var userEmail = intent.getStringExtra(Constants.GUEST_EMAIL)
        if(userEmail == null) {
            userEmail = tokenManager.getUserEmail()
            binding.llGuestExtraOptions.visibility = View.GONE
        } else{
            val ourEmail = tokenManager.getUserEmail()
            if (userEmail == ourEmail) {
                userEmail = ourEmail
                binding.llGuestExtraOptions.visibility = View.GONE
            } else {
                isGuestUser = true
            }
        }

        viewModel = ViewModelProvider(this,
            ProfileViewModelFactory(UserRepository(BackendRetrofitClient.instance, tokenManager, favoriteGamesDb.dao))
        )[ProfileViewModel::class.java]

        gamesHistoryAdapter = initializeGameHistoryAdapter()
        binding.rvRecentGames.adapter = gamesHistoryAdapter
        binding.rvRecentGames.setHasFixedSize(true)

        friendsAdapter = initializeFriendsAdapter()
        binding.rvFriends.adapter = friendsAdapter
        binding.rvFriends.setHasFixedSize(true)


        viewModel.gameHistoryList.observe(this) {
            Log.d(TAG, "onCreate History: $it")
            gamesHistoryAdapter.updateGameHistoryList(it)
        }
        viewModel.friendsList.observe(this) {
            Log.d(TAG, "onCreate friends: $it")
            friendsAdapter.updateFriendList(it)
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
        viewModel.getFriendsList(userEmail)

        viewModel.friendRequestStatus.observe(this){
            if (it.isNotBlank()){
                Log.e(TAG, "viewModel friendRequestStatus: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
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
    private fun initializeGameHistoryAdapter() : GamesHistoryAdapter {
        return GamesHistoryAdapter(
            GamesHistoryAdapter.GameHistoryOnClickListener(
                {
                    openGameHistory(it)
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
            )
        )
    }
    private fun initializeFriendsAdapter() : FriendsAdapter {
        return FriendsAdapter(
            FriendsAdapter.FriendOnClickListener {
                openFriend(it)
            }
        )
    }
    private fun openFriend(it : Friend) {
        Log.i(TAG, "Opening friend with email: ${it.email} and name: ${it.name}")
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra(Constants.GUEST_EMAIL, it.email)
        startActivity(intent)
    }
    private fun openGameHistory(it : GameHistory) {
        val startMessage = GameStartMessage(
            whiteName = it.whiteName, blackName = it.blackName,
            whiteEmail = "", blackEmail = "",
            whiteImage = it.whiteImage, blackImage = it.blackImage,
            whiteElo = it.whiteElo, blackElo = it.blackElo
        )

        val game = Game(id = "", gameResult = it.result, boardsFen = mutableListOf(it.startBoardFen))
        Game.setInstance(game)
        // updatePiecesResId()

        val intent = Intent(this@ProfileActivity, GameActivity::class.java)
        intent.putExtra(Constants.MODE, Constants.ANALYSIS_MODE)
        intent.putExtra(Constants.MOVES_LIST, it.gameMoves)
        intent.putExtra(Constants.START_DATA, startMessage)
        startActivity(intent)
    }
    private fun setDataInUi(user: User) {
        currentUser = user
        binding.tvUsername.text = user.name
        binding.tvElo.text = user.elo.toString()
        binding.tvGamesCount.text = (user.totalGames).toString()
        binding.tvGamesDrawn.text = user.gamesDrawn.toString()
        binding.tvGamesWon.text = user.gamesWon.toString()
        binding.tvGamesLost.text = user.gamesLost.toString()
        if(user.isActive){
            binding.activityBox.visibility = View.VISIBLE
        } else{
            binding.activityBox.visibility = View.GONE
        }
        binding.tvUserCreation.text = String.format(resources.getString(R.string.joined_at), user.accountCreation)
        Glide
            .with(this@ProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.ivProfileImage)
    }


}