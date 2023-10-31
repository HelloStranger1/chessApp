package com.hellostranger.chess_app

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.models.entites.GameHistory
import com.hellostranger.chess_app.models.entites.User
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val gameHistoryRepository: GameHistoryRepository,
) : ViewModel() {
    val gameHistoryList = MutableLiveData<List<GameHistory>>()

    var favoriteGamesList = MutableLiveData<List<GameHistory>>()
    val userDetails = MutableLiveData<User>()
    var isFriendsWithUser = MutableLiveData(false)

    val failed = MutableLiveData<String>()

    private val coroutineExceptionHandler = CoroutineExceptionHandler{ _, throwable -> throwable.printStackTrace() }

    fun getUserDetails(email : String){

        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response = gameHistoryRepository.getUserByEmail(email)
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
            val response = gameHistoryRepository.getFriends()
            if(response.isSuccessful && response.body() != null){
                val userFriends : List<User> = response.body()!!
                for(friend : User in userFriends){
                    if(friend.email == friendEmail){
                        isFriendsWithUser.postValue(true)
                    }
                }
            } else if(response.isSuccessful){
                Log.e("TAG", "Guess you just don't have any friends lol")
            } else{
                Log.e("TAG", "Couldn't get friends-details. response: $response, body: ${response.body()}")
                failed.postValue(response.body().toString())
            }
        }
    }
    fun sendFriendRequest(email: String) = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler){ gameHistoryRepository.sendFriendRequest(email) }

    fun removeFriend(email : String) = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler){gameHistoryRepository.deleteFriend(email) }
    fun onEvent(event: GameHistoryEvent){
        when(event){
            is GameHistoryEvent.OpenGame ->{
                //TODO: When user wants to open a game, convert the FEN strings to a Game object and launch an activity with it
            }
            is GameHistoryEvent.SaveGame ->{
                event.gameHistory.isSaved = true
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    gameHistoryRepository.upsertFavoriteGameHistory(event.gameHistory)
                }

            }

            is GameHistoryEvent.DeleteGame -> {
                event.gameHistory.isSaved = false
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    gameHistoryRepository.removeGameHistoryFromFavorites(event.gameHistory)
                }
            }
        }
    }

    fun getAllGameHistories(email : String){
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val response = gameHistoryRepository.getAllGameHistoriesByEmail(email)
            if(response.isSuccessful){
                for(gameHistory in response.body()!!){
                    if(gameHistoryRepository.getFavoriteGameHistoryById(gameHistoryId = gameHistory.id) != null){
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

    fun getFavoriteGameHistories(){
        favoriteGamesList = gameHistoryRepository.getFavoriteGameHistories() as MutableLiveData<List<GameHistory>>
    }


}