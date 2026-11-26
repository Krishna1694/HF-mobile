package com.zion.mobile

import android.util.Log
import android.view.Gravity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import android.content.Context
import android.widget.LinearLayout
import android.service.voice.VoiceInteractionSession

class HFVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private lateinit var statusText: TextView

    override fun onCreate() {
        super.onCreate()
        statusText = TextView(context).apply {
            text = "Zion session started"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        statusText = TextView(context).apply {
            text = "Listening..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(32, 24, 32, 24)
            addView(
                statusText,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        window?.window?.apply {
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        setContentView(layout)
    }
    
}