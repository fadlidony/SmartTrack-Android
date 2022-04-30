package com.idivisiontech.transporttracker;


import android.content.Context;
import android.graphics.Point;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idivisiontech.transporttracker.RaspberryClient.RaspberryRepository;
import com.idivisiontech.transporttracker.RaspberryClient.data.IndexResult;
import com.idivisiontech.transporttracker.preferences.SettingPreferences;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoCallEventsReceiver;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.OnCallConnectedListener;
import org.abtollc.sdk.OnCallDisconnectedListener;
import org.abtollc.sdk.OnCallErrorListener;
import org.abtollc.sdk.OnCallHeldListener;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRemoteAlertingListener;
import org.abtollc.sdk.OnToneReceivedListener;

import com.idivisiontech.transporttracker.Helpers.AudioRouter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ScreenAV extends AppCompatActivity implements OnCallConnectedListener,
        OnRemoteAlertingListener, OnCallDisconnectedListener,
        OnCallHeldListener, OnToneReceivedListener, OnCallErrorListener, OnInitializeListener {

    protected static final String THIS_FILE = "ScreenAV";


    public static final String POINT_TIME = "pointTime";
    public static final String TOTAL_TIME = "totalTime";
    public boolean IS_HEADPHONE_AVAILBLE=false;

    private AbtoPhone phone;
    private int activeCallId = AbtoPhone.INVALID_CALL_ID;

    private TextView status;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private WakeLock mScreenWakeLock;
    private WakeLock mProximityWakeLock;

    private Point videoViewSize;
    private boolean mInitialAutoSendVideoState;
    private String TAG = "screenAVTAG";
    private SeekBar volumeBar;

    /**
     * executes when activity have been created;
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        super.onCreate(savedInstanceState);

        initWakeLocks();
        setContentView(R.layout.screen_caller);

        phone = ((AbtoApplication) getApplication()).getAbtoPhone();
        mInitialAutoSendVideoState = phone.getConfig().isEnabledAutoSendRtpVideo();

        //Event handlers
        phone.setCallConnectedListener(this);
        phone.setCallDisconnectedListener(this);
        phone.setOnCallHeldListener(this);
        phone.setRemoteAlertingListener(this);
        phone.setToneReceivedListener(this);


//       AudioRouter.Companion.RecordAudioFromHeadphone(this.getApplicationContext());


//        forceLoudSpeaker();
//        RecordAudioFromHeadphone(this.getApplicationContext());





        //Verify mode, in which was started this activity
        boolean bIsIncoming        = getIntent().getBooleanExtra(AbtoPhone.IS_INCOMING, false);
        boolean startedFromService = getIntent().getBooleanExtra(AbtoPhone.ABTO_SERVICE_MARKER, false);
        if (startedFromService) {
            phone.initialize(true);
            phone.setInitializeListener(this);
        } else {
            answerCallByIntent();
        }

        // Cancel incoming call notification
        activeCallId = getIntent().getIntExtra(AbtoPhone.CALL_ID, AbtoPhone.INVALID_CALL_ID);
        //if(bIsIncoming) CallEventsReceiver.cancelIncCallNotification(this, activeCallId);//TODO
        Log.d(THIS_FILE, "callId - " + activeCallId);

        TextView name = (TextView) findViewById(R.id.caller_contact_name);
        name.setText(getIntent().getStringExtra(AbtoPhone.REMOTE_CONTACT));

        mTotalTime = getIntent().getLongExtra(TOTAL_TIME, 0);
        mPointTime = getIntent().getLongExtra(POINT_TIME, 0);
        if (mTotalTime != 0) {
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }

        status = (TextView) findViewById(R.id.caller_call_status);
        status.setText(bIsIncoming ? "Incoming call" : "Dialing...");




        //Outgoing call mode
        startOutgoingCallByIntent();





        if(bIsIncoming){
            pickUp(null);
            setTitle("Telepon dari Operator");
        }else{
            setTitle("Call Help Desk");
        }

        turnOffPA();

        volumeBar = findViewById(R.id.seekBar);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setVolume(Float.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }





    public void RecordAudioFromHeadphone(Context context) {

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);

        int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
        int BytesPerElement = 2; // 2 bytes in 16bit format

        //Step 2: Init AudioRecorder TODO for you

        //Step 3: Route mic to headset
//            setPreferredInputMic(getWiredHeadPhoneMic(this.getApplicationContext()));
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
    }

    public boolean setPreferredInputMic(AudioDeviceInfo mAudioDeviceInfo) {
        AudioRecord mAudioRecord= null;
        Log.e("Mic ketemu", String.valueOf(mAudioDeviceInfo));
         boolean a = false;
         a= mAudioRecord.setPreferredDevice(mAudioDeviceInfo);
         return a;
    }

    private AudioDeviceInfo getWiredHeadPhoneMic(Context context){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] mAudioDeviceOutputList = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        for(int i=0;i<mAudioDeviceOutputList.length;i++){
            if (mAudioDeviceOutputList[i].getType()==AudioDeviceInfo.TYPE_WIRED_HEADSET){

                return mAudioDeviceOutputList[i];

            }
        }

        return null;


    }

    public void forceLoudSpeaker(){

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(false);


    }

    private void turnOffPA() {
        SettingPreferences.Companion.getInstance(getApplicationContext()).setVoiceOfferOn(false);


        RaspberryRepository.Companion.getInstance().services.voiceOverOff().enqueue(new Callback<IndexResult>(){

            @Override
            public void onResponse(Call<IndexResult> call, Response<IndexResult> response) {
                Log.d(TAG," VOICE OFF OK");
            }

            @Override
            public void onFailure(Call<IndexResult> call, Throwable t) {
                Log.d(TAG," VOICE OFF NOT OK");
            }
        });

    }

    private void setVolume(Float progress){
        if(phone != null){
            try {
                phone.setSpeakerLevel(progress / 20);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void answerCallByIntent() {
        if ( getIntent().getBooleanExtra(AbtoCallEventsReceiver.KEY_PICK_UP_AUDIO, false) ) {
            pickUp(null);
        }
        if ( getIntent().getBooleanExtra(AbtoCallEventsReceiver.KEY_PICK_UP_VIDEO, false) ) {
            pickUpVideo(null);
        }
    }

    private void startOutgoingCallByIntent()
    {
        //Skip if call is incoming
        if ( getIntent().getBooleanExtra(AbtoPhone.IS_INCOMING, true)  ) return;

        //Get number and mode
        String sipNumber  = getIntent().getStringExtra(AbtoPhone.REMOTE_CONTACT);
        boolean bVideo    = getIntent().getBooleanExtra(TrackerActivity.START_VIDEO_CALL, false);

        // Start new call
        try {
            if(bVideo) phone.startVideoCall(sipNumber, phone.getCurrentAccountId());
            else       phone.startCall(sipNumber, phone.getCurrentAccountId());


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitializeState(OnInitializeListener.InitializeState state, String message) {
        if (state == InitializeState.SUCCESS) {

            phone.setInitializeListener(null);
            answerCallByIntent();
        }
    }

    @Override
    public void onCallConnected(String remoteContact) {


        if (mTotalTime == 0L) {
            mPointTime = System.currentTimeMillis();
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }

//        RecordAudioFromHeadphone(this.getApplicationContext());


        try {
            phone.setSpeakerLevel(1.0F);
            phone.setMicrophoneLevel(1.0F);
            phone.setSpeakerphoneOn(false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //phone.setMicrophoneMute(false);
        //phone.setMicrophoneLevel((float)3.0);


        if(phone.isVideoCall())
        {
            finish();
            disableProximity();
        }
        else {
            enableProximity();
        }

        status.setText("CallConnected");
    }

    @Override
    public void onCallDisconnected(String remoteContact, int callId, int statusCode) {
        if (callId == activeCallId)
        {

        }
        Log.d(TAG,""+callId);
        Log.d(TAG,""+activeCallId);
        finish();
        mTotalTime = 0;
    }

    @Override
    public void onCallError(String remoteContact, int statusCode, String message)
    {
        Toast.makeText(ScreenAV.this, "onCallError: " + statusCode, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCallHeld(HoldState state) {
        if (state == HoldState.LOCAL_HOLD) 	status.setText("Local Hold");else
        if (state == HoldState.REMOTE_HOLD) status.setText("Remote Hold"); else
        if (state == HoldState.ACTIVE) 		status.setText("Active");
    }

    @Override
    public void onRemoteAlerting(long accId, int statusCode) {
        String statusText = "";

        if (activeCallId == AbtoPhone.INVALID_CALL_ID) 	activeCallId = phone.getActiveCallId();

        switch (statusCode) {
            case TRYING: 		        statusText = "Trying";			break;
            case RINGING:		        statusText = "Ringing";			break;
            case SESSION_PROGRESS:		statusText = "Session in progress";		break;
        }
        status.setText(statusText);
    }

    @Override
    public void onToneReceived(char tone) {
        Toast.makeText(ScreenAV.this, "DTMF received: " + tone, Toast.LENGTH_SHORT).show();

    }

    public void hangUP(View view) {
        try {
            //if(bIsIncoming) phone.rejectCall();else//TODO
            phone.hangUp();
        } catch (RemoteException e) {
            Log.e(THIS_FILE, e.getMessage());
        }
    }

    public void holdCall(View view) {
        try {
            phone.holdRetriveCall();
            //mInitialAutoSendVideoState = !mInitialAutoSendVideoState;
            //phone.muteLocalVideo(mInitialAutoSendVideoState);
            //phone.sendTone(1);
            phone.answerCall(200, false);

        } catch (RemoteException e) {
            Log.e(THIS_FILE, e.getMessage());
        }
    }

    public void pickUp(View view) {
        try {
            phone.answerCall(200, false);
        } catch (RemoteException e) {
            Log.e(THIS_FILE, e.getMessage());
        }
    }

    public void pickUpVideo(View view) {
        try {
            phone.answerCall(200, true);
        } catch (RemoteException e) {
            Log.e(THIS_FILE, e.getMessage());
        }
    }



    // ==========Timer==============
    private long mPointTime = 0;
    private long mTotalTime = 0;
    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            mTotalTime += System.currentTimeMillis() - mPointTime;
            mPointTime = System.currentTimeMillis();
            int seconds = (int) (mTotalTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            int finalSeconds = seconds;
            runOnUiThread(() -> {
                if (finalSeconds < 10) {
                    status.setText("" + minutes + ":0" + finalSeconds);
                } else {
                    status.setText("" + minutes + ":" + finalSeconds);
                }
            });

            mHandler.postDelayed(this, 1000);
        }
    };

    // =============================

    @Override
    protected void onPause() {

        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
        }

        mHandler.removeCallbacks(mUpdateTimeTask);
        disableProximity();

        super.onPause();
    }

    @Override
    protected void onResume() {

        if (mTotalTime != 0L) {
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }

        if (mScreenWakeLock != null) {
            mScreenWakeLock.acquire();
        }
        super.onResume();

    }

    /**
     * executes when activity is destroyed;
     */
    public void onDestroy() {
        super.onDestroy();

        // Disable listener


        mHandler.removeCallbacks(mUpdateTimeTask);

        phone.setCallConnectedListener(null);
        phone.setCallDisconnectedListener(null);
        phone.setOnCallHeldListener(null);
        phone.setRemoteAlertingListener(null);
        phone.setToneReceivedListener(null);
        phone.setVideoEventListener(null);
        disableProximity();
    }


    public void onStop() {
        super.onStop();
    }

    /**
     * overrides panel buttons keydown functionality;
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK|| keyCode == KeyEvent.KEYCODE_HOME) {

            try {
                phone.hangUp();
            } catch (RemoteException e) {
                Log.e(THIS_FILE, e.getMessage());
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    private void initWakeLocks() {

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        int flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
        mScreenWakeLock = powerManager.newWakeLock(flags, "com.abtotest.voiptest:wakelogtag");
        mScreenWakeLock.setReferenceCounted(false);

        int field= 0x00000020;
        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable t) {
        }
        mProximityWakeLock = powerManager.newWakeLock(field, getLocalClassName());
    }


    private void enableProximity() {
        if (!mProximityWakeLock.isHeld()){
            mProximityWakeLock.acquire();
        }
    }

    private void disableProximity() {
        if (mProximityWakeLock.isHeld()) {
            mProximityWakeLock.release();
        }
    }



}
