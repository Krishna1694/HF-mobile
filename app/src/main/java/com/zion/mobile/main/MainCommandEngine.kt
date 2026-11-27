package com.zion.mobile.main

import android.content.Context
import com.zion.mobile.core.AppData
import com.zion.mobile.core.ContactData
import com.zion.mobile.core.SpeechManager
import com.zion.mobile.commands.AppCommands
import com.zion.mobile.commands.CommandParser
import com.zion.mobile.commands.CommandIntent
import com.zion.mobile.commands.ContactCommands

class MainCommandEngine(
    private val context: Context,
    private val speechManager: SpeechManager
) {
    
    private val appdata = AppData(context)
    private val contactdata = ContactData(context)

    private lateinit var installedApps: Map<String, String>

    private lateinit var parser: CommandParser
    private lateinit var appCommands: AppCommands
    private lateinit var contactCommands: ContactCommands

    fun initialize() {
        installedApps = appdata.loadInstalledApps()

        parser = CommandParser(installedApps)
        appCommands = AppCommands(
            context,
            speechManager,
            installedApps
        )
        contactCommands = ContactCommands(
            context,
            speechManager,
            contactdata
        )
    }

    fun handle(text: String) {
        val command = parser.parseCommand(text)

        when (command.intent) {
            CommandIntent.GREETING ->
                speechManager.speak("Hello.")

            CommandIntent.GET_NAME ->
                speechManager.speak("My name is Zion.")

            CommandIntent.REFRESH_APPS -> {
                installedApps = appdata.loadInstalledApps()
                speechManager.speak("Apps refreshed.")
            }

            CommandIntent.OPEN_APP -> {
                appCommands.openApp(command.target)
            }

            CommandIntent.CALL -> {
                contactCommands.callContact(command.target, command.recent)
            }

            CommandIntent.UNKNOWN ->
                speechManager.speak("I don't understand that yet.")
        }
    }
}