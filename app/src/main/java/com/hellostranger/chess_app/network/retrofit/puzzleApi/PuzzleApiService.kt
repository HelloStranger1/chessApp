package com.hellostranger.chess_app.network.retrofit.puzzleApi

import com.hellostranger.chess_app.BuildConfig
import com.hellostranger.chess_app.models.entities.Puzzle
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface PuzzleApiService {

    @Headers(
        "X-RapidAPI-Key: ${BuildConfig.PUZZLE_API_KEY}",
        "X-RapidAPI-Host: chess-puzzles2.p.rapidapi.com"
    )
    @GET("/random")
    suspend fun getRandomPuzzle(@Query("number_of_puzzles") numberOfPuzzles : Int) : Response<List<Puzzle>>
}