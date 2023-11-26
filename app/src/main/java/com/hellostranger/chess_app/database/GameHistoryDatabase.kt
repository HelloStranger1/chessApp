package com.hellostranger.chess_app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hellostranger.chess_app.models.entites.GameHistory

@Database(
    entities = [GameHistory::class],
    version = 1,
    exportSchema = false
)
/*@TypeConverters(Converters::class)*/
abstract class GameHistoryDatabase : RoomDatabase() {
    abstract val dao : GameHistoryDao
}