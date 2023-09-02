package com.hellostranger.chess_app

import com.hellostranger.chess_app.network.retrofit.general.ApiService
import com.hellostranger.chess_app.utils.TokenManager

class GameHistoryRepository(private val  retrofitService : ApiService, val tokenManager: TokenManager) {
    suspend fun getAllGameHistories() = retrofitService.getGamesHistory(tokenManager.getUserEmail())
}