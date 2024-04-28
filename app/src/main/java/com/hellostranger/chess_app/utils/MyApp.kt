package com.hellostranger.chess_app.utils

import android.app.Application
import androidx.room.Room
import com.hellostranger.chess_app.database.GameHistoryDatabase

class MyApp : Application() {
    enum class PieceTheme {
        DEFAULT,
        PLANT,
    }
    companion object{
        lateinit var tokenManager: TokenManager
        lateinit var favoriteGameDB : GameHistoryDatabase
        var pieceTheme : PieceTheme = PieceTheme.DEFAULT
    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        favoriteGameDB = Room.databaseBuilder(
                applicationContext, GameHistoryDatabase::class.java, Constants.FAVORITE_GAMES_DB
        ).fallbackToDestructiveMigration().build()

    }
}