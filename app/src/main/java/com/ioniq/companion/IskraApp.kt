package com.ioniq.companion

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ioniq.companion.ui.CrashReportActivity
import com.ioniq.companion.util.AppLogger
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Custom Application class with a global on-device crash catcher.
 * Intercepts any unexpected exception, saves the stacktrace, and launches
 * an on-screen Crash Report screen with a 1-tap "Copy to Clipboard" button.
 */
class IskraApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            AppLogger.e("CRASH", "FATAL CRASH on thread ${thread.name}: $stackTrace", throwable)

            // Persist to SharedPreferences so it can also be viewed in the DTC tab
            val prefs = getSharedPreferences("iskra_crash_store", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_crash_trace", stackTrace)
                .putString("last_crash_message", throwable.message ?: "Unknown Exception")
                .putLong("last_crash_time", System.currentTimeMillis())
                .apply()

            // Launch the on-device crash viewer activity
            try {
                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra("stack_trace", stackTrace)
                    putExtra("error_message", throwable.message ?: "Unknown Exception")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                defaultUncaughtHandler?.uncaughtException(thread, throwable)
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
