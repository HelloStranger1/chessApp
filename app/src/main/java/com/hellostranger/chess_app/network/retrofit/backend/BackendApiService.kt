package com.hellostranger.chess_app.network.retrofit.backend

import com.hellostranger.chess_app.dto.requests.FriendRequest
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.dto.requests.JoinRequest
import com.hellostranger.chess_app.dto.requests.UpdateRequest
import com.hellostranger.chess_app.models.rvEntities.GameHistory
import com.hellostranger.chess_app.models.entities.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApiService {

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

    @POST("/api/users/{userEmail}/send-friend-request")
    suspend fun sendFriendRequest(@Path("userEmail") userEmail: String, @Query("recipientEmail") recipientEmail : String) : Response<String>

    @GET("/api/users/{userEmail}/get-friend-requests")
    suspend fun getFriendRequests(@Path("userEmail") userEmail: String) : Response<List<FriendRequest>>

    @POST("/api/users/{userEmail}/accept-friend-request")
    suspend fun acceptFriendRequest(@Path("userEmail") userEmail: String, @Query("requestId") requestId : Int) : Response<String>

    @POST("/api/users/{userEmail}/reject-friend-request")
    suspend fun rejectFriendRequest(@Path("userEmail") userEmail: String, @Query("requestId") requestId : Int) : Response<String>

    @GET("/api/users/get-friends/{userEmail}")
    suspend fun getFriends(@Path("userEmail") userEmail: String) : Response<List<User>>

    @POST("/api/users/remove-friend/{userEmail}")
    suspend fun deleteFriend(@Path("userEmail") userEmail : String, @Query("friendEmail") friendEmail : String) : Response<String>




}