package com.idivisiontech.transporttracker.RaspberryClient

import android.util.Log
import com.idivisiontech.transporttracker.ServerOperator.ServerApiInterface
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RaspberryRepository() {

    private var raspiIp:String? = null
    private var url:String = ""
    private lateinit var client: OkHttpClient
    private lateinit var retrofit: Retrofit
    lateinit var services: RaspberryInterface

    companion object {
        const val IS_LOCAL = true
        const val TYPE_ARRIVING = "arriving_announcement"
        const val TYPE_GOING_TO = "going_to_announcement"
        private var INSTANCE:RaspberryRepository? = null
        fun getInstance(): RaspberryRepository{
            if(INSTANCE == null){
                val rp = RaspberryRepository()
                if(IS_LOCAL){
                    rp.setIp("192.168.1.254")
                }else{
                    rp.setIp("103.226.49.73")
                }

                INSTANCE = rp
            }

            return INSTANCE as RaspberryRepository
        }

        fun getInstance(ip: String) : RaspberryRepository{
            if(INSTANCE == null){
                val rp = RaspberryRepository()
                rp.setIp(ip)
                INSTANCE = rp
            }
            return INSTANCE as RaspberryRepository
        }

        const val RUNNING_TEXT_INDOOR_TYPE = "indoor"
        const val RUNNING_TEXT_FRONT_TYPE = "front"
        const val RUNNING_TEXT_BACK_TYPE = "back"



    }


    fun setIp(ip: String){
        raspiIp = ip
        updateUrl()
        createClient()
    }

    private fun createClient() {
        client = OkHttpClient().newBuilder()
                .addInterceptor{ chain ->
                    val original  = chain.request()

                    var request = original.newBuilder().url(original.url()).build()


                    chain.proceed(request)
                }
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(this.url)
                .client(client)
                .build()

        services = retrofit.create(RaspberryInterface::class.java)
    }


    private fun updateUrl(){
        var port = if(IS_LOCAL){
            "5000"
        }else{
            "12001"
        }
        url = "http://$raspiIp:$port"
        Log.i("rapi-repo", url)
    }


}