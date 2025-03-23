package com.example.healthtracker

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_FILE_NAME = "crash_log.txt"

    fun log(context: Context, tag: String, message: String) {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            val writer = FileWriter(logFile, true)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.append("[$time] [$tag]: $message\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogContent(context: Context): String {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        return if (logFile.exists()) logFile.readText() else "No logs found."
    }
}
