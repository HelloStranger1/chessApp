package com.hellostranger.chess_app.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.hellostranger.chess_app.models.entites.GameHistory

@Dao
interface GameHistoryDao {

    @Upsert
    suspend fun upsertGameHistory(gameHistory: GameHistory)

    @Delete
    suspend fun deleteGameHistory(gameHistory: GameHistory)

    @Query("SELECT * FROM GameHistory ORDER BY gameDate ASC")
    fun getGames() : LiveData<List<GameHistory>>

    @Query("SELECT * FROM GameHistory WHERE id = :gameHistoryId")
    fun getSpecificGame(gameHistoryId: Int) : GameHistory?
}