package com.idivisiontech.transporttracker;



import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.soten.libs.base.MessageResult;
import com.soten.libs.base.config.ConfigParser;
import com.soten.libs.hf.HFManager;
import com.soten.libs.hf.base.CMD;
import com.soten.libs.hf.base.ERROR;
import com.soten.libs.hf.impl.HF;
import com.soten.libs.hf.impl.HFMessageResult;
import com.soten.libs.hf.impl.HFModelListener;
import com.soten.libs.utils.StringUtils;
import java.util.Locale;

public class HFActivity extends Activity implements Handler.Callback, AdapterView.OnItemSelectedListener {
    private TextView mLog;
    private EditText et1, et2, et3, et4;
    private HFManager mManager;
    private HF hf;
    private Handler mHandler;
    private HFlListener hFlListener;
    private byte[] mCardId = new byte[4];
    byte keyMode = AUTHENTICATION_KEYA_MODE;
    public static final byte AUTHENTICATION_KEYA_MODE = 0x00;
    public static final byte AUTHENTICATION_KEYB_MODE = 0x04;
    public static final byte ANITICOLL_GRADE_FIRST = -109;



    private void changeLanguage(Locale locale) {
        if (!getSysLocale().equals(locale)) {
            Resources resources =getResources();
            Configuration config =resources.getConfiguration();
            DisplayMetrics dm=resources.getDisplayMetrics();
            config.locale= locale;
            resources.updateConfiguration(config,dm);
            recreate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hf);
        mLog = findViewById(R.id.log);
        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        et1 = findViewById(R.id.et1);
        et2 = findViewById(R.id.et2);
        et3 = findViewById(R.id.et3);
        et4 = findViewById(R.id.et4);
        Spinner spinner = findViewById(R.id.sp);
        spinner.setOnItemSelectedListener(this);
        mManager = HFManager.getInstance();
        hf = mManager.getModel();
        mHandler = new Handler(this);
        mManager.open(this);
        hFlListener = new HFlListener();
        mManager.register(hFlListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mManager.unregister(hFlListener);
        mManager.close(hf, this);
    }

    public void open(View view) {
        hf.openModule();
        Log.i("open","open");
    }

    public void close(View view) {
        hf.closeModule();
    }

    public void getCardID(View view) {
        hf.getCardID();
    }

    public void getVersion(View view) {
        hf.getVersion();
    }

    // 选卡
    public void select(View view) {
        hf.select(ANITICOLL_GRADE_FIRST, mCardId);
    }

    // 射频重启
    public void rfReset(View view) {
        hf.rfReset(0);
    }

    // 查询
    public void requestIDLECard() {
        hf.requestIDLECard();
    }

    //认证
    public void authentication(View view) {
        int id = 0;
        byte[] key = new byte[]{-1, -1, -1, -1, -1, -1};
        if (!et1.getText().toString().isEmpty() && !et2.getText().toString().isEmpty()) {
            id = Integer.valueOf(et1.getText().toString());
            byte[] input = StringUtils.toHexByteArray(et2.getText().toString());
            System.arraycopy(input, 0, key, 0, 6);
        }
        Log.d(ConfigParser.TAG, "authentication: keyMode = " + keyMode
                + " id = " + id
                + " key = " + StringUtils.toHexString(key));
        hf.authentication(keyMode,
                id,
                key);
    }

    //读块
    public void readBlock(View view) {
        int id = 2;
        if (!et3.getText().toString().isEmpty()) {
            id = Integer.valueOf(et3.getText().toString());
        }
        Log.d(ConfigParser.TAG, "readBlock: id = " + id);
        hf.readBlock(id);
    }

    //写块
    public void writeBlock(View view) {
        int id = 2;
        String data = "1234567890abcdef";
        if (!et3.getText().toString().isEmpty() && !et4.getText().toString().isEmpty()) {
            id = Integer.valueOf(et3.getText().toString());
            data = et4.getText().toString();
        }
        Log.d(ConfigParser.TAG, "writeBlock: id = " + id
                + " data = " + data);
        hf.writeBlock(id, data);
    }

    //数值操作
    public void operationValue(int blockId, int value) {
        hf.operationValue(blockId, value);
    }

    //读取 IIC 从机地址
    public void readIICAddress() {
        hf.readIICAddress();
    }

    //设置自动寻卡间隔时间
    public void setAutoSeekTime(View view) {
        hf.setAutoSeekTime((byte) 100);
    }

    //读取/恢复自动寻卡功能
    public void resumeAutoSeekTime(View view) {
        hf.SetAutoGetCardNum();
    }

    //设置串口波特率
    public void setSeialBaudrate(int baudrate) {
        hf.setSeialBaudrate(115200);
    }

    //获取串口波特率
    public void getSeialBaudrate(View view) {
        hf.getSeialBaudrate();
    }

    //检查射频范围内是否有卡存在
    public void requestAllCard(View view) {
        hf.requestAllCard();
    }

    //防冲突
    public void anticoll(View view) {
        hf.anticoll();
    }

    /**
     * @param msg A {@link Message Message} object
     * @return True if no further handling is desired
     */
    @Override
    public boolean handleMessage(Message msg) {
        HFMessageResult hfResult = (HFMessageResult) msg.obj;
        refreshUI(hfResult);
        return false;
    }

    private void refreshUI(HFMessageResult msg) {
        String Responses = "";
        if (msg.error == ERROR.SUCCESS) {
            switch (msg.cmd) {
                case CMD.GET_MODULE_VERSION:
                    Responses = msg.version;
                    break;
                case CMD.GET_CARD_ID:
                    mCardId = msg.cardId;
                    Responses = StringUtils.toHexString(msg.cardId);
                    break;
                case CMD._GET_BAUDRATE:
                    Responses = msg.data;
                    break;
                case CMD.READ_BLOCK:
                    String data = msg.data;
                    et4.setText(data);
                    break;
                case CMD.WARM_RESET:
                case CMD.CLOSE_MODULE:
                case CMD.AUTHENTICATION:
                case CMD.REQUEST:
                case CMD.SET_AUTO_SEEK_TIME:
                case CMD.RESUME_AUTO_SEEK_TIME:
                case CMD.SELECT:
                case CMD.ANTICOLL:
                case CMD.RF_RESET:
                case CMD.WRITE_BLOCK:
                    Responses = "Success";
                default:
            }
        } else {
            Responses = String.format("error: %s", ERROR.toString(msg.error));
        }
        if (mLog.getText() != null) {
            mLog.append(Responses + "\n");
            int offset = mLog.getLineCount() * mLog.getLineHeight();
            if (offset > mLog.getHeight()) {
                mLog.scrollTo(0, offset - mLog.getHeight());
            }
        } else {
            mLog.setText(String.format("%s\n", Responses));
        }
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String[] keymode = getResources().getStringArray(R.array.authentication_key_mode);
        if ("KEYA".equals(keymode[position])) {
            keyMode = AUTHENTICATION_KEYA_MODE;
        }else {
            keyMode = AUTHENTICATION_KEYB_MODE;
        }
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    private class HFlListener implements HFModelListener {

        @Override
        public void onReceive(MessageResult result) {
            if (result instanceof HFMessageResult) {
                HFMessageResult msg = (HFMessageResult) result;
                mHandler.obtainMessage(0x00, msg).sendToTarget();
            }
        }

        @Override
        public void onLostConnect(Exception e) {
        }
    }

    public Locale getSysLocale() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0有多语言设置获取顶部的语
            locale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            locale = Resources.getSystem().getConfiguration().locale;
        }
        return locale;
    }

}
