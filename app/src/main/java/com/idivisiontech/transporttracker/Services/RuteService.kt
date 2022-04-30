package com.idivisiontech.transporttracker.Services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.*
import com.google.gson.Gson
import com.idivisiontech.transporttracker.Api.osrm.OsrmApiRepository
import com.idivisiontech.transporttracker.Api.osrm.data.DrivingRouteResult
import com.idivisiontech.transporttracker.ConfirmWrongWay
import com.idivisiontech.transporttracker.Constants.Constant
import com.idivisiontech.transporttracker.Helpers.UsbTetherHelper
import com.idivisiontech.transporttracker.R
import com.idivisiontech.transporttracker.RaspberryClient.RaspberryRepository
import com.idivisiontech.transporttracker.RaspberryClient.data.MetaDataResponse
import com.idivisiontech.transporttracker.RaspberryClient.data.PlayResult
import com.idivisiontech.transporttracker.RaspberryClient.data.RunningTextUpdateResult
import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.Bus
import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.BusInfoResult
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.Halte
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.Route
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.RouteResult
import com.idivisiontech.transporttracker.ServerOperator.Data.updateLokasiResponse.UpdateLokasiResponse
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository
import com.idivisiontech.transporttracker.ServerOperator.SessionHelper
import com.idivisiontech.transporttracker.Services.data.BusLocation
import com.idivisiontech.transporttracker.TrackerActivity
import com.idivisiontech.transporttracker.TrackerService
import com.idivisiontech.transporttracker.preferences.SettingPreferences
import com.idivisiontech.transporttracker.preferences.SettingPreferences.Companion.getInstance
import com.soten.libs.base.MessageResult
import com.soten.libs.obd.OBDManager
import com.soten.libs.obd.base.OBDMessageResult
import com.soten.libs.obd.impl.OBDModelListener
import com.soten.libs.obd.impl.OBD_EST527
import com.soten.libs.utils.PowerManagerUtils
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Math.abs
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*


class RuteService : Service(), Handler.Callback, OBDModelListener {

    companion object{
        const val INTENT_FILER_NAME = "rute_service_receiver"
        const val INTENT_HALTE_JARAK_INFO = "intent_halte_jarak_info"
        const val INTENT_FILTER_JARAK_BUS_DEPAN = "intent_filter_jarak_bus_depan"
        const val INTENT_ACTION_FROM_ACTIVITY = "intent_filter_from_activity"

        const val INTENT_FILTER_OBD = "intent_filter_obd"
        const val EXTRA_SPEED_OBD = "extra_speed_obd"
        const val INTENT_SENSOR_INFO = "intent_sensor_info"

        const val RUNNING_TEXT_FRONT = "running_text_front"
        const val RUNNING_TEXT_INDOOR = "running_text_indoor"
        const val RUNNING_TEXT_BACK = "running_text_back"


        const val EXTRA_JARAK_BUS_DEPAN = "extra_jarak_bus_depan"
        const val EXTRA_BUS_DEPAN_NAME = "extra_bus_depan_name"
        const val EXTRA_GEOLOCATION = "extra_geolocation"

        const val EXTRA_TEMPERATURE_DATA = "extra_temperature_data"
        const val EXTRA_HUMIDITY_DATA = "extra_humidity_data"
        const val EXTRA_VIBRATION_X_DATA = "extra_vibration_x_data"
        const val EXTRA_VIBRATION_Y_DATA = "extra_vibration_y_data"
        const val EXTRA_VIBRATION_Z_DATA = "extra_vibration_z_data"
        const val EXTRA_VIBRATION_G_DATA = "extra_vibration_g_data"
        const val EXTRA_DOOR1_STATUS_DATA = "extra_door1_status_data"
        const val EXTRA_DOOR2_STATUS_DATA = "extra_door2_status_data"


        var session_key: String? = null
        private val TAG = RuteService::class.java.simpleName
        fun startService(context: Context, session_key: String) {
            val startIntent = Intent(context, RuteService::class.java)
            this.session_key = session_key
            ContextCompat.startForegroundService(context, startIntent)

        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, RuteService::class.java)
            context.stopService(stopIntent)
        }

        val INTERVAL = 5000.toLong()
        val DISTANCE = 10.toFloat()
        const val EXTRA_SPEED = "EXTRA_SPEED"
        const val EXTRA_ROUTE_NAME = "extra_route_name"
        const val EXTRA_HALTE_SELANJUTNYA = "extra_halte_selanjutnya"
        const val EXTRA_JARAK_KE_HALTE_SELANJUTNYA = "extra_jarak_ke_halte_selanjutnya"
        const val STATE_NOT_IN_RADIUS = "not_in_radius_state"
        const val STATE_IN_RADIUS = "in_radius_state"
        const val EXTRA_CURRENT_TRACKER_STATUS = "current_tracker_status"

        const val TRACKER_STATUS_ARRIVING = "tracker_status_arriving"
        const val TRACKER_STATUS_GOING_TO = "tracker_status_going_to"
        const val TRACKER_STATUS_TRAVELLING = "tracker_status_travelling"

        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val INTENT_FILTER_DATE = "intent_filter_date"
    }

//    private lateinit var receiver: BroadcastReceiver
    private var isRuteLoading: Boolean = false


    private var countThreshold: Int = 0
    private var lastThreshold: Double = 0.0
    private var jarakTemporary: Double = 0.0
    private var busHalteIndexValueListener: ValueEventListener? = null
    private var bearingTo: Float = 0.0F
    private var waktuKeBusDepan: Double = 0.0
    private var busInfo: Bus? = null
    private lateinit var busRuteIndex: DatabaseReference
    private var busLocationRef: DatabaseReference? = null
    private var busHalteIndex: DatabaseReference? = null
    private var busHalteIndexState: DatabaseReference? = null
    private var sessionHelper: SessionHelper? = null
    private var locationListeners: Array<android.location.LocationListener>? = null
    private val CHANNEL_ID = "Rute_Service"
    private lateinit  var mDatabase : DatabaseReference
    private var mBinder = MyBinder()
    private var notification: NotificationCompat.Builder? = null
    private var rute:Route? = null
    private lateinit var android_id: String
    private var drivingRouteResult: DrivingRouteResult? = null

    //bus rute  halte things
    private var posisi = -1
    private var halteYangDituju: Halte? = null
    private var jarakKeHalteDituju = 0.0
    private var waktuKeHalteDituju: Double = 0.0
    private var jarakKeHalteStatic = 0.0

    //bus and radius things
    private var curState = STATE_NOT_IN_RADIUS

    //Bus depan
    private var busDepanReference : DatabaseReference? = null
    private var busDepanAndroidId: String? = null

    val lastLocation: Location = Location(LocationManager.GPS_PROVIDER)
    var locationManager: LocationManager? = null
    private var tracker_status = TrackerService.TRACKER_STATUS_TRAVELLING
    private var busDepanLocation: BusLocation? = null
    private var jarakKeBusDepan = 0.0
    private var raspberryRepository: RaspberryRepository? = null
    private var isRunningTextOnDuty = MutableLiveData<Boolean>()

    private var is_indoor_need_to_be_changed = true

    private lateinit var mOdbEst527: OBD_EST527
    private lateinit var mManager: OBDManager
    private lateinit var mHandler: Handler

    private var humidity = 0.0
    private var temperature = 0.0
    private var vibration_x = 0.0
    private var vibration_y = 0.0
    private var vibration_z = 0.0
    private var vibration_g = 0.0
    private var door1_status = 0.0
    private var door2_status = 0.0

    private val MSG_OBDRT = 1000
    private val MSG_ENGINE = 2000


    private var obdData = object {
        var batteryVoltage:Float = 0.0F
        var engineSpeed = 0.0F
        var drivingSpeed = 0.0F
        var totalMileage = 0.0F
        var coolantTemperature = 0.0F
        var instantaneousFuelConsumption = 0.0F
        var averageFuelConsumption = 0.0F
        var fuelConsumptionThisTime = 0.0F
        var cumulativeFuelConsumption = 0.0F
        var timesOfThisAccleration = 0.0F
        var timesOfThisEmergencyDeclaration = 0.0F
        var engineRunTime = 0
        var numberOfCurentFaultCodes = 0
        var engineLoad = 0.0F

    }

    private var wrongWayState = object {
        var isWaitAction = false
    }

    private lateinit var serverApiRepository : ServerApiRepository


    lateinit var mainHandler: Handler

    private val updateTextTask = object : Runnable {
        override fun run() {
            checkHumidity()
            Log.d(TAG, "Update sensor : OK")
            mainHandler.postDelayed(this, 3000)
        }
    }


    fun updateLokasi(location: Location?){
        lastLocation?.set(location)
        Log.d(TAG, "Lokasi diperbarui : " + location?.longitude.toString() + ", " + location?.latitude.toString())
        checkHumidity()
        busJalan()
        checkJarakDenganBusDepan()
        sendToActivity()
        sendToFirebase()
    }

    private fun checkHumidity() {
        RaspberryRepository.getInstance().services.getMetaData().enqueue(object : Callback<MetaDataResponse> {
            override fun onFailure(call: Call<MetaDataResponse>, t: Throwable) {

            }

            override fun onResponse(call: Call<MetaDataResponse>, response: Response<MetaDataResponse>) {

                response.body()?.apply {
                    Log.d(TAG, "checkHumidity : ${response.body().toString()}")
                    humidity = this.data.temperature.toDouble()
                    temperature = this.data.humidity.toDouble()
                    vibration_x = this.data.vibration_x.toDouble()
                    vibration_y = this.data.vibration_y.toDouble()
                    vibration_z = this.data.vibration_z.toDouble()
                    vibration_g = this.data.vibration_g.toDouble()
                    door1_status = this.data.door1_status.toDouble()
                    door2_status = this.data.door2_status.toDouble()


                    SettingPreferences.getInstance(applicationContext).setTemperature(temperature)
                    SettingPreferences.getInstance(applicationContext).setHumidity(humidity)
                    SettingPreferences.getInstance(applicationContext).setVibrationX(vibration_x)
                    SettingPreferences.getInstance(applicationContext).setVibrationY(vibration_y)
                    SettingPreferences.getInstance(applicationContext).setVibrationZ(vibration_z)
                    SettingPreferences.getInstance(applicationContext).setVibrationG(vibration_g)
                    SettingPreferences.getInstance(applicationContext).setDoor1Sensor(door1_status)
                    SettingPreferences.getInstance(applicationContext).setDoor2Sensor(door1_status)

                }



                sendSensorData()
            }

        })
    }

    private fun checkJarakDenganBusDepan() {
        busDepanLocation.let{
            val asal = RouteHelper.getLongLat(lastLocation)
            val tujuan = "${it?.lng},${it?.lat}"
            OsrmApiRepository.getInstance().services.drivingRoute(asal, tujuan).enqueue(object : Callback<DrivingRouteResult> {
                override fun onFailure(call: Call<DrivingRouteResult>, t: Throwable) {

                }

                override fun onResponse(call: Call<DrivingRouteResult>, response: Response<DrivingRouteResult>) {
                    if (response.isSuccessful()) {
                        updateJarakBusDepan(response.body())
                    }

                }

            })
        }
    }

    private fun updateJarakBusDepan(result: DrivingRouteResult?) {
        result.let{
            jarakKeBusDepan = it?.routes?.get(0)?.distance as Double
            waktuKeBusDepan = it?.routes?.get(0)?.duration as Double
            sendJarakBusDepanToActivity(jarakKeBusDepan, busDepanAndroidId)
        }
    }

    private fun sendJarakBusDepanToActivity(jarakKeBusDepan: Double, busDepanAndroidId: String?) {
        val intent = Intent(INTENT_FILTER_JARAK_BUS_DEPAN)

        intent.putExtra(EXTRA_BUS_DEPAN_NAME, busDepanLocation?.nama_bus)
        intent.putExtra(EXTRA_JARAK_BUS_DEPAN, jarakKeBusDepan)

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    private val busDepanValueEventListener = object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onDataChange(p0: DataSnapshot?) {
            p0?.value.let{
                var gson = Gson()
                var jsonString = it.toString()
                Log.d(TAG, "busDepanValueEventListener ${jsonString}")
                busDepanLocation = p0?.getValue(BusLocation::class.java) //gson.fromJson(jsonString, BusLocation::class.java)
            }
        }
    }


    private fun checkBusDepan() {
        if(rute != null){
            val halteSekarang = rute?.halte?.get(posisi)
            if (halteSekarang != null){
                val ref_string = "halte-rute-state/${rute?.id}/${halteSekarang?.id}/${halteSekarang?.pivot?.order}"
                val halteRuteStateReference = FirebaseDatabase.getInstance().getReference(ref_string)
                halteRuteStateReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError?) {

                    }

                    override fun onDataChange(p0: DataSnapshot?) {
                        p0.let {
                            if (p0?.value.toString().isNullOrEmpty()) {
                                busDepanAndroidId = null
                                sendJarakBusDepanToActivity(0.0, null)
                            } else {
                                p0?.value.let {
                                    if (it.toString() != android_id) {
                                        busDepanAndroidId = it.toString()
                                        initBusDepanReference()
                                    }
                                }
                            }
                            halteRuteStateReference?.setValue(android_id)
                        }
                    }

                })
            }
        }
    }

    private fun initBusDepanReference() {
        this.busDepanReference.let{
            it?.removeEventListener(busDepanValueEventListener)
        }
        if(busDepanAndroidId != null) {
            val ref_string = "bus-locations/${busDepanAndroidId}"
            val busDepanReference = FirebaseDatabase.getInstance().getReference(ref_string)
            busDepanReference.addValueEventListener(busDepanValueEventListener)
            this.busDepanReference = busDepanReference
        }

    }

    private fun busJalan() {

        if(rute != null){
            if(posisi + 1 >= (rute?.halte?.size as Int) && rute?.halte?.size != 0){
                posisi = -1
            }
            halteYangDituju = rute?.halte?.get(posisi + 1)

            val longLatAsal = RouteHelper.getLongLat(lastLocation)
            val longLatTujuan = halteYangDituju?.lang.toString() + "," + halteYangDituju?.lat

            Log.d(TAG, "HALTE NAME " + halteYangDituju?.name)

            val destLoc = Location("Tujuan")
            destLoc.latitude = halteYangDituju?.lat as Double
            destLoc.longitude = halteYangDituju?.lang as Double

            val bearing = lastLocation.bearingTo(destLoc)

            bearingTo = abs(bearing % 360)

            OsrmApiRepository.getInstance().services.drivingRoute(longLatAsal, longLatTujuan).enqueue(object : Callback<DrivingRouteResult> {
                override fun onFailure(call: Call<DrivingRouteResult>, t: Throwable) {
                    Log.d(TAG, "JARAK : ERROR")
                }

                override fun onResponse(call: Call<DrivingRouteResult>, response: Response<DrivingRouteResult>) {
                    if (response.isSuccessful) {
                        updateDrivingRouteResult(response.body())
                    }
                }

            })
        }
    }

    private var receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent?.extras?.getString("action_next")
            Log.d(TAG, "MESSENGER " + action)
            when(action){
                ConfirmWrongWay.ACTION_LANJUT -> {
                    wrongWayState.isWaitAction = false
                    countThreshold = 0
                }

                ConfirmWrongWay.ACTION_PINDAH_HALTE -> {
                    if (lastThreshold >= Constant.THRESHOLD) {
                        tracker_status = TRACKER_STATUS_GOING_TO

                        curState = STATE_NOT_IN_RADIUS
                        updateIndexHalte()
                        Toast.makeText(this@RuteService, "Menuju " + halteYangDituju?.name, Toast.LENGTH_LONG).show()
                        playGoingToSound(halteYangDituju?.id as Int, rute?.id as Int)
                        checkBusDepan()
                        is_indoor_need_to_be_changed = true
                        jarakKeHalteStatic = 0.0
                        countThreshold = 0
                    }
                    wrongWayState.isWaitAction = false
                }

                ConfirmWrongWay.ACTION_MATIKAN_FUNGSI -> {
                    SettingPreferences.getInstance(applicationContext).disableAnnouncement()
                    wrongWayState.isWaitAction = false
                    countThreshold = 0
                    disableRT()
                }

            }
        }

    }

    private fun startNotifWrongWay(){
        val intent = Intent(applicationContext, ConfirmWrongWay::class.java)
        startActivity(intent)
    }

    private fun updateDrivingRouteResult(body: DrivingRouteResult?) {
        if(!SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()){
            return
        }
        body.let {
            jarakKeHalteDituju = it?.routes?.get(0)?.distance as Double
            waktuKeHalteDituju = it?.routes?.get(0)?.duration as Double

            if(jarakKeHalteStatic == 0.0){
                jarakKeHalteStatic = jarakKeHalteDituju
            }else if(jarakKeHalteStatic > jarakKeHalteDituju){
               jarakKeHalteStatic = jarakKeHalteDituju
            }

            val distance = jarakKeHalteDituju
            if(wrongWayState.isWaitAction == false){


                if(jarakTemporary == 0.0){
                    jarakTemporary = distance
                }else {
                    if(jarakTemporary < distance && obdData.drivingSpeed >= 10 && curState == STATE_NOT_IN_RADIUS){
                        countThreshold+=1
                    }else{
                        countThreshold=0
                    }
                    Log.d(TAG, "Threshold Counter = $countThreshold")
                    if(countThreshold >= 5){
                        val thresh = distance - jarakKeHalteStatic
                        lastThreshold = thresh
                        startNotifWrongWay()
                        wrongWayState.isWaitAction = true
//
//                        if (thresh >= Constant.THRESHOLD) {
//                            tracker_status = TRACKER_STATUS_GOING_TO
//
//                            curState = STATE_NOT_IN_RADIUS
//                            updateIndexHalte()
//                            Toast.makeText(this, "Menuju " + halteYangDituju?.name, Toast.LENGTH_LONG).show()
//                            playGoingToSound(halteYangDituju?.id as Int, rute?.id as Int)
//                            checkBusDepan()
//                            is_indoor_need_to_be_changed = true
//                            jarakKeHalteStatic = 0.0
//                            countThreshold=0
//                        }
                    }
                }
            }else{

            }

            Log.d(TAG, "JARAK ke halte berikutnya = " + jarakKeHalteDituju)


            jarakTemporary = distance

            if(distance <= Constant.RADIUS  && curState == STATE_NOT_IN_RADIUS){
                tracker_status = TRACKER_STATUS_ARRIVING

                curState = STATE_IN_RADIUS
                playArrivingSound(halteYangDituju?.id as Int, rute?.id as Int);
                Toast.makeText(this, "Akan sampai pada halte " + halteYangDituju?.name, Toast.LENGTH_LONG).show()
            }else if(distance > Constant.RADIUS && curState == STATE_IN_RADIUS){
                tracker_status = TRACKER_STATUS_GOING_TO
                curState = STATE_NOT_IN_RADIUS
                updateIndexHalte()
                countThreshold = 0
                Toast.makeText(this, "Menuju " + halteYangDituju?.name, Toast.LENGTH_LONG).show()
                playGoingToSound(halteYangDituju?.id as Int, rute?.id as Int)
                checkBusDepan()
                is_indoor_need_to_be_changed = true
                jarakKeHalteStatic = 0.0



            }else {

            }


            if(is_indoor_need_to_be_changed){
                sendToIndoorRunningText()
                is_indoor_need_to_be_changed = false
            }
            sendHalteAndJarakInfo()
        }
    }

    private val soundPlayCallback = object : Callback<PlayResult>{
        override fun onFailure(call: Call<PlayResult>, t: Throwable) {
            Toast.makeText(applicationContext, "ERROR SAAT AKSES", Toast.LENGTH_LONG).show()
        }

        override fun onResponse(call: Call<PlayResult>, response: Response<PlayResult>) {
            if(response.isSuccessful){
                Toast.makeText(applicationContext, "Audio berhasil diputar", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(applicationContext, "ERROR SAAT AKSES GAGAL SUKSES", Toast.LENGTH_LONG).show()
            }
        }

    }
    private fun playArrivingSound(halteId: Int, ruteId: Int) {
        if(SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()){
            UsbTetherHelper.getInstance().getIpAddress(UsbTetherHelper.VIA_USB).let {
                val raspiIP = UsbTetherHelper.getInstance().getIpAddress(UsbTetherHelper.VIA_USB)
                if (raspiIP != null) {
                    val instance = RaspberryRepository.getInstance(raspiIP)
                    try {
                        instance.services.playAudio(ruteId, halteId, RaspberryRepository.TYPE_ARRIVING).enqueue(soundPlayCallback)
                    } catch (e: Exception) {
                        Log.d(TAG, "ERROR PLAY AUDIO " + e.message)
                    }
                }

            }
        }
    }

    private fun playGoingToSound(halteId: Int, ruteId: Int){
        if(SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()) {
            UsbTetherHelper.getInstance().getIpAddress(UsbTetherHelper.VIA_USB).let {
                val raspiIP = UsbTetherHelper.getInstance().getIpAddress(UsbTetherHelper.VIA_USB)
                if (raspiIP != null){
                    val instance = RaspberryRepository.getInstance(raspiIP)
                    try{
                        instance.services.playAudio(ruteId, halteId, RaspberryRepository.TYPE_GOING_TO).enqueue(soundPlayCallback)
                    }catch (e: Exception){
                        Log.d(TAG, "ERROR PLAY AUDIO " + e.message)
                    }
                }

            }
        }
    }

    private fun updateIndexHalte() {
        Log.d(TAG, " POSISI ${posisi} HALTE TOTAL : ${(rute?.halte?.size as Int)}")
        if(posisi + 1 == rute?.halte?.size){
            posisi = -1
        }else{
            posisi++
        }

        FirebaseDatabase.getInstance().getReference("bus-halte-index/" + android_id + "/index").setValue(posisi)
        halteYangDituju = rute?.halte?.get((posisi + 1) % (rute?.halte?.size as Int))
    }

    private fun sendToFirebase() {
        val transportStatus: MutableMap<String, Any> = HashMap()
        transportStatus["android_id"] = android_id
        transportStatus["lat"] = lastLocation.latitude
        transportStatus["lng"] = lastLocation.longitude
        transportStatus["time"] = Date().time
        transportStatus["speed"] = (lastLocation.speed) * 3600 / 1000
        transportStatus["nama_halte"] = when(halteYangDituju){
            null -> ""
            else -> halteYangDituju?.name as String
        }
        transportStatus["nama_rute"] = when(rute) {
            null -> ""
            else -> rute?.name as String
        }
        transportStatus["kode_rute"] = when(rute){
            null -> ""
            else -> rute?.code as String
        }
        transportStatus["jarak_halte"] = jarakKeHalteDituju
        transportStatus["waktu_halte"] = waktuKeHalteDituju
        transportStatus["nama_bus"] = when(busInfo){
            null -> ""
            else -> busInfo?.name as String
        }

        transportStatus["bus_depan"] = when(busDepanLocation){
            null -> ""
            else -> busDepanLocation?.nama_bus as String
        }

        transportStatus["jarak_bus_depan"] = jarakKeBusDepan
        transportStatus["waktu_bus_depan"] = waktuKeBusDepan
        transportStatus["nama_bus_depan"] = when(busDepanLocation){
            null -> ""
            else -> busDepanLocation?.nama_bus as String
        }
        transportStatus["bearing_to"] = bearingTo
        transportStatus["obd_data"] = this.obdData
        transportStatus["humidity"] = this.humidity
        transportStatus["temperature"] = this.temperature
        transportStatus["vibrationdirectionx"] = this.vibration_x
        transportStatus["vibrationdirectiony"] = this.vibration_y
        transportStatus["vibrationdirectionz"] = this.vibration_z
        transportStatus["vibrationforce"] = this.vibration_g
        transportStatus["door1_status"] = this.door1_status
        transportStatus["door2_status"] = this.door2_status


        busLocationRef?.setValue(transportStatus)

//        serverApiRepository.services.updateLokasi(
//                speed = obdData.drivingSpeed,
//                lat = lastLocation.latitude.toString(),
//                long = lastLocation.longitude.toString(),
//                compass = bearingTo.toString()
//        ).enqueue(object : Callback<UpdateLokasiResponse>{
//            override fun onFailure(call: Call<UpdateLokasiResponse>, t: Throwable) {
//                Log.d(TAG,"updateLokasi : Gagal ${t.message}")
//            }
//
//            override fun onResponse(call: Call<UpdateLokasiResponse>, response: Response<UpdateLokasiResponse>) {
//                Log.d(TAG,"updateLokasi : ${response.isSuccessful.toString()} ${response.body().toString()}")
//            }
//
//        })

        serverApiRepository.services.updateMetadata(
                speed = (lastLocation.speed) * 3600 / 1000,
                longitude = lastLocation.longitude.toString(),
                latitude = lastLocation.latitude.toString(),
                compass = bearingTo.toString(),
                temperature = temperature.toString(),
                humidity = humidity.toString(),
                vibration_direction_x = vibration_x.toString(),
                vibration_direction_y = vibration_y.toString(),
                vibration_direction_z = vibration_z.toString(),
                vibrationforce = vibration_g.toString(),
                ecukilometertravel = obdData.totalMileage.toString(),
                ecufuelusage = obdData.cumulativeFuelConsumption.toString(),
                ecuspeed = obdData.drivingSpeed.toString(),
                voltage = obdData.batteryVoltage.toString()
        ).enqueue(object : Callback<UpdateLokasiResponse> {
            override fun onFailure(call: Call<UpdateLokasiResponse>, t: Throwable) {
                Log.d(TAG, "updateLokasi : Gagal ${t.message}")
            }

            override fun onResponse(call: Call<UpdateLokasiResponse>, response: Response<UpdateLokasiResponse>) {
                Log.d(TAG, "updateLokasi : ${response.isSuccessful.toString()} ${response.body().toString()}")
            }

        })
    }

    fun sendToActivity(){
        val speed:Float = (lastLocation.speed) * 3600 / 1000

        val intent = Intent(INTENT_FILER_NAME)
        val df = DecimalFormat()
        df.maximumFractionDigits = 2
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale("us_US"))
        intent.putExtra(EXTRA_SPEED, df.format(speed))

        var rute_name = "..."
        if(rute != null){
            rute_name = rute?.name as String
        }
        //if(speed > 0.5) {
        intent.putExtra(EXTRA_ROUTE_NAME, rute_name)

        intent.putExtra(EXTRA_GEOLOCATION, RouteHelper.getLatLong(lastLocation))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        //}
    }

    fun sendHalteAndJarakInfo(){
        val intent = Intent(INTENT_HALTE_JARAK_INFO)

        var jarak = RouteHelper.meterToKM(jarakKeHalteDituju)
        intent.putExtra(EXTRA_JARAK_KE_HALTE_SELANJUTNYA, jarak)

        var halte_name = halteYangDituju?.name
        intent.putExtra(EXTRA_HALTE_SELANJUTNYA, halte_name)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }




    override fun onCreate() {
        super.onCreate()
        android_id = Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID)
        mDatabase = FirebaseDatabase.getInstance().getReference()
        initLocationManager()
        sessionHelper = SessionHelper(applicationContext, session_key)
        serverApiRepository = ServerApiRepository(applicationContext)
        serverApiRepository.updateSessionKey(session_key as String)
        initRute()
        busLocationRef = FirebaseDatabase.getInstance().getReference("bus-locations/" + android_id)
        val busHalteIndex = FirebaseDatabase.getInstance().getReference("bus-halte-index/" + android_id + "/index")
        busHalteIndexValueListener = object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(p0: DataSnapshot?) {
                if(p0?.value != null) {
                    posisi = Integer.parseInt(p0?.value.toString())
                    Log.d(TAG, "POSISI AMBIL DARI FIREBASE = " + posisi)
                }else{
                    busHalteIndex.setValue(0)
                }
            }

        }
        busHalteIndex.addValueEventListener(busHalteIndexValueListener)
        this.busHalteIndex = busHalteIndex

        val busHalteIndexState = FirebaseDatabase.getInstance().getReference("bus-halte-index/" + android_id + "/state")
        busHalteIndexState.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(p0: DataSnapshot?) {
                if (p0?.value == null) {
                    busHalteIndexState.setValue(Constant.BUS_RUTE_ACCEPT)
                } else if (p0?.value.toString() == Constant.BUS_RUTE_PENDING) {
                    initRute()
                    busHalteIndexState.setValue(Constant.BUS_RUTE_ACCEPT)
                }
            }

        })

        this.busHalteIndexState = busHalteIndexState

        raspberryRepository = RaspberryRepository.getInstance()
        isRunningTextOnDuty.postValue(false)
        isRunningTextOnDuty.observeForever(object : Observer, androidx.lifecycle.Observer<Boolean> {
            override fun update(o: Observable?, arg: Any?) {

            }

            override fun onChanged(t: Boolean?) {
                SettingPreferences.getInstance(this@RuteService).setRunningTextOnDuty(t as Boolean)
            }

        })

        val dateFormatter = SimpleDateFormat("dd-MM-yyyy")
        val timeFormatter = SimpleDateFormat("HH:mm")
        val jam = GlobalScope.launch {
           while(this.isActive){
               try{
                   val current = Date()
                   val dateString = dateFormatter.format(current)
                   val timeString = timeFormatter.format(current)
                   val intent = Intent(INTENT_FILTER_DATE)
                   intent.putExtra(EXTRA_DATE, dateString)
                   intent.putExtra(EXTRA_TIME, timeString)
                   LocalBroadcastManager.getInstance(this@RuteService).sendBroadcast(intent)
                   delay(60000)
               }catch (e: Exception){
                   e.printStackTrace()
               }
           }
        }

        val sensor_check = GlobalScope.launch {
            while(this.isActive){
                try{
                    checkHumidity()
                    Log.d("DEBUG", "Update sensor: OK")
                    delay(3000)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }






    }

    private fun initObd() {
        Log.d(TAG, "INIT OBD")
        try{
            mManager = OBDManager.getInstance()
            mOdbEst527 = mManager.getModel()
            mHandler = Handler(this)
            mManager.open(this)
            val mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            PowerManagerUtils.open(mPowerManager, 0x14) // OBD_POWER

            PowerManagerUtils.open(mPowerManager, 0x15) // OBD_RESET
            mManager.register(this)
            mHandler.sendEmptyMessage(MSG_ENGINE)
        }catch (e: Exception){
            Log.d(TAG, "INIT OBD EXCEPTION CATCHED : ")
            e.printStackTrace()
        }
    }




    fun initRute(){
        Log.d(TAG, "init Rute : start initiation")
        sessionHelper?.serverRepository?.services?.rute()?.enqueue(object : Callback<RouteResult> {
            override fun onFailure(call: Call<RouteResult>, t: Throwable) {
                Toast.makeText(applicationContext, "Rute bermasalah!", Toast.LENGTH_LONG).show()
                Log.d(TAG, "init Rute : rute bermasalah onfailure")
                t.printStackTrace()
            }

            override fun onResponse(call: Call<RouteResult>, response: Response<RouteResult>) {
                if (response.isSuccessful) {
                    rute = response.body()?.route
                    deleteAudioConfig()
                    Log.d(TAG, "kode rute ${rute?.code}")
                    initRunningText()
                } else {
                    Log.d(TAG, "kode rute tidka ditemukan ${response.errorBody().toString()} ${response.body().toString()} ${response.toString()}")
                }
            }

        })
        sessionHelper?.serverRepository?.services?.busInfo()?.enqueue(object : Callback<BusInfoResult> {
            override fun onFailure(call: Call<BusInfoResult>, t: Throwable) {

            }

            override fun onResponse(call: Call<BusInfoResult>, response: Response<BusInfoResult>) {
                if (response.isSuccessful) {
                    busInfo = response.body()?.bus as Bus
                }
            }

        })

    }

    private fun deleteAudioConfig() {
        raspberryRepository?.services?.deleteAudioConfig(rute?.id as Int)?.enqueue(object : Callback<PlayResult> {
            override fun onFailure(call: Call<PlayResult>, t: Throwable) {
                Log.i(TAG, "deleteAudioConfig : onFailure " + t.message)
            }

            override fun onResponse(call: Call<PlayResult>, response: Response<PlayResult>) {
                Log.i(TAG, "deleteAudioConfig : " + response.body()?.toString())
            }

        })
    }

    private fun initRunningText() {
        GlobalScope.launch{
            /*while(isRunningTextOnDuty.value as Boolean){
                delay(1000)
                Log.d(TAG,"RUNTEXT initRunningText isRunningTextOnDuty true")
            }*/
            if(isRunningTextOnDuty.value == false){
                isRunningTextOnDuty.postValue(true)
                if(SettingPreferences.getInstance(applicationContext).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_FRONT)) {



                    raspberryRepository?.services?.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE, rute?.name as String, rute?.code as String)?.enqueue(object : Callback<RunningTextUpdateResult> {
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            isRunningTextOnDuty.postValue(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            SettingPreferences.getInstance(applicationContext).setNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_FRONT, rute?.name as String)
                            SettingPreferences.getInstance(applicationContext).setKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_FRONT, rute?.code as String)
                            raspberryRepository?.services?.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE, rute?.name as String, rute?.code as String)?.enqueue(object : Callback<RunningTextUpdateResult> {
                                override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                                    isRunningTextOnDuty.postValue(false)
                                }

                                override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                                    SettingPreferences.getInstance(applicationContext).setNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_BACK, rute?.name as String)
                                    SettingPreferences.getInstance(applicationContext).setKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_BACK, rute?.code as String)
                                    isRunningTextOnDuty.postValue(false)
                                }

                            })
                        }

                    })
                }
            }
        }

    }

    private fun sendToIndoorRunningText(){
        Log.d(TAG, "RUNTEXT sendToIndoorRunningText INITIATED")

        GlobalScope.launch {
            /*while(isRunningTextOnDuty.value as Boolean){
                //wait until runningText Finish
                Log.d(TAG,"RUNTEXT sendToIndoorRunningText isRunningTextOnDuty true")
                delay(1000)
            }*/

            while(halteYangDituju == null){

            }
            Log.d(TAG, "RUNTEXT sendTOIndoorRuningText Halteyangdituju tidak null")
            isRunningTextOnDuty.postValue(true)
            var timeStart = Date().time
            if(SettingPreferences.getInstance(applicationContext).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_INDOOR)){
                Log.d(TAG, "RUNTEXT sendTOIndoorRuningText INDOOR enabled")
                raspberryRepository?.services?.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE, halteYangDituju?.name as String, "")?.enqueue(object : Callback<RunningTextUpdateResult> {
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        isRunningTextOnDuty.postValue(false)
                        Log.d(TAG, "RUNTEXT sendToIndoorRunningText GAGAL KIRIM REQUEST")
                        t.printStackTrace()
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        isRunningTextOnDuty.postValue(false)
                        Log.d(TAG, "RUNTEXT sendToIndoorRunningText SUKSES KIRIM REQUEST LAMANYA : ${(Date().time - timeStart) / 1000}")
                    }

                })
            }else{
                Log.d(TAG, "RUNTEXT sendTOIndoorRuningText INDOOR disabled")
            }

            SettingPreferences.getInstance(applicationContext).setNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_INDOOR, halteYangDituju?.name as String)

        }
    }



    fun updateRuteInfo(){

    }



    private fun initLocationManager() {
        if (locationManager == null)
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        val locationListenerNetwork = object : android.location.LocationListener{
            override fun onLocationChanged(location: Location?) {
                try{
                    updateLokasi(location)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }

        }

        val locationListenerGps = locationListenerNetwork

        locationListeners = arrayOf(locationListenerNetwork, locationListenerGps)

        try {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL, DISTANCE, locationListeners!![0])
        } catch (e: SecurityException) {
            Log.e(TAG, "Fail to request location update", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Network provider does not exist", e)
        } catch (e: Exception){
            e.printStackTrace()
        }


        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, locationListeners!![1])
        } catch (e: SecurityException) {
            Log.e(TAG, "Fail to request location update", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "GPS provider does not exist", e)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.let {
                it.createNotificationChannel(serviceChannel)
            }
        }
    }


    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SERVICE DIMULAI")

        createNotificationChannel()
        val notificationIntent = Intent(this, TrackerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Transjakarta Rute")
                .setContentText("Rute Sekarang : ")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
        this.notification = notification
        startForeground(2, notification.build())
        //return super.onStartCommand(intent, flags, startId)
        /*return START_NOT_STICKY*/
        initObd()
        initActivityBroadcastToThisService()
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    private fun initActivityBroadcastToThisService() {
        val filter = IntentFilter(INTENT_ACTION_FROM_ACTIVITY)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locationManager != null)
            for (i in 0..locationListeners!!.size) {
                try {
                    locationManager?.removeUpdates(locationListeners!![i])
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove location listeners")
                }
            }
        busHalteIndex?.removeEventListener(busHalteIndexValueListener)
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)

    }



    internal inner class MyBinder : Binder() {
        val getService: RuteService = this@RuteService
    }


    internal class RouteHelper {
        companion object{

            fun getLongLat(location: Location) : String{
                return location.longitude.toString() + ',' + location.latitude
            }

            fun getLatLong(location: Location) : String{
                return location.latitude.toString() + ',' + location.longitude
            }

            fun meterToKM(number: Double): Double{
                return number / 1000
            }
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        try {
            when (msg.what) {
                MSG_OBDRT -> {
                    val oResult = msg.obj as OBDMessageResult
                    refreshData(oResult.rawResponce)
                    Log.d(TAG, "handleMessage " + oResult.rawResponce.toString())
                    //tes.text = oResult.rawResponce.toString()
                }
                MSG_ENGINE -> {
                    mOdbEst527.getEngineTime()
                    mHandler.removeMessages(MSG_ENGINE)
                    mHandler.sendEmptyMessageDelayed(MSG_ENGINE, 3000)
                    //Log.d(TAG,msg.obj.toString())
                    //tes.text = msg.obj.toString()
                }
            }
        }catch (e: Exception){

        }
        return true
    }

    private fun refreshData(rawResponses: Array<String>?) {
        if (rawResponses == null) {
            return
        }
        Log.d("pbt", "rawResponse : " + rawResponses.get(0))

        try{
            if (rawResponses.get(0).contains("031")) {
                val value: String = rawResponses.get(0).split("=").toTypedArray().get(1)
                if (value != null) {
                    val time = value.toInt()
                    obdData.engineRunTime = time
                    //tes.text = "Engine runtime：$text"
                }
            }

            for (rawRespons in rawResponses) {
                val values = rawRespons.split(",").toTypedArray()
                Log.d(TAG, "rawRespons " + values.toString())
                values.get(0).let {
                    when(it){
                        "\$OBD-RT" -> {
                            obdData.batteryVoltage = values[1].toFloat()
                            obdData.engineSpeed = values[2].toFloat()
                            obdData.drivingSpeed = values[3].toFloat()
                            obdData.totalMileage = values[10].toFloat()
                            SettingPreferences.getInstance(applicationContext).setOdometerNumber(obdData.totalMileage.toInt())
                            obdData.coolantTemperature = values[6].toFloat()
                            obdData.instantaneousFuelConsumption = values[7].toFloat()
                            obdData.averageFuelConsumption = values[8].toFloat()
                            obdData.fuelConsumptionThisTime = values[11].toFloat()
                            obdData.cumulativeFuelConsumption = values[12].toFloat()
                            obdData.timesOfThisAccleration = values[14].toFloat()
                            obdData.timesOfThisEmergencyDeclaration = values[15].toFloat()
                            obdData.engineLoad = values[5].toFloat()

                            Log.d(TAG, String.format("Battery voltage:%sV", values[1]))
                            Log.d(TAG, String.format("Engine speed:%sRpm", values[2]))
                            Log.d(TAG, String.format("Driving speed:%sKm/h", values[3]))
                            /*mTexts.get(2).setText(String.format("Driving speed:%sKm/h", values[3]))
                            mTexts.get(3).setText(String.format("Throttle opening:%s%%", java.lang.Double.valueOf(values[4])))
                            mTexts.get(4).setText(String.format("Engine load:%s%%", java.lang.Double.valueOf(values[5])))
                            mTexts.get(5).setText(String.format("Coolant temperature:%s℃", java.lang.Double.valueOf(values[6])))
                            if (values[3] == "0") {
                                mTexts.get(6).setText(String.format("Instantaneous fuel consumption:%sL/h", java.lang.Double.valueOf(values[7])))
                            } else {
                                mTexts.get(6).setText(String.format("Instantaneous fuel consumption:%sL/100km", java.lang.Double.valueOf(values[7])))
                            }
                            mTexts.get(7).setText(String.format("Average fuel consumption:%sL/100km", java.lang.Double.valueOf(values[8])))
                            mTexts.get(8).setText(String.format("Current mileage:%skm", values[9]))
                            mTexts.get(9).setText(String.format("Total mileage:%skm", values[10]))
                            mTexts.get(10).setText(String.format("Fuel consumption this time:%sL", java.lang.Double.valueOf(values[11])))
                            mTexts.get(11).setText(String.format("Cumulative fuel consumption:%sL", java.lang.Double.valueOf(values[12])))
                            mTexts.get(12).setText(String.format("Number of current DTCs:%s", values[13]))
                            mTexts.get(13).setText(String.format("The number of rapid accelerations:%sTimes", values[14]))
                            mTexts.get(14).setText(String.format("The number of rapid decelerations:%sTimes", values[15]))*/
                        }

                        else -> {
                            Log.d(TAG, "handleMessage : " + it)
                        }
                    }

                }
            }

            sendObdData()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    override fun onReceive(result: MessageResult?) {
        if (result is OBDMessageResult) {
            val oResult = result as OBDMessageResult
            mHandler.obtainMessage(MSG_OBDRT, oResult).sendToTarget()
        }
    }

    override fun onLostConnect(p0: java.lang.Exception?) {

    }

    private fun sendObdData() {
        val intent = Intent(INTENT_FILTER_OBD)
        intent.putExtra(EXTRA_SPEED_OBD, obdData.drivingSpeed)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendSensorData() {
        val intent = Intent(INTENT_SENSOR_INFO)
        intent.putExtra(EXTRA_TEMPERATURE_DATA, this.temperature)
        intent.putExtra(EXTRA_HUMIDITY_DATA, this.humidity)
        intent.putExtra(EXTRA_VIBRATION_X_DATA, this.vibration_x)
        intent.putExtra(EXTRA_VIBRATION_Y_DATA, this.vibration_y)
        intent.putExtra(EXTRA_VIBRATION_Z_DATA, this.vibration_z)
        intent.putExtra(EXTRA_VIBRATION_G_DATA, this.vibration_g)
        intent.putExtra(EXTRA_DOOR1_STATUS_DATA, this.door1_status)
        intent.putExtra(EXTRA_DOOR2_STATUS_DATA, this.door2_status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    fun disableRT() {
        getInstance(applicationContext).disableRunningText(RUNNING_TEXT_BACK)
        getInstance(applicationContext).disableRunningText(RUNNING_TEXT_FRONT)
        getInstance(applicationContext).disableRunningText(RUNNING_TEXT_INDOOR)
        raspberryRepository!!.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE, "stop", "0").enqueue(object : Callback<RunningTextUpdateResult?> {
            override fun onResponse(call: Call<RunningTextUpdateResult?>, response: Response<RunningTextUpdateResult?>) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }

            override fun onFailure(call: Call<RunningTextUpdateResult?>, t: Throwable) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }
        })
        raspberryRepository!!.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE, "stop", "0").enqueue(object : Callback<RunningTextUpdateResult?> {
            override fun onResponse(call: Call<RunningTextUpdateResult?>, response: Response<RunningTextUpdateResult?>) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }

            override fun onFailure(call: Call<RunningTextUpdateResult?>, t: Throwable) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }
        })
        raspberryRepository!!.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE, "stop", "0").enqueue(object : Callback<RunningTextUpdateResult?> {
            override fun onResponse(call: Call<RunningTextUpdateResult?>, response: Response<RunningTextUpdateResult?>) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }

            override fun onFailure(call: Call<RunningTextUpdateResult?>, t: Throwable) {
                getInstance(applicationContext).setRunningTextOnDuty(false)
            }
        })
    }

}
