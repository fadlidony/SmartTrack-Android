package com.idivisiontech.transporttracker.RaspberryClient

import com.idivisiontech.transporttracker.RaspberryClient.data.*
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RaspberryInterface {
    @GET("/")
    fun index() : Call<IndexResult>


    @GET("/audio/play")
    fun playAudio(
            @Query("rute_id") rute_id: Int,
            @Query("halte_id") halte_id: Int,
            @Query("type") type: String
    ) : Call<PlayResult>

    @GET("running-text/update")
    fun updateRunningText(
            @Query("tipe") type: String,
            @Query("text") text: String,
            @Query("kode_rute") kode_rute: String
    ) : Call<RunningTextUpdateResult>

    @GET("/voiceover/on")
    fun voiceOverOn() : Call<IndexResult>

    @GET("/voiceover/off")
    fun voiceOverOff() : Call<IndexResult>

    @GET("/humidity")
    fun getHumidity() : Call<HumidityResponse>

    @GET("delete-audio-config")
    fun deleteAudioConfig(
        @Query("id") rute_id: Int
    ) : Call<PlayResult>

    @GET("/metadata")
    fun getMetaData() : Call<MetaDataResponse>

}