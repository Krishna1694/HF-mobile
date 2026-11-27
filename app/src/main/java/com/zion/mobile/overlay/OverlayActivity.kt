package com.zion.mobile.overlay

import android.os.Bundle
import android.app.Activity
import android.view.Gravity
import android.graphics.Color
import android.widget.TextView
import com.zion.mobile.core.SpeechManager

class OverlayActivity : Activity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var commandEngine: OverlayCommandEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechManager = SpeechManager(this)
        speechManager.initialize()

        commandEngine = OverlayCommandEngine(this, speechManager)
        commandEngine.initialize()

        val text = TextView(this).apply {
            text = "Zion\nListening..."
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }

        setContentView(text)

        window.setGravity(Gravity.BOTTOM)

        speechManager.listen { input ->
            commandEngine.handle(input)
        }
    }
}