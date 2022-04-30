package com.idivisiontech.transporttracker.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.idivisiontech.transporttracker.R
import kotlinx.android.synthetic.main.incoming_call_dialog.*

class CallDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.incoming_call_dialog)
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }


    fun setOnClickListener(onItemClickCallback: OnItemClickCallback){
        window?.findViewById<Button>(R.id.btn_hangup_dialog)?.setOnClickListener(onItemClickCallback)
    }

    fun setStatus(text: String){
        window?.findViewById<TextView>(R.id.status_text)?.text = text
    }


    interface OnItemClickCallback : View.OnClickListener {}


}