package com.zion.mobile

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color
import android.widget.TextView
import android.widget.LinearLayout
import com.zion.mobile.core.SpeechManager
import android.service.voice.VoiceInteractionSession

class HFVoiceInteractionSession(context: android.content.Context) :
    VoiceInteractionSession(context) {

    private lateinit var statusText: TextView
    private lateinit var speechText: TextView
    private lateinit var speechManager: SpeechManager

    override fun onCreate() {
        super.onCreate()

        speechManager = SpeechManager(context)
        speechManager.initialize()

        val titleText = TextView(context).apply {
            text = "Zion"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        statusText = TextView(context).apply {
            text = "Listening..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        speechText = TextView(context).apply {
            text = ""
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(40, 32, 40, 32)

            addView(
                titleText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                statusText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            )

            addView(
                speechText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            )
        }

        setContentView(layout)

        window?.window?.apply {
            setGravity(Gravity.BOTTOM)

            attributes = attributes.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        statusText.text = "Listening..."

        speechManager.listen { text ->
            speechText.text = "\"$text\""
            statusText.text = "Processing..."
        }
    }

    override fun onDestroy() {
        speechManager.shutdown()
        super.onDestroy()
    }
}