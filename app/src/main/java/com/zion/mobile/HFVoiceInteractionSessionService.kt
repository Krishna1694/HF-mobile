package com.zion.mobile

import android.util.Log
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class HFVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle): VoiceInteractionSession {
        Log.d("ZION_ASSIST", "onNewSession called")
        return HFVoiceInteractionSession(this)
    }
}