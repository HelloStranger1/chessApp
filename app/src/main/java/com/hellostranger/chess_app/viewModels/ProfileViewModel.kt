package com.hellostranger.chess_app.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.rv.GameHistoryEvent
import com.hellostranger.chess_app.database.UserRepository
import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.models.entities.User
import com.hellostranger.chess_app.models.rvEntities.Friend
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    val gameHistoryList = MutableLiveData<List<GameHistory>>()
    val friendsList = MutableLiveData<List<Friend>>()
    var favoriteGamesList = MutableLiveData<List<GameHistory>>()
    val userDetails = MutableLiveData<User>()
    val isFriendsWithUser = MutableLiveData(false)
    val friendRequestStatus = MutableLiveData("")


    val failed = MutableLiveData<String>()

    private val coroutineExceptionHandler = CoroutineExceptionHandler{ _, throwable -> throwable.printStackTrace() }

    fun getUserDetails(email : String){

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response = userRepository.getUserByEmail(email)
            if(response.isSuccessful){
                userDetails.postValue(response.body())
            } else{
                Log.e("TAG", "Couldn't load user, response is: $response and body: ${response.body()}")
                failed.postValue(response.body().toString())
            }
        }
    }

    fun areFriendsWithUser(friendEmail : String)  {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler){
            val response = userRepository.getFriends(userRepository.tokenManager.getUserEmail())
            if(response.isSuccessful && response.body() != null){
                val userFriends : List<User> = response.body()!!
                for(friend : User in userFriends){
                    if(friend.email == friendEmail){
                        isFriendsWithUser.postValue(true)
                        return@launch
                    }
                }
                isFriendsWithUser.postValue(false)
            } else if(response.isSuccessful){
                Log.e("TAG", "Guess you just don't have any friends lol")
            } else{
                Log.e("TAG", "Couldn't get friends-details. response: $response, body: ${response.body()}")
                failed.postValue(response.body().toString())
            }
        }
    }
    fun sendFriendRequest(email: String) = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler){
        val response = userRepository.sendFriendRequest(email)
        if(response.isSuccessful && response.body() != null){
            friendRequestStatus.postValue(response.body())
        } else if(!response.isSuccessful){
            Log.e("TAG", "Friend request failed. error: $response , ${response.body()}")
            friendRequestStatus.postValue((response.body() ?: "").toString())
        }
    }

    fun removeFriend(email : String) = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler){
        val response = userRepository.deleteFriend(email)
        Log.e("TAG", "response from remove friend was: $response, ${response.body()}")
        areFriendsWithUser(email)
    }
    fun onEvent(event: GameHistoryEvent){
        when(event){
            is GameHistoryEvent.OpenGame ->{
                //TODO: When user wants to open a game, convert the FEN strings to a Game object and launch an activity with it
            }
            is GameHistoryEvent.SaveGame ->{
                event.gameHistory.isSaved = true
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    userRepository.upsertFavoriteGameHistory(event.gameHistory)
                }

            }

            is GameHistoryEvent.DeleteGame -> {
                event.gameHistory.isSaved = false
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    userRepository.removeGameHistoryFromFavorites(event.gameHistory)
                }
            }
        }
    }

    fun getAllGameHistories(email : String){
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response = userRepository.getAllGameHistoriesByEmail(email)
            if(response.isSuccessful){
                for(gameHistory in response.body()!!){
                    if(userRepository.getFavoriteGameHistoryById(gameHistoryId = gameHistory.id) != null){
                        gameHistory.isSaved = true
                    }
                }
                gameHistoryList.postValue(response.body())
            } else{
                Log.e("TAG", "Couldn't get game history. response is: $response and body: ${response.body()}")
                failed.postValue(response.body().toString())
            }
        }
    }

    fun getFriendsList(email: String) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
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
                failed.postValue(response.body().toString())
            }
        }
    }

    fun getFavoriteGameHistories(){
        favoriteGamesList = userRepository.getFavoriteGameHistories() as MutableLiveData<List<GameHistory>>
    }


}