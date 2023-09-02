package com.hellostranger.chess_app.network.retrofit.general

import com.hellostranger.chess_app.models.gameModels.Game
import com.hellostranger.chess_app.dto.JoinRequest
import com.hellostranger.chess_app.dto.UpdateRequest
import com.hellostranger.chess_app.models.entites.GameHistory
import com.hellostranger.chess_app.models.entites.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("/api/games/join/{gameId}")
    suspend fun joinGame(@Path("gameId") gameId : String, @Body request: JoinRequest) : Response<Game>

    @POST("/api/games/join/random")
    suspend fun joinRandomGame(@Body request: JoinRequest) : Response<Game>

    @GET("/api/users/{userEmail}")
    suspend fun getUserByEmail(@Path("userEmail") userEmail : String) : Response<User>

    @PUT("/api/users/upload-image-URL/{userEmail}")
    suspend fun uploadProfileImage(@Path("userEmail") userEmail: String, @Body updateRequest: UpdateRequest) : Response<String>

    @PUT("/api/users/update-user-name/{userEmail}")
    suspend fun updateUserName(@Path("userEmail") userEmail: String, @Body updateRequest: UpdateRequest) : Response<String>

    @GET("/api/users/games-history/{userEmail}")
    suspend fun getGamesHistory(@Path("userEmail") userEmail: String) : Response<List<GameHistory>>


}