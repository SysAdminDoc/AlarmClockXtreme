package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.sysadmindoc.alarmclock.data.support.CrashLogScrubber
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
 * Crash logs are written to: files/crash_logs/crash_TIMESTAMP.txt, already
 * scrubbed of URLs, hosts, addresses and secrets.
 * Each file is trimmed after 50 entries to prevent unbounded growth.
 * Logs stay in app-private storage unless the user explicitly exports a
 * support bundle; the app never uploads crash logs automatically.
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

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US).format(Date())
        @Suppress("DEPRECATION")
        val threadId = thread.id
        val file = File(dir, "crash_${timestamp}_${threadId}.txt")

        val header = buildString {
            appendLine("Thread: ${thread.name}")
            appendLine("Time: $timestamp")
            appendLine("Version: ${getVersionInfo(context)}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("---")
        }

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        // Scrub before the bytes ever reach disk. The support bundle scrubs on
        // export, but a log sitting in app-private storage can still be pulled
        // by a device backup or a debug pull, and a stack trace routinely names
        // the host or URL the user configured.
        //
        // The header is written as-is. Every line of it is ours, none of it is
        // user data, and the timestamp reads as a phone number to the scrubber,
        // so passing it through would only cost us the one field that says when
        // the crash happened.
        file.writeText(header + CrashLogScrubber.scrub(sw.toString()))
        Log.e(TAG, "Crash log written to: ${file.absolutePath}")
    }

    private fun getVersionInfo(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${PackageInfoCompat.getLongVersionCode(pInfo)})"
        } catch (e: Exception) { "Unknown" }
    }

    /**
     * Get crash log files, newest first. Support export uses the files so names
     * and timestamps stay intact inside the generated bundle.
     */
    fun getLogFiles(context: Context): List<File> {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
