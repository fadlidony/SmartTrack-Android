package com.idivisiontech.transporttracker

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.idivisiontech.transporttracker.RaspberryClient.RaspberryRepository
import com.idivisiontech.transporttracker.RaspberryClient.data.RunningTextUpdateResult
import com.idivisiontech.transporttracker.preferences.SettingPreferences
import kotlinx.android.synthetic.main.activity_runningtext.*
import kotlinx.android.synthetic.main.content_main2.*
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RunningtextActivity : AppCompatActivity() {
    private var indoorRT: Boolean = false
    private var frontRT: Boolean = false
    private var backRT: Boolean = false
    var state_back = MutableLiveData<Boolean>()
    var state_front = MutableLiveData<Boolean>()
    var state_indoor = MutableLiveData<Boolean>()

    val button_off = Color.parseColor("#A22020")
    val button_on = Color.parseColor("#226125")

    val text_on = "ENABLED"
    val text_off = "DISABLED"

    private val TAG = RunningtextActivity::class.java.name
    lateinit var settingPref: SettingPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runningtext)

        //initState()
        settingPref = SettingPreferences.getInstance(this)

        back.setOnClickListener {
            finish()
        }


        backRT = settingPref.isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_BACK)
        frontRT = settingPref.isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_FRONT)
        indoorRT = settingPref.isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_INDOOR)

        button_back.setBackgroundColor(if (backRT) button_on else button_off)
        button_front.setBackgroundColor(if (frontRT) button_on else button_off)
        button_indoor.setBackgroundColor(if (indoorRT) button_on else button_off)

        button_back.text = if (backRT) text_on else text_off
        button_front.text = if (frontRT) text_on else text_off
        button_indoor.text = if (indoorRT) text_on else text_off


        Toast.makeText(applicationContext,"BACK FRONT INDOOR = $backRT $frontRT $indoorRT", Toast.LENGTH_LONG).show()

        button_back.setOnClickListener {
            if(SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()){
                backRT = (button_back.text !== text_on)
                toggleAllButton()
                if(backRT){
                    settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                    val ruteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_BACK)
                    val ruteKode = SettingPreferences.getInstance(applicationContext).getKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_BACK)
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,ruteName, ruteKode).enqueue(object : Callback<RunningTextUpdateResult>{
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                }else{
                    settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                }
            }else{
                Toast.makeText(applicationContext,"Tidak bisa mengaktifkan Running Text karena fungsi Announcement sedang tidak aktif!",Toast.LENGTH_LONG).show()
            }
        }

        button_indoor.setOnClickListener {
            if(SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()) {
                indoorRT = (button_indoor.text !== text_on)
                toggleAllButton()
                if (indoorRT) {
                    settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)
                    val ruteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_INDOOR)
//                Toast.makeText(applicationContext,"button indoor onclick $ruteName ....",Toast.LENGTH_LONG).show()
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE, ruteName, "").enqueue(object : Callback<RunningTextUpdateResult> {
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                } else {
                    settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE, "stop", "0").enqueue(object : Callback<RunningTextUpdateResult> {
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                }
            }else{
                Toast.makeText(applicationContext,"Tidak bisa mengaktifkan Running Text karena fungsi Announcement sedang tidak aktif!",Toast.LENGTH_LONG).show()
            }
        }

        button_front.setOnClickListener {
            if(SettingPreferences.getInstance(applicationContext).getAnnouncementSetting()) {
                frontRT = (button_front.text !== text_on)
                toggleAllButton()
                if (frontRT) {
                    settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                    val ruteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_FRONT)
                    val ruteKode = SettingPreferences.getInstance(applicationContext).getKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_FRONT)
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE, ruteName, ruteKode).enqueue(object : Callback<RunningTextUpdateResult> {
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                } else {
                    settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                    RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE, "stop", "0").enqueue(object : Callback<RunningTextUpdateResult> {
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })
                }
            }else{
                Toast.makeText(applicationContext,"Tidak bisa mengaktifkan Running Text karena fungsi Announcement sedang tidak aktif!",Toast.LENGTH_LONG).show()
            }
        }

        /*button_back.setOnClickListener {
            if (state_back.value as Boolean) {
                button_back.setBackgroundColor(button_off)
                button_back.text = "DISABLE"
                settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                state_back.postValue(false)
                val ruteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_BACK)
                val ruteKode = SettingPreferences.getInstance(applicationContext).getKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_BACK)
                RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,ruteName, ruteKode).enqueue(object : Callback<RunningTextUpdateResult>{
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                })
            } else {
                button_back.setBackgroundColor(button_on)
                button_back.text = "ENABLE"
                state_back.postValue(true)
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                })
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
            }
        }
        button_front.setOnClickListener {
            if (state_front.value as Boolean) {
                state_front.postValue(false)
                settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                val ruteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_FRONT)
                val ruteKode = SettingPreferences.getInstance(applicationContext).getKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_FRONT)
                RaspberryRepository.getInstance().services
                        .updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE,ruteName,ruteKode)
                        .enqueue(object : Callback<RunningTextUpdateResult>{
                                override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                                   Toast.makeText(applicationContext, "Error menghubungi RunningText",Toast.LENGTH_SHORT).show()
                                }

                                override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                                    Toast.makeText(applicationContext, "Berhasil menghubungi RunningText",Toast.LENGTH_SHORT).show()
                                }

                            })

            } else {
                button_front.setBackgroundColor(button_off)
                button_front.text = "DISABLE"


                state_front.postValue(true)
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                })
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
            }
        }
        button_indoor.setOnClickListener {
            if (state_indoor.value as Boolean) {
                button_indoor.setBackgroundColor(button_off)
                button_indoor.text = "DISABLE"
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)

                state_indoor.postValue(false)
                val halteName = SettingPreferences.getInstance(applicationContext).getNameRute(SettingPreferences.RUTE_NAME_RUNNING_TEXT_INDOOR)
                val ruteKode = SettingPreferences.getInstance(applicationContext).getKodeRute(SettingPreferences.RUTE_KODE_RUNNING_TEXT_INDOOR)
                RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,halteName,ruteKode).enqueue(object : Callback<RunningTextUpdateResult>{
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                })
            } else {
                button_indoor.setBackgroundColor(button_on)
                button_indoor.text = "ENABLE"
                state_indoor.postValue(true)
                settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)
                RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                    override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                    override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                        settingPref.setRunningTextOnDuty(false)
                    }

                })
                settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)
            }
        }*/
    }

    private fun initState() {
        state_back.postValue(SettingPreferences.getInstance(this).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_BACK))
        state_front.postValue(SettingPreferences.getInstance(this).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_FRONT))
        state_indoor.postValue(SettingPreferences.getInstance(this).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_INDOOR))

        SettingPreferences.getInstance(this).preferences.registerOnSharedPreferenceChangeListener(object : SharedPreferences.OnSharedPreferenceChangeListener{
            override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
                if(key == SettingPreferences.IS_RUNNING_TEXT_ON_DUTY){
                    val enable = sharedPreferences?.getBoolean(key,false) as Boolean
                    Log.d(TAG,"KEY is RUNNING TEXT ON DUTY ${enable}")
                    //toggleAllButton(enable)
                }
            }

        })

        state_back.observe(this, object : Observer<Boolean> {
            override fun onChanged(enable: Boolean) {
                if (enable) {
                    button_back.setBackgroundColor(button_on)
                    button_back.text = "ENABLE"
                } else {
                    button_back.setBackgroundColor(button_off)
                    button_back.text = "DISABLE"
                }

                if(!enable){
                    settingPref.setRunningTextOnDuty(true)
                    /*RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })*/
                    settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                }else{
                    settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_BACK)
                }
            }

        })

        state_front.observe(this, object : Observer<Boolean> {
            override fun onChanged(enable: Boolean) {

                if (enable) {
                    button_front.setBackgroundColor(button_off)
                    button_front.text = "DISABLE"
                } else {
                    button_front.setBackgroundColor(button_on)
                    button_front.text = "ENABLE"
                }

                if(!enable){
                    settingPref.setRunningTextOnDuty(true)
                    /*RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })*/
                    settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                }else{
                    settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_FRONT)
                }
            }

        })

        state_indoor.observe(this, object : Observer<Boolean> {
            override fun onChanged(enable: Boolean) {
                if (enable) {
                    button_indoor.setBackgroundColor(button_off)
                    button_indoor.text = "DISABLE"

                } else {
                    button_indoor.setBackgroundColor(button_on)
                    button_indoor.text = "ENABLE"

                }

                if(!enable){
                    settingPref.setRunningTextOnDuty(true)
                    /*RaspberryRepository.getInstance().services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,"stop","0").enqueue(object : Callback<RunningTextUpdateResult>{
                        override fun onFailure(call: Call<RunningTextUpdateResult>, t: Throwable) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                        override fun onResponse(call: Call<RunningTextUpdateResult>, response: Response<RunningTextUpdateResult>) {
                            settingPref.setRunningTextOnDuty(false)
                        }

                    })*/
                    //settingPref.enableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)
                }else{
                    //settingPref.disableRunningText(SettingPreferences.RUNNING_TEXT_INDOOR)

                }
            }

        })
    }

    private fun toggleAllButton(){
        //button_back.isClickable = !isRunningTextOnDuty
        //button_front.isClickable = !isRunningTextOnDuty
        //button_indoor.isClickable = !isRunningTextOnDuty
        button_back.setBackgroundColor(if (backRT) button_on else button_off)
        button_front.setBackgroundColor(if (frontRT) button_on else button_off)
        button_indoor.setBackgroundColor(if (indoorRT) button_on else button_off)
        button_back.text = if (backRT) text_on else text_off
        button_front.text = if (frontRT) text_on else text_off
        button_indoor.text = if (indoorRT) text_on else text_off
    }
}
