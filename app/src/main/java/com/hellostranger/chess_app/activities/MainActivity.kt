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
import com.hellostranger.chess_app.utils.TokenManager
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.databinding.ActivityMainBinding
import com.hellostranger.chess_app.dto.requests.JoinRequest
import com.hellostranger.chess_app.gameHelpers.FenConvertor
import com.hellostranger.chess_app.gameHelpers.PuzzlesList
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import com.hellostranger.chess_app.network.retrofit.puzzleApi.PuzzleRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarMainActivity : Toolbar

    private var tokenManager : TokenManager = MyApp.tokenManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        binding.navView.setNavigationItemSelectedListener(this)
        val fenConvertor = FenConvertor()
        fenConvertor.convertFENToGame("r3k3/pp2np1Q/3B4/5b2/1P3n2/5N2/P1PK1PPP/q4B1R w q - 1 22")
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            Log.e(TAG, "(Fetching User) user email is: ${tokenManager.getUserEmail()}")
            val response =
                BackendRetrofitClient.instance.getUserByEmail(tokenManager.getUserEmail())
            Log.e(TAG, "(Fetching User) response is: $response and the body is: ${response.body()}")
            if(response.isSuccessful && response.body() != null){
                runOnUiThread {
                    updateNavigationUserDetails(response.body()!!)
                }
            }
        }


        binding.appBarMain.mainContent.btnJoinRandom.setOnClickListener {
            val playerEmail = tokenManager.getUserEmail()
            showProgressDialog("Joining random game...")
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val response =
                    BackendRetrofitClient.instance.joinRandomGame(JoinRequest(playerEmail))
                Log.e(TAG, "(Joining Random Game) Response is: $response and is it successful? ${response.isSuccessful}")
                if(response.isSuccessful && response.body() != null){
                    Log.e(TAG, "Game joined, body: ${response.body()}")
                    Game.setInstance(response.body()!!)
                    updatePiecesResId()
                    runOnUiThread {
                        hideProgressDialog()
                        val intent = Intent(this@MainActivity, GameActivity::class.java)
                        intent.putExtra("MODE", Constants.ONLINE_MODE)
                        startActivity(intent)
                    }
                }
            }
            Toast.makeText(this@MainActivity, "Game Joined", Toast.LENGTH_SHORT).show()
        }


        binding.appBarMain.mainContent.btnCreate.setOnClickListener {
            showProgressDialog("Fetching puzzle...")
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                val response =
                    PuzzleRetrofitClient.instance.getRandomPuzzle(4)
                if(response.isSuccessful && response.body() != null){
                    Log.e("TAG", "Got puzzles. response: ${response.body()}")
                    PuzzlesList.instance.addPuzzles(response.body()!!)
                    Log.e("TAG", "PuzzleList: ${PuzzlesList.instance}")
                    val intent = Intent(this@MainActivity, PuzzleActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        binding.appBarMain.mainContent.btnJoinSpecific.setOnClickListener{
            Log.e(TAG, "email is: ${binding.appBarMain.mainContent.etGameId.text}")
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(Constants.GUEST_EMAIL, binding.appBarMain.mainContent.etGameId.text.toString() )
            startActivity(intent)
        }


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

    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
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
}