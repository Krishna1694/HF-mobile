package com.zion.mobile

import android.content.Intent
import android.speech.RecognitionService

class HFRecognitionService : RecognitionService() {

    override fun onStartListening(
        recognizerIntent: Intent,
        listener: Callback
    ) {
    }

    override fun onStopListening(listener: Callback) {
    }

    override fun onCancel(listener: Callback) {
    }
}