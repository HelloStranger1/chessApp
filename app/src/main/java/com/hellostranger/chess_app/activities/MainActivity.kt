package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.chess_models.Game
import com.hellostranger.chess_app.chess_models.Player
import com.hellostranger.chess_app.databinding.ActivityMainBinding
import com.hellostranger.chess_app.firebase.FirestoreClass
import com.hellostranger.chess_app.models.User
import com.hellostranger.chess_app.retrofit.RetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbarMainActivity : Toolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()

        binding.navView.setNavigationItemSelectedListener(this)

        FirestoreClass().signInUser(this)

        binding.appBarMain.mainContent.btnCreate.setOnClickListener {
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            showProgressDialog("Creating game...")
            scope.launch {
                val response =
                    RetrofitClient.instance.createGame()
                if(response.isSuccessful && response.body() != null){
                    Log.e("TAG", "Game started, body: ${response.body()}")
                    Game.setInstance(response.body()!!)
                    runOnUiThread {
                        hideProgressDialog()
                    }
                }
            }
            Toast.makeText(this@MainActivity, "Game Created. you can now join.", Toast.LENGTH_LONG).show()
        }

        binding.appBarMain.mainContent.btnJoinRandom.setOnClickListener {
            var playerName = findViewById<TextView>(R.id.tv_username).text.toString()
            var playerId = getCurrentUserID()
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            showProgressDialog("Joining random game...")
            scope.launch {
                val response =
                    RetrofitClient.instance.joinRandomGame(Player(playerId, playerName))
                if(response.isSuccessful && response.body() != null){
                    Log.e("TAG", "Game joined, body: ${response.body()}")
                    Game.setInstance(response.body()!!)
                    updatePiecesResId()
                    runOnUiThread {
                        hideProgressDialog()
                        val intent = Intent(this@MainActivity, GameActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            Toast.makeText(this@MainActivity, "Game Joined", Toast.LENGTH_SHORT).show()
        }


        binding.appBarMain.mainContent.etGameId
        binding.appBarMain.mainContent.btnJoinSpecific
    }
    fun updateNavigationUserDetails(user : User){
        findViewById<TextView>(R.id.tv_username).text = user.name
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
                Toast.makeText(this@MainActivity,"My Profile", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_sign_out ->{
                FirebaseAuth.getInstance().signOut()

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