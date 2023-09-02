package com.hellostranger.chess_app.network.retrofit.general

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.network.retrofit.AuthInterceptor
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object GeneralRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080"

    private var gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    val instance: ApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(AuthRetrofitClient.instance))
            .build()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
        val retrofit = retrofitBuilder.build()

        retrofit.create(ApiService::class.java)

    }
}