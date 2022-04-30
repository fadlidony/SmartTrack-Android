package com.idivisiontech.transporttracker;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.idivisiontech.transporttracker.Services.RuteService;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class IklanActivity extends AppCompatActivity {

    public static final String EXTRA_IP = "extra_ip";
    private Button back;
    private WebView webView;
    private final String TAG = "IklanActivity";
    private String raspi_player = "http://192.168.1.254:8080";
    private String[] args = {"cat", "/proc/net/arp"};
    private String raspi_hw_prefix = "b8:27:eb";
    private TextView tvStatus;
    //private String raspi_hw_prefix = "98:74:da";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iklan);

        /*back = (Button) findViewById(R.id.back);*/
        webView = (WebView) findViewById(R.id.web_view);

        webView.getSettings().setJavaScriptEnabled(true);
        tvStatus = (TextView) findViewById(R.id.status_text_iklan);


        loadWeb();
        /*String arp_result = toRead();
        Log.d(TAG,"RESULT ARP: "+ arp_result);
        String[] result = arp_result.split("\n");


        if(result.length <= 1){
            this.setStatus("Tidak ada perangkat media yang terhubung");
            new AlertDialog.Builder(this)
                    .setMessage("Media Player Tidak Ditemukan, pastikan USB Tethering dinyalakan")
                    .setTitle("ERROR")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create().show();
        }else {
            Boolean flag = false;
            for (Integer i = 1; i < result.length; i++) {
                String str = result[i].toLowerCase();
                if (str.contains(raspi_hw_prefix)) {
                    flag = true;
                    String[] parts = str.split("    ");
                    Log.d(TAG, "IP RASPI = " + parts[0]);
                    raspi_player = "http://" + parts[0] + ":8080";
                    this.setStatus("Perangkat ditemukan : " + raspi_player);
                    loadWeb();
                    break;
                }
            }
            if(flag){
                this.setStatus("Perangkat Media ditemukan!");
            }else{
                this.setStatus("Perangkat Media tidak ditemukan!");
            }
        }*/
        //enableUsbTether();

        /*back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IklanActivity.this, TrackerActivity.class);
                startActivity(intent);
                finish();
            }
        });*/
    }

    private void setStatus(String str){
        tvStatus.setText(getString(R.string.iklan_status,str));
    }

    private String toRead()
    {
        ProcessBuilder cmd;
        String result="";

        try{
            cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            while(in.read(re) != -1){
                System.out.println(new String(re));
                result = result + new String(re);
            }
            in.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
        return result;
    }

    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
            Log.d(TAG, "Can Write Settings: " + retVal);
            if(retVal){
                Toast.makeText(this, "Write allowed :-)", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "Write not allowed :-(", Toast.LENGTH_LONG).show();
                FragmentManager fm = getFragmentManager();
                //PopupWritePermission dialogFragment = new PopupWritePermission();
                //dialogFragment.show(fm, getString(R.string.popup_writesettings_title));
            }
        }
        return retVal;
    }

    private void enableUsbTether() {
        Object systemService = getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d(TAG,"IKLAN USB SEARCHING");
        for (Method method : systemService.getClass().getDeclaredMethods()) {
            Log.d(TAG, "IKLAN USB " + method.getName());
            if (method.getName().equals("tether")) {
                try {
                    method.invoke(systemService, "usb0");
                } catch (IllegalArgumentException e) {
                    Log.d(TAG,"IKLAN USB Argument E " + e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.d(TAG,"IKLAN USB ILEGAL E " + e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.d(TAG,"IKLAN USB Invo E " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadWeb() {
        Intent intent = new Intent(RuteService.INTENT_ACTION_FROM_ACTIVITY);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        webView.clearCache(true);
        webView.loadUrl(raspi_player);
    }


}
