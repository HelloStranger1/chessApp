package com.hellostranger.chess_app.viewModels

import android.util.Log
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

const val PROFILE_VM_TAG = "ProfileViewModel"
@ExperimentalUnsignedTypes
class ProfileViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    val gameHistoryList = MutableLiveData<List<GameHistory>>()
    val friendsList = MutableLiveData<List<Friend>>()
    val userDetails = MutableLiveData<User>()
    val isFriendsWithUser = MutableLiveData(false)
    val friendRequestStatus = MutableLiveData("")


    fun getUserDetails(email : String) = viewModelScope.launch(Dispatchers.IO) {
        val response = userRepository.getUserByEmail(email)
        if(response.isSuccessful){
            userDetails.postValue(response.body())
        } else{
            Log.e(PROFILE_VM_TAG, "Couldn't load user, response is: $response and body: ${response.body()}")
        }
    }

    fun areFriendsWithUser(friendEmail : String)  = viewModelScope.launch(Dispatchers.IO) {
        val response = userRepository.getFriends(userRepository.tokenManager.getUserEmail())
        if (!response.isSuccessful || response.body() == null) {
            Log.e(PROFILE_VM_TAG, "Error when fetching user friends. response: $response, error body: ${response.errorBody()}")
            return@launch
        }
        val userFriends : List<User> = response.body()!!
        for(friend : User in userFriends){
            if(friend.email == friendEmail){
                isFriendsWithUser.postValue(true)
                return@launch
            }
        }
        isFriendsWithUser.postValue(false)
    }
    fun sendFriendRequest(email: String) = viewModelScope.launch(Dispatchers.IO){
        val response = userRepository.sendFriendRequest(email)
        if(response.isSuccessful && response.body() != null){
            friendRequestStatus.postValue(response.body())
        } else if(!response.isSuccessful){
            Log.e(PROFILE_VM_TAG, "Friend request failed. error: $response , ${response.errorBody()}")
            friendRequestStatus.postValue((response.body() ?: "").toString())
        }
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
    fun getAllGameHistories(email : String) = viewModelScope.launch(Dispatchers.IO) {
        val response = userRepository.getAllGameHistoriesByEmail(email)
        if (!response.isSuccessful || response.body() == null) {
            Log.e(PROFILE_VM_TAG, "Couldn't fetch all game histories. response is: $response and error: ${response.errorBody()}")
        }
        if(response.isSuccessful){
            for(gameHistory in response.body()!!){
                if(userRepository.getFavoriteGameHistoryById(gameHistoryId = gameHistory.id) != null){
                    gameHistory.isSaved = true
                }
            }
            gameHistoryList.postValue(response.body())
        } else{
            Log.e("TAG", "Couldn't get game history. response is: $response and body: ${response.body()}")
        }
    }


    fun getFriendsList(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.getFriends(email)
            if (response.isSuccessful) {
                val tempFriendList : MutableList<Friend> = mutableListOf()
                for (user in response.body()!!) {
                    val friend = Friend(user.image, user.email, user.name)
                    tempFriendList.add(friend)
                }
                friendsList.postValue(tempFriendList)
            } else {
                Log.e("TAG", "Couldn't get friends list")
            }
        }
    }



}