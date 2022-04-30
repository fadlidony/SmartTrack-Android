package com.idivisiontech.transporttracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.idivisiontech.transporttracker.Services.RuteService
import kotlinx.android.synthetic.main.activity_confirm_wrong_way.*

class ConfirmWrongWay : AppCompatActivity() {

    companion object{
        const val ACTION_LANJUT = "action_lanjut";
        const val ACTION_MATIKAN_FUNGSI = "action_matikan_fungsi";
        const val ACTION_PINDAH_HALTE = "action_pindah_halte";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_wrong_way)
        setFinishOnTouchOutside(false)

        initButton()
    }

    private fun initButton() {
        btnLanjut.setOnClickListener {
            Toast.makeText(this, "Melanjutkan Perjalanan", Toast.LENGTH_LONG).show()
            sendToService(ACTION_LANJUT)
        }

        btnMatikanFungsi.setOnClickListener{
            Toast.makeText(this,"Mematikan Fungsi Auto Announcement", Toast.LENGTH_LONG).show()
            sendToService(ACTION_MATIKAN_FUNGSI)
        }

        btnPindahHalte.setOnClickListener{
            Toast.makeText(this, "Pindah ke Halte Berikutnya", Toast.LENGTH_LONG).show()
            sendToService(ACTION_PINDAH_HALTE)
        }
    }


    private fun sendToService(action: String){
        val intent = Intent(RuteService.INTENT_ACTION_FROM_ACTIVITY)
        intent.putExtra("action_next", action)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        finish()
    }

    override fun onBackPressed() {

    }
}
