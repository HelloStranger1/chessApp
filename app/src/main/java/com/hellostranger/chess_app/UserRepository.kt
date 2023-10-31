package com.hellostranger.chess_app

import com.hellostranger.chess_app.network.retrofit.general.ApiService
import com.hellostranger.chess_app.utils.TokenManager

class UserRepository(private val  retrofitService : ApiService, val tokenManager: TokenManager) {
    suspend fun getUser() = retrofitService.getUserByEmail(tokenManager.getUserEmail())

    suspend fun getUserByEmail(email : String) = retrofitService.getUserByEmail(email)


}

