package com.hellostranger.chess_app.database

import com.hellostranger.chess_app.database.GameHistoryDao
import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.network.retrofit.backend.BackendApiService
import com.hellostranger.chess_app.utils.TokenManager

class UserRepository(private val  retrofitService : BackendApiService, val tokenManager: TokenManager, private val gameHistoryDao : GameHistoryDao) {
    suspend fun getAllGameHistories() = retrofitService.getGamesHistory(tokenManager.getUserEmail())

    suspend fun getAllGameHistoriesByEmail(email : String) = retrofitService.getGamesHistory(email)

    suspend fun upsertFavoriteGameHistory(gameHistory: GameHistory) = gameHistoryDao.upsertGameHistory(gameHistory)

    suspend fun removeGameHistoryFromFavorites(gameHistory : GameHistory) = gameHistoryDao.deleteGameHistory(gameHistory.localId)

    fun getFavoriteGameHistories() = gameHistoryDao.getGames()
    fun getFavoriteGameHistoryById(gameHistoryId: Int) = gameHistoryDao.getSpecificGame(gameHistoryId)

    suspend fun getUser() = retrofitService.getUserByEmail(tokenManager.getUserEmail())

    suspend fun getUserByEmail(email : String) = retrofitService.getUserByEmail(email)

    suspend fun sendFriendRequest(recipientEmail : String) = retrofitService.sendFriendRequest(tokenManager.getUserEmail(), recipientEmail)
    suspend fun getFriendRequest() = retrofitService.getFriendRequests(tokenManager.getUserEmail())

    suspend fun acceptFriendRequest(requestId : Int) = retrofitService.acceptFriendRequest(tokenManager.getUserEmail(), requestId)

    suspend fun getFriends() = retrofitService.getFriends(tokenManager.getUserEmail())

    suspend fun deleteFriend(friendEmail : String) = retrofitService.deleteFriend(tokenManager.getUserEmail(), friendEmail)

}