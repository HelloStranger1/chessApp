package com.hellostranger.chess_app.retrofit

import com.hellostranger.chess_app.chess_models.Game
import com.hellostranger.chess_app.dto.JoinRequest
import com.hellostranger.chess_app.chess_models.Player
import com.hellostranger.chess_app.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("/games/create")
    suspend fun createGame() : Response<Game>

    @POST("/games/join")
    suspend fun joinGame(@Body request: JoinRequest) : Response<Game>

    @POST("/games/join/random")
    suspend fun joinRandomGame(@Body request: Player) : Response<Game>

    /*@POST("/player/register")
    suspend fun registerPlayer(@Body request : RegisterRequest) : Response<String>

    @GET("/player/name/{firebaseUuid}")
    suspend fun getPlayerName(@Path("firebaseUuid") firebaseUuid : String) : Response<String>

    @GET("/player/email/{firebaseUuid}")
    suspend fun getPlayerEmail(@Path("firebaseUuid") firebaseUuid : String) : Response<String>

    @GET("/player/elo/{firebaseUuid}")
    suspend fun getPlayerElo(@Path("firebaseUuid") firebaseUuid : String) : Response<Long>

    @DELETE("/player/{firebaseUuid}")
    suspend fun deletePlayer(@Path("firebaseUuid") firebaseUuid : String) : Response<String>*/

    /*@GET("/game/{gameId}/playerOne/name")
    suspend fun getPlayerOneName(@Path("gameId") gameId : String) : Response<String>

    @GET("/game/{gameId}/playerOne/uid")
    suspend fun getPlayerOneUid(@Path("gameId") gameId : String) : Response<String>

    @GET("/game/{gameId}/playerTwo/name")
    suspend fun getPlayerTwoName(@Path("gameId") gameId : String) : Response<String>

    @GET("/game/{gameId}/playerTwo/uid")
    suspend fun getPlayerTwoUid(@Path("gameId") gameId : String) : Response<String>*/

}