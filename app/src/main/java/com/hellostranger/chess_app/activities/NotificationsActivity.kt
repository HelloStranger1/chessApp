package com.hellostranger.chess_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.hellostranger.chess_app.rv.adapters.NotificationAdapter
import com.hellostranger.chess_app.databinding.ActivityNotificationsBinding
import com.hellostranger.chess_app.dto.requests.FriendRequest
import com.hellostranger.chess_app.models.rvEntities.Notification
import com.hellostranger.chess_app.network.retrofit.backend.BackendRetrofitClient
import com.hellostranger.chess_app.utils.Constants
import com.hellostranger.chess_app.utils.MyApp
import com.hellostranger.chess_app.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalUnsignedTypes
class NotificationsActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private var tokenManager : TokenManager = MyApp.tokenManager
    private lateinit var friendsAdapter : NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        friendsAdapter = initializeFriendsAdapter()

        binding.rvFriendRequests.adapter = friendsAdapter

        // Updates the requests every 10s
        lifecycleScope.launch(Dispatchers.IO) {
            while(true){
                updateRequests()
                delay(10_000)
            }

        }
    }

    /**
     * Fetches and updates the friend requests from the server.
     */
    private suspend fun updateRequests(){
        val response = BackendRetrofitClient.instance.getFriendRequests(tokenManager.getUserEmail())
        if(!response.isSuccessful || response.body() == null) {
            Log.e("Notification Activity", "Couldn't fetch friend requests")
            return
        }
        val friendRequests = handleResponse(
            request = {BackendRetrofitClient.instance.getFriendRequests(tokenManager.getUserEmail())},
            errorMessage = "Couldn't fetch friend requests"
        )
        friendRequests?.let {
            val notificationList : MutableList<Notification> = mutableListOf()
            for(friendRequest : FriendRequest in it){
                notificationList.add(Notification(friendRequest.id, friendRequest.sender.image, friendRequest.sender.name, friendRequest.sender.email))
            }
            runOnUiThread {
                friendsAdapter.updateNotificationList(notificationList)
            }

        }

    }

    private fun initializeFriendsAdapter() : NotificationAdapter {
        return NotificationAdapter(
            NotificationAdapter.NotificationOnClickListener(
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        BackendRetrofitClient.instance.acceptFriendRequest(tokenManager.getUserEmail(), it.id)
                        updateRequests()
                    }
                },
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        BackendRetrofitClient.instance.rejectFriendRequest(tokenManager.getUserEmail(), it.id)
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