package com.hellostranger.chess_app.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hellostranger.chess_app.models.rvEntities.GameHistory

@Dao
interface GameHistoryDao {

    @Upsert
    suspend fun upsertGameHistory(gameHistory: GameHistory)

    @Query("DELETE FROM GameHistory WHERE id = :gameHistoryId")
    suspend fun deleteGameHistory(gameHistoryId: Int)

    @Query("SELECT * FROM GameHistory ORDER BY gameDate ASC")
    fun getGames() : LiveData<List<GameHistory>>

    @Query("SELECT * FROM GameHistory WHERE id = :gameHistoryId")
    fun getSpecificGame(gameHistoryId: Int) : GameHistory?
}