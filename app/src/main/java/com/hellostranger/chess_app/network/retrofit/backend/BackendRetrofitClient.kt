package com.hellostranger.chess_app.network.retrofit.backend

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hellostranger.chess_app.network.retrofit.auth.AuthInterceptor
import com.hellostranger.chess_app.network.retrofit.auth.AuthRetrofitClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object BackendRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080"

    private var gson: Gson = Gson()
    val instance: BackendApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(AuthRetrofitClient.instance))
            .build()
        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))

        val retrofit = retrofitBuilder.build()

        retrofit.create(BackendApiService::class.java)

    }
}