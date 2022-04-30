package com.idivisiontech.transporttracker

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.MediaController
import com.pedro.vlc.VlcListener
import com.pedro.vlc.VlcVideoLibrary
import kotlinx.android.synthetic.main.activity_cctv_viewer.*
import java.util.*


class CctvViewerActivity : AppCompatActivity(), VlcListener {

    override fun onComplete() {

    }

    override fun onError() {
        vlcVideoLibrary.let{
            it?.stop()
        }
    }

    private val options = listOf(":fullscreen","fullscreen")
    private var vlcVideoLibrary: VlcVideoLibrary? = null
    private var cameraUrls = listOf(
            "rtsp://admin:admin123@192.168.1.222:554/cam/realmonitor?channel=1&subtype=0",
            "rtsp://admin:admin123@192.168.1.222:554/cam/realmonitor?channel=2&subtype=0",
            "rtsp://admin:admin123@192.168.1.222:554/cam/realmonitor?channel=3&subtype=0",
            "rtsp://admin:admin123@192.168.1.222:554/cam/realmonitor?channel=4&subtype=0"
    )

    /*private var cameraUrls = listOf(
            "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov",
            "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov",
            "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov",
            "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov"
    )*/

    private lateinit var buttons: List<Button>;
    private val COLOR_GREEN = 0x226125
    private val COLOR_RED = 0x226125

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cctv_viewer)

        /*
        video_view.setMediaController(mediaController)
        video_view.setVideoURI(Uri.parse("tsp://admin:admin123@192.168.1.222:554/cam/realmonitor?channel=1&subtype=1"))
        video_view.requestFocus()*/

        vlcVideoLibrary = VlcVideoLibrary(this, this, surfaceView)
        //vlcVideoLibrary.setO
        vlcVideoLibrary.let {
            //it?.setOptions(options)
        }


        buttons = listOf(btnCam1, btnCam2, btnCam3, btnCam4)

        for((index, button) in buttons.withIndex()){
            button.setOnClickListener({view ->
                playCamera(index)
            })
        }




        btnBack.setOnClickListener({
            finish()
        })
        title = "CAMERA DVR"

    }



    private fun playCamera(index: Int) {
        textPilih.visibility = View.GONE
        vlcVideoLibrary.let {vidPlayer ->
            vidPlayer?.stop()
            vidPlayer?.play(cameraUrls[index])
        }
        for ((i, button) in buttons.withIndex()){
            if(i == index){
                button.setBackgroundColor(resources.getColor(R.color.greenButton))
            }else{
                button.setBackgroundColor(resources.getColor(R.color.redButton))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vlcVideoLibrary.let{
            it?.stop()
        }
    }


}
