package com.idivisiontech.transporttracker.Api.osrm

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OsrmApiRepository {

    companion object{
        private const val BASE_URL = "http://103.226.49.73:13432/"
        private var INSTANCE: OsrmApiRepository? = null
        fun getInstance(): OsrmApiRepository{
            if(INSTANCE == null){
                INSTANCE = OsrmApiRepository()
            }

            return INSTANCE as OsrmApiRepository
        }
    }

    private var retrofit: Retrofit
    var services: OsrmApiInterface
    private var client: OkHttpClient

    init {
        client = OkHttpClient().newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .build()

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(client)
                .build()

        services = retrofit.create(OsrmApiInterface::class.java);
    }
}