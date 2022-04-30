package com.idivisiontech.transporttracker.licenseServer

import com.idivisiontech.transporttracker.licenseServer.data.UpdateResult
import com.idivisiontech.transporttracker.licenseServer.data.ValidityResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface LicenseServerServices {
    @GET("updates")
    fun getUpdates() : Call<UpdateResult>

    @GET("validity/{android_id}")
    fun getValidityStatus(@Path("android_id") android_id: String) : Call<ValidityResponse>

}