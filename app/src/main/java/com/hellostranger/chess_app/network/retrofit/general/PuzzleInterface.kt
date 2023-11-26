package com.hellostranger.chess_app.network.retrofit.general

import com.hellostranger.chess_app.dto.Puzzle
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface PuzzleInterface {

    @Headers(
        "X-RapidAPI-Key: 91a821c889mshbb9b9f0094e1b54p1a3638jsnec35711e33f0",
        "X-RapidAPI-Host: chess-puzzles2.p.rapidapi.com"
    )
    @GET("/random")
    suspend fun getRandomPuzzle(@Query("number_of_puzzles") numberOfPuzzles : Int) : Response<List<Puzzle>>
}