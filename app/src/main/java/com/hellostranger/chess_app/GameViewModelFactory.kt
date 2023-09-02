package com.hellostranger.chess_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hellostranger.chess_app.models.gameModels.Game

class GameViewModelFactory(
    private val game: Game
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if(modelClass.isAssignableFrom(GameViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            GameViewModel(this.game) as T
        } else{
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}