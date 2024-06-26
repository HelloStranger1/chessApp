package com.hellostranger.chess_app.database

import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.network.retrofit.backend.BackendApiService
import com.hellostranger.chess_app.utils.TokenManager

@ExperimentalUnsignedTypes
class UserRepository(private val  retrofitService : BackendApiService, val tokenManager: TokenManager, private val gameHistoryDao : GameHistoryDao) {

    suspend fun getAllGameHistoriesByEmail(email : String) = retrofitService.getGamesHistory(email)

    suspend fun upsertFavoriteGameHistory(gameHistory: GameHistory) = gameHistoryDao.upsertGameHistory(gameHistory)

    suspend fun removeGameHistoryFromFavorites(gameHistory : GameHistory) = gameHistoryDao.deleteGameHistory(gameHistory.id)

    fun getFavoriteGameHistoryById(gameHistoryId: Int) = gameHistoryDao.getSpecificGame(gameHistoryId)
    suspend fun getSavedGameHistories() = gameHistoryDao.getGames()


    suspend fun getUserByEmail(email : String) = retrofitService.getUserByEmail(email)

    suspend fun sendFriendRequest(recipientEmail : String) = retrofitService.sendFriendRequest(tokenManager.getUserEmail(), recipientEmail)


    suspend fun getFriends(email : String) = retrofitService.getFriends(email)

    suspend fun deleteFriend(friendEmail : String) = retrofitService.deleteFriend(tokenManager.getUserEmail(), friendEmail)

}