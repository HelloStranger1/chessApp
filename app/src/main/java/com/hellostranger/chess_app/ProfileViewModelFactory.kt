package com.hellostranger.chess_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.hellostranger.database.GameHistoryDao

class ProfileViewModelFactory(
    private val gameHistoryRepository: GameHistoryRepository,
    private val userRepository: UserRepository,
    private val gameHistoryDao: GameHistoryDao
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass : Class<T>, extras: CreationExtras) : T{
        return if(modelClass.isAssignableFrom(ProfileViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            ProfileViewModel(this.gameHistoryRepository, this.gameHistoryDao, this.userRepository) as T
        } else{
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}