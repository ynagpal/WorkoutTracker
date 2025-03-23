package com.example.healthtracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logTextView = TextView(this).apply {
            text = Logger.getLogContent(this@LogViewerActivity)
            setPadding(16, 16, 16, 16)
            setTextIsSelectable(true)
        }

        setContentView(logTextView)
    }
}
