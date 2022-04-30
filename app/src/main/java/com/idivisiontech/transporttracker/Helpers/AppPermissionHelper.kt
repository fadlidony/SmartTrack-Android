package com.idivisiontech.transporttracker.Helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AppPermissionHelper(var activity: Activity, val permissionReadyListener: PermissionReady) {

    private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.USE_SIP,
            Manifest.permission.BLUETOOTH
    )

    private val PERMISSIONS_REQUEST = 290

    init {
        if(!hasAllPermissions()){
            askForPermissions()
        }else{
            onPermissionsAvailable()
        }
    }

    private fun askForPermissions() {
        ActivityCompat.requestPermissions(activity,
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST)
    }

    private fun hasAllPermissions(): Boolean {
        var hasPermissions = true;
        for(permission in REQUIRED_PERMISSIONS){
            hasPermissions = hasPermissions && hasPermission(permission)
        }

        return hasPermissions
    }

    private fun hasPermission(permission: String): Boolean {
        val status = ContextCompat.checkSelfPermission(activity, permission)
        return status == PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionsAvailable() {
        if (permissionReadyListener == null) return
        permissionReadyListener.onPermissionReady()
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (hasAllPermissions()) {
                onPermissionsAvailable()
            } else {
                Toast.makeText(activity, "You did not provide permissions", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    interface PermissionReady {
        fun onPermissionReady()
    }

}