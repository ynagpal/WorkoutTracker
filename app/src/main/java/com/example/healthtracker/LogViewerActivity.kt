package com.example.healthtracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

class LogViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val toggleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            val crashLogsBtn = RadioButton(this@LogViewerActivity).apply {
                text = "Crash Logs"
                id = 1
                isChecked = true
            }
            val workoutLogsBtn = RadioButton(this@LogViewerActivity).apply {
                text = "Workout Logs"
                id = 2
            }
            addView(crashLogsBtn)
            addView(workoutLogsBtn)
        }

        val logTextView = TextView(this).apply {
            setPadding(24)
            setTextIsSelectable(true)
            textSize = 14f
        }

        toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            val fileName = if (checkedId == 2) "workout_log.txt" else "crash_log.txt"
            logTextView.text = Logger.getLogContent(this, fileName)
        }

        layout.addView(toggleGroup)
        layout.addView(ScrollView(this).apply {
            addView(logTextView)
        })

        setContentView(layout)

        // Load initial crash log
        logTextView.text = Logger.getLogContent(this, "crash_log.txt")
    }
}
