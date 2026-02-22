package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple crash logger that writes uncaught exceptions to a file.
 * Useful for debugging before integrating a full crash reporting SDK.
 *
 * Install in Application.onCreate():
 *   CrashLogger.install(this)
 *
 * Crash logs are written to: files/crash_logs/crash_TIMESTAMP.txt
 * Each file is trimmed after 50 entries to prevent unbounded growth.
 */
object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val MAX_LOG_FILES = 50
    private const val DIR_NAME = "crash_logs"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeLog(context, thread, throwable)
            } catch (_: Exception) {
                // Can't crash inside crash handler
            }
            // Chain to default handler (shows system crash dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeLog(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }

        // Trim old logs
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
        if (files.size >= MAX_LOG_FILES) {
            files.take(files.size - MAX_LOG_FILES + 1).forEach { it.delete() }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "crash_$timestamp.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("Thread: ${thread.name}")
        pw.println("Time: $timestamp")
        pw.println("Version: ${getVersionInfo(context)}")
        pw.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        pw.println("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        pw.println("---")
        throwable.printStackTrace(pw)
        pw.flush()

        file.writeText(sw.toString())
        Log.e(TAG, "Crash log written to: ${file.absolutePath}")
    }

    private fun getVersionInfo(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }

    /**
     * Get all crash logs, newest first.
     */
    fun getLogs(context: Context): List<String> {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.readText() }
            ?: emptyList()
    }

    /**
     * Clear all crash logs.
     */
    fun clearLogs(context: Context) {
        val dir = File(context.filesDir, DIR_NAME)
        dir.listFiles()?.forEach { it.delete() }
    }
}
