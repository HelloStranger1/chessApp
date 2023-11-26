package com.hellostranger.chess_app.network.retrofit.general

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.models.gameModels.pieces.Piece
import com.hellostranger.chess_app.models.gameModels.pieces.PieceJsonDeserializer
import com.hellostranger.chess_app.network.retrofit.AuthInterceptor
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object GeneralRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080"

    private var gson: Gson = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(Piece::class.java, PieceJsonDeserializer())
        .create()
    val instance: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(AuthRetrofitClient.instance))
            .build()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))

        val retrofit = retrofitBuilder.build()

        retrofit.create(ApiService::class.java)

    }
}