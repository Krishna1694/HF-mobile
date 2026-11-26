package com.zion.mobile

import android.service.voice.VoiceInteractionService

class HFVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()

        android.widget.Toast.makeText(
            this,
            "Zion VoiceInteractionService ready",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}