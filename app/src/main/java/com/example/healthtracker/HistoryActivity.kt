package com.example.healthtracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {
    private lateinit var workoutHistoryText: TextView
    private lateinit var sleepHistoryText: TextView
    private lateinit var dbWorkoutHelper: WorkoutDatabaseHelper
    private lateinit var dbSleepHelper: SleepDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        workoutHistoryText = findViewById(R.id.workoutHistoryText)
        sleepHistoryText = findViewById(R.id.sleepHistoryText)

        dbWorkoutHelper = WorkoutDatabaseHelper(this)
        dbSleepHelper = SleepDatabaseHelper(this)

        displayWorkoutHistory()
        displaySleepHistory()
    }

    private fun displayWorkoutHistory() {
        val workoutData = dbWorkoutHelper.getAllWorkouts()
        val historyText = StringBuilder()
        for (workout in workoutData) {
            historyText.append("Duration: ${workout.duration} min, Steps: ${workout.steps}, Heart Rate: ${workout.heartRate} BPM, Calories: ${workout.calories} kcal\n\n")
        }
        workoutHistoryText.text = historyText.toString()
    }

    private fun displaySleepHistory() {
        val sleepData = dbSleepHelper.getAllSleepData()
        val historyText = StringBuilder()
        for (sleep in sleepData) {
            historyText.append("Deep: ${sleep.deepSleep} min, Light: ${sleep.lightSleep} min, REM: ${sleep.remSleep} min, Awake: ${sleep.awakeTime} min, Score: ${sleep.sleepScore}\n\n")
        }
        sleepHistoryText.text = historyText.toString()
    }
}
