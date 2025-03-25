package com.example.healthtracker

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {

    private const val CRASH_LOG_FILE = "crash_log.txt"
    private const val WORKOUT_LOG_FILE = "workout_log.txt"

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun log(context: Context, tag: String, message: String) {
        val logLine = "[${getTimestamp()}] [$tag]: $message"
        writeToFile(context, CRASH_LOG_FILE, logLine)
    }

    fun logWorkout(context: Context, message: String) {
        val logLine = "[${getTimestamp()}] $message"
        writeToFile(context, WORKOUT_LOG_FILE, logLine)
    }

    fun getCrashLogs(context: Context): String {
        val file = File(context.filesDir, CRASH_LOG_FILE)
        return if (file.exists()) file.readText() else "No crash logs found."
    }

    fun getWorkoutLogs(context: Context): String {
        val file = File(context.filesDir, WORKOUT_LOG_FILE)
        return if (file.exists()) file.readText() else "No workout logs found."
    }

    private fun writeToFile(context: Context, filename: String, content: String) {
        try {
            val file = File(context.filesDir, filename)
            val writer = FileWriter(file, true)
            writer.appendLine(content)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
