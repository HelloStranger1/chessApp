package com.hellostranger.chess_app.viewModels.factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.hellostranger.chess_app.database.UserRepository
import com.hellostranger.chess_app.viewModels.ProfileViewModel

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass : Class<T>, extras: CreationExtras) : T{
        return if(modelClass.isAssignableFrom(ProfileViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            ProfileViewModel(this.userRepository) as T
        } else{
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}