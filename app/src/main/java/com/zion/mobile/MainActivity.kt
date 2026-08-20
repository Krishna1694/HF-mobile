package com.zion.mobile

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.content.pm.PackageManager

class MainActivity : Activity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var resultText: TextView


    private fun requestMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultText = TextView(this).apply {
            text = "Tap the button and speak"
            textSize = 24f
        }

        val button = Button(this).apply {
            text = "🎤 Speak"
            setOnClickListener {
                startListening()
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 100, 40, 40)
            addView(resultText)
            addView(button)
        }

        setContentView(layout)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        requestMicrophonePermission()
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                resultText.text = matches?.firstOrNull() ?: "Didn't understand"
            }

            override fun onError(error: Int) {
                resultText.text = "Error: $error"
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }

        resultText.text = "🎙 Listening..."
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }
}