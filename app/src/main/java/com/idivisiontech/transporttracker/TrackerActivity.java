/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.idivisiontech.transporttracker;

import android.Manifest;
import android.app.ActivityManager;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationListener;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import androidx.annotation.Nullable;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.idivisiontech.transporttracker.Data.FirebaseData.PanicMoment;
import com.idivisiontech.transporttracker.Data.Gmap.MatrixDistance;
import com.idivisiontech.transporttracker.Data.Halte;
import com.idivisiontech.transporttracker.Data.Pivot;
import com.idivisiontech.transporttracker.Data.Rute;
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper;
import com.idivisiontech.transporttracker.Helpers.UsbTetherHelper;
import com.idivisiontech.transporttracker.RaspberryClient.RaspberryRepository;
import com.idivisiontech.transporttracker.RaspberryClient.data.IndexResult;
import com.idivisiontech.transporttracker.RaspberryClient.data.RunningTextUpdateResult;
import com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo.Bus;
import com.idivisiontech.transporttracker.ServerOperator.Data.LogoutResult;
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile;
import com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo.Route;
import com.idivisiontech.transporttracker.ServerOperator.Data.settingInfo.SettingData;
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository;
import com.idivisiontech.transporttracker.ServerOperator.SessionHelper;
import com.idivisiontech.transporttracker.Services.RuteService;
import com.idivisiontech.transporttracker.ViewModel.TrackerViewModel;
import com.idivisiontech.transporttracker.dialog.CallDialog;
import com.idivisiontech.transporttracker.dialog.PanicSignalDialog;
import com.idivisiontech.transporttracker.dialog.SpeedLimitExceededDialog;
import com.idivisiontech.transporttracker.preferences.SettingPreferences;
import com.squareup.picasso.Picasso;

import org.abtollc.api.SipProfileState;
import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.AbtoPhoneCfg;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRegistrationListener;
import org.abtollc.utils.codec.Codec;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackerActivity extends AppCompatActivity implements View.OnClickListener, OnRegistrationListener, OnInitializeListener {

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int REQUEST_CODE_USB_TETHER = 101;
    private static String[] PERMISSIONS_REQUIRED = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    private static final int STATUS_OK = 0;
    private static final int STATUS_NOT_OK = 1;
    private SharedPreferences mPrefs;

    private Button mStartButton;
    private Button buttonLogout;
    private EditText mTransportIdEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private TextView server_status;
    private TextView tvSpeed;
    private SwitchCompat mSwitch;
    private Snackbar mSnackbarPermissions;
    private Snackbar mSnackbarGps;
    private Button announcement, greeting, iklan, sensor, runningtext, call_help, panic_button;
    private LocationManager locationManager;
    private LocationListener listener;
    String username;
    private boolean state_announcement = true;
    private boolean state_greeting = true;
    private Intent mRuteService = null;
    private DatabaseReference mDatabase = null;
    private DatabaseReference mRuteIdReference = null;
    private SharedPreferences sharedPref = null;
    private PreferenceHelper preferenceHelper = null;
    private ValueEventListener ruteValueChangeListener = null;
    private DatabaseReference mRuteReference = null;
    private TextView tvRute = null;
    private Rute ruteSekarang = null;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = null;
    private TextView tvJarakHalte;
    private TextView tvUnitJarak;
    private final String TAG = TrackerActivity.class.getSimpleName();
    private TextView tvHalteName;
    private boolean goingToFlag = false;
    private boolean arrivingFlag = false;
    private SessionHelper sessionHelper;
    private ImageView profileImageView;
    private TrackerViewModel trackerViewModel;
    private TextView driverNameTv;
    private ServerApiRepository serverApiRepository;
    private TextView tvBusName;
    private Integer refresh_step = 0;
    private Button btnRefresh;
    private String ipRaspi;
    private RaspberryRepository raspberryRepository;
    private String session_key;

    private Bus busDetail = null;

    //ZDK
    private String z_hostname = null;
    private Integer max_speed = 80;
    private String z_user = "1313";
    private String z_pass = "1313";
    private String z_call = null;
    public CallDialog callDialog = null;
    public CallDialog outCallDialog = null;
    private TextView zoiperStatusTv;
    final String RUNNING_TEXT_FRONT = "running_text_front";
    final String RUNNING_TEXT_INDOOR = "running_text_indoor";
    final String RUNNING_TEXT_BACK = "running_text_back";

    public static String START_VIDEO_CALL = "START_VIDEO_CALL";

    //Speed Limit
    private SpeedLimitExceededDialog speedLimitExceededDialog = null;

    //PANIC MOMENT
    private PanicSignalDialog panicSignalDialog = null;

    //abtoPhone
    AbtoPhone abtoPhone;
    int accExpire;

    //    private CallEventsHandler outCallStatusListener = new CallEventsHandler(){
//        @Override
//        public void onCallStatusChanged(com.zoiper.zdk.Call call, CallStatus status) {
//
//            if(status.lineStatus().equals(CallLineStatus.Terminated)){
//                try {
//                    runOnUiThread(() -> {
//                        try {
//                            ((TextView) outCallDialog.getWindow().findViewById(R.id.status_text)).setText("Telepon Dimatikan");
//                            outCallDialog.dismiss();
//                            stopDialingSound();
//
//                        } catch (Exception e) {
//                            Log.d(TAG,"ON TERMINATED LINE EXCEPTION");
//                            e.printStackTrace();
//                        }
//                    });
//                    trackerViewModel.endCallReq(serverApiRepository);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//
//            }else if(status.lineStatus().equals(CallLineStatus.Ringing)){
//                try{
//                    runOnUiThread(() -> {
//                        ((TextView) outCallDialog.getWindow().findViewById(R.id.status_text)).setText("Ringing Operator....");
//                    });
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//                playDialingSound();
//
//            } else if(status.lineStatus().equals(CallLineStatus.Active)){
//                try{
//                    runOnUiThread(() -> {
//                        ((TextView) outCallDialog.getWindow().findViewById(R.id.status_text)).setText("Operator Sedang berbicara....");
//                    });
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//                stopDialingSound();
//            }
//        }
//
//
//        @Override
//        public void onCallExtendedError(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallNetworkStatistics(com.zoiper.zdk.Call call, NetworkStatistics networkStatistics) {
//
//        }
//
//        @Override
//        public void onCallNetworkQualityLevel(com.zoiper.zdk.Call call, int callChannel, int qualityLevel) {
//
//        }
//
//        @Override
//        public void onCallSecurityLevelChanged(com.zoiper.zdk.Call call, CallMediaChannel channel, CallSecurityLevel level) {
//
//        }
//
//        @Override
//        public void onCallDTMFResult(com.zoiper.zdk.Call call, Result result) {
//
//        }
//
//        @Override
//        public void onCallTransferSucceeded(com.zoiper.zdk.Call call) {
//
//        }
//
//        @Override
//        public void onCallTransferFailure(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallTransferStarted(com.zoiper.zdk.Call call, String name, String number, String uri) {
//
//        }
//
//        @Override
//        public void onCallZrtpFailed(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallZrtpSuccess(com.zoiper.zdk.Call call, String zidHex, int knownPeer, int cacheMismatch, int peerKnowsUs, ZRTPSASEncoding sasEncoding, String sas, ZRTPHashAlgorithm hash, ZRTPCipherAlgorithm cipher, ZRTPAuthTag authTag, ZRTPKeyAgreement keyAgreement) {
//
//        }
//
//        @Override
//        public void onCallZrtpSecondaryError(com.zoiper.zdk.Call call, int callChannel, ExtendedError error) {
//
//        }
//    };
    private TextView tvBusDepanName;
    private TextView tvBusDepanJarak;
    private LinearLayout lyBusDepan;
    private final int button_off = Color.parseColor("#A22020");
    private final int button_on = Color.parseColor("#226125");
    private TextView tvSpeedMax;
    private Button btnSpeakerOn;
    private Button btnSpeakerOff;
    private TextView voiceOverDetikanTv;

    private Thread voiceOverDetikanThread = null;
    private Boolean voiceOveThreadrActive = false;
    private TextView jamTv;
    private TextView tanggalTv;
    private MediaPlayer dialingSound = null;
    private MediaPlayer ringtoneSound = null;
    private DialogFragment dialog = null;


    /**
     * Configures UI elements, and starts validation if inputs have previously been entered.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ANRWatchDog().start();
        setContentView(R.layout.content_main2);
        final String ZoiPer = "com.zoiper.android.app";



        initViews();
        initClickEvent();
        callDialog = new CallDialog(this);
        outCallDialog = new CallDialog(this);

        Log.d(TAG,"TRACKER ACTIVITY ONCREATE");
        Intent i = getIntent();
        username = i.getStringExtra("username");

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mPrefs = getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE);

        zoiperStatusTv =(TextView) findViewById(R.id.zoiperStatus);
        server_status=(TextView) findViewById(R.id.serverStatus);
        tvSpeed=(TextView) findViewById(R.id.speed);

        server_status.setText("Connected");

        // in onCreate method
        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        preferenceHelper = new PreferenceHelper(sharedPref);
        String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        //mDatabase.child("bus-rute").child(android_id).child("rute_id").get;
        mRuteIdReference = FirebaseDatabase.getInstance().getReference().child("bus-rute").child(android_id);
        //Log.d("ANDROID-ID",android_id);

        session_key = preferenceHelper.get(SessionHelper.SESSION_KEY);
        if(session_key == ""){
            stopService(new Intent(this, TrackerService.class));
            RuteService.Companion.stopService(this);
            Intent backToLogin = new Intent(this,LoginActivity.class);
            Toast.makeText(this, "Anda belum Login", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(backToLogin);
        }

        sessionHelper = new SessionHelper(this,session_key);
        serverApiRepository = new ServerApiRepository(this);
        serverApiRepository.updateSessionKey(session_key);
        sessionHelper.setServerRepository(serverApiRepository);

        //      trackerViewModel = ViewModelProviders.of(this).get(TrackerViewModel.class);

        trackerViewModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(TrackerViewModel.class);



        allViewModelObserver();







        //mRuteService = new Intent(TrackerActivity.this, RuteService.class);
        //startService(mRuteService);

        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key == "matrix_distance_next_halte"){
                    Gson gson = new Gson();
                    MatrixDistance matrixDistance = gson.fromJson(sharedPreferences.getString(key,"").toString(),MatrixDistance.class);
                    String kec = matrixDistance.getRows().get(0).getElements().get(0).getDistance().getText();
                    tvJarakHalte.setText(kec);
                }
            }
        };

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("gps:", String.valueOf(location.getLongitude()));

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };



        announcement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state_announcement){
                    announcement.setBackgroundColor(button_off);
                    state_announcement=false;
                    SettingPreferences.Companion.getInstance(getApplicationContext()).disableAnnouncement();
                    disableRT();
                }else{
                    announcement.setBackgroundColor(button_on);
                    state_announcement=true;
                    SettingPreferences.Companion.getInstance(getApplicationContext()).enableAnnouncement();
                    enableRT();

                }

            }
        });


        call_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCall(z_call,false);
//                createCallHelp();
                /*Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.zoiper.zdk.android.demo", "com.zoiper.zdk.android.demo.MainActivity"));
                intent.putExtra("EXTRA_HOSTNAME","103.226.49.73:6354");
                intent.putExtra("EXTRA_USER","1313");
                intent.putExtra("EXTRA_PASS","1313");
                intent.putExtra("EXTRA_CALL","1314");*/
                //startActivity(intent);
            }
        });


        panic_button.setOnClickListener(this);




        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmStop();
            }
        });

        iklan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackerActivity.this, IklanActivity.class);
                intent.putExtra(IklanActivity.EXTRA_IP,UsbTetherHelper.Companion.getInstance().getIpAddress(UsbTetherHelper.VIA_USB));
                startActivity(intent);
            }
        });

        runningtext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackerActivity.this, RunningtextActivity.class);
                startActivity(intent);
            }
        });

        sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackerActivity.this, SensorActivity.class);
                startActivity(intent);
            }
        });

        jamTv = (TextView) findViewById(R.id.jam);
        tanggalTv = (TextView) findViewById(R.id.tanggal);

        String pola="EEEE, dd-MM-yyyy";
        Thread t = new Thread(){
            @Override
            public void run(){
                try{

                    while (!isInterrupted()){
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    long date = System.currentTimeMillis();
                                    SimpleDateFormat waktu = new SimpleDateFormat("hh:mm:ss");
                                    String waktustring = waktu.format(date);
                                    Date tanggalDanWaktu = new Date();
                                    String Tanggal = tampilkanTanggalDanWaktu(tanggalDanWaktu, pola, null);
                                    tanggalTv.setText(Tanggal);
                                    jamTv.setText(waktustring);
                                }catch(Exception e){

                                }
                            }
                        });

                    }
                }catch (InterruptedException e){

                }


            }
        };

        //t.start();


        if (isServiceRunning(TrackerService.class)) {
            // If service already running, simply update UI.
            checkLocationPermission();
            setTrackingStatus(R.string.tracking);
        } else {
            // First time running - check for inputs pre-populated from build.
            checkLocationPermission();
        }

        updateRaspiIp();
        initRuteService();
        initVoip();
        initSettings();
//        printCurrentRegistrationStatus();

    }

    public void disableRT(){
        SettingPreferences.Companion.getInstance(getApplicationContext()).disableRunningText(RUNNING_TEXT_BACK);
        SettingPreferences.Companion.getInstance(getApplicationContext()).disableRunningText(RUNNING_TEXT_FRONT);
        SettingPreferences.Companion.getInstance(getApplicationContext()).disableRunningText(RUNNING_TEXT_INDOOR);
        raspberryRepository.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,"stop","0").enqueue(new Callback<RunningTextUpdateResult>(){
            @Override
            public void onResponse(Call<RunningTextUpdateResult> call, Response<RunningTextUpdateResult> response) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
            @Override
            public void onFailure(Call<RunningTextUpdateResult> call, Throwable t) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
        });
        raspberryRepository.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE,"stop","0").enqueue(new Callback<RunningTextUpdateResult>(){
            @Override
            public void onResponse(Call<RunningTextUpdateResult> call, Response<RunningTextUpdateResult> response) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
            @Override
            public void onFailure(Call<RunningTextUpdateResult> call, Throwable t) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
        });
        raspberryRepository.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,"stop","0").enqueue(new Callback<RunningTextUpdateResult>(){
            @Override
            public void onResponse(Call<RunningTextUpdateResult> call, Response<RunningTextUpdateResult> response) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
            @Override
            public void onFailure(Call<RunningTextUpdateResult> call, Throwable t) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
        });
    }

    public void enableRT(){
        SettingPreferences.Companion.getInstance(getApplicationContext()).enableRunningText(RUNNING_TEXT_BACK);
        SettingPreferences.Companion.getInstance(getApplicationContext()).enableRunningText(RUNNING_TEXT_FRONT);
        SettingPreferences.Companion.getInstance(getApplicationContext()).enableRunningText(RUNNING_TEXT_INDOOR);
        restoreRT(RaspberryRepository.RUNNING_TEXT_FRONT_TYPE,SettingPreferences.RUTE_NAME_RUNNING_TEXT_FRONT,SettingPreferences.RUTE_KODE_RUNNING_TEXT_FRONT);
        restoreRT(RaspberryRepository.RUNNING_TEXT_BACK_TYPE,SettingPreferences.RUTE_NAME_RUNNING_TEXT_BACK,SettingPreferences.RUTE_KODE_RUNNING_TEXT_BACK);
        restoreRT(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,SettingPreferences.RUTE_NAME_RUNNING_TEXT_INDOOR,SettingPreferences.RUTE_KODE_RUNNING_TEXT_INDOOR);
    }

    public void restoreRT(String type, String name,String kode){
        String ruteName = SettingPreferences.Companion.getInstance(getApplicationContext()).getNameRute(name);
        String ruteKode = "";
        if (type.equals(String.valueOf(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE))){
            ruteKode ="";
        }else {
            ruteKode = SettingPreferences.Companion.getInstance(getApplicationContext()).getNameRute(kode);
        }


        raspberryRepository.services.updateRunningText(type,ruteName,ruteKode).enqueue(new Callback<RunningTextUpdateResult>(){
            @Override
            public void onResponse(Call<RunningTextUpdateResult> call, Response<RunningTextUpdateResult> response) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
            @Override
            public void onFailure(Call<RunningTextUpdateResult> call, Throwable t) {
                SettingPreferences.Companion.getInstance(getApplicationContext()).setRunningTextOnDuty(false);
            }
        });
    }

    private void initVoip() {
        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();
        abtoPhone.setInitializeListener(this);

        AbtoPhoneCfg config = abtoPhone.getConfig();
        config.setCodecPriority(Codec.OPUS , (short) 250);
/*        config.setCodecPriority(Codec.GSM , (short) 10);
        config.setCodecPriority(Codec.H263 , (short) 102);
        config.setCodecPriority(Codec.ILBC , (short) 200);*/

        /*config.setCodecPriority(Codec.G729, (short) 0);
        config.setCodecPriority(Codec.GSM, (short) 0);
        config.setCodecPriority(Codec.PCMU, (short) 200);
        config.setCodecPriority(Codec.PCMA, (short) 100);

        config.setCodecPriority(Codec.H264, (short) 220);
        config.setCodecPriority(Codec.H263_1998, (short) 210);*/


        config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.UDP);
        config.setKeepAliveInterval(AbtoPhoneCfg.SignalingTransportType.UDP, 30);
        //config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.TCP);
        //config.setTLSVerifyServer(true);

        config.setSipPort(0);
        config.setDTMFmode(AbtoPhoneCfg.DTMF_MODE.INFO);

        //config.setSTUNEnabled(true);
        //config.setSTUNServer("stun.l.google.com:19302");
        config.setUseSRTP(false);
        config.setEnableAutoSendRtpVideo(false);
        config.setUserAgent(abtoPhone.version());
        config.setHangupTimeout(3000);

        config.setSTUNEnabled(false);
        config.setSipPort(32323);

        config.setLogLevel(5, true);
        config.setMwiEnabled(true);



        //Log.setLogLevel(5);
        //Log.setUseFile(true);

        // Start initializing - !app has invoke this method only once!

        abtoPhone.initialize(true);//start service in 'sticky' mode - when app removed from recent service will be restarted automatically
        //abtoPhone.initializeForeground(null);//start service in foreground mode
    }

    private void initSettings(){
        SettingPreferences settings = SettingPreferences.Companion.getInstance(getApplicationContext());
        if(settings.getAnnouncementSetting() == true){
            announcement.setBackgroundColor(button_on);
            state_announcement=true;
        }else{
            announcement.setBackgroundColor(button_off);
            state_announcement=false;
        }

        setVoiceOverOn(settings.isVoiceOverOn());

    }

//    private void createCallHelp() {
//        try {
//            currentOutCall = account.createCall(z_call, true, false);
//        }catch(Exception e){
//            Toast.makeText(getApplicationContext(),"Zoiper Belum Aktif",Toast.LENGTH_SHORT).show();
//        }
//        if( currentOutCall != null) {
//            AudioModeUtils.setAudioMode(this,AudioManager.MODE_IN_COMMUNICATION);
//            AudioManager audioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
//            audioManager.setMode(AudioManager.MODE_NORMAL);
//            //audioManager.setSpeakerphoneOn(true);
//            trackerViewModel.sendCallReq(serverApiRepository);
//            currentOutCall.setCallStatusListener(outCallStatusListener);
//                outCallDialog.show();
//                /*((TextView) outCallDialog.getWindow().findViewById(R.id.status_text)).setText("Memanggil Operator");*/
//                outCallDialog.setStatus("Memanggil Operator");
//                playDialingSound();
//                outCallDialog.setOnClickListener((View v) -> {
//                    stopDialingSound();
//                    outCallDialog.dismiss();
//                    stopDialingSound();
//                    currentOutCall.hangUp();
//                });
//            /*((Button) outCallDialog.getWindow().findViewById(R.id.btn_hangup_dialog)).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                }
//            });*/
//        }
//    }

    private void initRuteService(){
        if(!isServiceRunning((RuteService.class))){
            RuteService.Companion.startService(this, session_key);
        }


    }

//    private void initZoiperService(){
//        if(!isServiceRunning((ZoiperService.class))){
//            ZoiperService.Companion.startService(this,account);
//        }
//    }

    private void initViews() {
        announcement = (Button) findViewById(R.id.announcement);
        sensor = (Button) findViewById(R.id.sensor);
        iklan = (Button) findViewById(R.id.iklan);
        call_help = (Button) findViewById(R.id.call_help);
        panic_button = (Button) findViewById(R.id.panic_button);
        runningtext = (Button) findViewById(R.id.runningtext);
        buttonLogout = (Button) findViewById(R.id.buttonLogout);
        tvRute = (TextView) findViewById(R.id.rute);
        tvJarakHalte = (TextView) findViewById(R.id.jarakHalte);
        tvUnitJarak = (TextView) findViewById(R.id.unitJarak);
        tvHalteName = (TextView) findViewById(R.id.halte);
        profileImageView = (ImageView) findViewById(R.id.profile_iv);
        driverNameTv = findViewById(R.id.driver_name_tv);
        tvBusName = (TextView) findViewById(R.id.tv_bus_name);
        btnRefresh = (Button) findViewById(R.id.cctv_btn);

        tvBusDepanName = (TextView) findViewById(R.id.bus_depan_name_tv);

        tvBusDepanJarak = (TextView) findViewById(R.id.bus_depan_jarak_tv);

        lyBusDepan = (LinearLayout) findViewById(R.id.ly_bus_depan);

        tvSpeedMax = (TextView) findViewById(R.id.tvMaxSpeed);

        btnSpeakerOn = (Button) findViewById(R.id.btnSpeakerOn);
        btnSpeakerOff = (Button) findViewById(R.id.btnSpeakerOff);
        voiceOverDetikanTv = (TextView) findViewById(R.id.voiceOverDetikan);
    }

    private void initClickEvent(){
        btnRefresh.setOnClickListener(this);

        btnSpeakerOn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                setVoiceOverOn(true);
            }
        });

        btnSpeakerOff.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                setVoiceOverOn(false);
            }
        });
    }

    private void setVoiceOverOn(boolean on) {
        SettingPreferences.Companion.getInstance(getApplicationContext()).setVoiceOfferOn(on);
        if(on){
            btnSpeakerOn.setVisibility(View.INVISIBLE);
            btnSpeakerOff.setVisibility(View.VISIBLE);
            raspberryRepository.services.voiceOverOn().enqueue(new Callback<IndexResult>(){

                @Override
                public void onResponse(Call<IndexResult> call, Response<IndexResult> response) {
                    Log.d(TAG," VOICE ON OK");
                }

                @Override
                public void onFailure(Call<IndexResult> call, Throwable t) {
                    Log.d(TAG," VOICE ON NOT OK");
                }
            });
            startVoiceOverDetikan();
        }else{
            btnSpeakerOn.setVisibility(View.VISIBLE);
            btnSpeakerOff.setVisibility(View.INVISIBLE);
            raspberryRepository.services.voiceOverOff().enqueue(new Callback<IndexResult>(){

                @Override
                public void onResponse(Call<IndexResult> call, Response<IndexResult> response) {
                    Log.d(TAG," VOICE OFF OK");
                }

                @Override
                public void onFailure(Call<IndexResult> call, Throwable t) {
                    Log.d(TAG," VOICE OFF NOT OK");
                }
            });
            voiceOverDetikanTv.setText("PA System Inactive");
            stopVoiceOverDetikan();
        }
    }

    private void stopVoiceOverDetikan() {

        voiceOveThreadrActive = false;
    }

    private void startVoiceOverDetikan() {
        voiceOveThreadrActive = true;
        if(voiceOverDetikanThread == null){
            voiceOverDetikanThread = new Thread(){
                @Override
                public void run() {
                    super.run();
                    Integer i = 60;
                    while(i >= 0){
                        if(voiceOveThreadrActive) {
                            final Integer detik = i;
                            Log.d(TAG, "DETIKAN " + i);
                            try {
                                Thread.sleep(1000);
                                runOnUiThread(() -> {
                                    voiceOverDetikanTv.setText(getString(R.string.voice_over_detikan_text, detik));
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        i--;
                    }
                    runOnUiThread(() -> {
                        setVoiceOverOn(false);
                    });
                    Thread.currentThread().interrupt();
                }
            };
        }

        voiceOverDetikanThread.start();
    }

    private void allViewModelObserver() {
        trackerViewModel.updateProfile(serverApiRepository);

        trackerViewModel.getProfile().observe(this, new Observer<Profile>() {
            @Override
            public void onChanged(@Nullable Profile profile) {

                setBiodata(profile);
                checkRefreshStep();

            }
        });

        trackerViewModel.getRute().observe(this, new Observer<Route>() {
            @Override
            public void onChanged(Route route) {
                checkRefreshStep();
                if(route == null){
                    Toast.makeText(TrackerActivity.this,"Ada masalah saat mengunduh Rute", Toast.LENGTH_LONG)
                            .show();
                }else{
                    tvRute.setText(route.getName());
                }
            }
        });

        trackerViewModel.getBusInfo().observe(this, new Observer<Bus>() {


            @Override
            public void onChanged(Bus bus) {
                checkRefreshStep();
                if(bus == null){
                    Toast.makeText(TrackerActivity.this,"Ada masalah saat mengunduh info Bus", Toast.LENGTH_LONG)
                            .show();
                }else{
                    tvBusName.setText(bus.getName());
                    busDetail = bus;
                }
            }
        });

        //panic Moment Observer
        trackerViewModel.getPanicResult().observe(this, new Observer<PanicMoment>() {

            @Override
            public void onChanged(PanicMoment panicMoment) {
                if(panicMoment != null){
                    SettingPreferences.Companion.getInstance(getApplicationContext()).setPanicMomentId(panicMoment.getFb_id());
                    if(panicSignalDialog == null){
                        panicSignalDialog = new PanicSignalDialog(TrackerActivity.this);
                    }

                    if(!panicSignalDialog.isShowing()){
                        panicSignalDialog.show();
                    }
                    panicSignalDialog.setStatusText(panicMoment.getState());
                    if(panicMoment.getState().equals("PENDING")){
                        panic_button.setClickable(false);
                        Toast.makeText(TrackerActivity.this,"Sinyal Panic Telah Dikirim ke Operator", Toast.LENGTH_LONG)
                                .show();
                    }else if(panicMoment.getState().equals("ACCEPT")){
                        SettingPreferences.Companion.getInstance(getApplicationContext()).deletePanicMomentId();
                        panic_button.setClickable(true);
                        Toast.makeText(TrackerActivity.this,"Respon dari operator = " + panicMoment.getState(), Toast.LENGTH_LONG)
                                .show();
                        panicSignalDialog.dismiss();
                    }else if(panicMoment.getState().equals("EXIT")){
                        logoutFromServer();
                        stopLocationService();
                        RuteService.Companion.stopService(TrackerActivity.this);
                    }
                }
            }
        });

        trackerViewModel.getSettingData().observe(this, new Observer<ArrayList<SettingData>>(){

            @Override
            public void onChanged(ArrayList<SettingData> settingData) {
                if(settingData.size() > 0){
                    for(SettingData item : settingData){
                        if(item.getKey().equals("voip_host")){
                            z_hostname = item.getValue();

                        }else if(item.getKey().equals("km_max_speed")){
                            max_speed = Integer.valueOf(item.getValue());
                            updateMaxSpeedText();
                        }else if(item.getKey().equals("op_number")){
                            z_call = item.getValue();
//                            z_call = "555";
                            SettingPreferences.Companion.getInstance(getApplicationContext()).setOperatorNumber(z_call);
                        }
                    }
                }
            }
        });

        //FROM Rute Service

        trackerViewModel.getRuteName().observe(this, new Observer<String>() {

            @Override
            public void onChanged(String s) {
                if(s != null){
                    tvRute.setText(s);
                }
            }
        });

        trackerViewModel.getCurrentSpeed().observe(this, new Observer<String>(){

            @Override
            public void onChanged(String s) {
                if(s != null){
                    //tvSpeed.setText(s);
                }
            }
        });

        trackerViewModel.createSpeedLimitValueListener();
        trackerViewModel.getMaxSpeed().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(integer > 0 ){
                    max_speed = integer;
                    updateMaxSpeedText();
                }
            }
        });




    }

    private void updateMaxSpeedText() {
        tvSpeedMax.setText(getString(R.string.max_speed_text,max_speed.toString()));
    }


    private void checkRefreshStep() {
        refresh_step++;
        if(refresh_step == 2){
            //btnRefresh.setText("REFRESH");
            //btnRefresh.setClickable(true);
        }
    }

    private void setBiodata(Profile profile) {
        if(profile != null) {
            Log.d(TAG, "PROFILE AVATAR : " + profile.getAvatar());
            Picasso.get().load(profile.getAvatar()).fit().into(profileImageView);
            this.driverNameTv.setText(profile.getName());
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


//    void configure_button() {
//        // first check for permissions
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
//                        , 10);
//            }
//            return;
//        }
//        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
//        pilih_rute.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //noinspection MissingPermission
//                if (ActivityCompat.checkSelfPermission(TrackerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(TrackerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//                locationManager.requestLocationUpdates("gps", 5000, 0, listener);
//            }
//        });
//    }







    public static void openApp(Context context, String appName, String packageName) {
        if (isAppInstalled(context, packageName))
            if (isAppEnabled(context, packageName))
                context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
            else Toast.makeText(context, appName + " app is not enabled.", Toast.LENGTH_SHORT).show();
        else Toast.makeText(context, appName + " app is not installed.", Toast.LENGTH_SHORT).show();
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    private static boolean isAppEnabled(Context context, String packageName) {
        boolean appStatus = false;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packageName, 0);
            if (ai != null) {
                appStatus = ai.enabled;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appStatus;
    }


    private void setTrackingStatus(int status) {
        TextView status1 = (TextView) findViewById(R.id.trackingStatus);
        status1.setText(getString(status));
        Log.i("status:",status1.getText().toString());
        if (status1.getText().toString().equals("Tracking ✓")){
            server_status.setText("Connected");
        }else {
            server_status.setText("Not Connected");
        }

    }

    private BroadcastReceiver ruteServiceObdReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Float speed = intent.getFloatExtra(RuteService.EXTRA_SPEED_OBD,0.0F);
            tvSpeed.setText(speed.toString());
            if(speed >= 0.5){
                if(speed > max_speed){
                    speedLimitExceeded(Float.valueOf(speed));
                }else{
                    if(speedLimitExceededDialog != null){
                        if(speedLimitExceededDialog.isShowing()){
                            runOnUiThread(() -> {
                                speedLimitExceededDialog.dismiss();
                                trackerViewModel.isSpeedLimitExceeded().setValue(false);
                            });
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver ruteServiceHalteAndJarakInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String halte_name = intent.getStringExtra(RuteService.EXTRA_HALTE_SELANJUTNYA);
            Double jarak = intent.getDoubleExtra(RuteService.EXTRA_JARAK_KE_HALTE_SELANJUTNYA,0.0);
            String jarak_str = String.format("%.2f",jarak);
            tvJarakHalte.setText(jarak_str);
            Log.d(TAG,"BR halte and jarak " + halte_name + " jarak = " + jarak_str);
            tvHalteName.setText(halte_name);
            tvHalteName.setSelected(true);
        }
    };

    private BroadcastReceiver ruteServiceJarakBusDepanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String busDepanName = intent.getStringExtra(RuteService.EXTRA_BUS_DEPAN_NAME);
            Double busDepanJarak = intent.getDoubleExtra(RuteService.EXTRA_JARAK_BUS_DEPAN,0.0);
            if(busDepanName == null){
                tvBusDepanJarak.setText("-");
                tvBusDepanName.setText("-");
            }else{
                //lyBusDepan.setVisibility(View.VISIBLE);
                tvBusDepanJarak.setText(busDepanJarak.toString() + " M");
                tvBusDepanName.setText(busDepanName);
            }
        }
    };

    private BroadcastReceiver ruteServiceDateTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String date = intent.getStringExtra(RuteService.EXTRA_DATE);
                String time = intent.getStringExtra(RuteService.EXTRA_TIME);
                tanggalTv.setText(date);
                jamTv.setText(time);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
    /**
     * Receives status messages from the tracking service.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String halte_name = String.valueOf(intent.getStringExtra(TrackerService.NEXT_HALTE_NAME));

            Double jarak = Double.valueOf(intent.getIntExtra(TrackerService.NEXT_HALTE_JARAK,0));
            String unit = "M";
            if(jarak > 500){
                jarak = jarak / 1000;
                unit = "KM";
            }

            DecimalFormat df = new DecimalFormat("#.##");
            setTrackingStatus(intent.getIntExtra(getString(R.string.status), 0));
            //speed.setText(intent.getStringExtra("speed"));
            //tvJarakHalte.setText(String.valueOf(df.format(jarak)) + unit);
            //tvHalteName.setText(halte_name);
            String tracking_status = intent.getStringExtra(TrackerService.CURRENT_TRACKER_STATUS);
            String state = intent.getStringExtra(TrackerService.STATE);

            /*if(tracking_status == TrackerService.TRACKER_STATUS_ARRIVING && state == TrackerService.STATE_NOT_IN_RADIUS){
                Toast.makeText(context, "Bus akan sampai halte "+halte_name+" sebentar lagi", Toast.LENGTH_LONG).show();
            }else if(tracking_status == TrackerService.TRACKER_STATUS_GOING_TO && state == TrackerService.STATE_IN_RADIUS){
                Toast.makeText(context,"Bus dalam perjalanan menuju Halte " + halte_name, Toast.LENGTH_LONG).show();
            }else{
                Log.d(TAG, "onReceive: Bus Dalam Travelling");
            }*/

            if(tracking_status == TrackerService.TRACKER_STATUS_ARRIVING && arrivingFlag == false){
                //Toast.makeText(context, "Bus akan sampai halte "+halte_name+" sebentar lagi", Toast.LENGTH_LONG).show();
                //goingToFlag = false;
            }else if(tracking_status == TrackerService.TRACKER_STATUS_GOING_TO && goingToFlag == false){
                //Toast.makeText(context,"Bus dalam perjalanan menuju Halte " + halte_name, Toast.LENGTH_LONG).show();
                goingToFlag = true;
            }else {
                //arrivingFlag = true;
                //Log.d(TAG, "onReceive: Bus Dalam Travelling");
            }

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(TrackerService.STATUS_INTENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(ruteServiceBroadcastReceiver,
                new IntentFilter(RuteService.INTENT_FILER_NAME));
        LocalBroadcastManager.getInstance(this).registerReceiver(ruteServiceHalteAndJarakInfoReceiver,
                new IntentFilter(RuteService.INTENT_HALTE_JARAK_INFO));
        LocalBroadcastManager.getInstance(this).registerReceiver(ruteServiceJarakBusDepanReceiver,
                new IntentFilter(RuteService.INTENT_FILTER_JARAK_BUS_DEPAN));
        LocalBroadcastManager.getInstance(this).registerReceiver(ruteServiceDateTimeReceiver,
                new IntentFilter(RuteService.INTENT_FILTER_DATE));

        LocalBroadcastManager.getInstance(this).registerReceiver(ruteServiceObdReceiver,
                new IntentFilter(RuteService.INTENT_FILTER_OBD));

//        if (account != null) {
//            account.setStatusEventListener(accountEventsHandler);
//        }
//        printCurrentRegistrationStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ruteServiceBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ruteServiceHalteAndJarakInfoReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ruteServiceJarakBusDepanReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ruteServiceObdReceiver);
//        if (account != null) {
//            account.dropStatusEventListener(accountEventsHandler);
//        }
//        printCurrentRegistrationStatus();

    }

    private BroadcastReceiver ruteServiceBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String speed = intent.getStringExtra(RuteService.EXTRA_SPEED);
            String route_name = intent.getStringExtra(RuteService.EXTRA_ROUTE_NAME);
            String geolocation = intent.getStringExtra(RuteService.EXTRA_GEOLOCATION);
            trackerViewModel.getCurrentSpeed().postValue(speed);
            trackerViewModel.getRuteName().postValue(route_name);
            trackerViewModel.getCurrentGeoLocation().postValue(geolocation);
            if(SettingPreferences.Companion.getInstance(getApplicationContext()).getAnnouncementSetting()){
                announcement.setBackgroundColor(button_on);
                state_announcement=true;
            }else{
                announcement.setBackgroundColor(button_off);
                state_announcement=false;
            }

            Log.d(TAG,"BroadCastReceiver : " + speed);
        }
    };

    private void speedLimitExceeded(Float speed) {
        if(speedLimitExceededDialog == null){
            speedLimitExceededDialog = new SpeedLimitExceededDialog(this);
        }

        if(trackerViewModel.isSpeedLimitExceeded().getValue()){

        }else{
            trackerViewModel.isSpeedLimitExceeded().setValue(true);
            trackerViewModel.sendSpeedLimitLog(serverApiRepository, speed);
            runOnUiThread(() -> {
                speedLimitExceededDialog.setSpeed(max_speed.toString());
                speedLimitExceededDialog.show();
            });
        }
    }

    /**
     * First validation check - ensures that required inputs have been
     * entered, and if so, store them and runs the next check.
     */
    private void checkInputFields() {
        if (mTransportIdEditText.length() == 0 || mEmailEditText.length() == 0 ||
                mPasswordEditText.length() == 0) {
            Toast.makeText(TrackerActivity.this, R.string.missing_inputs, Toast.LENGTH_SHORT).show();
        } else {
            // Store values.
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(getString(R.string.transport_id), mTransportIdEditText.getText().toString());
            editor.putString(getString(R.string.email), mEmailEditText.getText().toString());
            editor.putString(getString(R.string.password), mPasswordEditText.getText().toString());
            editor.apply();
            // Validate permissions.
            checkLocationPermission();
            mSwitch.setEnabled(true);
        }
    }

    /**
     * Second validation check - ensures the app has location permissions, and
     * if not, requests them, otherwise runs the next check.
     */
    private void checkLocationPermission() {
        int locationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int storagePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (locationPermission != PackageManager.PERMISSION_GRANTED
                || storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST);
        } else {
            checkGpsEnabled();
        }
    }

    /**
     * Third and final validation check - ensures GPS is enabled, and if not, prompts to
     * enable it, otherwise all checks pass so start the location tracking service.
     */
    private void checkGpsEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            reportGpsError();
        } else {
            resolveGpsError();
            startLocationService();
        }
    }

    /**
     * Callback for location permission request - if successful, run the GPS check.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            // We request storage perms as well as location perms, but don't care
            // about the storage perms - it's just for debugging.
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        reportPermissionsError();
                    } else {
                        resolvePermissionsError();
                        checkGpsEnabled();
                    }
                }
            }
        }
    }

    private void startLocationService() {
        // Before we start the service, confirm that we have extra power usage privileges.
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        Intent intent = new Intent();
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        //startService(new Intent(this, TrackerService.class));
    }

    private void stopLocationService() {
        //stopService(new Intent(this, TrackerService.class));
        RuteService.Companion.stopService(this);
        Intent intent = new Intent(TrackerActivity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        /*getMenuInflater().inflate(R.menu.main_activity, menu);

        // Get the action view used in your toggleservice item
        final MenuItem toggle = menu.findItem(R.id.menu_switch);
        mSwitch = (SwitchCompat) toggle.getActionView().findViewById(R.id.switchInActionBar);
        mSwitch.setEnabled(mTransportIdEditText.length() > 0 && mEmailEditText.length() > 0 &&
                mPasswordEditText.length() > 0);
        mSwitch.setChecked(mStartButton.getVisibility() != View.VISIBLE);
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((SwitchCompat) v).isChecked()) {
                    checkInputFields();
                } else {
                    confirmStop();
                }
            }
        });*/
        return super.onCreateOptionsMenu(menu);
    }

    private void confirmStop() {
        new AlertDialog.Builder(this)
                .setMessage("Apakah anda yakin ingin keluar dan menghentikan service GPS?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        logoutFromServer();
                        stopLocationService();
                        RuteService.Companion.stopService(TrackerActivity.this);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void logoutFromServer() {
        serverApiRepository.services.logout().enqueue(new Callback<LogoutResult>() {
            @Override
            public void onResponse(Call<LogoutResult> call, Response<LogoutResult> response) {
                notifLogout(response);
                finish();
            }

            @Override
            public void onFailure(Call<LogoutResult> call, Throwable t) {

            }
        });
    }

    private void notifLogout(Response<LogoutResult> response) {
        //Toast.makeText(this,"Berhasil Logout, lama mengemudi = " + response.body().getDriving_time(),Toast.LENGTH_LONG);
    }

    private void reportPermissionsError() {
        if (mSwitch != null) {
            mSwitch.setChecked(false);
        }
        Snackbar snackbar = Snackbar
                .make(
                        findViewById(R.id.rootView),
                        getString(R.string.location_permission_required),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.enable, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(android.provider.Settings
                                .ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        /*TextView textView = (TextView) sbView.findViewById(
                android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);*/
        snackbar.show();
    }

    /*@Override
    public void onStart() {
        super.onStart();
        ValueEventListener chosenRuteListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //dataSnapshot.getValue();
                //BusRute busRute = dataSnapshot.getValue(BusRute.class);
                Log.d("TRACKERACTIVITY","ID RUTE TERPILIH = " + dataSnapshot.child("rute_id").getValue());

                Log.i("INFO","CHANGED VALUE");
                Object data = dataSnapshot.child("rute_id").getValue();
                if(data != null) {
                    String rute_id = data.toString();
                    Log.d(TAG, "onDataChange: RUTE_ID_BARU " + rute_id);
                    updateRuteTerpilih(Integer.valueOf(rute_id));
                }else{
                //    Toast.makeText(TrackerActivity.this, "Maaf, Android ini belum terdaftar pada server", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mRuteIdReference.addValueEventListener(chosenRuteListener);
    }*/

    private void updateRuteTerpilih(int id) {
        preferenceHelper.save("id_rute_sekarang",id);
        mRuteReference = FirebaseDatabase.getInstance().getReference().child("rute").child(String.valueOf(id));
        if(ruteValueChangeListener != null){
            mRuteReference.removeEventListener(ruteValueChangeListener);
        }

        ruteValueChangeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //try{
                String name = dataSnapshot.child("name").getValue().toString();
                String created_at = dataSnapshot.child("created_at").getValue().toString();
                String updated_at = dataSnapshot.child("updated_at").getValue().toString();
                Integer id = Integer.valueOf(dataSnapshot.child("id").getValue().toString());
                ArrayList<Halte> haltes = new ArrayList<Halte>();

                for(DataSnapshot halteSnapShot : dataSnapshot.child("halte").getChildren()){
                    String arrived_greeting = halteSnapShot.child("arrived_greeting").getValue().toString();
                    String arriving_announcement = halteSnapShot.child("arriving_announcement").getValue().toString();
                    String created_at_halte = halteSnapShot.child("created_at").getValue().toString();
                    String going_to_announcement = halteSnapShot.child("going_to_announcement").getValue().toString();
                    Integer id_halte = Integer.valueOf(halteSnapShot.child("id").getValue().toString());
                    String lang = (halteSnapShot.child("lang").getValue().toString());
                    String lat = (halteSnapShot.child("lat").getValue().toString());
                    String name_halte = halteSnapShot.child("name").getValue().toString();

                    DataSnapshot pivotSnapShot = halteSnapShot.child("pivot");
                    Integer pivot_halte_id = Integer.valueOf(pivotSnapShot.child("halte_id").getValue().toString());
                    Integer pivot_route_id = Integer.valueOf(pivotSnapShot.child("route_id").getValue().toString());
                    Integer pivot_order = Integer.valueOf(pivotSnapShot.child("order").getValue().toString());

                    Pivot pivot = new Pivot(
                            pivot_halte_id,
                            pivot_order,
                            pivot_route_id
                    );

                    String updated_at_halte = halteSnapShot.child("updated_at").getValue().toString();
                    Halte halte = new Halte(arrived_greeting,
                            arriving_announcement,
                            created_at_halte,
                            going_to_announcement,
                            id_halte,
                            lang,
                            lat,
                            name_halte,
                            pivot,
                            updated_at_halte
                    );
                    haltes.add(halte);

                }

                Rute rute = new Rute(
                        created_at,
                        haltes,
                        id,
                        name,
                        updated_at
                );
                Gson gson = new Gson();
                String json = gson.toJson(rute);
                preferenceHelper.save("rute_sekarang",json);
                ruteSekarang = rute;
                Log.d("RUTE-TERPILIH",rute.getName());
                tvRute.setText(rute.getName());
                Log.d("RUTE_TERPILIH",dataSnapshot.child("name").getValue().toString());
                //}catch(Exception e){
                //  e.printStackTrace();
                //}
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mRuteReference.addValueEventListener(ruteValueChangeListener);

    }

    private void resolvePermissionsError() {
        if (mSnackbarPermissions != null) {
            mSnackbarPermissions.dismiss();
            mSnackbarPermissions = null;
        }
    }

    private void reportGpsError() {
        if (mSwitch != null) {
            mSwitch.setChecked(false);
        }
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.rootView), getString(R.string
                        .gps_required), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.enable, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        /*TextView textView = (TextView) sbView.findViewById(android.support.design.R.id
                .snackbar_text);
        textView.setTextColor(Color.YELLOW);*/
        snackbar.show();

    }

    private void resolveGpsError() {
        if (mSnackbarGps != null) {
            mSnackbarGps.dismiss();
            mSnackbarGps = null;
        }
    }


    private String checkNetwork() {
        boolean wifiDataAvailable = false;
        boolean mobileDataAvailable = false;
        ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = conManager.getAllNetworkInfo();
        for (NetworkInfo netInfo : networkInfo) {
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
                if (netInfo.isConnected())
                    wifiDataAvailable = true;
            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                if (netInfo.isConnected())
                    mobileDataAvailable = true;
        }

        if (wifiDataAvailable==true){
            return "Wi-Fi";
        }else if(mobileDataAvailable==true){
            return "Mobile";
        }else{
            return "Not Connected";
        }
    }


    protected static String tampilkanTanggalDanWaktu(Date tanggalDanWaktu,
                                                     String pola, Locale lokal) {
        String tanggalStr = null;
        SimpleDateFormat formatter = null;
        if (lokal == null) {
            formatter = new SimpleDateFormat(pola);
        } else {
            formatter = new SimpleDateFormat(pola, lokal);
        }

        tanggalStr = formatter.format(tanggalDanWaktu);
        return tanggalStr;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.panic_button){
            new AlertDialog.Builder(this)
                    .setTitle("PANIC BUTTON")
                    .setMessage("Kirim Sinyal Panik ke Operator?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YA, KIRIM!", new Dialog.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            kirimSinyalPanic();
                        }
                    })
                    .setNegativeButton("TIDAK",null)
                    .show();

        }else if (v.getId() == R.id.cctv_btn){
            /*Log.d(TAG,"BUTTON-CLICK : REFRESH");
            Button btn = (Button) v;
            btn.setClickable(false);
            btn.setText("Refreshing..");
            refresh_step = 0;
            trackerViewModel.updateProfile(serverApiRepository);
            updateRaspiIp();*/
            Intent intent = new Intent(this, CctvViewerActivity.class);
            startActivity(intent);

        }
    }

    private void updateRaspiIp(){
        ipRaspi = "192.168.1.254";
        if(ipRaspi == null){
            showDialogToEnableUsbTether();
            Toast.makeText(this,"Media Player (TV) Tidak terhubung (1)",Toast.LENGTH_LONG).show();
        }else if(ipRaspi.length() > 256){
            showDialogToEnableUsbTether();
            Toast.makeText(this,"Media Player (TV) Tidak terhubung (2)",Toast.LENGTH_LONG).show();
        }else{
            //raspberryRepository = RaspberryRepository.Companion.getInstance(ipRaspi);
            //updateRunningText();
        }
        raspberryRepository = RaspberryRepository.Companion.getInstance();
    }

    private void updateRunningText() {
        SettingPreferences settingPreferences = SettingPreferences.Companion.getInstance(this);
        //settingPreferences.
        updateIndoorRunningText();
    }

    private void updateIndoorRunningText() {

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                while(ruteSekarang == null){
                    //wait
                }
                raspberryRepository.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,ruteSekarang.getName(),"");
            }
        };
        if(SettingPreferences.Companion.getInstance(this).isRunningTextEnabled(SettingPreferences.RUNNING_TEXT_INDOOR)) {
            thread.start();
        }else{
            raspberryRepository.services.updateRunningText(RaspberryRepository.RUNNING_TEXT_INDOOR_TYPE,"stop","");
        }
    }

    private void showDialogToEnableUsbTether() {
        new AlertDialog.Builder(this)
                .setTitle("MEDIA PLAYER BELUM TERHUBUNG")
                .setMessage("Hubungkan media player via USB Tethering. klik 'PERGI KE PENGATURAN' untuk menyalakan USB Tethering ")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("PERGI KE PENGATURAN", new Dialog.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                        startActivityForResult(intent, REQUEST_CODE_USB_TETHER);
                    }
                })
                .setNegativeButton("TUTUP",null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 101){
            updateRaspiIp();
        }
    }


    private void kirimSinyalPanic() {

        trackerViewModel.sendPanicSignal(serverApiRepository);
    }

    //ZOIPER THINGS


//    @Override
//    public void onZoiperLoaded() {
//        Log.d(TAG,"onZoiperLoaded");
//        zdkContext = getZdkContext();
//        Thread loadBusInfo = new Thread(){
//            @Override
//            public void run() {
//                super.run();
//                while ( busDetail == null || z_hostname == null || z_call == null){
//                    //infinite loop til bussdetail and z_hostname is initialized
//                }
//                z_user = busDetail.getNo_voip();
//                z_pass = busDetail.getNo_voip();
//                runOnUiThread(() -> {
//                    createAccount();
//                    registerUser();
//                    //initZoiperService();
//                });
//            }
//        };
//        if(account == null) loadBusInfo.start();
//
//
//        /*if(z_hostname == null || z_user == null || z_pass == null){
//            Toast.makeText(this,"ERROR",Toast.LENGTH_LONG).show();
//            //finish();
//        }else {*/
//
//
//        //}
//
//    }

//    private void createAccount() {
//        String hostname = z_hostname;
//        String username = z_user;
//        String password = z_pass;
//
//        if (hostname == null || username == null || password == null) {
//            Log.d(TAG,"ADA YANG KOSONG");
//            return;
//        }
//
//        if(account != null){
//            Toast.makeText(this, "Account already created.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Log.d(TAG,"ZOIPER AKUN : " + hostname + "," + username + "," + password);
//        Log.d(TAG,"TIDAK KOSONG");
//
//        AccountProvider accountProvider = zdkContext.accountProvider();
//
//        account = accountProvider.createUserAccount();
//
//        // Set listeners on the account
//        account.setStatusEventListener(accountEventsHandler);
//
//        // Account name - not to be confused with username
//        account.accountName(username);
//
//        // Configurations
//        account.mediaCodecs(getAudioCodecs());
//        account.configuration(createAccountConfig(accountProvider, hostname, username, password));
//
//        printStatus("Created");
//    }
//
//    private void registerUser() {
//        if (account.registrationStatus() != AccountStatus.Registered) {
//            Result createUserResult = account.createUser();
//            Result registerAccountResult = account.registerAccount();
//            Log.d(TAG,"RESULT : " + createUserResult.text() + " AND " + registerAccountResult.text());
//
//            zdkContext.accountProvider().setAsDefaultAccount(account);
//
//
//            //account.setStatusEventListener(this);
//            //startCallActivity();
//        }
//
//        printCurrentRegistrationStatus();
//    }
//
//    private void printCurrentRegistrationStatus() {
//        if (account != null) {
//            AccountStatus accountStatus = account.registrationStatus();
//
//            if (accountStatus != null) {
//                printStatus(accountStatus.toString());
//
//            }
//        }
//    }
//
//    private void printStatus(String status) {
//        /*if (tvStatus != null) {
//            tvStatus.setText(status);
//        }*/
//        Log.d(TAG,"STATUS ZOIPER = " + status);
//        zoiperStatusTv.setText(status);
//    }
//
//    @NonNull
//    private List<AudioVideoCodecs> getAudioCodecs() {
//        List<AudioVideoCodecs> codecs = new ArrayList<>();
////        codecs.add(AudioVideoCodecs.OPUS_FULL);
//        codecs.add(AudioVideoCodecs.OPUS_WIDE);
////        codecs.add(AudioVideoCodecs.OPUS_NARROW);
///*        codecs.add(AudioVideoCodecs.OPUS_SUPER);
//        codecs.add(AudioVideoCodecs.GSM);
//        codecs.add(AudioVideoCodecs.PCMA);
//        codecs.add(AudioVideoCodecs.PCMU);*/
//        //codecs.add(AudioVideoCodecs.iLBC_20);
//        //codecs.add(AudioVideoCodecs.iLBC_30);
////        codecs.add(AudioVideoCodecs.vp8);
//        return codecs;
//    }
//
//    private AccountConfig createAccountConfig(AccountProvider ap,
//                                              String hostname,
//                                              String username,
//                                              String password){
//        final AccountConfig accountConfig = ap.createAccountConfiguration();
//
//        accountConfig.userName(username); //@
//        accountConfig.password(password); //@
//
//        accountConfig.type(ProtocolType.SIP); //@
//
//        accountConfig.sip(createSIPConfig(ap, hostname));
//
//        accountConfig.reregistrationTime(60); //@
//
//        return accountConfig;
//    }
//
//    private SIPConfig createSIPConfig(AccountProvider ap, String hostname){
//        final SIPConfig sipConfig = ap.createSIPConfiguration();
//
//        sipConfig.transport(TransportType.UDP); //@
//
//        sipConfig.domain(hostname); //@
//        sipConfig.rPort(RPortType.SignalingAndMedia); //@
//
//        sipConfig.enablePrivacy(Configuration.PRIVACY);
//        sipConfig.enablePreconditions(Configuration.PRECONDITIONS);
//        sipConfig.enableSRTP(Configuration.SRTP); // Works only with TLS!
//
//        if(Configuration.STUN){
//            sipConfig.stun(createStunConfig(ap));
//        }
//        if(Configuration.ZRTP){
//            sipConfig.zrtp(createZRTPConfig(ap));
//        }
//
//        sipConfig.rtcpFeedback(Configuration.RTCP_FEEDBACK
//                ? RTCPFeedbackType.Compatibility
//                : RTCPFeedbackType.Off);
//
//        return sipConfig;
//    }
//
//    private StunConfig createStunConfig(AccountProvider ap){
//        final StunConfig stunConfig = ap.createStunConfiguration();
//        stunConfig.stunEnabled(true);
//        stunConfig.stunServer("stun.zoiper.com");
//        stunConfig.stunPort(3478);
//        stunConfig.stunRefresh(30000);
//        return stunConfig;
//    }
//
//    private ZRTPConfig createZRTPConfig(AccountProvider ap) {
//        List<ZRTPHashAlgorithm> hashes = new ArrayList<>();
//        hashes.add(ZRTPHashAlgorithm.s384);
//        hashes.add(ZRTPHashAlgorithm.s256);
//
//        List<ZRTPCipherAlgorithm> ciphers = new ArrayList<>();
//        ciphers.add(ZRTPCipherAlgorithm.cipher_aes3);
//        ciphers.add(ZRTPCipherAlgorithm.cipher_aes2);
//        ciphers.add(ZRTPCipherAlgorithm.cipher_aes1);
//
//        List<ZRTPAuthTag> auths = new ArrayList<>();
//        auths.add(ZRTPAuthTag.hs80);
//        auths.add(ZRTPAuthTag.hs32);
//
//        List<ZRTPKeyAgreement> keyAgreements = new ArrayList<>();
//        keyAgreements.add(ZRTPKeyAgreement.dh3k);
//        keyAgreements.add(ZRTPKeyAgreement.dh2k);
//        keyAgreements.add(ZRTPKeyAgreement.ec38);
//        keyAgreements.add(ZRTPKeyAgreement.ec25);
//
//        List<ZRTPSASEncoding> sasEncodings = new ArrayList<>();
//        sasEncodings.add(ZRTPSASEncoding.sasb256);
//        sasEncodings.add(ZRTPSASEncoding.sasb32);
//
//        final ZRTPConfig zrtpConfig = ap.createZRTPConfiguration();
//
//        zrtpConfig.enableZRTP(true);
//        zrtpConfig.hash(hashes);
//        zrtpConfig.cipher(ciphers);
//        zrtpConfig.auth(auths);
//        zrtpConfig.keyAgreement(keyAgreements);
//        zrtpConfig.sasEncoding(sasEncodings);
//        zrtpConfig.cacheExpiry(-1); // No expiry
//
//        return zrtpConfig;
//    }
//
//    AccountEventsHandler accountEventsHandler = new AccountEventsHandler() {
//        @Override
//        public void onAccountStatusChanged(Account account, AccountStatus status, int statusCode) {
//            runOnUiThread(() -> {
//                printStatus(status.name());
//            });
//        }
//
//        @Override
//        public void onAccountRetryingRegistration(Account account, int isRetrying, int inSeconds) {
//
//        }
//
//        @Override
//        public void onAccountIncomingCall(Account account, com.zoiper.zdk.Call call) {
//
//            try{
//                Log.d(TAG,"CALL : " + call.calleeName());
//                //this.currentCall = call;
//                call.setCallStatusListener(callEventsHandler);
//                runOnUiThread(() -> {
//                    try{
//                        callDialog.show();
//                        ((Button) callDialog.getWindow().findViewById(R.id.btn_hangup_dialog)).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                currentCall.hangUp();
//                            }
//                        });
//                        ((TextView) callDialog.getWindow().findViewById(R.id.status_text)).setText("Operator sedang menelpon");
//                    } catch(Exception e){
//                        e.printStackTrace();
//                    }
//                });
//
//
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//
//            //runOnUiThread(() -> AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_IN_COMMUNICATION));
//            AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_IN_COMMUNICATION);
//            //playRingtone();
//            call.acceptCall();
//            call = account.getActiveCalls().get(0);
////            btnHangup.setVisibility(View.VISIBLE);
//            currentCall = call;
//        }
//
//        @Override
//        public void onAccountChatMessageReceived(Account account, String pPeer, String pContent) {
//
//        }
//
//        @Override
//        public void onAccountPushTokenReceived(Account account, String pushToken) {
//
//        }
//
//        @Override
//        public void onAccountExtendedError(Account account, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onAccountUserSipOutboundMissing(Account account) {
//
//        }
//
//        @Override
//        public void onAccountCallOwnershipChanged(Account account, com.zoiper.zdk.Call call, OwnershipChange action) {
//
//        }
//    };

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    /*
    @Override
    public void onAccountStatusChanged(Account account, AccountStatus status, int statusCode) {

    }

    @Override
    public void onAccountRetryingRegistration(Account account, int isRetrying, int inSeconds) {

    }

    @Override
    public void onAccountIncomingCall(Account account, com.zoiper.zdk.Call call) {

        //runOnUiThread(() -> onIncomingCall(call));
        //if(call.status().lineStatus().equals(CallLine))
       onIncomingCall(call);
        //onIncomingCall(call);
    }*/

//    CallEventsHandler callEventsHandler = new CallEventsHandler(){
//        @Override
//        public void onCallStatusChanged(com.zoiper.zdk.Call call, CallStatus status) {
//            Log.d(TAG,"CALL STATUS : " + status.lineStatus());
//            /*runOnUiThread(() -> {
//                try{
//                    callDialog.updateStatus("Operator sedang menelpon. STATUS " + status.lineStatus().toString());
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            });*/
//
//            if(status.lineStatus().equals(CallLineStatus.Terminated) || status.lineStatus().equals(CallLineStatus.NA)){
//                //runOnUiThread();
//                //runOnUiThread(() ->{
//                /*getParent().*/
//                try {
//                    runOnUiThread(() -> {
//                        try {
//                            ((TextView) callDialog.getWindow().findViewById(R.id.status_text)).setText("Telepon Dimatikan");
//                            callDialog.dismiss();
//                        } catch (Exception e) {
//                            Log.d(TAG,"ON TERMINATED LINE EXCEPTION");
//                            e.printStackTrace();
//                        }
//                    });
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//                //});
//                //btnHangup.setVisibility(View.GONE);
//                //runOnUiThread(() -> btnHangup.setVisibility(View.GONE));
//                    /*runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            btnHangup.setVisibility(View.GONE);
//                        }
//                    });*/
////                    Toast.makeText(getParent(),"CALL DITUUTP oleh OPERATOR", Toast.LENGTH_LONG).show();
//                //AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_NORMAL);
//                //runOnUiThread(() -> AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_NORMAL));
//                //call.dropAllEventListeners();
//            }
//        }
//
//
//        @Override
//        public void onCallExtendedError(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallNetworkStatistics(com.zoiper.zdk.Call call, NetworkStatistics networkStatistics) {
//
//        }
//
//        @Override
//        public void onCallNetworkQualityLevel(com.zoiper.zdk.Call call, int callChannel, int qualityLevel) {
//
//        }
//
//        @Override
//        public void onCallSecurityLevelChanged(com.zoiper.zdk.Call call, CallMediaChannel channel, CallSecurityLevel level) {
//
//        }
//
//        @Override
//        public void onCallDTMFResult(com.zoiper.zdk.Call call, Result result) {
//
//        }
//
//        @Override
//        public void onCallTransferSucceeded(com.zoiper.zdk.Call call) {
//
//        }
//
//        @Override
//        public void onCallTransferFailure(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallTransferStarted(com.zoiper.zdk.Call call, String name, String number, String uri) {
//
//        }
//
//        @Override
//        public void onCallZrtpFailed(com.zoiper.zdk.Call call, ExtendedError error) {
//
//        }
//
//        @Override
//        public void onCallZrtpSuccess(com.zoiper.zdk.Call call, String zidHex, int knownPeer, int cacheMismatch, int peerKnowsUs, ZRTPSASEncoding sasEncoding, String sas, ZRTPHashAlgorithm hash, ZRTPCipherAlgorithm cipher, ZRTPAuthTag authTag, ZRTPKeyAgreement keyAgreement) {
//
//        }
//
//        @Override
//        public void onCallZrtpSecondaryError(com.zoiper.zdk.Call call, int callChannel, ExtendedError error) {
//
//        }
//    };

    private void playRingtone(){
        if(ringtoneSound == null){
//            AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_IN_COMMUNICATION);
            ringtoneSound = MediaPlayer.create(getApplicationContext(), R.raw.incoming);
        }

        if(!ringtoneSound.isPlaying()){
            ringtoneSound.start();
        }
    }

    private void stopRingtone(){
        if(ringtoneSound != null){
            ringtoneSound.stop();
        }
    }

    private void playDialingSound(){
        if(dialingSound == null){
            dialingSound = MediaPlayer.create(getApplicationContext(), R.raw.dialing);
        }
        if(!dialingSound.isPlaying()){
            dialingSound.setLooping(true);
            Log.d(TAG, "playDialingSound played");
            dialingSound.start();
        }

    }

    private void stopDialingSound(){
        if(dialingSound != null){
            dialingSound.stop();
        }
    }

    @Override
    public void onRegistered(long accId) {
        if(dialog != null) dialog.dismiss();

        //Unsubscribe reg events
        abtoPhone.setRegistrationStateListener(null);
        Log.d(TAG,"voip onRegistered " + accId);
        setVoipStatus("REGISTERED");
    }

    @Override
    public void onUnRegistered(long l) {
        setVoipStatus("UNREGISTERED");
    }

    @Override
    public void onRegistrationFailed(long accId, int statusCode, String statusText) {
        if(dialog != null) dialog.dismiss();

//        AlertDialog.Builder fail = new AlertDialog.Builder(TrackerActivity.this);
//        fail.setTitle("Registration failed");
//        fail.setMessage(statusCode + " - " + statusText);
//        fail.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
        setVoipStatus("FAILED");
//        fail.show();
    }

    @Override
    public void onInitializeState(InitializeState state, String message) {
        switch (state) {
            case START:
                setVoipStatus("starting");
            case INFO:
            case WARNING: break;
            case FAIL:

                new AlertDialog.Builder(TrackerActivity.this)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dlg, int which) {
                                dlg.dismiss();

                            }
                        }).create().show();
                setVoipStatus("FAILED");
                break;
            case SUCCESS:
                //Detect is account registered.
                //If 'yes' - go directly to MainActivity
                //If 'no' - go to RegisterActivity
                setVoipStatus("Registering..");
                if(!isAccountRegistered()){
                    createVoipAccount();
                }
                break;

            default:
                break;
        }
    }

    public void setVoipStatus(String msg){
        zoiperStatusTv.setText(msg);
    }

    public void startCall(String no_hp, boolean bVideo)   {
        //Get phone number to dial
        String sipNumber = no_hp;
        if(TextUtils.isEmpty(sipNumber))  return;


        if(!sipNumber.contains("sip:") ) sipNumber = "sip:" + sipNumber;
        if(!sipNumber.contains("@") )   sipNumber += "@" + z_hostname;

        Intent intent = new Intent(this, ScreenAV.class);
        intent.putExtra(AbtoPhone.IS_INCOMING, false);//!
        intent.putExtra(AbtoPhone.REMOTE_CONTACT, sipNumber);
        intent.putExtra(START_VIDEO_CALL, bVideo);
        startActivity(intent);
    }

    private void createVoipAccount() {
        Thread loadBusInfo = new Thread(){
            @Override
            public void run() {
                super.run();
                while ( busDetail == null || z_hostname == null || z_call == null){
                    //infinite loop til bussdetail and z_hostname is initialized
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                z_user = busDetail.getNo_voip();
                z_pass = busDetail.getNo_voip();
                registerVoip();
            }
        };
        loadBusInfo.start();
        int accId = (int)abtoPhone.getCurrentAccountId();
        accExpire = abtoPhone.getConfig().getAccountExpire(accId);

    }

    private void registerVoip() {
        abtoPhone.setRegistrationStateListener(this);
        int regTimeout =  300;


        // Add account
        //abtoPhone.getConfig().setContactDetailsUri("");
        //abtoPhone.getConfig().setContactDetails(";token="+URLEncoder.encode("yyyy+xxx 111 <l>"));
        abtoPhone.getConfig().addAccount(z_hostname, null, z_user, z_pass, null, "", regTimeout, false);

        //Register
        try {
            abtoPhone.register();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean isAccountRegistered()
    {
        //Get current account
        long acc = abtoPhone.getCurrentAccountId();
        if ((acc == -1) || !abtoPhone.isActive()) return false;

        //Check accounts status (service keeps it registered)
        try {
            SipProfileState accState = abtoPhone.getSipProfileState(acc);
            if ((accState != null) && accState.isActive() && (accState.getStatusCode() == 200))
            {
                return true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }


//    private void onIncomingCall(com.zoiper.zdk.Call call) {
//        Log.d(TAG,"CALL : " + call.calleeName());
//        //this.currentCall = call;
//
//        call.setCallStatusListener(callEventsHandler);
//        //runOnUiThread(() -> AudioModeUtils.setAudioMode(getApplicationContext(), AudioManager.MODE_IN_COMMUNICATION));
//        call.acceptCall();
//        call = account.getActiveCalls().get(0);
//        this.currentCall = call;
//    }

    /*
    @Override
    public void onAccountChatMessageReceived(Account account, String pPeer, String pContent) {

    }

    @Override
    public void onAccountPushTokenReceived(Account account, String pushToken) {

    }

    @Override
    public void onAccountExtendedError(Account account, ExtendedError error) {

    }

    @Override
    public void onAccountUserSipOutboundMissing(Account account) {

    }

    @Override
    public void onAccountCallOwnershipChanged(Account account, com.zoiper.zdk.Call call, OwnershipChange action) {

    }*/


    //CALL EVENT


    private class Configuration {

        private static final boolean PRIVACY = false;

        private static final boolean PRECONDITIONS = false;

        private static final boolean STUN = false;

        private static final boolean SRTP = false;

        private static final boolean ZRTP = false;

        private static final boolean VIDEO_FMTP = false;

        private static final boolean RTCP_FEEDBACK = false;
    }
}
