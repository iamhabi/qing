package com.habidev.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.runBlocking


class QingSpeechRecognizer(context: Context): RecognitionListener {
    private val tag = "QingSpeechRecognizer"

    private var commandFunction: CommandFunction
    private var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    private var context: Context

    init {
        this.context = context

        commandFunction = context as CommandFunction

        initRecognizerIntent()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)
    }

    fun startListening() {
        speechRecognizer.startListening(recognizerIntent)
//        commandFunction.startRecording()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(tag, "Ready For Speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(tag, "Beginning of Speech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d(tag, "Rms Changed")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d(tag, "Buffer Received")
    }

    override fun onEndOfSpeech() {
        Log.d(tag, "End of Speech")

        startListening()
    }

    override fun onError(error: Int) {
        Log.e(tag, "Error$error")

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> startListening()
        }
    }

    override fun onResults(results: Bundle?) {
        Log.d(tag, "Results")

//        commandFunction.stopRecording()

        val resultList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        Log.d(tag, resultList.toString())

        commandFunction.command(resultList)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Log.d(tag, "Partial Results")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(tag, "Event$eventType")
    }

    private fun initRecognizerIntent() {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "kr")
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }
}