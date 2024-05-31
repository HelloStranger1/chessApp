package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
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

@ExperimentalUnsignedTypes
class ProfileActivity : BaseActivity() {
    private lateinit var binding : ActivityProfileBinding
    private lateinit var currentUser : User
    private var tokenManager : TokenManager = MyApp.tokenManager

    private lateinit var viewModel : ProfileViewModel
    private lateinit var gamesHistoryAdapter : GamesHistoryAdapter
    private lateinit var friendsAdapter : FriendsAdapter

    private var isGuestUser : Boolean = false


    override fun onRestart() {
        super.onRestart()
        currentUser.email.let { viewModel.getUserDetails(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize Room DB
        val favoriteGamesDb = MyApp.favoriteGameDB

        //Take the userEmail From The intent, Compare it against the logged-in userEmail.
        extractUserInfo(intent)

        // Create the view model
        viewModel = ViewModelProvider(this,
            ProfileViewModelFactory(UserRepository(BackendRetrofitClient.instance, tokenManager, favoriteGamesDb.dao))
        )[ProfileViewModel::class.java]


        setUpGameHistoryAdapter()
        setUpFriendsAdapter()


        viewModel.gameHistoryList.observe(this) {
            gamesHistoryAdapter.updateGameHistoryList(it)
        }

        viewModel.friendsList.observe(this) {
            friendsAdapter.updateFriendList(it)
        }

        viewModel.userDetails.observe(this){
            setDataInUi(it)
        }

        viewModel.isFriendsWithUser.observe(this){
            if(it){
                binding.tvSendFriendRequest.visibility = View.GONE
                binding.tvUnfriend.visibility = View.VISIBLE
            } else{
                binding.tvSendFriendRequest.visibility = View.VISIBLE
                binding.tvUnfriend.visibility = View.GONE
            }

        }

        viewModel.getAllGameHistories(currentUser.email)
        viewModel.getUserDetails(currentUser.email)
        viewModel.areFriendsWithUser(currentUser.email)
        viewModel.getFriendsList(currentUser.email)

        viewModel.friendRequestStatus.observe(this){
            if (it.isNotBlank()){
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        binding.tvSendFriendRequest.setOnClickListener {
            viewModel.sendFriendRequest(currentUser.email)
        }

        binding.tvUnfriend.setOnClickListener {
            viewModel.removeFriend(currentUser.email)
            binding.tvSendFriendRequest.visibility = View.VISIBLE
            binding.tvUnfriend.visibility = View.GONE

        }

        binding.ivToolbarOptions.setOnClickListener{
            if(!isGuestUser){
                val intent = Intent(this@ProfileActivity, UpdateProfileActivity::class.java)
                intent.putExtra(Constants.USER, currentUser)
                startActivity(intent)
            }
        }

        binding.ivToolbarBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * set ups the game history adapter, and sets it in the correct places
     */
    private fun setUpGameHistoryAdapter() {
        gamesHistoryAdapter = initializeGameHistoryAdapter()
        binding.rvRecentGames.adapter = gamesHistoryAdapter
        binding.rvRecentGames.setHasFixedSize(true)
    }

    /**
     * set ups the friends adapter, and sets it in the correct places
     */
    private fun setUpFriendsAdapter() {
        friendsAdapter = initializeFriendsAdapter()
        binding.rvFriends.adapter = friendsAdapter
        binding.rvFriends.setHasFixedSize(true)
    }

    /**
     * Creates the game history adapter and the necessary onClick listener
     */
    private fun initializeGameHistoryAdapter() : GamesHistoryAdapter {
        return GamesHistoryAdapter(
            GamesHistoryAdapter.GameHistoryOnClickListener(
                {
                    openGameHistory(it)
                },
                {
                    if(it.isSaved){
                        Toast.makeText(this@ProfileActivity, "Delete Game from favorites", Toast.LENGTH_LONG).show()
                        viewModel.saveGame(it)
                    } else{
                        Toast.makeText(this@ProfileActivity, "Save Game", Toast.LENGTH_LONG).show()
                        viewModel.deleteGame(it)

                    }
                }
            )
        )
    }

    /**
     * Similar to initializeGameHistoryAdapter
     */
    private fun initializeFriendsAdapter() : FriendsAdapter {
        return FriendsAdapter(
            FriendsAdapter.FriendOnClickListener {
                openFriend(it)
            }
        )
    }

    /**
     * Gets the user info that was passed in with the intent, and updates isGuestUser
     */
    private fun extractUserInfo(intent: Intent) {
        currentUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.USER, User::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Constants.USER)!!
        }

        val ourEmail = tokenManager.getUserEmail()
        if (currentUser.email == ourEmail) {
            binding.llGuestExtraOptions.visibility = View.GONE
        } else {
            isGuestUser = true
        }
    }


    private fun openFriend(it : Friend) {
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