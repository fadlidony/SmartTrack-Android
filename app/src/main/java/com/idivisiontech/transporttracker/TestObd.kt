package com.idivisiontech.transporttracker

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.soten.libs.base.MessageResult
import com.soten.libs.obd.OBDManager
import com.soten.libs.obd.base.OBDMessageResult
import com.soten.libs.obd.impl.OBDModelListener
import com.soten.libs.obd.impl.OBD_EST527
import com.soten.libs.utils.PowerManagerUtils

class TestObd : AppCompatActivity(), Handler.Callback, OBDModelListener {

    private lateinit var mOdbEst527: OBD_EST527
    private lateinit var mManager: OBDManager
    private lateinit var mHandler: Handler

    private val MSG_OBDRT = 1000
    private val MSG_ENGINE = 2000
    private val TAG = "TESOBDTAG"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_obd)
        Log.d(TAG,"INIT")
        mManager = OBDManager.getInstance()
        mOdbEst527 = mManager.getModel()
        mHandler = Handler(this)
        mManager.open(this)
        val mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        PowerManagerUtils.open(mPowerManager, 0x14) // OBD_POWER

        PowerManagerUtils.open(mPowerManager, 0x15) // OBD_RESET
        Log.d(TAG,"INIT ONCREATE BERES")

    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_OBDRT -> {
                val oResult = msg.obj as OBDMessageResult
                refreshUI(oResult.rawResponce)
                Log.d(TAG,oResult.rawResponce.toString())
                //tes.text = oResult.rawResponce.toString()
            }
            MSG_ENGINE -> {
                mOdbEst527.getEngineTime()
                mHandler.removeMessages(MSG_ENGINE)
                mHandler.sendEmptyMessageDelayed(MSG_ENGINE, 3000)
                //Log.d(TAG,msg.obj.toString())
                //tes.text = msg.obj.toString()
            }
        }
        return true
    }

    private fun refreshUI(rawResponses: Array<String>?) {
        if (rawResponses == null) {
            return
        }
        Log.d("pbt", "rawResponse : " + rawResponses.get(0))
        //tes.text = rawResponses.get(0)
        if (rawResponses.get(0).contains("031")) {
            val value: String = rawResponses.get(0).split("=").toTypedArray().get(1)
            if (value != null) {
                val time = value.toInt()
                var min = time / 60
                val sec = time - min * 60
                val hour = min / 60
                min = min - hour * 60
                Log.d("pbt", "value : $value")
                var text = ""
                if (hour != 0) {
                    text = hour.toString() + "小时"
                }
                if (min != 0) {
                    text = text + min + "分"
                }
                if (sec != 0) {
                    text = text + sec + "秒"
                }
                Log.d(TAG,"Engine runtime：$text")
                //tes.text = "Engine runtime：$text"
            }
        }

        for (rawRespons in rawResponses) {
            val values = rawRespons.split(",").toTypedArray()
            Log.d(TAG,values.toString())
            values.get(0).let {
                when(it){
                    "\$OBD-RT" -> {
                        Log.d(TAG, String.format("Battery voltage:%sV", values[1]))
                        Log.d(TAG, String.format("Engine speed:%sRpm", values[2]))
                        Log.d(TAG, String.format("Driving speed:%sKm/h", values[3]))
                        /*mTexts.get(2).setText(String.format("Driving speed:%sKm/h", values[3]))
                        mTexts.get(3).setText(String.format("Throttle opening:%s%%", java.lang.Double.valueOf(values[4])))
                        mTexts.get(4).setText(String.format("Engine load:%s%%", java.lang.Double.valueOf(values[5])))
                        mTexts.get(5).setText(String.format("Coolant temperature:%s℃", java.lang.Double.valueOf(values[6])))
                        if (values[3] == "0") {
                            mTexts.get(6).setText(String.format("Instantaneous fuel consumption:%sL/h", java.lang.Double.valueOf(values[7])))
                        } else {
                            mTexts.get(6).setText(String.format("Instantaneous fuel consumption:%sL/100km", java.lang.Double.valueOf(values[7])))
                        }
                        mTexts.get(7).setText(String.format("Average fuel consumption:%sL/100km", java.lang.Double.valueOf(values[8])))
                        mTexts.get(8).setText(String.format("Current mileage:%skm", values[9]))
                        mTexts.get(9).setText(String.format("Total mileage:%skm", values[10]))
                        mTexts.get(10).setText(String.format("Fuel consumption this time:%sL", java.lang.Double.valueOf(values[11])))
                        mTexts.get(11).setText(String.format("Cumulative fuel consumption:%sL", java.lang.Double.valueOf(values[12])))
                        mTexts.get(12).setText(String.format("Number of current DTCs:%s", values[13]))
                        mTexts.get(13).setText(String.format("The number of rapid accelerations:%sTimes", values[14]))
                        mTexts.get(14).setText(String.format("The number of rapid decelerations:%sTimes", values[15]))*/
                    }

                    else -> {
                        Log.d(TAG,it)
                    }
                }

            }
        }
    }

    override fun onReceive(result: MessageResult?) {
        if (result is OBDMessageResult) {
            val oResult = result as OBDMessageResult
            mHandler.obtainMessage(MSG_OBDRT, oResult).sendToTarget()
        }
    }

    override fun onLostConnect(p0: Exception?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeMessages(MSG_ENGINE)
        mManager.close(mOdbEst527, this)
    }

    override fun onResume() {
        super.onResume()
        mManager.register(this)
        mHandler.sendEmptyMessage(MSG_ENGINE)
    }

    override fun onPause() {
        super.onPause()
        mManager.unregister(this)
    }
}
