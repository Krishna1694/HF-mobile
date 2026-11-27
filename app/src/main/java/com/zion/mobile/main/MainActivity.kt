package com.zion.mobile.main

import android.Manifest
import android.os.Bundle
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.content.pm.PackageManager
import com.zion.mobile.core.ContactData
import com.zion.mobile.core.SpeechManager

class MainActivity : Activity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var resultText: TextView
    private lateinit var contactData: ContactData
    
    private lateinit var commandEngine: MainCommandEngine

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
            contactData.loadContacts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechManager = SpeechManager(this)
        speechManager.initialize()

        contactData = ContactData(this)

        commandEngine = MainCommandEngine(this, speechManager)
        commandEngine.initialize()
        
        resultText = TextView(this).apply {
            text = "Tap the button and speak"
            textSize = 24f
        }

        val button = Button(this).apply {
            text = "🎤 Speak"
            setOnClickListener {
                speechManager.listen { text ->
                    commandEngine.handle(text)
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 100, 40, 40)
            addView(resultText)
            addView(button)
        }

        setContentView(layout)
        
        requestPermissions()
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
            contactData.loadContacts()
        }
    }

    override fun onDestroy() {
        speechManager.shutdown()
        super.onDestroy()
    }
}