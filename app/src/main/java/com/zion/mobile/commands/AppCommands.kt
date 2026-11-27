package com.zion.mobile.commands

import android.content.Context
import com.zion.mobile.core.SpeechManager

class AppCommands(
    private val context: Context,
    private val speech: SpeechManager,
    private val installedApps: Map<String, String>
) {

    fun openApp(target: String?) {
        if (target == null) {
            speech.speak("I couldn't find that app.")
            return
        }

        val packageName = installedApps[target]

        if (packageName != null) {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)

            if (intent != null) {
                context.startActivity(intent)
                return
            }
        }

        speech.speak("I couldn't find that app.")
    }
}