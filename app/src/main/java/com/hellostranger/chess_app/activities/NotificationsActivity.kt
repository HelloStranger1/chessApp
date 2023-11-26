package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.hellostranger.chess_app.NotificationAdapter
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.databinding.ActivityNotificationsBinding
import com.hellostranger.chess_app.dto.FriendRequest
import com.hellostranger.chess_app.models.entites.Notification
import com.hellostranger.chess_app.network.retrofit.general.GeneralRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    private var tokenManager : TokenManager = MyApp.tokenManager

    private lateinit var gamesAdapter : NotificationAdapter
    private lateinit var friendsAdapter : NotificationAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }

        gamesAdapter = initializeGamesAdapter()
        friendsAdapter = initializeFriendsAdapter()

        binding.rvFriendRequests.adapter = friendsAdapter
        binding.rvGameChallenges.adapter = gamesAdapter

        lifecycleScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            while(true){
                updateRequests()
                delay(10_000)
            }

        }







    }

    private suspend fun updateRequests(){
        val response = GeneralRetrofitClient.instance.getFriendRequests(tokenManager.getUserEmail())
        if(response.isSuccessful && response.body() != null){
            val notificationList : MutableList<Notification> = mutableListOf()
            for(friendRequest : FriendRequest in response.body()!!){
                notificationList.add(
                    Notification(friendRequest.id, friendRequest.sender.image, friendRequest.sender.name, friendRequest.sender.email)
                )
            }
            runOnUiThread {
                friendsAdapter.updateNotificationList(notificationList)
            }
        }
    }
    private fun initializeGamesAdapter() : NotificationAdapter{
        return NotificationAdapter(
            NotificationAdapter.NotificationOnClickListener(
                {

                },
                {

                },
                {

                }
            )
        )
    }
    private fun initializeFriendsAdapter() : NotificationAdapter{
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        return NotificationAdapter(
            NotificationAdapter.NotificationOnClickListener(
                {
                    CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                        GeneralRetrofitClient.instance.acceptFriendRequest(tokenManager.getUserEmail(), it.id)
                        updateRequests()
                    }
                },
                {
                    CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                        GeneralRetrofitClient.instance.rejectFriendRequest(tokenManager.getUserEmail(), it.id)
                        updateRequests()
                    }
                },
                {
                    val intent = Intent(this@NotificationsActivity, ProfileActivity::class.java)
                    intent.putExtra(Constants.GUEST_EMAIL, it.email)
                    startActivity(intent)
                }
            )
        )
    }
}