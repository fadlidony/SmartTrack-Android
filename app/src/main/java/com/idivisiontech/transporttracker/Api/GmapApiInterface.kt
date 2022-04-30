package com.idivisiontech.transporttracker.Api

import com.idivisiontech.transporttracker.Data.Gmap.MatrixDistance
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GmapApiInterface {

    /**
     *     https://maps.googleapis.com/maps/api/distancematrix/json
     *     ?units=meters&origins=-6.9718029,110.3993041&destinations=-7.0487406,110.3875193&key=AIzaSyDCBuygErC6XkGjwP5HABQaBtPKKhnjREY
     */

    @GET("maps/api/distancematrix/json")
    fun distanceMatrix(@Query("units") units: String,
                        @Query("origins") origins: String,
                        @Query("destinations") destinations: String,
                        @Query("mode") mode: String
                       ) : Call<MatrixDistance>
}