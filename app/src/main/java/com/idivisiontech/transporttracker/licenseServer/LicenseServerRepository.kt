package com.idivisiontech.transporttracker.licenseServer


import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LicenseServerRepository{

    private val KEY = "da35a439-7a38-49cb-95d4-6fb90f583e3f"

    private var client = OkHttpClient().newBuilder()
            .addInterceptor {
                var request = it.request()
                var newRequest = request.newBuilder()
                        .addHeader("paket", KEY)
                        .build()
                it.proceed(newRequest)
            }
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    private val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("http://103.226.49.73:13437/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val services: LicenseServerServices = retrofit.create(LicenseServerServices::class.java)

    companion object{
        private var INSTANCE:LicenseServerRepository? = null
        fun getInstance() : LicenseServerRepository {
            if(INSTANCE == null){
                INSTANCE = LicenseServerRepository()
            }
            return INSTANCE as LicenseServerRepository
        }
    }
}