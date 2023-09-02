package com.hellostranger.chess_app

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellostranger.chess_app.models.entites.GameHistory
import com.hellostranger.chess_app.models.entites.User
import com.hellostranger.chess_app.database.GameHistoryDao
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.notify

class ProfileViewModel(
    private val gameHistoryRepository: GameHistoryRepository,
    private val gameHistoryDao: GameHistoryDao,
    private val userRepository: UserRepository
) : ViewModel() {
    val gameHistoryList = MutableLiveData<List<GameHistory>>()

    var favoriteGamesList = MutableLiveData<List<GameHistory>>()
    val userDetails = MutableLiveData<User>()

    val failed = MutableLiveData<String>()


    fun getUserDetails(){
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable -> throwable.printStackTrace() }
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            val response = userRepository.getUser()
            if(response.isSuccessful){
                userDetails.postValue(response.body())
            } else{
                Log.e("TAG", "Couldn't load user, response is: $response and body: ${response.body()}")
                failed.postValue(response.body().toString())
            }
        }
    }

    fun onEvent(event: GameHistoryEvent){
        when(event){
            is GameHistoryEvent.OpenGame ->{
                //TODO: When user wants to open a game, convert the FEN strings to a Game object and launch an activity with it
            }
            is GameHistoryEvent.SaveGame ->{
                event.gameHistory.isSaved = true
                val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable -> throwable.printStackTrace() }
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    gameHistoryDao.upsertGameHistory(event.gameHistory)
                }

            }

            is GameHistoryEvent.DeleteGame -> {
                event.gameHistory.isSaved = false
                val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable -> throwable.printStackTrace() }
                CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch{
                    gameHistoryDao.deleteGameHistory(event.gameHistory)
                }
            }
        }
    }

    fun getAllGameHistories(){
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable -> throwable.printStackTrace() }
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            val response = gameHistoryRepository.getAllGameHistories()
            if(response.isSuccessful){
                for(gameHistory in response.body()!!){
                    if(gameHistoryDao.getSpecificGame(gameHistoryId = gameHistory.id) != null){
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
        favoriteGamesList = gameHistoryDao.getGames() as MutableLiveData<List<GameHistory>>
    }


}