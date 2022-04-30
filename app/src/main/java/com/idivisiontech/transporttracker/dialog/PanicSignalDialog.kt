package com.idivisiontech.transporttracker.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.idivisiontech.transporttracker.R

class PanicSignalDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.panic_signal_dialog)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    fun setStatusText(status: String){
        window?.findViewById<TextView>(R.id.statusTv)?.text = status
    }


    interface OnItemClickCallback : View.OnClickListener {}
}