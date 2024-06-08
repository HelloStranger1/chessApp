package com.hellostranger.chess_app.activities

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
/**
 * Activity for displaying and managing a user's profile.
 */
class ProfileActivity : BaseActivity() {
    private lateinit var binding : ActivityProfileBinding // Binding for the UI
    private lateinit var currentUser : User // The current user
    private var tokenManager : TokenManager = MyApp.tokenManager // Manages tokens and user email

    private lateinit var viewModel : ProfileViewModel // The view model
    private lateinit var gamesHistoryAdapter : GamesHistoryAdapter // Game History recyclerview adapter
    private lateinit var friendsAdapter : FriendsAdapter // friends recyclerview adapter
    private var isShowingRecent = true // To Switch between Recent games and saved games. If guest, we don't care (only recent)

    private var isGuestUser : Boolean = false


    /**
     * Called when the activity is restarted. Fetches user details.
     */
    override fun onRestart() {
        super.onRestart()
        currentUser.email.let { viewModel.getUserDetails(it) }
    }

    /**
     * Called when the activity is first created. Initializes the activity.
     */
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


        // Observe the game history list and update the adapter when it changes.
        viewModel.gameHistoryList.observe(this) {
            gamesHistoryAdapter.updateGameHistoryList(it)
        }

        // Observe the friends list and update the adapter when it changes.
        viewModel.friendsList.observe(this) {
            friendsAdapter.updateFriendList(it)
        }

        // Observe the user details and update the UI when they change.
        viewModel.userDetails.observe(this){
            setDataInUi(it)
        }

        // Observe the friendship status and update the UI accordingly.
        viewModel.isFriendsWithUser.observe(this){
            if(it){
                binding.tvSendFriendRequest.visibility = View.GONE
                binding.tvUnfriend.visibility = View.VISIBLE
            } else{
                binding.tvSendFriendRequest.visibility = View.VISIBLE
                binding.tvUnfriend.visibility = View.GONE
            }

        }

        // Initial data fetch
        viewModel.getAllGameHistories(currentUser.email)
        viewModel.getUserDetails(currentUser.email)
        viewModel.areFriendsWithUser(currentUser.email)
        viewModel.getFriendsList(currentUser.email)

        // Observe the friend request status and display a toast message.
        viewModel.friendRequestStatus.observe(this){
            if (it.isNotBlank()){
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Send friend request on click.
        binding.tvSendFriendRequest.setOnClickListener {
            viewModel.sendFriendRequest(currentUser.email)
        }

        // Remove friend on click.
        binding.tvUnfriend.setOnClickListener {
            viewModel.removeFriend(currentUser.email)
            binding.tvSendFriendRequest.visibility = View.VISIBLE
            binding.tvUnfriend.visibility = View.GONE
        }

        binding.rbRecentGames.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isShowingRecent = true
                binding.tvGamesRvDesc.text = resources.getString(R.string.recent_games)
                viewModel.swapToRecentGameHistories()
            } else {
                isShowingRecent = false
                binding.tvGamesRvDesc.text = resources.getString(R.string.saved_games)
                viewModel.swapToSavedGameHistories()
            }
        }

        // Open update profile activity on click if not a guest user.
        binding.ivToolbarOptions.setOnClickListener{
            if(!isGuestUser){
                val intent = Intent(this@ProfileActivity, UpdateProfileActivity::class.java)
                intent.putExtra(Constants.USER, currentUser)
                startActivity(intent)
            }
        }

        // Handle back navigation.
        binding.ivToolbarBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Sets up the friends adapter and assigns it to the RecyclerView.
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
     * Creates the game history adapter and the necessary onClick listener.
     * @return GamesHistoryAdapter - The initialized adapter.
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
                        viewModel.deleteGame(it)
                    } else{
                        Toast.makeText(this@ProfileActivity, "Save Game", Toast.LENGTH_LONG).show()
                        viewModel.saveGame(it)
                    }
                }
            )
        )
    }

    /**
     * Creates the friends adapter and the necessary onClick listener.
     * @return FriendsAdapter - The initialized adapter.
     */
    private fun initializeFriendsAdapter() : FriendsAdapter {
        return FriendsAdapter(
            FriendsAdapter.FriendOnClickListener {
                openFriend(it)
            }
        )
    }

    /**
     * Extracts user info passed in with the intent and updates isGuestUser.
     * @param intent: Intent - The intent containing user info.
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
            binding.radioGroup.visibility = View.GONE
        }
    }


    /**
     * Opens the friend's profile.
     * @param it: Friend - The friend to be opened.
     */
    private fun openFriend(it : Friend) {
        val intent = Intent(this, ProfileActivity::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            handleResponse(
                {BackendRetrofitClient.instance.getUserByEmail(it.email)},
                "Couldn't fetch guest user"
            )?.let {
                intent.putExtra(Constants.USER, it)
                startActivity(intent)
            }
        }
    }

    /**
     * Opens the game history.
     * @param it: GameHistory - The game history to be opened.
     */
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
        intent.putExtra(Constants.USER, currentUser)
        startActivity(intent)
    }

    /**
     * Sets the user data in the UI.
     * @param user: User - The user data to be displayed.
     */
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