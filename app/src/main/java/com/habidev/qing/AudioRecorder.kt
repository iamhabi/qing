package com.habidev.qing

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorder(context: Context) {
    private val tag: String = "AudioRecord"
    private var recorder: MediaRecorder

    init {
        Log.d(tag, "Initialize Recorder")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        val simpleDataFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        val currentDateTime = simpleDataFormat.format(Date())

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            val saveDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                File(context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS), "qing")
            } else {
                TODO("VERSION.SDK_INT < S")
            }

            saveDir.mkdirs()

            val saveDirPath = saveDir.path
            val recordFile = File("$saveDirPath/$currentDateTime")

            setOutputFile(recordFile)

            prepareRecorder()
        }
    }

    private fun prepareRecorder() {
        try {
            Log.d(tag, "Prepare Recorder")

            recorder.prepare()
        } catch (e: java.lang.Exception) {
            Log.e(tag, "Failed Prepare Recorder$e")
        }
    }

    fun startRecord() {
        Log.d(tag, "Start Record")

        recorder.start()
    }

    fun stopRecord() {
        Log.d(tag, "Stop Record")

        recorder.stop()
        recorder.release()
    }
}