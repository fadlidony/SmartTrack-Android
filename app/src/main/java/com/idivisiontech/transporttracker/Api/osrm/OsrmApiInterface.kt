package com.idivisiontech.transporttracker.Api.osrm

import com.idivisiontech.transporttracker.Api.osrm.data.DrivingRouteResult
import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface OsrmApiInterface {

    @GET("/route/v1/driving/{long_lat_asal};{long_lat_tujuan}")
    fun drivingRoute(@Path("long_lat_asal") longLatAsal: String, @Path("long_lat_tujuan") longLatTujuan: String) : Call<DrivingRouteResult>

    @GET("/route/v1/driving/{long_lat_asal};{long_lat_tujuan}")
    fun drivingRouteObservable(@Path("long_lat_asal") longLatAsal: String, @Path("long_lat_tujuan") longLatTujuan: String) : Deferred<DrivingRouteResult>

}