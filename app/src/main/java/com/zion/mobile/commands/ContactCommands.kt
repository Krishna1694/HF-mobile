package com.zion.mobile.commands

import android.Manifest
import android.content.Intent
import android.content.Context
import android.provider.CallLog
import com.zion.mobile.core.ContactData
import android.content.pm.PackageManager
import com.zion.mobile.core.SpeechManager

class ContactCommands(
    private val context: Context,
    private val speechManager: SpeechManager,
    private val contactData: ContactData,
) {

    private val allContacts = contactData.loadContacts()

    private var pendingContacts: Map<String, List<ContactMatch>> = emptyMap()

    fun callContact(target: String?, recent: Boolean = false) {
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
            .distinctBy {
                "${it.name}|${normalizePhoneNumber(it.number)}"
            }
            .groupBy { it.name }

        pendingContacts = contacts

        if (contacts.isEmpty()) {
            speechManager.speak("I couldn't find that contact.")
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
                speechManager.speak("I found $name. Would you like me to call?") {
                    speechManager.listen { text ->
                        placeCall(text, contact.first())
                    }
                }
                return
            }

            val endings = contact.joinToString(", ") {
                speakableDigits(it.number)
            }

            speechManager.speak("There are ${contact.size} numbers for $name ending with $endings. Which one would you like to call?") {
                speechManager.listen { text ->
                    handleNumberChoice(text)
                }
            }

            return
        }

        if (contacts.size > 1) {
            val names = contacts.keys.joinToString(", ")

            speechManager.speak("Found multiple contacts: $names. Whom to call?") {
                speechManager.listen { text ->
                    handleContactChoice(text)
                }
            }
        }
    }

    private fun recentCalls(contacts: List<ContactMatch>) {
        if (contacts.isEmpty()) {
            speechManager.speak("I couldn't find any recent call for that contact.")
            return
        }

        var latestNumber: String? = null
        var latestName: String? = null

        val cursor = context.contentResolver.query(
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
            val numberIndex =
                it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)

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
            speechManager.speak("I couldn't find a recent call for that name.")
            return
        }

        speechManager.speak("I found $latestName ending with ${speakableDigits(latestNumber!!)}. Would you like me to call?") {
            speechManager.listen { text ->
                placeCall(text, contacts.first {it.number == latestNumber && it.name == latestName})
            }
        }
    }

    fun handleContactChoice(input: String) {
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
                    levenshteinDistance(
                        normalizeText(it),
                        normalized
                    ) <= 2
                }
            }
        }

        if (matches.size == 1) {
            val contact = matches.values.first()
            val name = matches.keys.first()

            if (contact.size == 1) {
                speechManager.speak("I found $name. Would you like me to call?") {
                    speechManager.listen { text ->
                        placeCall(text, contact.first())
                    }
                }
            } else {
                val endings = contact.joinToString(", ") {
                    speakableDigits(it.number)
                }

                speechManager.speak("There are ${contact.size} numbers for $name ending with $endings. Which one would you like to call?") {
                    speechManager.listen { text ->
                        handleNumberChoice(text)
                    }
                }
            }

            return
        }

        if (matches.size > 1) {
            val names = matches.keys.joinToString(", ")

            speechManager.speak("Found multiple contacts: $names. Whom to call?") {
                speechManager.listen { text ->
                    handleContactChoice(text)
                }
            }

            return
        }

        speechManager.speak("Couldn't find that contact. Please try again.")
    }

    fun handleNumberChoice(input: String) {
        val normalized = normalizeText(input)

        val contact = pendingContacts.values.firstOrNull()

        if (contact == null) {
            speechManager.speak("I couldn't find those numbers.")
            return
        }

        val index = when {
            normalized == "first" ||
            normalized == "one" ||
            normalized == "1" -> 0

            normalized == "second" ||
            normalized == "two" ||
            normalized == "2" -> 1

            normalized == "third" ||
            normalized == "three" ||
            normalized == "3" -> 2

            else -> -1
        }

        if (index >= 0 && index < contact.size) {
            val selected = contact[index]

            speechManager.speak("Calling the number ending in ${speakableDigits(selected.number)}.") {
                placeCall("yes", selected)
            }

            return
        }

        val digits = normalized
            .split(" ")
            .filter { it.all { char -> char.isDigit() } }
            .joinToString("")

        val matches = contact.filter {
            normalizePhoneNumber(it.number).endsWith(digits)
        }

        if (matches.size == 1) {
            val selected = matches.first()

            speechManager.speak("Calling the number ending in ${speakableDigits(selected.number)}.") {
                placeCall("yes", selected)
            }

            return
        }

        if (matches.size > 1) {
            speechManager.speak("Found more than one number matching that. Please try again.")
            return
        }

        speechManager.speak("I couldn't identify that number. Please try again.")
    }

    private fun placeCall(
        confirmation: String,
        contact: ContactMatch
    ) {
        val input = normalizeText(confirmation)

        when {
            input.contains("yes") ||
            input.contains("yeah") ||
            input.contains("yep") -> {

                val intent = Intent(
                    Intent.ACTION_CALL,
                    android.net.Uri.parse("tel:${contact.number}")
                )

                if (
                    context.checkSelfPermission(
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    context.startActivity(intent)
                } else {
                    speechManager.speak("I don't have permission to make calls.")
                }
            }

            input.contains("no") ||
            input.contains("cancel") -> {
                speechManager.speak("Okay, call cancelled.")
            }

            else -> {
                speechManager.speak("Please say yes or no.")
            }
        }
    }

    private fun speakableDigits(number: String): String {
        return number
            .takeLast(3)
            .map { it.toString() }
            .joinToString(" ")
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.filter { it.isDigit() }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshteinDistance(
        a: String,
        b: String
    ): Int {
        val dp = Array(a.length + 1) {
            IntArray(b.length + 1)
        }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] +
                        if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
        }

        return dp[a.length][b.length]
    }
}