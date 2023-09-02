package com.hellostranger.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hellostranger.chess_app.models.entites.GameHistory

@Database(
    entities = [GameHistory::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class GameHistoryDatabase : RoomDatabase() {
    abstract val dao : GameHistoryDao
}