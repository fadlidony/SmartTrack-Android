package com.idivisiontech.transporttracker

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.idivisiontech.transporttracker.Helpers.AppPermissionHelper
import com.idivisiontech.transporttracker.Helpers.PreferenceHelper
import com.idivisiontech.transporttracker.ServerOperator.Data.Profile
import com.idivisiontech.transporttracker.ServerOperator.ServerApiRepository
import com.idivisiontech.transporttracker.ServerOperator.SessionHelper
import com.idivisiontech.transporttracker.preferences.SettingPreferences
import kotlinx.android.synthetic.main.activity_splash.*
import retrofit2.Call
import retrofit2.Response
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.idivisiontech.transporttracker.Services.RuteService
import com.idivisiontech.transporttracker.licenseServer.LicenseServerRepository
import com.idivisiontech.transporttracker.licenseServer.data.UpdateResult
import com.idivisiontech.transporttracker.licenseServer.data.ValidityResponse
import retrofit2.Callback


class SplashActivity : AppCompatActivity() {

    private var versionName: String = ""
    private val TAG = SplashActivity::class.java.simpleName
    private lateinit var android_id: String
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var sharedPref: SharedPreferences
    private lateinit var appPermissionHelper: AppPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        android_id = Settings.Secure.getString(contentResolver,
                Settings.Secure.ANDROID_ID)
        textStatus.text = "Meminta Perijinan...."
        Log.d(TAG, "android-id = ${android_id}" )
        getVersionName()
        appPermissionHelper = AppPermissionHelper(this, object : AppPermissionHelper.PermissionReady{
            override fun onPermissionReady() {
                //nextStep()
                textStatus.text = "Perijinan telah diberikan"
                checkUpdate(object: Callback<UpdateResult>{
                    override fun onFailure(call: Call<UpdateResult>, t: Throwable) {
                        Toast.makeText(applicationContext,"Gagal Menghubungi Server License",Toast.LENGTH_LONG).show()
                        Log.d(TAG,"checkUpdate  fail ${t.message}}")
                        finish()
                    }

                    override fun onResponse(call: Call<UpdateResult>, response: Response<UpdateResult>) {
                        Log.d(TAG,"checkUpdate  ${response.isSuccessful.toString()} ${response.body().toString()}")
                        if(response.isSuccessful){
                            val updateResult = response.body()
                            if(updateResult?.version == versionName){
                                textStatus.text = "Versi Terbaru"
                                checkLicense()
                            }else{
                                textStatus.text = "Versi baru telah tersedia,silahkan update"
                                val alert = AlertDialog.Builder(this@SplashActivity)
                                        .setTitle("Update Baru tersedia")
                                        .setMessage("Terdapat Versi baru : ${updateResult?.version}")
                                        .setCancelable(updateResult?.is_skipable as Boolean)
                                        .setPositiveButton("Update!",object : DialogInterface.OnClickListener{
                                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                                RuteService.stopService(applicationContext)
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateResult?.file_url))
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent)
                                                finish()
                                            }

                                        })
                                        .setNegativeButton("Skip") { dialog: DialogInterface?, which: Int ->
                                            checkLicense()
                                        }
                                        .create()

                                alert.setCanceledOnTouchOutside(false)
                                alert.show()
                            }
                        }else{
                            Toast.makeText(applicationContext,"Gagal Menghubungi Server License",Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }

                })
            }
        })

    }

    private fun checkLicense() {
        LicenseServerRepository.getInstance().services.getValidityStatus(android_id).enqueue(object : Callback<ValidityResponse>{
            override fun onFailure(call: Call<ValidityResponse>, t: Throwable) {
                Toast.makeText(applicationContext,"Gagal menghubungi Server", Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: Call<ValidityResponse>, response: Response<ValidityResponse>) {
                if(response.isSuccessful){
                    val license = response.body()
                    license.let {
                        if(it?.valid as Boolean){
                            textStatus.text = "License Perangkat Terdaftar!"
                            nextStep()
                        }else{
                            textStatus.text = "License Perangkat Tidak Terdaftar!"
                            Toast.makeText(applicationContext,"License Perangkat Tidak Terdaftar!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }else{
                    Toast.makeText(applicationContext,"Gagal menghubungi Server On Response", Toast.LENGTH_LONG).show()
                }
            }

        })

        //nextStep()
    }

    private fun checkUpdate(callback: Callback<UpdateResult>) {
            LicenseServerRepository.getInstance().services.getUpdates().enqueue(callback)
        }

    private fun getVersionName() {
        versionName = applicationContext.getPackageManager()
                .getPackageInfo(applicationContext.getPackageName(), 0).versionName
        tvVersion.text = "${versionName} | ${android_id}"
    }

    private fun nextStep() {
        textStatus.text = "Memeriksa Sesi Login"
        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        preferenceHelper = PreferenceHelper(sharedPref)
        var session_key = preferenceHelper.get(SessionHelper.SESSION_KEY)
        if(session_key != null){
            val sessionHelper = SessionHelper(this, session_key)
            var serverApiRepository = ServerApiRepository(this)
            serverApiRepository.updateSessionKey(session_key)
            sessionHelper.serverRepository = serverApiRepository
            sessionHelper.serverRepository.services.profile().enqueue(object : retrofit2.Callback<Profile> {

                override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                    progressBar.setVisibility(View.GONE)

                    Log.d("sesi login respon", "${response.errorBody().toString()} ${response.body().toString()} ${response.toString()}")
                    if (response.code() == 401 || response.code() == 400) {
                        Toast.makeText(this@SplashActivity, "Sesi Login Tidak ditemukan, Silahkan Coba login", Toast.LENGTH_SHORT).show()
                        textStatus.text = "Sesi Login Tidak ditemukan, Silahkan Coba login"
                        preferenceHelper.delete(SessionHelper.SESSION_KEY)
                        finish()
                        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                        startActivity(intent)
                    } else {
                        SettingPreferences.getInstance(applicationContext).setVoiceOfferOn(false)
                        textStatus.text = "Sesi Login Valid!"
                        Toast.makeText(this@SplashActivity, "Sesi Login Valid!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@SplashActivity, TrackerActivity::class.java)
                        startActivity(intent)
                        finish()
                    }


                }

                override fun onFailure(call: Call<Profile>, t: Throwable) {
                    //preferencesHelper.delete(SessionHelper.SESSION_KEY);
                    progressBar.setVisibility(View.GONE)
                    Toast.makeText(this@SplashActivity, "Gagal Menghubungi Server : FAILURE", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        appPermissionHelper.onRequestPermissionsResult(requestCode)
    }
}
