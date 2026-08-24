package com.zion.mobile

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech


class MainActivity : Activity() {

    private val installedApps = mutableMapOf<String, String>()

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var resultText: TextView


    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(
                permissions.toTypedArray(),
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

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speak("I'm UP")
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
        
        loadInstalledApps()

        requestPermissions()
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val text = matches?.firstOrNull() ?: return
                resultText.text = text

                val commandData = parseCommand(text)
                executeCommand(commandData)
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

    private fun loadInstalledApps() {
        installedApps.clear()

        val apps = packageManager.getInstalledApplications(0)

        for (app in apps) {
            val appName = packageManager
                .getApplicationLabel(app)
                .toString()
                .lowercase()

            installedApps[appName] = app.packageName
        }
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

    private fun parseCommand(text: String): Command {
        val input = text.lowercase().trim()

        return when {
            input.contains("hello") -> Command(CommandIntent.GREETING)

            input.contains("your name") -> Command(CommandIntent.GET_NAME)

            input.contains("refresh apps") -> Command(CommandIntent.REFRESH_APPS)

            input.contains("open ") ||
            input.contains("start ") ||
            input.contains("launch ") -> {
                var target: String? = null

                for ((appName, _) in installedApps) {
                    if (input.contains(appName)) {
                        target = appName
                        break
                    }
                }
                Command(CommandIntent.OPEN_APP, target)
            }
            
            input.contains("call ") ||
            input.contains("dial ") -> {
                val target = input
                    .replaceFirst("call ", "")
                    .replaceFirst("dial ", "")
                    .trim()

                Command(CommandIntent.CALL, target)
            }

            else ->
                Command(CommandIntent.UNKNOWN)
        }
    }

    private fun executeCommand(commandData: Command) {
        when (commandData.intent) {
            CommandIntent.GREETING -> {
                speak("Hello! How can I help?")
            }

            CommandIntent.GET_NAME -> {
                speak("My name is Zion.")
            }

            CommandIntent.OPEN_APP -> {
                openApp(commandData.target)
            }

            CommandIntent.REFRESH_APPS -> {
                loadInstalledApps()
                speak("Apps refreshed.")
            }

            CommandIntent.CALL -> {
                callContact(commandData.target)
            }

            CommandIntent.UNKNOWN -> {
                speak("I don't understand that yet.")
            }
        }
    }

    private fun openApp(target: String?) {
        if (target == null) {
            speak("I couldn't find that app.")
            return
        }

        val packageName = installedApps[target]

        if (packageName != null) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                startActivity(intent)
                return
            }
        }

        speak("I couldn't find that app.")
    }

    private fun speak(text: String) {
        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "zion"
        )
    }
    
    override fun onDestroy() {
        speechRecognizer.destroy()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}