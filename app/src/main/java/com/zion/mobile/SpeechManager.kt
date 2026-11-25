package com.zion.mobile

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.RecognizerIntent
import android.speech.RecognitionListener

class SpeechManager(
    private val context: Context
) {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer

    private var speechErrorCallback: ((Int) -> Unit)? = null
    private var speechCallback: ((String) -> Unit)? = null

    fun initialize() {
        //Text initialization here
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speak("I'm UP")
            }
        }

        // move SpeechRecognizer initialization here
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val text = matches?.firstOrNull() ?: return
                speechCallback?.invoke(text)
                speechCallback = null
            }

            override fun onError(error: Int) {
                speechErrorCallback?.invoke(error)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // move RecognitionListener here

    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        // move your existing speak() logic here
        textToSpeech.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    onDone?.invoke()
                }

                override fun onError(utteranceId: String?) {}
            }
        )

        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "zion"
        )
    }

    fun listen(callback: (String) -> Unit) {
        // move your existing listenForSpeech() logic here
        speechCallback = callback

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }

        speechRecognizer.startListening(intent)
    }

    fun shutdown() {
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}