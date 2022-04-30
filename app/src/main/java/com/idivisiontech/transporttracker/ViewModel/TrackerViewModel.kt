package com.idivisiontech.transporttracker.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.idivisiontech.transporttracker.Data.FirebaseData.PanicMoment
import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.Bus
import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.BusInfoResult
import com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallEnd.CallEndResult
import com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallReq.CallReqResult
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.Route
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.RouteResult
import com.idivisiontech.transporttracker.ServerOperator.Data.panicSignal.PanicSignalResult
import com.idivisiontech.transporttracker.ServerOperator.Data.settingInfo.SettingData
import com.idivisiontech.transporttracker.ServerOperator.Data.settingInfo.SettingsResult
import com.idivisiontech.transporttracker.ServerOperator.Data.speedLog.SpeedLogResult
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class TrackerViewModel() : ViewModel() {
    private val TAG = TrackerViewModel::class.java.simpleName
    private val m_profile = MutableLiveData<Profile>()
    private val m_route = MutableLiveData<Route>()
    private val m_busInfo = MutableLiveData<Bus>()
    private val m_settings = MutableLiveData<ArrayList<SettingData>>()
    val ruteName = MutableLiveData<String>()
    val currentSpeed = MutableLiveData<String>()
    val currentGeoLocation = MutableLiveData<String>()
    val callReq = MutableLiveData<CallReqResult>()
    val isSpeedLimitExceeded = MutableLiveData<Boolean>()
    val maxSpeed = MutableLiveData<Int>()

    //error MutableLiveData
    private val isErrorPanicSignalToServer = MutableLiveData<Boolean>()

    private val fbReferencePanicButton = FirebaseDatabase.getInstance().getReference("panic-button")
    private val fbReferenceSpeedLog = FirebaseDatabase.getInstance().getReference("speed-limit")
    private val fbReferenceSpeedLimiter = FirebaseDatabase.getInstance().getReference("settings/km_max_speed/value")

    val panicResult = MutableLiveData<PanicMoment>()

    init {
        ruteName.postValue(null)
        currentSpeed.postValue(null)
        currentGeoLocation.postValue(null)
        isSpeedLimitExceeded.postValue(false)
        maxSpeed.postValue(80)
    }

    fun sendCallReq(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.callReq().enqueue(object: Callback<CallReqResult>{
            override fun onFailure(call: Call<CallReqResult>, t: Throwable) {

            }

            override fun onResponse(call: Call<CallReqResult>, response: Response<CallReqResult>) {
                Log.d(TAG,response.body()?.message)
                callReq.postValue(response.body())
            }

        })
    }

    fun createSpeedLimitValueListener(){
        fbReferenceSpeedLimiter.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                if(p0?.value != null){
                    maxSpeed.postValue(p0?.value.toString().toInt())
                }
            }

        })
    }

    fun sendSpeedLimitLog(serverApiRepository: ServerApiRepository, speed: Float){
        serverApiRepository.services.speedLog(speed).enqueue(object : Callback<SpeedLogResult>{
            override fun onFailure(call: Call<SpeedLogResult>, t: Throwable) {
                Log.d(TAG,"sendSpeedLimitLog ERROR sendSpeed laravel ${t.message}")

            }

            override fun onResponse(call: Call<SpeedLogResult>, response: Response<SpeedLogResult>) {
                if(response.isSuccessful){
                    Log.d(TAG,"sendSpeedLimitLog sukses sendSpeed")
                    val id = fbReferenceSpeedLog.push().key.toString()

                    fbReferenceSpeedLog.child(id).setValue(response.body()?.speed_data).addOnCompleteListener{
                        Log.d(TAG,"sendSpeedLimitLog sukses firebase")
                    }

                    isSpeedLimitExceeded.postValue(false)
                    Log.d(TAG,"sendSpeedLimitLog sukses")
                }
            }

        })

    }

    fun endCallReq(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.callEnd(callReq.value?.data?.id as Int).enqueue(object : Callback<CallEndResult>{
            override fun onFailure(call: Call<CallEndResult>, t: Throwable) {

            }

            override fun onResponse(call: Call<CallEndResult>, response: Response<CallEndResult>) {
                if(response.isSuccessful){
                    callReq.postValue(null)
                }
            }

        })
    }

    fun updateProfile(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.profile().enqueue(object : Callback<Profile>{
            override fun onFailure(call: Call<Profile>, t: Throwable) {
                m_profile.postValue(null)
            }

            override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                if(response.code() == 200){
                    m_profile.postValue(response.body())
                    setBusInfo(serverApiRepository)
                    setSettings(serverApiRepository)
                }
            }

        })
    }

    fun sendPanicSignal(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.sendPanicSignal(currentGeoLocation?.value as String).enqueue(object : Callback<PanicSignalResult> {
            override fun onFailure(call: Call<PanicSignalResult>, t: Throwable) {
                isErrorPanicSignalToServer.postValue(true)
                t.printStackTrace()
            }

            override fun onResponse(call: Call<PanicSignalResult>, response: Response<PanicSignalResult>) {
                isErrorPanicSignalToServer.postValue(false)
                Log.d(TAG,response.message())
                if(response.isSuccessful){
                    sendPanicSignalToFirebase(response.body())
                }
            }

        })
    }

    private fun sendPanicSignalToFirebase(body: PanicSignalResult?) {

        body?.panic_data.let{ panicResultFromServer ->
            val panic_id = fbReferencePanicButton.push().key.toString()
            val ts = Date().time
            val panicMoment = PanicMoment(panic_id,ts,panicResultFromServer?.id as Int,panicResultFromServer.status,"")
            val fbPanicMoment = fbReferencePanicButton.child(panic_id)
            fbPanicMoment.setValue(panicMoment).addOnCompleteListener{
                fbPanicMoment.addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError?) {

                    }

                    override fun onDataChange(p0: DataSnapshot?) {
                        try {
                            val res = p0?.getValue(PanicMoment::class.java) as PanicMoment
                            panicResult.postValue(res)
                            Log.d("TrackerViewModel","Panic Result " + res.state)
                            if(res.state != "PENDING"){
                                fbPanicMoment.removeEventListener(this)
                            }
                        }catch(e: Exception){
                            val panRes = panicResult.value as PanicMoment
                            panRes.state = "ACCEPT"
                            panicResult.postValue(panRes)
                        }

                    }

                })
            }
        }

    }


    fun setSettings(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.settings().enqueue(object : Callback<SettingsResult>{
            override fun onFailure(call: Call<SettingsResult>, t: Throwable) {

            }

            override fun onResponse(call: Call<SettingsResult>, response: Response<SettingsResult>) {
                if(response.isSuccessful){
                    m_settings.postValue(response?.body()?.setting_data)
                }
            }

        })
    }

    fun setRute(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.rute().enqueue(object : Callback<RouteResult>{
            override fun onFailure(call: Call<RouteResult>, t: Throwable) {

                m_route.postValue(null);
            }

            override fun onResponse(call: Call<RouteResult>, response: Response<RouteResult>) {
                m_route.postValue(response.body()?.route);
            }

        })
    }

    fun setBusInfo(serverApiRepository: ServerApiRepository){
        serverApiRepository.services.busInfo().enqueue(object : Callback<BusInfoResult>{
            override fun onFailure(call: Call<BusInfoResult>, t: Throwable) {
                m_busInfo.postValue(null);
            }

            override fun onResponse(call: Call<BusInfoResult>, response: Response<BusInfoResult>) {
                m_busInfo.postValue(response.body()?.bus)
            }

        })
    }

    fun getProfile(): LiveData<Profile>{
        return m_profile
    }

    fun getRute(): LiveData<Route>{
        return m_route
    }

    fun getBusInfo(): LiveData<Bus>{
        return m_busInfo
    }

    fun getSettingData() : LiveData<ArrayList<SettingData>>{
        return m_settings
    }


}