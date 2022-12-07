package com.habidev.qing

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.habidev.qing.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"

    private val requestPermission = 200
    private var permissionAccepted = false
    private var permissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE)
    }

    private lateinit var viewBinding: ActivityMainBinding

    private var recognitionService: RecognitionService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        ActivityCompat.requestPermissions(this, permissions, requestPermission)

//          AudioFocus
//        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//        val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_ASSISTANT)
//                .setContentType(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//                .build()
//        } else {
//            TODO("VERSION.SDK_INT < O")
//        }
//
//        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
//            .setAudioAttributes(audioAttributes)
//            .build()
//
//        audioManager.requestAudioFocus(focusRequest)

        val serviceIntent = Intent(this, RecognitionService::class.java)

        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(tag, "Service Connected")

            val binder = service as RecognitionService.RecognitionBinder
            recognitionService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(tag, "Service Disconnected")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionAccepted = if (requestCode == requestPermission) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionAccepted) finish()
    }
}