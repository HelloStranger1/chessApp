package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.Game
import com.hellostranger.chess_app.core.board.GameResult
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.databinding.ActivityMainBinding
import com.hellostranger.chess_app.dto.requests.JoinRequest
import com.hellostranger.chess_app.gameHelpers.PuzzlesList
import com.hellostranger.chess_app.models.entities.Puzzle
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import com.hellostranger.chess_app.network.retrofit.puzzleApi.PuzzleRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.KeepAlive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

@ExperimentalUnsignedTypes
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentUser : User
    private val currentEmail : String
        get() = tokenManager.getUserEmail()

    private var tokenManager : TokenManager = MyApp.tokenManager
    private val puzzleClient  = PuzzleRetrofitClient.instance
    private val backendClient = BackendRetrofitClient.instance

    override fun onRestart() {
        super.onRestart()
        fetchAndUpdateUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        binding.navView.setNavigationItemSelectedListener(this)

        startService(Intent(this, KeepAlive::class.java))

        // Fetching the user to display in the UI.
        fetchAndUpdateUser()

        // Overriding onBackPressed so that if the drawer is open, it closes that.
        onBackPressedDispatcher.addCallback(this) {
            if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }


        binding.appBarMain.mainContent.btnJoinRandom.setOnClickListener {
            startRandomGame()
        }

        binding.appBarMain.mainContent.btnAgainstBot.setOnClickListener{
            launchIntoGameAgainstBot()
        }

        binding.appBarMain.mainContent.btnDoPuzzle.setOnClickListener {
            startPuzzlesMode()
        }

        binding.appBarMain.mainContent.btnLookupUser.setOnClickListener{
            visitGuestUserProfile()
        }

        binding.appBarMain.mainContent.btnCreatePrivate.setOnClickListener {
            createPrivateGame()
        }

        binding.appBarMain.mainContent.btnJoinSpecific.setOnClickListener {
            startPrivateGame()
        }
    }

    /**
     * Joins a private game whose code the user placed in a text box, and launches into the game activity.
     */
    private fun startPrivateGame() {
        // Gets the game code
        val code: String = binding.appBarMain.mainContent.etGameId.text.toString()
        if (code.isBlank()) {
            showToast("You need to put in a code!")
            return
        }
        // Joins the game and launches into the activity.
        performActionWithProgressDialog("Joining private game...") {
            joinOnlineGame(JoinRequest(currentEmail), code)?.let {
                Game.setInstance(it)
                startActivity(Intent(this@MainActivity, GameActivity::class.java).apply {
                    putExtra(Constants.MODE, Constants.ONLINE_MODE)
                })
            }
        }
    }

    /**
     * Creates a private game and displays the code to the user as a snackbar.
     */
    private fun createPrivateGame() {
        performActionWithProgressDialog("Creating game...") {
            // Create the private game using the server API
            val privateCode = handleResponse(
                request = { backendClient.createPrivateGame() },
                errorMessage = "Failed to create private game."
            )

            // Display the code using a snackbar.
            privateCode?.let {
                showDismissibleSnackbar("Private code is: $it")
            }
        }
    }

    /**
     * Fetches the details of a different user and launches into the profile screen with their details.
     */
    private fun visitGuestUserProfile() {
        // Gets the user email
        val email: String = binding.appBarMain.mainContent.etUserEmail.text.toString()
        if (email.isBlank()) {
            showToast("Please enter an email.")
            return
        }

        performActionWithProgressDialog("Searching for user...") {
            // Receives the user details from the server
            val guestUser: User? = handleResponse(
                request = { backendClient.getUserByEmail(email) },
                errorMessage = "Couldn't find a user with the email $email"
            )
            // Launches into the profile activity.
            guestUser?.let {
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java).apply {
                    putExtra(Constants.USER, it)
                })
            }
        }
    }

    /**
     * Fetches a list of puzzles from an API on rapidapi and starts the puzzle activity
     */
    private fun startPuzzlesMode() {
        performActionWithProgressDialog("Fetching puzzle...") {
            // Fetch the puzzles
             handleResponse(
                request = { puzzleClient.getRandomPuzzle(Constants.DEFAULT_PUZZLE_AMOUNT) },
                errorMessage = "Couldn't fetch puzzles. Puzzle amount was: ${Constants.DEFAULT_PUZZLE_AMOUNT}"
            )?.let {
                 // Start the activity.
                 PuzzlesList.instance.addPuzzles(it)
                 startActivity(Intent(this@MainActivity, PuzzleActivity::class.java))
             }

        }
    }

    /**
     * Starts and join a random game, and launches into the activity.
     */
    private fun startRandomGame() {
        performActionWithProgressDialog("Joining random game...") {
            joinOnlineGame(JoinRequest(currentEmail))?.let {
                Game.setInstance(it)
                startActivity(Intent(this@MainActivity, GameActivity::class.java).apply {
                    putExtra(Constants.MODE, Constants.ONLINE_MODE)
                })
            }
        }
    }


    /**
     * Used to show a simple toast. Runs on the UI thread
     * @param message: The message to display in the toast.
     * Uses The Toast.LENGTH_LONG by default.
     */
    private fun showToast(message: String) = runOnUiThread {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows the user a snackbar that they can dismiss. Runs on the UI Thread
     * @param message: The message to display to the user.
     */
    private fun showDismissibleSnackbar(message: String): Unit = runOnUiThread {
        Snackbar.make(binding.appBarMain.mainContent.mainContent, message, Snackbar.LENGTH_INDEFINITE)
            .setAction("DISMISS") {}
            .setActionTextColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setBackgroundTint(ContextCompat.getColor(applicationContext, R.color.greyTint))
            .show()
    }

    /**
     * Used to make an action that takes a long time (Network requests and so on), while showing the user a progress dialog.
     * @param message: The message to display in the progress dialog
     * @param action: The action to be performed.
     */
    private fun performActionWithProgressDialog(message : String, action : suspend () -> Unit) {
        showProgressDialog(message, false)
        CoroutineScope(Dispatchers.IO).launch {
            action()
            runOnUiThread {
                hideProgressDialog()
            }
        }
    }

    /**
     * Launches into a game against a bot.
     */
    private fun launchIntoGameAgainstBot() {
        Game.setInstance(Game("", GameResult.InProgress))
        val intent = Intent(this@MainActivity, GameActivity::class.java).apply {
            putExtra(Constants.MODE, Constants.AI_MODE)
            putExtra(Constants.USER, currentUser)
        }
        startActivity(intent)
    }


    /**
     * Fetches the detail's of the current user, and updates them in the UI.
     */
    private fun fetchAndUpdateUser() = CoroutineScope(Dispatchers.IO).launch {
        // Request details from API.
        val user = handleResponse(
            request = {backendClient.getUserByEmail(currentEmail)},
            errorMessage = "Couldn't fetch User."
        )
        // Update details in UI.
        user?.let {
            runOnUiThread {updateNavigationUserDetails(it)}
        }
    }


    /**
     * Joins an online game and returns the game data.
     * @param joinRequest: The Join request, containing the user email.
     * @param code: A game code that can be used in the case of a private game. if empty, assumed to be a random game.
     * @return The game itself. can be null if there was an error.
     */
    private suspend fun joinOnlineGame(joinRequest: JoinRequest, code: String = "") : Game? {
        val request : (suspend () -> Response<Game>) = if (code == "") {
            { backendClient.joinRandomGame(joinRequest)}
        } else {
            { backendClient.joinPrivateGame(code, joinRequest)}
        }
        return handleResponse(request, "Couldn't join online game. our email was: ${joinRequest.playerEmail}")
    }

    /**
     * Updates the user information in the UI.
     * @param user: The user details
     */
    private fun updateNavigationUserDetails(user : User){
        val headerView =  binding.navView.getHeaderView(0)
        val navUserImage = headerView.findViewById<ImageView>(R.id.iv_user_image)

        Glide
            .with(this@MainActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)


        headerView.findViewById<TextView>(R.id.tv_username).text = user.name
        currentUser = user
    }


    /**
     * Initializes the toolbar.
     */
    private fun setupActionBar(){
        val toolbarMainActivity = binding.appBarMain.toolbarMainActivity
        setSupportActionBar(toolbarMainActivity)
        toolbarMainActivity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbarMainActivity.setNavigationOnClickListener {
            toggleDrawer()
        }

    }

    private fun toggleDrawer(){
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * Handles possible choices in our drawer.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile ->{
                // Goes into the profile activity with our details
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(Constants.USER, currentUser)
                startActivity(intent)
            }
            R.id.nav_notifications ->{
                // Goes to the notification activity.
                startActivity(Intent(this, NotificationsActivity::class.java))
            }
            R.id.nav_sign_out ->{
                // Clears the session, logs out, and finishes.
                tokenManager.clearSession()
                CoroutineScope(Dispatchers.IO).launch {
                    AuthRetrofitClient.instance.logout()
                }
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, KeepAlive::class.java))
    }
}