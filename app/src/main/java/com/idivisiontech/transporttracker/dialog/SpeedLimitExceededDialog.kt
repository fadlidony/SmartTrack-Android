package com.idivisiontech.transporttracker.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.idivisiontech.transporttracker.R
import kotlinx.android.synthetic.main.speed_limit_exceeded_dialog.*

class SpeedLimitExceededDialog(context: Context) : Dialog(context) {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.speed_limit_exceeded_dialog)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }


    fun setSpeed(speed: String){
        window?.findViewById<TextView>(R.id.max_speed_km)?.text = speed
    }
}