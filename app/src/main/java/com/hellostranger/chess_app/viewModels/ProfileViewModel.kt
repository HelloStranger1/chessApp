package com.hellostranger.chess_app.viewModels

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.database.UserRepository
import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.models.rvEntities.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

const val PROFILE_VM_TAG = "ProfileViewModel"
@ExperimentalUnsignedTypes
class ProfileViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    val gameHistoryList = MutableLiveData<List<GameHistory>>()
    private val recentGameHistories = ArrayList<GameHistory>()
    private val savedGameHistories = ArrayList<GameHistory>()
    val friendsList = MutableLiveData<List<Friend>>()
    val userDetails = MutableLiveData<User>()
    val isFriendsWithUser = MutableLiveData(false)
    val friendRequestStatus = MutableLiveData("")


    fun getUserDetails(email : String) = viewModelScope.launch(Dispatchers.IO) {
        handleResponse(
            {userRepository.getUserByEmail(email)},
            "Couldn't load user"
        )?.let {
            userDetails.postValue(it)
        }
    }

    fun areFriendsWithUser(friendEmail : String)  = viewModelScope.launch(Dispatchers.IO) {
        handleResponse(
            {userRepository.getFriends(userRepository.tokenManager.getUserEmail())},
            "Error when fetching user friends."
        )?.let{
            for (friend : User in it) {
                if (friend.email == friendEmail) {
                    isFriendsWithUser.postValue(true)
                    return@launch
                }
            }
        }
        isFriendsWithUser.postValue(false)
    }
    fun sendFriendRequest(email: String) = viewModelScope.launch(Dispatchers.IO){
        val response = handleResponse(
            {userRepository.sendFriendRequest(email)},
            "Friend request failed."
        )
        friendRequestStatus.postValue(response ?: "")
    }

    fun removeFriend(email : String) = viewModelScope.launch(Dispatchers.IO){
        userRepository.deleteFriend(email)
        withContext(Dispatchers.Main) {
            areFriendsWithUser(email)
        }
    }

    fun saveGame(gameHistory : GameHistory) {
        gameHistory.isSaved = true
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsertFavoriteGameHistory(gameHistory)
        }
    }
    fun deleteGame(gameHistory: GameHistory) {
        gameHistory.isSaved = false
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.removeGameHistoryFromFavorites(gameHistory)
        }
    }
    fun getAllGameHistories(email : String) {
        viewModelScope.launch(Dispatchers.IO) {
            val gameHistories = handleResponse(
                {userRepository.getAllGameHistoriesByEmail(email)},
                "Couldn't fetch all game histories."
            )
            recentGameHistories.clear()
            gameHistories?.let {
                recentGameHistories.addAll(it)
                for (gameHistory in it) {
                    if(userRepository.getFavoriteGameHistoryById(gameHistory.id) != null){
                        gameHistory.isSaved = true
                    }
                }
                gameHistoryList.postValue(it)
            }
            savedGameHistories.clear()
            val savedGames = userRepository.getSavedGameHistories().filter {
                recentGameHistories.contains(it)
            }
            savedGames.forEach {
                it.isSaved = true
            }
            savedGameHistories.addAll(savedGames)
        }
    }

    fun swapToSavedGameHistories() {
        gameHistoryList.postValue(savedGameHistories)
    }
    fun swapToRecentGameHistories() {
        gameHistoryList.postValue(recentGameHistories)
    }
    fun getFriendsList(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userList = handleResponse(
                {userRepository.getFriends(email)},
                "Couldn't get friends list"
            )
            userList?.let {
                val tempFriendList : MutableList<Friend> = mutableListOf()
                for (user in it) {
                    val friend = Friend(user.image, user.email, user.name)
                    tempFriendList.add(friend)
                }
                friendsList.postValue(tempFriendList)
            }
        }
    }

    /**
     * A wrapper function to make a request, check if it failed, and log out if it did.
     * @param request: The function to make the request (retrofit)
     * @param errorMessage: An error message to be logged out in case on an error
     * @return The response of the request. the type is generic, matching the request. If the request failed, will be null, otherwise it is not null.
     */
    private suspend fun <T> handleResponse(
        request : suspend () -> Response<T>,
        errorMessage : String
    ) : T? {
        val response = request()
        if (!response.isSuccessful || response.body() == null) {
            Log.e(PROFILE_VM_TAG, "$errorMessage. error is: ${response.errorBody()?.toString()}")
            return null
        }
        return response.body()!!
    }



}