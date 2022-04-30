package com.idivisiontech.transporttracker.ServerOperator

import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.BusInfoResult
import com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallEnd.CallEndResult
import com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallReq.CallReqResult
import com.idivisiontech.transporttracker.ServerOperator.Data.LoginResult
import com.idivisiontech.transporttracker.ServerOperator.Data.LogoutResult
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile
import com.idivisiontech.transporttracker.ServerOperator.Data.QrCode.QrCodeResult
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.RouteResult
import com.idivisiontech.transporttracker.ServerOperator.Data.maintenance.SensorResponse
import com.idivisiontech.transporttracker.ServerOperator.Data.panicSignal.PanicSignalResult
import com.idivisiontech.transporttracker.ServerOperator.Data.settingInfo.SettingsResult
import com.idivisiontech.transporttracker.ServerOperator.Data.speedLog.SpeedLogResult
import com.idivisiontech.transporttracker.ServerOperator.Data.updateLokasiResponse.UpdateLokasiResponse
import retrofit2.Call
import retrofit2.http.*

interface ServerApiInterface {

    @POST("driver/login")
    @FormUrlEncoded
    fun login(@Field("rfid") rfid_token: String, @Field("android") android: String) : Call<LoginResult>

    @GET("driver/logout")
    fun logout() : Call<LogoutResult>

    @GET("driver/profile")
    fun profile(): Call<Profile>

    @GET("driver/route")
    fun rute() : Call<RouteResult>

    @GET("driver/bus")
    fun busInfo(): Call<BusInfoResult>

    @POST("driver/panic")
    @FormUrlEncoded
    fun sendPanicSignal(@Field("geolocation") geolocation: String) : Call<PanicSignalResult>

    @GET("driver/settings")
    fun settings() : Call<SettingsResult>

    @POST("driver/callend")
    @FormUrlEncoded
    fun callEnd(@Field("id") id: Int) : Call<CallEndResult>

    @POST("driver/callreq")
    fun callReq() : Call<CallReqResult>

    @POST("driver/speed-log")
    @FormUrlEncoded
    fun speedLog(@Field("speed") speed: Float) : Call<SpeedLogResult>

    @GET("driver/qr-code/{device_id}")
    fun getImageQr(@Path("device_id") device_id: String) : Call<QrCodeResult>

    @GET("driver/maintenance")
    fun getMaintenance(): Call<SensorResponse>

    @GET("driver/maintenance")
    fun getMaintenanceAndUpdateOdometer(@Query("odometer") odometer: Int) : Call<SensorResponse>

    @POST("driver/update-lokasi")
    @FormUrlEncoded
    fun updateLokasi(@Field("speed") speed: Float,
                     @Field("lat") lat: String,
                     @Field("long") long: String,
                     @Field("compass") compass: String) : Call<UpdateLokasiResponse>

    @POST("driver/update-meta-data")
    @FormUrlEncoded
    fun updateMetadata(
            @Field("speed") speed: Float,
            @Field("longitude") longitude: String,
            @Field("latitude") latitude: String,
            @Field("compass") compass: String,
            @Field("temperature") temperature: String,
            @Field("humidity") humidity: String,
            @Field("vibration_direction_x") vibration_direction_x: String,
            @Field("vibration_direction_y") vibration_direction_y: String,
            @Field("vibration_direction_z") vibration_direction_z: String,
            @Field("vibrationforce") vibrationforce: String,
            @Field("ecukilometertravel") ecukilometertravel: String,
            @Field("ecufuelusage") ecufuelusage: String,
            @Field("ecuspeed") ecuspeed: String,
            @Field("voltage") voltage: String
    ) : Call<UpdateLokasiResponse>

}