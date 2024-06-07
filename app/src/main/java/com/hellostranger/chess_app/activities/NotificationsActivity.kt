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
/**
 * Activity for displaying and handling notifications, particularly friend requests.
 */
class NotificationsActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private var tokenManager : TokenManager = MyApp.tokenManager
    private lateinit var friendsAdapter : NotificationAdapter

    /**
     * Called when the activity is first created. Initializes the activity.
     */
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
        handleResponse(
            request = {BackendRetrofitClient.instance.getFriendRequests(tokenManager.getUserEmail())},
            errorMessage = "Couldn't fetch friend requests"
        )?.let {
            val notificationList : MutableList<Notification> = mutableListOf()
            for(friendRequest : FriendRequest in it){
                // Adding each friend request as a notification to the list
                notificationList.add(
                    Notification(
                        friendRequest.id,
                        friendRequest.sender.image,
                        friendRequest.sender.name,
                        friendRequest.sender.email
                    )
                )
            }
            runOnUiThread {
                // Updating the adapter with the new list of notifications
                friendsAdapter.updateNotificationList(notificationList)
            }

        }

    }

    /**
     * Initializes the adapter for displaying friend requests and sets up click listeners.
     * @return NotificationAdapter - The initialized adapter.
     */
    private fun initializeFriendsAdapter() : NotificationAdapter {
        return NotificationAdapter(
            NotificationAdapter.NotificationOnClickListener(
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Accepting the friend request
                        BackendRetrofitClient.instance.acceptFriendRequest(tokenManager.getUserEmail(), it.id)

                        // Updating the requests after acceptance
                        updateRequests()
                    }
                },
                {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Rejecting the friend request
                        BackendRetrofitClient.instance.rejectFriendRequest(tokenManager.getUserEmail(), it.id)

                        // Updating the requests after rejection
                        updateRequests()
                    }
                },
                {
                    val intent = Intent(this@NotificationsActivity, ProfileActivity::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        handleResponse(
                            {BackendRetrofitClient.instance.getUserByEmail(it.email)},
                            "Couldn't fetch Guest User"
                        )?.let { guest ->
                            // Passing the fetched guest user data to the ProfileActivity
                            intent.putExtra(Constants.USER, guest)
                            startActivity(intent)
                        }
                    }
                }
            )
        )
    }
}