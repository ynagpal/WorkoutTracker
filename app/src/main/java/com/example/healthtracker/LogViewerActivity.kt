package com.example.healthtracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LogViewerActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var toggleGroup: RadioGroup
    private lateinit var crashLogButton: RadioButton
    private lateinit var workoutLogButton: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        toggleGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }

        crashLogButton = RadioButton(this).apply {
            text = "Crash Logs"
            isChecked = true
        }

        workoutLogButton = RadioButton(this).apply {
            text = "Workout Logs"
        }

        toggleGroup.addView(crashLogButton)
        toggleGroup.addView(workoutLogButton)

        logTextView = TextView(this).apply {
            setPadding(8, 16, 8, 16)
            setTextIsSelectable(true)
            textSize = 14f
        }

        layout.addView(toggleGroup)
        layout.addView(logTextView)

        setContentView(layout)

        // Default load
        logTextView.text = Logger.getCrashLogs(this)

        toggleGroup.setOnCheckedChangeListener { _, checkedId ->
            logTextView.text = when (checkedId) {
                crashLogButton.id -> Logger.getCrashLogs(this)
                workoutLogButton.id -> Logger.getWorkoutLogs(this)
                else -> ""
            }
        }
    }
}
