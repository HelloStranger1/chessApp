package com.hellostranger.chess_app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.Game
import com.hellostranger.chess_app.core.GameResult
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "MainActivity"
@ExperimentalUnsignedTypes
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarMainActivity : Toolbar

    private lateinit var currentUser : User

    private var tokenManager : TokenManager = MyApp.tokenManager


    private val coroutineExceptionHandler = CoroutineExceptionHandler{ _, throwable ->
        throwable.printStackTrace()
    }

    override fun onRestart() {
        super.onRestart()
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            fetchUser()?.let {
                runOnUiThread { updateNavigationUserDetails(it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        binding.navView.setNavigationItemSelectedListener(this)


        startService(Intent(this, KeepAlive::class.java))


        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            fetchUser()?.let {
                runOnUiThread { updateNavigationUserDetails(it) }
            }
        }


        binding.appBarMain.mainContent.btnJoinRandom.setOnClickListener {
            showProgressDialog("Joining random game...", false)
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val game : Game? = joinRandomGame(JoinRequest(tokenManager.getUserEmail()))
                game?.let {
                    Game.setInstance(it)
                    // updatePiecesResId()
                    runOnUiThread {
                        hideProgressDialog()
                        launchIntoGame(Constants.ONLINE_MODE)
                    }
                }
            }
        }
        binding.appBarMain.mainContent.btnAgainstBot.setOnClickListener{
            Game.setInstance(Game("", GameResult.InProgress))
            val intent = Intent(this@MainActivity, GameActivity::class.java)
            intent.putExtra(Constants.MODE, Constants.AI_MODE)
            intent.putExtra(Constants.USER_EMAIL, currentUser.email)
            intent.putExtra(Constants.USER_NAME, currentUser.name)
            intent.putExtra(Constants.USER_ELO, currentUser.elo)
            intent.putExtra(Constants.USER_IMAGE, currentUser.image)
            startActivity(intent)
        }


        binding.appBarMain.mainContent.btnDoPuzzle.setOnClickListener {
            showProgressDialog("Fetching puzzle...", false)
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {

                val puzzlesList : List<Puzzle>? = fetchPuzzles(Constants.DEFAULT_PUZZLE_AMOUNT)

                puzzlesList?.let {
                    PuzzlesList.instance.addPuzzles(it)
                    hideProgressDialog()
                    val intent = Intent(this@MainActivity, PuzzleActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        binding.appBarMain.mainContent.btnLookupUser.setOnClickListener{
            val email : String = binding.appBarMain.mainContent.etUserEmail.text.toString()
            if (email.isEmpty() || email.isBlank()) {
                Toast.makeText(this@MainActivity, "Please enter an email.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            var success = true
            runBlocking {
                val response = BackendRetrofitClient.instance.getUserByEmail(email)
                if (!response.isSuccessful || response.body() == null) {
                    Toast.makeText(this@MainActivity, "Couldn't find a user with this email", Toast.LENGTH_LONG).show()
                    success = false
                }
            }
            if (!success) {
                return@setOnClickListener
            }
            Log.e(TAG, "email is: ${binding.appBarMain.mainContent.etUserEmail.text}")

            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(Constants.GUEST_EMAIL, binding.appBarMain.mainContent.etUserEmail.text.toString() )
            startActivity(intent)
        }

        binding.appBarMain.mainContent.btnCreatePrivate.setOnClickListener {
            Log.e(TAG, "Creating private game")
            showProgressDialog("Creating game...")
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val response = BackendRetrofitClient.instance.createPrivateGame()
                if(!response.isSuccessful){
                    Log.e(TAG, "Failed to create private game. Response is:" + response.errorBody())
                } else{
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Code is: " + response.body(), Toast.LENGTH_LONG).show()
                    }
                    Log.e(TAG, "Private code is: " + response.body())
                }
                runOnUiThread {
                    hideProgressDialog()
                }
            }
        }
        binding.appBarMain.mainContent.btnJoinSpecific.setOnClickListener {
            val code : String = binding.appBarMain.mainContent.etGameId.text.toString()
            Log.e(TAG, "Join private game. code is: $code")
            if(code.isEmpty() || code.isBlank()){
                runOnUiThread{
                    Toast.makeText(this@MainActivity, "You need to put a code!!", Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }
            showProgressDialog("Joining private game...")

            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val game : Game? = joinPrivateGame(JoinRequest(tokenManager.getUserEmail()), code)
                game?.let {
                    Game.setInstance(it)
                    // updatePiecesResId()
                    runOnUiThread {
                        hideProgressDialog()
                        launchIntoGame(Constants.ONLINE_MODE)
                    }
                }
            }

        }


    }

    private fun launchIntoGame(mode : String) {
        val intent = Intent(this@MainActivity, GameActivity::class.java)
        intent.putExtra(Constants.MODE, mode)
        startActivity(intent)
    }

    private suspend fun joinRandomGame(joinRequest: JoinRequest) : Game? {
        val response =
            BackendRetrofitClient.instance.joinRandomGame(joinRequest)
        if(!response.isSuccessful){
            Log.e(TAG, "(joinRandomGame) Response isn't successful. Error is: " + response.errorBody())
            return null
        }
        if(response.body() == null){
            Log.e(TAG, "(joinRandomGame) Response body is null.")
            return null
        }
        Log.e(TAG, "Game joined, body: ${response.body()}")
        return response.body()!!

    }
    private suspend fun joinPrivateGame(joinRequest: JoinRequest, code : String) : Game? {
        val response =
            BackendRetrofitClient.instance.joinPrivateGame(code, joinRequest)
        if(!response.isSuccessful){
            Log.e(TAG, "(joinPrivate) Response isn't successful. Error is: " + response.errorBody())
            return null
        }
        if(response.body() == null){
            Log.e(TAG, "(joinPrivate) Response body is null.")
            return null
        }
        Log.e(TAG, "Game joined, body: ${response.body()}")
        return response.body()!!

    }

    private suspend fun fetchPuzzles(puzzlesAmount : Int) : List<Puzzle>? {
        val response =
            PuzzleRetrofitClient.instance.getRandomPuzzle(puzzlesAmount)
        if (!response.isSuccessful) {
            Log.e(TAG, "(fetchPuzzle) Response isn't successful. Error is: " + response.errorBody())
            return null
        }
        if (response.body() == null) {
            Log.e(TAG, "(fetchPuzzle) Response body is null.")
            return null
        }
        return response.body()!!
    }

    private suspend fun fetchUser() : User?{
        val response =
            BackendRetrofitClient.instance.getUserByEmail(tokenManager.getUserEmail())
        if(!response.isSuccessful){
            Log.e(TAG, "(fetchUser) Response isn't successful. Error is: " + response.errorBody())
            return null
        }
        if(response.body() == null){
            Log.e(TAG, "(fetchUser) Response body is null.")
            return null
        }
        return response.body()!!
    }
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
    private fun setupActionBar(){
        toolbarMainActivity = binding.appBarMain.toolbarMainActivity
        setSupportActionBar(binding.appBarMain.toolbarMainActivity)
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile ->{
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_notifications ->{
                startActivity(Intent(this, NotificationsActivity::class.java))
            }
            R.id.nav_sign_out ->{
                tokenManager.clearSession()
                val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                    throwable.printStackTrace()
                }
                val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
                scope.launch {
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