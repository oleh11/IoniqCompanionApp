package com.ioniq.companion.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
) {
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val formattedTime: String get() = timeFormatter.format(Date(timestamp))

    fun toFormattedString(): String {
        val lvl = level.name.padEnd(5)
        val tg = tag.padEnd(12)
        return "[$formattedTime] [$lvl] [$tg] $message"
    }
}

/**
 * High-performance observable logger with in-memory ring buffer and clipboard/export utilities.
 */
object AppLogger {

    private const val MAX_LOG_ENTRIES = 600
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        log(tag, message, LogLevel.DEBUG)
        Log.d("Iskra-$tag", message)
    }

    fun i(tag: String, message: String) {
        log(tag, message, LogLevel.INFO)
        Log.i("Iskra-$tag", message)
    }

    fun w(tag: String, message: String) {
        log(tag, message, LogLevel.WARN)
        Log.w("Iskra-$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message | Ex: ${throwable.message}" else message
        log(tag, fullMsg, LogLevel.ERROR)
        Log.e("Iskra-$tag", fullMsg, throwable)
    }

    @Synchronized
    private fun log(tag: String, message: String, level: LogLevel) {
        val entry = LogEntry(tag = tag, message = message, level = level)
        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOG_ENTRIES) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun exportFormattedLogs(): String {
        val sb = StringBuilder()
        sb.append("=== ISKRA EV COMPANION TELEMETRY & BLUETOOTH LOG ===\n")
        sb.append("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("Total entries: ${_logs.value.size}\n\n")

        _logs.value.forEach { entry ->
            sb.append(entry.toFormattedString()).append("\n")
        }
        return sb.toString()
    }
}
