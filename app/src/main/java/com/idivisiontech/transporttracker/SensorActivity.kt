package com.idivisiontech.transporttracker

import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper
import com.idivisiontech.transporttracker.ServerOperator.Data.maintenance.SensorItem
import com.idivisiontech.transporttracker.ServerOperator.Data.maintenance.SensorResponse
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository
import com.idivisiontech.transporttracker.ServerOperator.SessionHelper
import com.idivisiontech.transporttracker.Services.RuteService
import com.idivisiontech.transporttracker.Services.RuteService.Companion.session_key
import com.idivisiontech.transporttracker.adapter.maintenance.SensorItemAdapter
import com.idivisiontech.transporttracker.preferences.SettingPreferences
import kotlinx.android.synthetic.main.activity_sensor.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SensorActivity : AppCompatActivity() {

    private lateinit var adapter: SensorItemAdapter
    private lateinit var serverApiRepository: ServerApiRepository
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var sharedPref: SharedPreferences
    private lateinit var settingPref: SettingPreferences
    private var TAG = SensorActivity::class.java.simpleName
    private var odometerNumber = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)


        back.setOnClickListener {
            finish()
        }
        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        preferenceHelper = PreferenceHelper(sharedPref)
        settingPref = SettingPreferences.getInstance(this)

        session_key = preferenceHelper.get(SessionHelper.SESSION_KEY)
        if (session_key === "") {
            stopService(Intent(this, TrackerService::class.java))
            RuteService.stopService(this)
            val backToLogin = Intent(this, LoginActivity::class.java)
            Toast.makeText(this, "Anda belum Login", Toast.LENGTH_SHORT).show()
            finish()
            startActivity(backToLogin)
        }

        adapter = SensorItemAdapter(this)
        listViewMaintenanceItem.adapter = adapter

        initData()
//        serverApiRepository = ServerApiRepository(this)
//        serverApiRepository.updateSessionKey(session_key as String)

//        odometerNumber = SettingPreferences.getInstance(applicationContext).getOdometerNumber()
//        tvOdometerNumber.text = "ODOMETER SAAT INI : ${odometerNumber} KM"

//        var actionGet: Call<SensorResponse>?
//        if(odometerNumber >= 0){
//            actionGet = serverApiRepository.services.getMaintenanceAndUpdateOdometer(odometerNumber)
//        }else{
//            actionGet = serverApiRepository.services.getMaintenance()
//        }
//
//        actionGet.enqueue(object : Callback<SensorResponse> {
//            override fun onFailure(call: Call<SensorResponse>, t: Throwable) {
//
//            }
//
//            override fun onResponse(call: Call<SensorResponse>, response: Response<SensorResponse>) {
//                loadData(response.body())
//            }
//
//        })




    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(ruteSensorDataReceiver,
                IntentFilter(RuteService.INTENT_SENSOR_INFO))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ruteSensorDataReceiver)
    }


    private fun loadData(data: List<SensorItem>) {
            adapter.sensorItems = data
            adapter.notifyDataSetChanged()
    }

    private fun initData(){
        val sensor_data: List<SensorItem> = listOf(
                SensorItem("Temperature", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_TEMPERATURE)),
                SensorItem("Humidity", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_HUMIDITY)),
                SensorItem("Vibration X", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_VIBRATION_X)),
                SensorItem("Vibration Y", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_VIBRATION_Y)),
                SensorItem("Vibration Z", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_VIBRATION_Z)),
                SensorItem("Vibration G", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_VIBRATION_G)),
                SensorItem("Door 1 Status", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_DOOR1_SENSOR)),
                SensorItem("Door 2 Status", SettingPreferences.getInstance(applicationContext).getSensor(SettingPreferences.KEY_DOOR2_SENSOR))
        )


        loadData(sensor_data)
    }

    private val ruteSensorDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("Data Sensor:","oke")
            val temperature = intent.getDoubleExtra(RuteService.EXTRA_TEMPERATURE_DATA, 0.0)
            val humidity = intent.getDoubleExtra(RuteService.EXTRA_HUMIDITY_DATA, 0.0)
            val vibration_x = intent.getDoubleExtra(RuteService.EXTRA_VIBRATION_X_DATA, 0.0)
            val vibration_y = intent.getDoubleExtra(RuteService.EXTRA_VIBRATION_Y_DATA, 0.0)
            val vibration_z = intent.getDoubleExtra(RuteService.EXTRA_VIBRATION_Z_DATA, 0.0)
            val vibration_g = intent.getDoubleExtra(RuteService.EXTRA_VIBRATION_G_DATA, 0.0)
            val door1_status = intent.getDoubleExtra(RuteService.EXTRA_DOOR1_STATUS_DATA, 0.0)
            val door2_status = intent.getDoubleExtra(RuteService.EXTRA_DOOR2_STATUS_DATA, 0.0)
            val sensor_data: List<SensorItem> = listOf(
                    SensorItem("Temperature", temperature),
                    SensorItem("Humidity", humidity),
                    SensorItem("Vibration X", vibration_x),
                    SensorItem("Vibration Y", vibration_y),
                    SensorItem("Vibration Z", vibration_z),
                    SensorItem("Vibration G", vibration_g),
                    SensorItem("Door 1 Status", door1_status),
                    SensorItem("Door 2 Status", door2_status)
            )

            loadData(sensor_data)

            }
        }
    }


