/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idivisiontech.transporttracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.OneoffTask
import com.google.android.gms.gcm.Task
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.idivisiontech.transporttracker.Api.GmapApiRepository
import com.idivisiontech.transporttracker.Constants.Constant
import com.idivisiontech.transporttracker.Data.Gmap.MatrixDistance
import com.idivisiontech.transporttracker.Data.Halte
import com.idivisiontech.transporttracker.Data.Rute
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper
import com.idivisiontech.transporttracker.TrackerService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.text.DecimalFormat
import java.util.*

class TrackerService : Service(), LocationListener {
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mFirebaseTransportRef: DatabaseReference? = null
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null
    private val mTransportStatuses = LinkedList<Map<String, Any>>()
    private var mNotificationManager: NotificationManager? = null
    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private var mWakelock: WakeLock? = null
    private var sharedPreferences: SharedPreferences? = null
    private var mPrefs: SharedPreferences? = null
    private var sharedPreferenceChangeListener: OnSharedPreferenceChangeListener? = null
    private var ruteSekarang: Rute? = null
    private var preferenceHelper: PreferenceHelper? = null
    private val CURRENT_HALTE_ORDER_NUMBER = "order_number_halte_sekarang"
    private var gmapApiRepository: GmapApiRepository? = null
    private var matrixDistance: MatrixDistance? = null
    private var curState = STATE_NOT_IN_RADIUS
    private var speed = ""
    private var tracker_status = TRACKER_STATUS_TRAVELLING
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        buildNotification()
        setStatusMessage(R.string.connecting, "0,00")

        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        val fb = FirebaseRemoteConfig.getInstance()
        fb.setConfigSettings(configSettings)
        fb.setDefaults(R.xml.remote_config_defaults)
        mFirebaseRemoteConfig = fb

        mPrefs = getSharedPreferences(getString(R.string.prefs), Context.MODE_PRIVATE)
        val sharedPreferences = getSharedPreferences("TRANSJAKARTA_MONITORING_APP", Context.MODE_PRIVATE)
        preferenceHelper = PreferenceHelper(sharedPreferences)
        val json = sharedPreferences.getString("rute_sekarang", "")
        Log.d(TAG, "onCreate: json$json")
        setRuteSekarang(json)
        sharedPreferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            Log.d(TAG, "onSharedPreferenceChanged: $key")
            if (key === "rute_sekarang") {
                val json = sharedPreferences.getString(key, "")
                setRuteSekarang(json)
                Log.d(TAG, "onSharedPreferenceChanged: json $json")
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        gmapApiRepository = GmapApiRepository()
        //        String email = mPrefs.getString(getString(R.string.email), "");
//        String password = mPrefs.getString(getString(R.string.password), "");
//        authenticate(email, password);
        this.sharedPreferences = sharedPreferences
        fetchRemoteConfig()
        loadPreviousStatuses()
    }

    private fun setRuteSekarang(json: String?) {
        if (!json!!.isEmpty()) {
            val gson = Gson()
            ruteSekarang = gson.fromJson(json, Rute::class.java)
            ruteSekarang.let {
                Log.d(TAG, "setRuteSekarang: " + it?.name)
            }
            //if(sharedPreferences.getInt(CURRENT_HALTE_ORDER_NUMBER,-2) == -2){
            preferenceHelper!!.save(CURRENT_HALTE_ORDER_NUMBER, -1)
            //}
        }
    }

    private fun findHalteById(id: String): Halte? {
        var chosen: Halte? = null
        var i = 0
        for (halte in ruteSekarang!!.halte) {
            if (i === Integer.valueOf(id)) {
                chosen = halte
                break
            }
            i++
        }
        return chosen
    }

    private fun findPrevHalteById(): Halte? {
        val id = preferenceHelper!!.getInt(CURRENT_HALTE_ORDER_NUMBER) - 1
        return if (id <= -1) {
            null
        } else ruteSekarang!!.halte[id]
    }

    private fun findNextHalteById(): Halte? {
        val id = preferenceHelper!!.getInt(CURRENT_HALTE_ORDER_NUMBER) + 1
        Log.d(TAG, "findNextHalteById: ID: $id")
        var chosen: Halte? = null
        var i = 0
        if (ruteSekarang!!.halte.size <= id) {
            return null
        }
        Log.d(TAG, "findNextHalteById: halte size = " + ruteSekarang!!.halte.size)
        for (halte in ruteSekarang!!.halte) {
            if (i === id) {
                chosen = halte
                break
            }
            i++
        }
        Log.d(TAG, "findNextHalteById: HALTE " + chosen!!.id)
        return chosen
    }

    private fun busJalan(origins: String) {
        Log.d(TAG, "busJalan: " + preferenceHelper!!.getInt(CURRENT_HALTE_ORDER_NUMBER))
        if (sharedPreferences!!.getInt(CURRENT_HALTE_ORDER_NUMBER, -2) != -2) {
            val matrixDistance: MatrixDistance? = null
            val halte = findNextHalteById()
            val destination = halte!!.lat + "," + halte.lang
            val that = this
            Log.d(TAG, "onLocationChanged: ORIGIN=$origins DESTINATION = $destination")
            gmapApiRepository!!.services.distanceMatrix("metric", origins, destination, "transit").enqueue(object : Callback<MatrixDistance> {
                override fun onResponse(call: Call<MatrixDistance>, response: Response<MatrixDistance>) {
                    that.matrixDistance = response.body()
                    updateDistance()
                    Log.d(TAG, "onResponse: " + response.body().toString())
                    setStatusMessage(R.string.tracking, speed)
                }

                override fun onFailure(call: Call<MatrixDistance>, t: Throwable) {}
            })
        }
    }

    private fun updateDistance() {
        if (matrixDistance != null) {
            val gson = Gson()
            val json = gson.toJson(matrixDistance)
            preferenceHelper!!.save("matrix_distance_next_halte", json)
            checkMoveToNextHalte()
        }
    }

    private fun checkMoveToNextHalte() {
        val distance = parseMatrixDistanceValue()
        setStatusMessage(R.string.tracking, speed)
        var nextHalte = findNextHalteById()
        Log.d(TAG, "checkMoveToNextHalte: jarak menuju HALTE " + nextHalte!!.name + " " + distance.toString())
        val prevHalte = findPrevHalteById()
        Log.d(TAG, "checkMoveToNextHalte: JARAK KE HALTE " + distance + " RADIUS " + Constant.RADIUS)
        Log.d(TAG, "checkMoveToNextHalte: STATE : $distance $curState")
        if (distance <= Constant.RADIUS && curState === STATE_NOT_IN_RADIUS) {
            Log.d(TAG, "checkMoveToNextHalte: KURANG DARI RADIUS DAN DILUAR RADIUS")
            tracker_status = TRACKER_STATUS_ARRIVING
            curState = STATE_IN_RADIUS
            Toast.makeText(this, "Akan Sampai pada Halte " + nextHalte.name, Toast.LENGTH_SHORT).show()
        } else if (distance > Constant.RADIUS && curState === STATE_IN_RADIUS) {
            tracker_status = TRACKER_STATUS_GOING_TO
            curState = STATE_NOT_IN_RADIUS
            val current_order_number = preferenceHelper!!.getInt(CURRENT_HALTE_ORDER_NUMBER) + 1
            preferenceHelper!!.save(CURRENT_HALTE_ORDER_NUMBER, current_order_number)
            nextHalte = findNextHalteById()
            Toast.makeText(this, "Menuju Halte " + nextHalte!!.name, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "checkMoveToNextHalte: pindah halte" + nextHalte.name)
        }
        /**
         *
         * if(prevHalte == null){
         * tracker_status = TRACKER_STATUS_GOING_TO;
         * }else if(distance >= Constant.RADIUS){
         *
         * }else{
         * tracker_status = TRACKER_STATUS_TRAVELLING;
         * }
         */
        if (distance < 5) {
        }
    }

    private fun parseMatrixDistanceValue(): Int {
        val distance = matrixDistance!!.rows[0].elements[0].distance.value
        /*String splits[] = distance.split(" ");
        Double number = Double.valueOf(splits[0]);
        if(distance.contains("km")){
            //return number * 1000.0;
        }else{
            //return number;
        }*/Log.d(TAG, "parseMatrixDistanceValue: DISTANCE$distance")
        return distance
    }

    override fun onDestroy() { // Set activity title to not tracking.
        setStatusMessage(R.string.not_tracking, "0,00")
        // Stop the persistent notification.
        mNotificationManager!!.cancel(NOTIFICATION_ID)
        // Stop receiving location updates.
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    this@TrackerService)
        }
        // Release the wakelock
        if (mWakelock != null) {
            mWakelock!!.release()
        }
        sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        super.onDestroy()
    }

    private fun authenticate(email: String, password: String) {
        val mAuth = FirebaseAuth.getInstance()
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    Log.i(TAG, "authenticate: " + task.isSuccessful)
                    if (task.isSuccessful) {
                        fetchRemoteConfig()
                        loadPreviousStatuses()
                    } else {
                        Toast.makeText(this@TrackerService, R.string.auth_failed,
                                Toast.LENGTH_SHORT).show()
                        stopSelf()
                    }
                }
    }

    private fun fetchRemoteConfig() {
        var cacheExpiration = CONFIG_CACHE_EXPIRY.toLong()
        if (mFirebaseRemoteConfig!!.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        mFirebaseRemoteConfig!!.fetch(cacheExpiration)
                .addOnSuccessListener {
                    Log.i(TAG, "Remote config fetched")
                    mFirebaseRemoteConfig!!.activateFetched()
                }
    }

    /**
     * Loads previously stored statuses from Firebase, and once retrieved,
     * start location tracking.
     */
    fun checkDevice(): String {
        return Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID)
    }

    private fun loadPreviousStatuses() {
        val android_id = Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID)
        Log.i("id: ", android_id)
        val transportId = mPrefs!!.getString(getString(R.string.transport_id), "")
        FirebaseAnalytics.getInstance(this).setUserProperty("android_id", android_id)
        val path = getString(R.string.firebase_path) + android_id
        val mFirebaseTransportRef = FirebaseDatabase.getInstance().getReference(path)
        mFirebaseTransportRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { //                if (snapshot != null) {
//
//                    for (DataSnapshot transportStatus : snapshot.getChildren()) {
//                        mTransportStatuses.add(Integer.parseInt(transportStatus.getKey()),
//                                (Map<String, Object>) transportStatus.getValue());
//                    }
//                }
                startLocationTracking()
            }

            override fun onCancelled(error: DatabaseError) { // TODO: Handle gracefully
            }
        })
        this.mFirebaseTransportRef = mFirebaseTransportRef
    }

    private val mLocationRequestCallback: ConnectionCallbacks = object : ConnectionCallbacks {
        @SuppressLint("InvalidWakeLockTag")
        override fun onConnected(bundle: Bundle?) {
            val request = LocationRequest()
            request.interval = mFirebaseRemoteConfig!!.getLong("LOCATION_REQUEST_INTERVAL")
            request.fastestInterval = mFirebaseRemoteConfig!!.getLong("LOCATION_REQUEST_INTERVAL_FASTEST")
            request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    request, this@TrackerService)
            setStatusMessage(R.string.tracking, "0,00")
            // Hold a partial wake lock to keep CPU awake when the we're tracking location.
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")
            mWakelock  = wakelock

            wakelock.acquire()
        }

        override fun onConnectionSuspended(reason: Int) { // TODO: Handle gracefully
        }
    }

    /**
     * Starts location tracking by creating a Google API client, and
     * requesting location updates.
     */
    private fun startLocationTracking() {
        val mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mLocationRequestCallback)
                .addApi(LocationServices.API)
                .build()
        this.mGoogleApiClient = mGoogleApiClient
        mGoogleApiClient.connect()

    }

    /**
     * Determines if the current location is approximately the same as the location
     * for a particular status. Used to check if we'll add a new status, or
     * update the most recent status of we're stationary.
     */
    private fun locationIsAtStatus(location: Location, statusIndex: Int): Boolean {
        if (mTransportStatuses.size <= statusIndex) {
            return false
        }
        val status = mTransportStatuses[statusIndex]
        val locationForStatus = Location("")
        locationForStatus.latitude = status["lat"] as Double
        locationForStatus.longitude = status["lng"] as Double
        val distance = location.distanceTo(locationForStatus)
        Log.d(TAG, String.format("Distance from status %s is %sm", statusIndex, distance))
        return distance < mFirebaseRemoteConfig!!.getLong("LOCATION_MIN_DISTANCE_CHANGED")
    }

    private val batteryLevel: Float
        private get() {
            val batteryStatus = registerReceiver(null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            var batteryLevel = -1
            var batteryScale = 1
            if (batteryStatus != null) {
                batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel)
                batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, batteryScale)
            }
            return batteryLevel / batteryScale.toFloat() * 100
        }

    private fun logStatusToStorage(transportStatus: Map<String, Any>) {
        try {
            val path = File(Environment.getExternalStoragePublicDirectory(""),
                    "transport-tracker-log.txt")
            if (!path.exists()) {
                path.createNewFile()
            }
            val logFile = FileWriter(path.absolutePath, true)
            logFile.append(transportStatus.toString() + "\n")
            logFile.close()
        } catch (e: Exception) {
            Log.e(TAG, "Log file error", e)
        }
    }

    private fun shutdownAndScheduleStartup(`when`: Int) {
        Log.i(TAG, "overnight shutdown, seconds to startup: $`when`")
        val task: Task = OneoffTask.Builder()
                .setService(TrackerTaskService::class.java)
                .setExecutionWindow(`when`.toLong(), `when` + 60.toLong())
                .setUpdateCurrent(true)
                .setTag(TrackerTaskService.TAG)
                .setRequiredNetwork(Task.NETWORK_STATE_ANY)
                .setRequiresCharging(false)
                .build()
        GcmNetworkManager.getInstance(this).schedule(task)
        stopSelf()
    }

    /**
     * Pushes a new status to Firebase when location changes.
     */
    override fun onLocationChanged(location: Location) {
        fetchRemoteConfig()
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY].toLong()
        val startupSeconds = (mFirebaseRemoteConfig!!.getDouble("SLEEP_HOURS_DURATION") * 3600).toInt()
        if (hour == mFirebaseRemoteConfig!!.getLong("SLEEP_HOUR_OF_DAY")) {
            shutdownAndScheduleStartup(startupSeconds)
            return
        }
        val decimalFormat = DecimalFormat("#.00")

        speed = String.format("%,.2f", location.speed * 3600 / 1000)
        val transportStatus: MutableMap<String, Any> = HashMap()
        val android_id = Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID)
        transportStatus["android_id"] = android_id
        transportStatus["lat"] = location.latitude
        transportStatus["lng"] = location.longitude
        transportStatus["time"] = Date().time
        transportStatus["power"] = batteryLevel
        transportStatus["network"] = checkNetwork()
        transportStatus["speed"] = speed
        Log.d(TAG,"Lokasi diperbarui dari Tracker : " + location.longitude + ",  " + location.latitude)
        //BUS JALAN
        val origin = location.latitude.toString() + "," + location.longitude
        //busJalan(origin);
//        if (locationIsAtStatus(location, 1) && locationIsAtStatus(location, 0)) {
//            // If the most recent two statuses are approximately at the same
//            // location as the new current location, rather than adding the new
//            // location, we update the latest status with the current. Two statuses
//            // are kept when the locations are the same, the earlier representing
//            // the time the location was arrived at, and the latest representing the
//            // current time.
//            mTransportStatuses.set(0, transportStatus);
// Only need to update 0th status, so we can save bandwidth.
        //mFirebaseTransportRef!!.setValue(transportStatus)
        //        } else {
//            // Maintain a fixed number of previous statuses.
//            while (mTransportStatuses.size() >= mFirebaseRemoteConfig.getLong("MAX_STATUSES")) {
//                mTransportStatuses.removeLast();
//            }
//            mTransportStatuses.addFirst(transportStatus);
//            // We push the entire list at once since each key/index changes, to
//            // minimize network requests.
//            mFirebaseTransportRef.setValue(mTransportStatuses);
//        }
        if (BuildConfig.DEBUG) {
            logStatusToStorage(transportStatus)
        }
        val info = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo
        val connected = info != null && info.isConnectedOrConnecting
        if (connected) {
            setStatusMessage(R.string.tracking, speed)
        } else {
            setStatusMessage(R.string.not_tracking, speed)
        }
    }

    private fun buildNotification() {
        var channel = ""
        channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel() else {
            ""
        }
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val resultPendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, TrackerActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val mNotificationBuilder = NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.bus_white)
                .setColor(getColor(R.color.colorPrimary))
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
        this.mNotificationBuilder = mNotificationBuilder
        startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Synchronized
    private fun createChannel(): String {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = "snap map fake location "
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel("snap map channel", name, importance)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel.enableLights(true)
        }
        mChannel.lightColor = Color.BLUE
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel)
        } else {
            stopSelf()
        }
        return "snap map channel"
    }

    private fun checkNetwork(): String {
        var wifiDataAvailable = false
        var mobileDataAvailable = false
        val conManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = conManager.allNetworkInfo
        for (netInfo in networkInfo) {
            if (netInfo.typeName.equals("WIFI", ignoreCase = true)) if (netInfo.isConnected) wifiDataAvailable = true
            if (netInfo.typeName.equals("MOBILE", ignoreCase = true)) if (netInfo.isConnected) mobileDataAvailable = true
        }
        return if (wifiDataAvailable == true) {
            "Wi-Fi"
        } else if (mobileDataAvailable == true) {
            "Wi-Fi"
        } else {
            "Not Connected"
        }
    }

    /**
     * Sets the current status message (connecting/tracking/not tracking).
     */
    private fun setStatusMessage(stringId: Int, speed: String) {
        mNotificationBuilder!!.setContentText(getString(stringId))
        mNotificationManager!!.notify(NOTIFICATION_ID, mNotificationBuilder!!.build())
        var jarak = 0
        val unit = "M"
        var halte_name = "..."
        //        Log.d(TAG, "setStatusMessage: m2 "+matrixDistance.toString());
        Log.d(TAG, "setStatusMessage Called")
        if (matrixDistance != null) {
            jarak = parseMatrixDistanceValue()
            halte_name = findNextHalteById()!!.name
            Log.d(TAG, "setStatusMessage: MATRIX DISTANCE" + matrixDistance.toString())
            //String splits[] = this.matrixDistance.getRows().get(0).getElements().get(0).getDistance().getText().split(" ");
//unit = splits[1];
//Log.d(TAG, "setStatusMessage: UNIT " + unit);
//            if(unit == "km"){
//                jarak/=1000.0;
//          }
//            Log.d(TAG, "setStatusMessage: jarak2" + splits[0]);
//jarak = Double.valueOf(matrixDistance.getRows().get(0).getElements().get(0).getDistance().getValue());
            Log.d(TAG, "setStatusMessage: checkOnNull $jarak")
        }
        // Also display the status message in the activity.
        val intent = Intent(STATUS_INTENT)
        intent.putExtra(getString(R.string.status), stringId)
        intent.putExtra("speed", speed)
        intent.putExtra("network", checkNetwork())
        intent.putExtra(NEXT_HALTE_NAME, halte_name)
        intent.putExtra(NEXT_HALTE_JARAK, jarak)
        intent.putExtra(NEXT_HALTE_JARAK_UNIT, unit)
        intent.putExtra(CURRENT_TRACKER_STATUS, tracker_status)
        intent.putExtra(STATE, curState)
        Log.d(TAG, "setStatusMessage: jarak:$jarak")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private val TAG = TrackerService::class.java.simpleName
        const val STATUS_INTENT = "status"
        private const val NOTIFICATION_ID = 1
        private const val FOREGROUND_SERVICE_ID = 1
        private const val CONFIG_CACHE_EXPIRY = 600 // 10 minutes.
        const val NEXT_HALTE_NAME = "next_halte_name"
        const val NEXT_HALTE_JARAK = "next_halte_jarak"
        const val NEXT_HALTE_JARAK_UNIT = "next_halte_jarak_unit"
        const val CURRENT_TRACKER_STATUS = "current_tracker_status"
        const val TRACKER_STATUS_ARRIVING = "tracker_status_arriving"
        const val TRACKER_STATUS_GOING_TO = "tracker_status_going_to"
        const val TRACKER_STATUS_TRAVELLING = "tracker_status_travelling"
        const val STATE_IN_RADIUS = "state_in_radius"
        const val STATE_NOT_IN_RADIUS = "state_not_in_radius"
        const val STATE = "STATE"
    }
}