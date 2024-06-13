package com.hellostranger.chess_app.network.retrofit.auth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AuthRetrofitClient {
    private const val BASE_URL = "http://ee1b-185-108-80-158.ngrok-free.app/"

    private var gson: Gson = Gson()
    val instance : AuthApiService by lazy {

        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
        val retrofit = retrofitBuilder.build()

        retrofit.create(AuthApiService::class.java)

    }
}