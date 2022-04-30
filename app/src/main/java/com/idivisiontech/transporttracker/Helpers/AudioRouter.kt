package com.idivisiontech.transporttracker.Helpers

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class AudioRouter {
    companion object {
        private val mAudioRecord: AudioRecord? = null
        fun RecordAudioFromHeadphone(context: Context) {
            //Step 1: Turn on Speaker
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = true

            //Step 2: Init AudioRecorder TODO for you

            //Step 3: Route mic to headset
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setPreferredInputMic(getWiredHeadPhoneMic(context))
//            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun setPreferredInputMic(mAudioDeviceInfo: AudioDeviceInfo?) {
            val result: Boolean? = mAudioRecord?.setPreferredDevice(mAudioDeviceInfo)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun getWiredHeadPhoneMic(context: Context): AudioDeviceInfo? {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mAudioDeviceOutputList = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            mAudioDeviceOutputList?.filterNotNull()?.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {

                    return device
                }
            }
            return null
        }
    }
}