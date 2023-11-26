package com.hellostranger.chess_app.network.retrofit.auth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.gameClasses.pieces.Piece
import com.hellostranger.chess_app.gameClasses.pieces.PieceJsonDeserializer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AuthRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080"

    private var gson: Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Piece::class.java, PieceJsonDeserializer())
        .create()
    val instance : AuthApiService by lazy {

        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
        val retrofit = retrofitBuilder.build()

        retrofit.create(AuthApiService::class.java)

    }
}