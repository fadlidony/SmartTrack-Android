package com.idivisiontech.transporttracker;

import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.idivisiontech.transporttracker.Data.FirebaseData.QrCodeLogin;
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper;
import com.idivisiontech.transporttracker.Helpers.UsbTetherHelper;
import com.idivisiontech.transporttracker.ServerOperator.Data.LoginResult;
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile;
import com.idivisiontech.transporttracker.ServerOperator.Data.QrCode.QrCodeResult;
import com.idivisiontech.transporttracker.ServerOperator.SessionHelper;
import com.idivisiontech.transporttracker.preferences.SettingPreferences;
import com.soten.libs.base.MessageResult;
import com.soten.libs.hf.HFManager;
import com.soten.libs.hf.base.CMD;
import com.soten.libs.hf.base.ERROR;
import com.soten.libs.hf.impl.HF;
import com.soten.libs.hf.impl.HFMessageResult;
import com.soten.libs.hf.impl.HFModelListener;
import com.soten.libs.utils.StringUtils;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements Handler.Callback {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText username_text;
    private EditText password_text;
    private ProgressBar progress_bar;
    private SessionHelper sessionHelper;
    private PreferenceHelper preferencesHelper;
    private HFManager mHfManager;
    private HFModelListener mHfListener;
    private HF hfModel;
    private Handler mHandler;
    private HFManager mManager;
    private HF hf;
    private HFlListener hFlListener;
    private byte[] mCardId;
    private Ringtone ringtone;
    private Boolean MODE_SAMSUNG = false;
    private ImageView qrIv;
    private DatabaseReference qrCodeLoginFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        /*password_text = (EditText) findViewById(R.id.editTextPassword);
        username_text = (EditText) findViewById(R.id.editTextEmail);*/
        progress_bar = (ProgressBar) findViewById(R.id.progressBar);
        qrIv = findViewById(R.id.qr_iv);
        qrIv.setOnClickListener(view -> {
            loadImageQr();
        });

        initUsbTether();
        preferencesHelper = new PreferenceHelper(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE));
        String session_key = preferencesHelper.get(SessionHelper.SESSION_KEY);
        sessionHelper = new SessionHelper(this, session_key == "" ? null : session_key);


        //initHFRC();
        if(sessionHelper.isLogedIn()){
            Log.d(TAG,"IS LOGED IN");
            progress_bar.setVisibility(View.VISIBLE);
            sessionHelper.getServerRepository().services.profile().enqueue(new retrofit2.Callback<Profile>(){

                @Override
                public void onResponse(Call<Profile> call, Response<Profile> response) {
                    progress_bar.setVisibility(View.GONE);
                    if(response.code() == 401){
                        Toast.makeText(LoginActivity.this, "Sesi Login Tidak ditemukan, Silahkan Coba login", Toast.LENGTH_SHORT).show();
                        preferencesHelper.delete(SessionHelper.SESSION_KEY);
                        enableScanQrLogin();
                    }else{
                        SettingPreferences.Companion.getInstance(getApplicationContext()).setVoiceOfferOn(false);
                        Toast.makeText(LoginActivity.this, "Berhasil Login!", Toast.LENGTH_SHORT).show();
                        qrCodeLoginFirebase.removeEventListener(qrCodeLoginListener);
                        finish();
                        Intent intent = new Intent(LoginActivity.this, TrackerActivity.class);
                        startActivity(intent);
                    }

                }

                @Override
                public void onFailure(Call<Profile> call, Throwable t) {
                    progress_bar.setVisibility(View.GONE);
                    Log.d(TAG,"GAGAL LOGIN");
                    //preferencesHelper.delete(SessionHelper.SESSION_KEY);

                    Toast.makeText(LoginActivity.this, "Gagal Menghubungi Server", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }else{
            enableScanQrLogin();
        }


        /*login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //login();
                //loginUsingRfid();
            }
        });*/

        initZoiperService();

        initRFID();

        initOBD();

    }

    private void enableScanQrLogin() {
        final String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Log.d("ANDROID-ID",android_id);
        loadImageQr();
        qrCodeLoginFirebase = FirebaseDatabase.getInstance().getReference("qr-code-login/" + android_id);

        qrCodeLoginFirebase.removeEventListener(qrCodeLoginListener);
        qrCodeLoginFirebase.addValueEventListener(qrCodeLoginListener);
    }

    private ValueEventListener qrCodeLoginListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if(dataSnapshot != null){



                if(dataSnapshot.child("status").getValue() != null && dataSnapshot.child("token").getValue() != null){
                    int code = Integer.parseInt(dataSnapshot.child("status").getValue().toString());
                    String token = dataSnapshot.child("token").getValue().toString();

                    QrCodeLogin qrCodeLogin = new QrCodeLogin(code, token);

                    Log.d(TAG,"enableScanQrLogin: " + qrCodeLogin.toString());
                    if(qrCodeLogin.getStatus() == 0 && !qrCodeLogin.getToken().isEmpty() ){


                        //rfid_text.setText(qrCodeLogin.getToken());
                        loginToServer(qrCodeLogin.getToken());

                        qrCodeLogin.setStatus(1);
                        qrCodeLogin.setToken("");
                        qrCodeLoginFirebase.setValue(qrCodeLogin);

                    }
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void loadImageQr() {
        final String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        sessionHelper.getServerRepository().services.getImageQr(android_id).enqueue(new Callback<QrCodeResult>() {
            @Override
            public void onResponse(Call<QrCodeResult> call, Response<QrCodeResult> response) {
                if(response.isSuccessful()){
                    Log.d(TAG,"load image QR : " + response.body().toString());
                    bindQrToImageView(response.body());
                }
            }

            @Override
            public void onFailure(Call<QrCodeResult> call, Throwable t) {
                Log.d(TAG,"error on access");
            }
        });
    }

    private void bindQrToImageView(QrCodeResult body) {
        if(body != null){
            if(body.getError() == false){
                Picasso.get().load(body.getData().getImage()).error(R.drawable.error_center_x).networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE).fit().into(qrIv);
                Log.d(TAG,"bindQrToImageView: " + body.getData().getImage());

                qrIv.postDelayed(() ->
                        runOnUiThread(() -> loadImageQr())
                , body.getData().getExpired_in() * 1000);
            }else{
                Log.d(TAG,"bindQrToImageView : NULL");
            }
        }
    }

    private void initOBD() {
        //new OBDManager()
    }

    private void initRFID() {

        if(MODE_SAMSUNG == false) {
            try{
                Log.d(TAG, "RFID initRFID");
                mManager = HFManager.getInstance();
                hf = mManager.getModel();
                mHandler = new Handler(this);
                mManager.open(this);
                hFlListener = new HFlListener();
                mManager.register(hFlListener);
                hf.openModule();
                hf.SetAutoGetCardNum();
            }catch(Exception e){

            }
        }
    }

    private void ring() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
        ringtone = RingtoneManager.getRingtone(this, uri);
        ringtone.play();
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC,100);
        toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_L,10000);
    }

    @Override
    public boolean handleMessage(Message msg) {
        HFMessageResult hfResult = (HFMessageResult) msg.obj;
        refreshUI(hfResult);
        return false;
    }

    @Override
    protected void onDestroy() {
        if(MODE_SAMSUNG == false){
            try {
                mManager.unregister(hFlListener);
                mManager.close(hf, this);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        super.onDestroy();

    }

    private void refreshUI(HFMessageResult msg) {
        Log.d(TAG,"RFID refreshUI CALLED");
        String Responses = "";
        if (msg.error == ERROR.SUCCESS) {
            switch (msg.cmd) {
                case CMD.GET_MODULE_VERSION:
                    Responses = msg.version;
                    break;
                case CMD.GET_CARD_ID:
                    mCardId = msg.cardId;
//                    ring();
                    Responses = StringUtils.toHexString(msg.cardId);
                    loginToServer(Responses);
                    break;
                case CMD._GET_BAUDRATE:
                    Responses = msg.data;
                    break;
                case CMD.READ_BLOCK:
                    String data = msg.data;
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
        Log.d(TAG,"RFID = " + Responses);
        /*if (mLog.getText() != null) {
            mLog.append(Responses + "\n");
            int offset = mLog.getLineCount() * mLog.getLineHeight();
            if (offset > mLog.getHeight()) {
                mLog.scrollTo(0, offset - mLog.getHeight());
            }
        } else {
            mLog.setText(String.format("%s\n", Responses));
        }*/
    }




    private void initUsbTether() {
        String ip = UsbTetherHelper.Companion.getInstance().getIpAddress(UsbTetherHelper.VIA_USB);
        Log.d(TAG,"USB TETHER LOGIN " + ip);
        /*if(ip == null){
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            startActivity(intent);
        }*/
    }

    private void initZoiperService() {
        /*Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.zoiper.zdk.android.demo", "com.zoiper.zdk.android.demo.MainActivity"));
        intent.putExtra("EXTRA_HOSTNAME","103.226.49.73:6354");
        intent.putExtra("EXTRA_USER","1313");
        intent.putExtra("EXTRA_PASS","1313");
        startActivity(intent);*/
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

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
            Log.d(TAG, "RFID ERROR " + e.getMessage());
        }
    }



    private void loginUsingRfid(){

        //loginToServer(rfid_text.getText().toString());

    }

    private void loginToServer(String token){
        progress_bar.setVisibility(View.VISIBLE);
        final String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Log.d(TAG,"loginToServer: token " + token);
        sessionHelper.getServerRepository().services.login(token, android_id).enqueue(new retrofit2.Callback<LoginResult>(){

            @Override
            public void onResponse(Call<LoginResult> call, Response<LoginResult> response) {
                Log.d(TAG,"LOGIN CODE REQ" + response.code());
                if(response.isSuccessful()){
                    if(response.body().getError() == false){
                        qrCodeLoginFirebase.removeEventListener(qrCodeLoginListener);
                        preferencesHelper.save(SessionHelper.SESSION_KEY,response.body().getKey());
                        Toast.makeText(LoginActivity.this, "Berhasil Login!", Toast.LENGTH_SHORT).show();
                        finish();
                        Intent intent = new Intent(LoginActivity.this, TrackerActivity.class);
                        startActivity(intent);
                    }else{
                        Log.d(TAG,"LOGIN GET ERROR = TRUE");
                        Log.d(TAG,"ANDROID-ID : " + android_id);
                        Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }else{
                    if(response.body() == null){
                        Log.d(TAG,"LOGIN TEST = NULL");
                    }else{
                        Log.d(TAG,"LOGIN TEST = BERISI");
                    }
                }

                stopRingTone();
/*

                Log.d(TAG,"REQ CODE = " + response.code());

                Log.d(TAG,"LOGIN : " + response.body().getMessage());
                if(response.body().getError() == false){
                    progress_bar.setVisibility(View.GONE);

                }else{

                }*/


            }

            @Override
            public void onFailure(Call<LoginResult> call, Throwable t)
            {
                Log.d(TAG,"LOGIN FAIL " + t.getMessage());
                Log.d(TAG,"FAIL LOGIN");
                stopRingTone();
            }
        });
    }

    private void stopRingTone() {
        if(ringtone != null) ringtone.stop();
    }

    public void login(){
        String email = username_text.getText().toString();
        String password = password_text.getText().toString();
        if (email.equals("")){
            Toast.makeText(this, "Username Tidak Boleh Kosong!", Toast.LENGTH_SHORT).show();
        }else if(password.equals("")){
            Toast.makeText(this, "Password Tidak Boleh Kosong!", Toast.LENGTH_SHORT).show();
        }else{
            authenticate(email, password);
        }
    }




    private void authenticate(final String email, String password) {
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String user = email;
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>(){
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        Log.i(TAG, "authenticate: " + task.isSuccessful());
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Berhasil Login!", Toast.LENGTH_SHORT).show();
                            finish();
                            Intent intent = new Intent(LoginActivity.this, TrackerActivity.class);
                            intent.putExtra("username", user);
                            startActivity(intent);
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}
