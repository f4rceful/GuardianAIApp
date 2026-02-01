package com.example.guardianai.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 is the special alias to your host loopback interface (localhost) from Android Emulator
    private const val BASE_URL = "http://10.0.2.2:8550/"

    val instance: GuardianApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(GuardianApiService::class.java)
    }
}
