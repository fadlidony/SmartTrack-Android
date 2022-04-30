package com.idivisiontech.transporttracker;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.soten.libs.base.MessageResult;
import com.soten.libs.obd.OBDManager;
import com.soten.libs.obd.base.OBDMessageResult;
import com.soten.libs.obd.impl.OBDModelListener;
import com.soten.libs.obd.impl.OBD_EST527;
import com.soten.libs.utils.PowerManagerUtils;

public class OBD_EST527Activity extends AppCompatActivity implements OBDModelListener,
        Handler.Callback {

    private OBDManager mManager;
    private OBD_EST527 mOdbEst527;
    private TextView mLog;
    private Handler mHandler;
    private TextView[] mTexts = new TextView[16];
    private static final int MSG_OBDRT = 1000;
    private static final int MSG_ENGINE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_obd);
        mManager = OBDManager.getInstance();
        mOdbEst527 = mManager.getModel();
        mHandler = new Handler(this);
        mManager.open(this);

        mTexts[0] = findViewById(R.id.text1);
        mTexts[1] = findViewById(R.id.text2);
        mTexts[2] = findViewById(R.id.text3);
        mTexts[3] = findViewById(R.id.text4);
        mTexts[4] = findViewById(R.id.text5);
        mTexts[5] = findViewById(R.id.text6);
        mTexts[6] = findViewById(R.id.text7);
        mTexts[7] = findViewById(R.id.text8);
        mTexts[8] = findViewById(R.id.text9);
        mTexts[9] = findViewById(R.id.text10);
        mTexts[10] = findViewById(R.id.text11);
        mTexts[11] = findViewById(R.id.text12);
        mTexts[12] = findViewById(R.id.text13);
        mTexts[13] = findViewById(R.id.text14);
        mTexts[14] = findViewById(R.id.text15);
        mTexts[15] = findViewById(R.id.text16);
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManagerUtils.open(mPowerManager, 0x14); // OBD_POWER
        PowerManagerUtils.open(mPowerManager, 0x15); // OBD_RESET
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_ENGINE);
        mManager.close(mOdbEst527, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mManager.register(this);
        mHandler.sendEmptyMessage(MSG_ENGINE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mManager.unregister(this);
    }

    public void requestHabitsData(View view) {
        mOdbEst527.requestHabitsData();
    }

    public void readErrorCode(View view) {
        mOdbEst527.getErrorCode();
    }

    public void clearErrorCode(View view) {
        mOdbEst527.clearErrorCode();
    }

    public void readVIN(View view) {
        mOdbEst527.getVIN();
    }

    public void openAllRealTimeData(View view) {
        mOdbEst527.openAllRealTimeData();
    }

    public void closeAllRealTimeData(View view) {
        mOdbEst527.closeAllRealTimeData();
    }

    public void getEquipmentInfo(View view) {
        mOdbEst527.getEquipmentInfo();
    }

    public void getSupportProtocol(View view) {
        String[] supportProtocolId = mOdbEst527.getSupportProtocolIdForEST527();
        StringBuilder builder = new StringBuilder();
        for (String protocolId : supportProtocolId) {
            builder.append(protocolId);
            builder.append("\r\n");
        }
        mLog.append(builder);
    }

    @Override
    public void onReceive(MessageResult result) {
        if (result instanceof OBDMessageResult) {
            OBDMessageResult oResult = (OBDMessageResult) result;
            mHandler.obtainMessage(MSG_OBDRT, oResult).sendToTarget();
        }
    }

    @Override
    public void onLostConnect(Exception e) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_OBDRT:
                OBDMessageResult oResult = (OBDMessageResult) msg.obj;
                refreshUI(oResult.getRawResponce());
                break;
            case MSG_ENGINE:
                mOdbEst527.getEngineTime();
                mHandler.removeMessages(MSG_ENGINE);
                mHandler.sendEmptyMessageDelayed(MSG_ENGINE,3000);
                break;
        }
        return true;
    }

    private void refreshUI(String[] rawResponses) {
        if (rawResponses == null) {
            return;
        }
        Log.d("pbt","rawResponse : " + rawResponses[0]);
        if (rawResponses[0].contains("031")) {
              String value = rawResponses[0].split("=")[1];
              if (value != null) {
                  int time = Integer.parseInt(value);
                  int min = time / 60;
                  int sec = time - min * 60;
                  int hour = min / 60;
                  min = min - hour * 60;
                  Log.d("pbt", "value : " + value);
                  String text = "";
                  if (hour != 0){
                      text = hour + "小时";
                  }
                  if (min != 0){
                      text = text + min + "分";
                  }
                  if (sec != 0){
                      text = text + sec + "秒";
                  }

                  mTexts[15].setText("Engine runtime："+text);
              }
        }
        for (String rawRespons : rawResponses) {
            String[] values = rawRespons.split(",");
            switch (values[0]) {
                case "$OBD-RT":
                    for (int i = mTexts.length - 1; i >= 9; i--) {
                        if (mTexts[i].getVisibility() == View.GONE) {
                            mTexts[i].setVisibility(View.VISIBLE);
                        }
                    }
                    mTexts[0].setText(String.format("Battery voltage:%sV", values[1]));
                    mTexts[1].setText(String.format("Engine speed:%sRpm", values[2]));
                    mTexts[2].setText(String.format("Driving speed:%sKm/h", values[3]));
                    mTexts[3].setText(String.format("Throttle opening:%s%%", Double.valueOf(values[4])));
                    mTexts[4].setText(String.format("Engine load:%s%%", Double.valueOf(values[5])));
                    mTexts[5].setText(String.format("Coolant temperature:%s℃", Double.valueOf(values[6])));
                    if (values[3].equals("0")) {
                        mTexts[6].setText(String.format("Instantaneous fuel consumption:%sL/h", Double.valueOf(values[7])));
                    } else {
                        mTexts[6].setText(String.format("Instantaneous fuel consumption:%sL/100km", Double.valueOf(values[7])));
                    }
                    mTexts[7].setText(String.format("Average fuel consumption:%sL/100km", Double.valueOf(values[8])));
                    mTexts[8].setText(String.format("Current mileage:%skm", values[9]));
                    mTexts[9].setText(String.format("Total mileage:%skm", values[10]));
                    mTexts[10].setText(String.format("Fuel consumption this time:%sL", Double.valueOf(values[11])));
                    mTexts[11].setText(String.format("Cumulative fuel consumption:%sL", Double.valueOf(values[12])));
                    mTexts[12].setText(String.format("Number of current DTCs:%s", values[13]));
                    mTexts[13].setText(String.format("The number of rapid accelerations:%sTimes", values[14]));
                    mTexts[14].setText(String.format("The number of rapid decelerations:%sTimes", values[15]));
                    break;
                case "$OBD-HBT":
                    mTexts[0].setText(String.format("Total number of ignitions:%s次", values[1]));
                    mTexts[1].setText(String.format("Cumulative travel time:%sh", values[2]));
                    mTexts[2].setText(String.format("Accumulated idle time:%sh", values[3]));
                    mTexts[3].setText(String.format("Average warm-up time:%ss", values[4]));
                    mTexts[4].setText(String.format("Average speed:%skm/h", values[5]));
                    mTexts[5].setText(String.format("Historical maximum speed:%skm/h", values[6]));
                    mTexts[6].setText(String.format("Historical maximum Rpm:%srpm", values[7]));
                    mTexts[7].setText(String.format("Accumulated rapid acceleration times:%sTimes", values[8]));
                    mTexts[8].setText(String.format("Accumulated rapid deceleration times:%sTimes", values[9]));
                    mTexts[9].setVisibility(View.GONE);
                    mTexts[10].setVisibility(View.GONE);
                    mTexts[11].setVisibility(View.GONE);
                    mTexts[12].setVisibility(View.GONE);
                    mTexts[13].setVisibility(View.GONE);
                    mTexts[14].setVisibility(View.GONE);
                    break;
                case "$OBD-TT":
                    mTexts[0].setText(String.format("Duration of the hot car:%sS", values[1]));
                    mTexts[1].setText(String.format("This idling time:%sMin", values[2]));
                    mTexts[2].setText(String.format("Duration of the trip:%sMin", values[3]));
                    mTexts[3].setText(String.format("Current mileage:%sKm", values[4]));
                    mTexts[4].setText(String.format("This idle fuel consumption:%sL", values[5]));
                    mTexts[5].setText(String.format("Fuel consumption this time:%sL", values[6]));
                    mTexts[6].setText(String.format("Current maximum Rpm:%sRPM", values[7]));
                    mTexts[7].setText(String.format("Current maximum speed:%sKm/h", values[8]));
                    mTexts[8].setText(String.format("The number of rapid accelerations:%sTimes", values[9]));
                    if (mTexts[9].getVisibility() == View.GONE) {
                        mTexts[9].setVisibility(View.VISIBLE);
                    }
                    mTexts[9].setText(String.format("The number of rapid decelerations:%sTimes", values[10]));
                    mTexts[10].setVisibility(View.GONE);
                    mTexts[11].setVisibility(View.GONE);
                    mTexts[12].setVisibility(View.GONE);
                    mTexts[13].setVisibility(View.GONE);
                    mTexts[14].setVisibility(View.GONE);
                    break;
            }

        }
    }

    int number;


}
