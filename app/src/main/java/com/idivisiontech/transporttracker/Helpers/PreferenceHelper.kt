package com.idivisiontech.transporttracker.Helpers

import android.content.SharedPreferences

class PreferenceHelper(val preferences: SharedPreferences) {


    fun save(key: String, value: String) {
        val prefsEditor = this.preferences.edit();
        prefsEditor.putString(key,value)
        prefsEditor.commit()
    }

    fun save(key: String, value: Int){
        val prefsEditor = this.preferences.edit();
        prefsEditor.putInt(key,value)
        prefsEditor.commit()
    }

    fun get(key: String) : String?{
        return this.preferences.getString(key,"")
    }

    fun getDouble(key: String) : Double{
        return this.preferences.getLong(key, 0L).toDouble()
    }

    fun getInt(key:String) : Int{
        return this.preferences.getInt(key,-1);
    }

    fun delete(key: String){
        val prefsEditor = this.preferences.edit()
        prefsEditor.remove(key)
        prefsEditor.commit()
    }

}