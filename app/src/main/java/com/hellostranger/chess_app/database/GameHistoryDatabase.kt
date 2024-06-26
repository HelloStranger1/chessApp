package com.hellostranger.chess_app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hellostranger.chess_app.models.rvEntities.GameHistory

@Database(
    entities = [GameHistory::class],
    version = 1,
    exportSchema = false
)
abstract class GameHistoryDatabase : RoomDatabase() {
    abstract val dao : GameHistoryDao
}