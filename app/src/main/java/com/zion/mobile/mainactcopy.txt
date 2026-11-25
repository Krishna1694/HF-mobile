package com.zion.mobile

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.CallLog

class MainActivity : Activity() {

    private val installedApps = mutableMapOf<String, String>()

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var resultText: TextView
    private var speechCallback: ((String) -> Unit)? = null
    
    private val allContacts = mutableListOf<ContactMatch>()
    private var pendingContacts: Map<String, List<ContactMatch>> = emptyMap()

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

        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
        }
        
        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(
                permissions.toTypedArray(),
                100
            )
        } else {
            loadContacts()
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
                listenForSpeech { text ->
                    val commandData = parseCommand(text)
                    executeCommand(commandData)
                }
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

                speechCallback?.invoke(text)
                speechCallback = null
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

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
        }

        return dp[a.length][b.length]
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

    private fun listenForSpeech(callback: (String) -> Unit) {
        speechCallback = callback

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
                var recent = false

                var target = input
                    .replaceFirst("call ", "")
                    .replaceFirst("dial ", "")
                    .trim()

                if (target.contains("recent")) {
                    target = target
                        .replace("recently", "")
                        .replace("recent", "")
                        .trim()

                    recent = true
                }

                Command(CommandIntent.CALL, target, recent)
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
                callContact(commandData.target, commandData.recent)
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

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        textToSpeech.setOnUtteranceProgressListener(
            object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        onDone?.invoke()
                    }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED
        ) {
            loadContacts()
        }
    }

    private fun loadContacts() {
        allContacts.clear()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            val numberIndex = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            while (it.moveToNext()) {
                allContacts.add(
                    ContactMatch(
                        name = it.getString(nameIndex),
                        number = it.getString(numberIndex)
                    )
                )
            }
        }
    }
    
    private fun recentCalls(contacts: List<ContactMatch>) {
        if (contacts.isEmpty()) {
            speak("I couldn't find any recent call for that contact.")
            return
        }

        var latestNumber: String? = null
        var latestName: String? = null

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)

            while (it.moveToNext()) {
                val callNumber = it.getString(numberIndex)

                val contact = contacts.firstOrNull {
                    normalizePhoneNumber(it.number).takeLast(10) ==
                        normalizePhoneNumber(callNumber).takeLast(10)
                }

                if (contact != null) {
                    latestNumber = contact.number
                    latestName = contact.name
                    break
                }
            }
        }

        if (latestNumber == null) {
            speak("I couldn't find a recent call for that name.")
            return
        }
        val selected = contacts.first {
            it.number == latestNumber && it.name == latestName
        }
        speak(
            "I found $latestName ending with ${
                speakableDigits(latestNumber!!)
            }. Would you like me to call?"
        ){
        listenForSpeech { text ->
            placeCall(text, selected)
        }
        }
    }

    private fun callContact(target: String?, recent: Boolean = false) {
        val matches = allContacts

        val normalizedTarget = normalizeText(target ?: "")

        val exactMatches = matches.filter {
            normalizeText(it.name) == normalizedTarget
        }

        val filteredMatches = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else {
            val partialMatches = matches.filter {
                normalizeText(it.name).contains(normalizedTarget)
            }

            if (partialMatches.isNotEmpty()) {
                partialMatches
            } else {
                matches.filter { contact ->
                    val contactWords = contact.name
                        .lowercase()
                        .trim()
                        .split(Regex("\\s+"))

                    val targetWords = target
                        ?.lowercase()
                        ?.trim()
                        ?.split(Regex("\\s+"))
                        ?: emptyList()

                    targetWords.all { targetWord ->
                        contactWords.any { contactWord ->
                            levenshteinDistance(targetWord, contactWord) <= 2
                        }
                    }
                }
            }
        }

        val contacts = filteredMatches
            .distinctBy { "${it.name}|${normalizePhoneNumber(it.number)}" }
            .groupBy { it.name }

        pendingContacts = contacts
        if (contacts.isEmpty()) {
            speak("I couldn't find that contact.")
            return
        }

        if (recent) {
            recentCalls(contacts.values.flatten())
            return
        }

        if (contacts.size == 1) {
            val contact = contacts.values.first()
            val name = contact.first().name

            if (contact.size == 1) {
                val number = contact.first().number

                speak("I found $name. Would you like me to call?"){
                    listenForSpeech { text ->
                        placeCall(text, contact.first())
                    }
                }
                return
            }
            val endings = contact.joinToString(", ") {
                speakableDigits(it.number)
            }
            speak("There are ${contact.size} numbers for $name ending with $endings. Which one would you like to call?") {
                listenForSpeech { text ->
                    handleNumberChoice(text)
                }
            }
            return
        }
        if (contacts.size > 1) {
            val names = contacts.keys.joinToString(", ")

            speak("Found multiple contacts: $names. Whom to call?"){
                listenForSpeech { text ->
                    handleContactChoice(text)
                }
            }
            return
        }

    }

    private fun speakableDigits(number: String): String {
        return number.takeLast(3).map { it.toString() }.joinToString(" ")
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.filter { it.isDigit() }
    }

    private fun handleContactChoice(input: String) {
        val normalized = normalizeText(input)

        val exactMatches = pendingContacts.filterKeys {
            normalizeText(it) == normalized
        }

        val matches = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else {
            val partialMatches = pendingContacts.filterKeys {
                normalizeText(it).contains(normalized)
            }

            if (partialMatches.isNotEmpty()) {
                partialMatches
            } else {
                pendingContacts.filterKeys {
                    levenshteinDistance(normalizeText(it), normalized) <= 2
                }
            }
        }

        if (matches.size == 1) {
            val contact = matches.values.first()
            val name = matches.keys.first()

            if (contact.size == 1) {
                speak("I found $name. Would you like me to call?"){
                    listenForSpeech { text ->
                        placeCall(text, contact.first())
                    }
                }
            } else {
                val endings = contact.joinToString(", ") {
                    speakableDigits(it.number)
                }

                speak("There are ${contact.size} numbers for $name ending with $endings. Which one would you like to call?") {
                    listenForSpeech { text ->
                        handleNumberChoice(text)
                    }
                }
            }

            return
        }

        if (matches.size > 1) {
            val names = matches.keys.joinToString(", ")

            speak("Found multiple contacts: $names. Whom to call?") {
                listenForSpeech { text ->
                    handleContactChoice(text)
                }
            }

            return
        }

        speak("Couldn't find that contact. Please try again.")
    }

    private fun handleNumberChoice(input: String) {
        val normalized = normalizeText(input)

        val contact = pendingContacts.values.firstOrNull()

        if (contact == null) {
            speak("I couldn't find those numbers.")
            return
        }

        val index = when {
            normalized == "first" || normalized == "one" || normalized == "1" -> 0
            normalized == "second" || normalized == "two" || normalized == "2" -> 1
            normalized == "third" || normalized == "three" || normalized == "3" -> 2
            else -> -1
        }

        if (index >= 0 && index < contact.size) {
            val selected = contact[index]

            speak("Calling the number ending in ${speakableDigits(selected.number)}."){
                placeCall("yes", selected)
            }
            return
        }

        // Check the last digits
        val digits = normalized
            .split(" ")
            .filter { it.all { char -> char.isDigit() } }
            .joinToString("")

        val matches = contact.filter {
            normalizePhoneNumber(it.number).endsWith(digits)
        }

        if (matches.size == 1) {
            val selected = matches.first()

            speak("Calling the number ending in ${speakableDigits(selected.number)}."){
                placeCall("yes", selected)
            }
            return
        }

        if (matches.size > 1) {
            speak("Found more than one number matching that. Please try again.")
            return
        }

        speak("I couldn't identify that number. Please try again.")
    }

    private fun placeCall(confirmation: String, contact: ContactMatch) {
        val input = normalizeText(confirmation)

        when {
            input.contains("yes") ||
            input.contains("yeah") ||
            input.contains("yep") -> {
                //speak("Calling ${contact.name}.")
                val intent = Intent(
                    Intent.ACTION_CALL,
                    android.net.Uri.parse("tel:${contact.number}")
                )

                if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(intent)
                }
            }

            input.contains("no") ||
            input.contains("cancel") -> {
                speak("Okay, call cancelled.")
            }

            else -> {
                speak("Please say yes or no.")
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}