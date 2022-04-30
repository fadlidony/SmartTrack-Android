package com.idivisiontech.transporttracker.ServerOperator

import android.content.Context
import com.idivisiontech.transporttracker.Api.GmapApiRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServerApiRepository(context:Context) {

    private lateinit var retrofit: Retrofit
    lateinit var services: ServerApiInterface
    private lateinit var client: OkHttpClient


    companion object{
        private const val BASE_URL = "http://smarttrack.knd.co.id/api/"
        //private const val BASE_URL = "http://192.168.1.4:8092/api/"
    }

    init {
        createClient(null)
    }

    fun updateSessionKey(session_key: String){
        createClient(session_key)
    }


    private fun createClient(session_key: String?){
        client = OkHttpClient().newBuilder()
                .addInterceptor{ chain ->
                    val original  = chain.request()

                    var request: Request

                    if(session_key == null){
                        request = original.newBuilder().url(original.url()).build()
                    }else{
                        request = original.newBuilder().url(original.url())
                                .addHeader("Authorization","Session " + session_key)
                                .build()
                    }

                    chain.proceed(request)
/*                    val original  = chain.request()

                    val request : Request
                    if(session_key == null){
                        request = original.newBuilder()
                                .url(original.url())
                                .build()
                    }else{
                        request = original.newBuilder()
                                .url(original.url())
                                .addHeader("Authorization","Session " + session_key)
                                .build()
                    }

                    chain.proceed(request)*/
                }
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(client)
                .build()

        services = retrofit.create(ServerApiInterface::class.java)
    }



}