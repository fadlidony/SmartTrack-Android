package com.idivisiontech.transporttracker.preferences

import android.content.Context
import android.util.Log
import java.io.IOException

class SettingPreferences(context: Context) {

    companion object{
        private const val PREFS_NAME = "tab_setting_prefs"
        private const val ANNOUNCEMENT = "announcement"
        const val RUNNING_TEXT_FRONT = "running_text_front"
        const val RUNNING_TEXT_INDOOR = "running_text_indoor"
        const val RUNNING_TEXT_BACK = "running_text_back"

        //text
        const val RUTE_NAME_RUNNING_TEXT_FRONT = "name_running_text_front"
        const val RUTE_KODE_RUNNING_TEXT_FRONT = "kode_running_text_front"
        const val RUTE_NAME_RUNNING_TEXT_BACK = "name_running_text_back"
        const val RUTE_KODE_RUNNING_TEXT_BACK = "kode_running_text_back"
        const val RUTE_NAME_RUNNING_TEXT_INDOOR = "name_running_text_indoor"
        const val RUTE_KODE_RUNNING_TEXT_INDOOR = "kode_running_text_indoor"

        const val IS_RUNNING_TEXT_ON_DUTY = "is_running_text_on_duty"

        const val KEY_ODOMETER_NUMBER = "odometer_number"

        const val KEY_VOIP_NUMBER = "key_voip_number"

        const val KEY_TEMPERATURE = "key_temperature"
        const val KEY_HUMIDITY = "key_humidity"
        const val KEY_VIBRATION_X = "key_vibration_x"
        const val KEY_VIBRATION_Y = "key_vibration_y"
        const val KEY_VIBRATION_Z = "key_vibration_z"
        const val KEY_VIBRATION_G = "key_vibration_g"
        const val KEY_DOOR1_SENSOR = "key_door1_sensor"
        const val KEY_DOOR2_SENSOR = "key_door2_sensor"




        private const val PANIC_MOMENT_ID = "panic_moment_id"
        private const val VOICE_OVER_ON = "voice_over_on"

        private var INSTANCE: SettingPreferences? = null

        fun getInstance(context: Context) : SettingPreferences {
            if(INSTANCE == null){
                //INSTANCE = SettingPreferences(context)
            }

            //return INSTANCE as SettingPreferences
            return SettingPreferences(context)
        }
    }

    val preferences = context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)

    fun enableAnnouncement(){
        val editor = preferences.edit()
        editor.putBoolean(ANNOUNCEMENT,true)
        editor.apply()
    }

    fun disableAnnouncement(){
        val editor = preferences.edit()
        editor.putBoolean(ANNOUNCEMENT,false)
        editor.apply()
    }

    fun getAnnouncementSetting() : Boolean{
        return preferences.getBoolean(ANNOUNCEMENT,true)
    }

    fun setPanicMomentId(str: String){
        val editor = preferences.edit()
        editor.putString(PANIC_MOMENT_ID,str)
        editor.apply()
    }

    fun getPanicMomentId() : String? = preferences.getString(PANIC_MOMENT_ID,null)

    fun deletePanicMomentId() {
        val editor = preferences.edit()
        editor.remove(PANIC_MOMENT_ID)
        editor.apply()
    }

    fun isRunningTextEnabled(type: String) : Boolean{
        return preferences.getBoolean(type, true)
    }

    fun disableRunningText(type: String){
        val editor = preferences.edit()
        editor.putBoolean(type, false)
        editor.apply()
    }

    fun enableRunningText(type: String){
        val editor = preferences.edit()
        editor.putBoolean(type, true)
        editor.apply()
    }

    fun isVoiceOverOn() : Boolean = preferences.getBoolean(VOICE_OVER_ON, false)

    fun setVoiceOfferOn(on: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(VOICE_OVER_ON, on)
        editor.apply()
    }

    fun setKodeRute(tipe: String,value: String){
        val editor = preferences.edit()
        editor.putString(tipe, value)
        editor.apply()
    }

    fun getKodeRute(tipe: String) : String {
        return preferences.getString(tipe,"") as String
    }

    fun getNameRute(tipe: String) : String{
        return preferences.getString(tipe,"") as String
    }

    fun getSensor(tipe: String) : Double{
        val str = preferences.getString(tipe,"")
        if (str=="" || str==null) {
            return 0.0
        }else{
            val double1: Double = str?.toDouble()?: 0.0
            return double1
        }

    }


    fun setNameRute(tipe: String,value: String){
        val editor = preferences.edit()
        editor.putString(tipe, value)
        editor.apply()
    }

    fun isRunningTextOnDuty() : Boolean{
        return preferences.getBoolean(IS_RUNNING_TEXT_ON_DUTY,false)
    }

    fun setRunningTextOnDuty(yes: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(IS_RUNNING_TEXT_ON_DUTY, yes)
        editor.apply()
    }

    fun setOdometerNumber(number : Int){
        val editor = preferences.edit()
        editor.putInt(KEY_ODOMETER_NUMBER,number)
        editor.apply()
    }

    fun getOdometerNumber(): Int {
        return preferences.getInt(KEY_ODOMETER_NUMBER,-1)
    }

    fun setOperatorNumber(number: String) {
        val editor = preferences.edit()
        editor.putString(KEY_VOIP_NUMBER,number)
        editor.apply()
    }

    fun gettOperatorNumber() : String? {
        return preferences.getString(KEY_VOIP_NUMBER,null)
    }

    fun setTemperature(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_TEMPERATURE,number.toString())
        editor.apply()
    }

    fun setHumidity(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_HUMIDITY,number.toString())
        editor.apply()
    }

    fun setVibrationX(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_VIBRATION_X,number.toString())
        editor.apply()
    }

    fun setVibrationY(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_VIBRATION_Y,number.toString())
        editor.apply()
    }

    fun setVibrationZ(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_VIBRATION_Z,number.toString())
        editor.apply()
    }

    fun setVibrationG(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_VIBRATION_G,number.toString())
        editor.apply()
    }

    fun setDoor1Sensor(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_DOOR1_SENSOR,number.toString())
        editor.apply()
    }

    fun setDoor2Sensor(number: Double) {
        val editor = preferences.edit()
        editor.putString(KEY_DOOR2_SENSOR,number.toString())
        editor.apply()
    }









}