package com.hellostranger.chess_app.network.retrofit.general

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.models.gameModels.pieces.Piece
import com.hellostranger.chess_app.models.gameModels.pieces.PieceJsonDeserializer
import com.hellostranger.chess_app.network.retrofit.AuthInterceptor
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object PuzzleRetrofitClient {
    private const val BASE_URL = "https://chess-puzzles2.p.rapidapi.com"

    private var gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    val instance: PuzzleInterface by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .build()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(PuzzleRetrofitClient.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(PuzzleRetrofitClient.gson))

        val retrofit = retrofitBuilder.build()

        retrofit.create(PuzzleInterface::class.java)

    }
}