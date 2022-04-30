package com.idivisiontech.transporttracker.Helpers

import android.util.Log
import java.io.IOException

class UsbTetherHelper {
    private val args = arrayOf("cat", "/proc/net/arp")
    private val raspi_hw_prefix = "b8:27:eb"
    private val ip_regex = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+".toRegex()

    companion object{
        const val VIA_USB = 1;
        const val VIA_WIFI = 2;
        private var INSTANCE: UsbTetherHelper? = null
        fun getInstance(): UsbTetherHelper{
            if(INSTANCE == null){
                INSTANCE = UsbTetherHelper()
            }
            return INSTANCE as UsbTetherHelper
        }
    }

    fun getIpAddress(type: Int): String?{
        val result_raw = toRead();
        val result = result_raw.split("\n")
        Log.d("USBTETHER ", result_raw)
        result.let {
            if(it.size <= 1){
                return null
            }else{
                if(type == VIA_USB){
                    Log.d("USBTETHER ", result.get(1))
                    val device = ip_regex.find(result.get(1))
                    if(device == null){
                        return null
                    }
                    return device.value
                }else if(type == VIA_WIFI){
                    for (str in result){
                        if(str.toLowerCase().contains(raspi_hw_prefix)){
                            val device = ip_regex.find(str)
                            if(device == null){
                                return null
                            }
                            return device.value
                        }
                    }
                }
            }
        }
        return null;
    }

    private fun toRead(): String {
        val cmd: ProcessBuilder
        var result = ""
        try {
            cmd = ProcessBuilder(*args)
            val process = cmd.start()
            val input = process.inputStream
            result = input.bufferedReader().use { it.readText() }
            input.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return result
    }
}