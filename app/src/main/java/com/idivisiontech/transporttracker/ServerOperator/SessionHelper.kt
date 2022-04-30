package com.idivisiontech.transporttracker.ServerOperator

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper
import com.idivisiontech.transporttracker.R
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile

class SessionHelper(context: Context,
                    private var session_key:String? = null){

    companion object{
        const val SESSION_KEY = "session_key"
    }

    private val preferencesHelper: PreferenceHelper
    var serverRepository: ServerApiRepository


    init {
        preferencesHelper = PreferenceHelper(context.getSharedPreferences(context.getString(R.string.preference_file_key), MODE_PRIVATE))
        serverRepository = ServerApiRepository(context)

        val sk = preferencesHelper.get("session_key")
        if(sk != null){
            serverRepository.updateSessionKey(sk)
        }
    }

    /*fun init(){

        if(preferencesHelper == null){
            val sharedPreferences = context?.getSharedPreferences(context?.getString(R.string.preference_file_key), MODE_PRIVATE) as SharedPreferences
            preferencesHelper = PreferenceHelper(sharedPreferences)
        }

        if(serverRepository == null){
            serverRepository = ServerApiRepository(context as Context)
        }

        if(session_key == null){
            session_key = preferencesHelper?.get("session_key")
        }

    }*/

    fun isLogedIn() : Boolean{
        if(session_key == null){
            return false
        }
        return true
    }

    fun getProfile() : Profile? {
        var json = preferencesHelper.get("profile_driver")
        if (json != null) {
            if(json.isEmpty()){
                return null
            }
        }

        val gson = Gson()

        return gson.fromJson(json,Profile::class.java)
    }


}