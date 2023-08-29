package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.MyApp
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.TokenManager
import com.hellostranger.chess_app.databinding.ActivityProfileBinding
import com.hellostranger.chess_app.models.User
import com.hellostranger.chess_app.retrofit.general.GeneralRetrofitClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding : ActivityProfileBinding
    private var currentUser : User? = null
    private var tokenManager : TokenManager = MyApp.tokenManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response = GeneralRetrofitClient.instance.getUserByEmail(tokenManager.getUserEmail())
            if(response.isSuccessful && response.body() != null){
                currentUser = response.body()!!
                runOnUiThread {
                    setDataInUi(response.body()!!)
                }
            }
        }

        binding.ivToolbarOptions.setOnClickListener{
            if(currentUser != null){
                val intent = Intent(this@ProfileActivity, UpdateProfileActivity::class.java)
                intent.putExtra("USER", currentUser)
                startActivity(intent)
            }

        }



    }

    private fun setDataInUi(user: User) {
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

    private fun setUpActionBar() {
        val toolbarProfileActivity = binding.toolbarMyProfileActivity
        setSupportActionBar(toolbarProfileActivity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_arrow_back)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbarProfileActivity.setNavigationOnClickListener { onBackPressed() }

    }
}