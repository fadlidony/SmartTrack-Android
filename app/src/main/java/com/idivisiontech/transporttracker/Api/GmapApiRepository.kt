package com.idivisiontech.transporttracker.Api

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GmapApiRepository {

    companion object{
        private const val BASE_URL = "https://maps.googleapis.com/"
        private const val API_KEY = "AIzaSyDCBuygErC6XkGjwP5HABQaBtPKKhnjREY"
    }

    private lateinit var retrofit: Retrofit
    lateinit var services: GmapApiInterface
    private lateinit var client: OkHttpClient

    init {
        client = OkHttpClient().newBuilder()
                .addInterceptor{ chain ->
                    val original  = chain.request()
                    val urlOriginal = original.url()

                    val url = urlOriginal.newBuilder()
                            .addQueryParameter("key", API_KEY)
                            .build()

                    val request = original.newBuilder()
                            .url(url)
                            .build()

                    chain.proceed(request)
                }
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .build()

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(client)
                .build()

        services = retrofit.create(GmapApiInterface::class.java);

    }
}