package com.hellostranger.chess_app.network.retrofit.puzzleApi

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object PuzzleRetrofitClient {
    private const val BASE_URL = "https://chess-puzzles2.p.rapidapi.com"

    private var gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    val instance: PuzzleApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .build()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))

        val retrofit = retrofitBuilder.build()

        retrofit.create(PuzzleApiService::class.java)

    }
}