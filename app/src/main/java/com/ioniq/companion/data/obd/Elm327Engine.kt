package com.ioniq.companion.data.obd

import com.ioniq.companion.data.bluetooth.BluetoothManager
import com.ioniq.companion.data.model.ConnectionState
import com.ioniq.companion.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * High-performance ELM327 command loop for E-GMP vehicles (Hyundai IONIQ 5 / EV6)
 * Handles AT configuration, ISO 15765-4 CAN headers, and multi-frame UDS assembly.
 */
class Elm327Engine(
    private val bluetoothManager: BluetoothManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private val TAG = "ELM327"

    private val _rawResponses = MutableSharedFlow<ObdResponse>(extraBufferCapacity = 64)
    val rawResponses: SharedFlow<ObdResponse> = _rawResponses.asSharedFlow()

    data class ObdResponse(
        val header: String,
        val command: String,
        val hexPayload: String
    )

    private val responseBuffer = StringBuilder()
    private val waitingLock = Any()
    private var expectedHeader: String = ""
    private var expectedCommand: String = ""

    init {
        scope.launch {
            bluetoothManager.incomingDataFlow.collect { line ->
                processIncomingLine(line)
            }
        }
    }

    private fun processIncomingLine(line: String) {
        val clean = line.replace(" ", "").trim()
        if (clean.isEmpty() || clean == "SEARCHING..." || clean == "STOPPED") return

        synchronized(waitingLock) {
            responseBuffer.append(clean).append("\n")
        }
    }

    fun startPolling() {
        stopPolling()
        pollingJob = scope.launch {
            AppLogger.i(TAG, "Starting ELM327 Initialization & Polling Sequence...")
            try {
                if (!initializeAdapter()) {
                    AppLogger.e(TAG, "ELM327 Adapter Initialization failed")
                    return@launch
                }
                AppLogger.i(TAG, "✓ ELM327 Initialized! Starting UDS Telemetry Polling Loop...")

                // Continuous UDS telemetry loop
                while (isActive) {
                    try {
                        if (bluetoothManager.connectionState.value !is ConnectionState.Connected) {
                            delay(1000)
                            continue
                        }

                        // 1. BMS Main Telemetry (Header 7E4, Service 22, PID 0101)
                        setHeader("7E4")
                        val bmsMain = queryPid("220101", 600)
                        if (bmsMain != null) {
                            _rawResponses.emit(ObdResponse("7E4", "220101", bmsMain))
                        }

                        // 2. VMCU Drive & Motor Telemetry (Header 7E2, Service 22, PID 0101)
                        setHeader("7E2")
                        val vmcu = queryPid("220101", 400)
                        if (vmcu != null) {
                            _rawResponses.emit(ObdResponse("7E2", "220101", vmcu))
                        }

                        // 3. Cell Voltages Batch #1 (Header 7E4, PID 0102) & #2 (PID 0103)
                        setHeader("7E4")
                        val cellBatch1 = queryPid("220102", 500)
                        if (cellBatch1 != null) {
                            _rawResponses.emit(ObdResponse("7E4", "220102", cellBatch1))
                        }

                        val cellBatch2 = queryPid("220103", 500)
                        if (cellBatch2 != null) {
                            _rawResponses.emit(ObdResponse("7E4", "220103", cellBatch2))
                        }

                        // 4. TPMS (Header 7A0, PID C00B)
                        setHeader("7A0")
                        val tpms = queryPid("22C00B", 400)
                        if (tpms != null) {
                            _rawResponses.emit(ObdResponse("7A0", "22C00B", tpms))
                        }

                        // 5. Climate / HVAC (Header 7B3, PID 0100)
                        setHeader("7B3")
                        val climate = queryPid("220100", 400)
                        if (climate != null) {
                            _rawResponses.emit(ObdResponse("7B3", "220100", climate))
                        }

                        delay(150) // High-speed refresh cycle (~3-4Hz)
                    } catch (e: Throwable) {
                        AppLogger.w(TAG, "Error in polling cycle: ${e.message}")
                        delay(500)
                    }
                }
            } catch (e: Throwable) {
                AppLogger.e(TAG, "Fatal error in ELM327 poller: ${e.message}", e)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun initializeAdapter(): Boolean {
        val initCommands = listOf(
            "ATZ",       // Reset ELM327
            "ATE0",      // Echo Off
            "ATL0",      // Linefeeds Off
            "ATS0",      // Spaces Off
            "ATH1",      // Headers On (vital for ECU response identification)
            "ATSP6",     // Protocol ISO 15765-4 CAN (11 bit ID, 500 kbaud)
            "ATCAF1",    // CAN Auto Formatting On
            "ATCFC1",    // CAN Flow Control On
            "ATFCSH7E4", // Set Flow Control Header to BMS
            "ATFCSD300000", // Standard flow control frame
            "ATFCSM1"    // Flow Control Mode 1
        )

        for (cmd in initCommands) {
            clearBuffer()
            AppLogger.d(TAG, "Init cmd: $cmd")
            bluetoothManager.sendCommand(cmd)
            delay(120)
        }
        return true
    }

    private suspend fun setHeader(headerHex: String) {
        if (expectedHeader != headerHex) {
            expectedHeader = headerHex
            bluetoothManager.sendCommand("ATSH$headerHex")
            delay(40)
        }
    }

    private suspend fun queryPid(pid: String, timeoutMs: Long = 500): String? {
        clearBuffer()
        expectedCommand = pid
        val sent = bluetoothManager.sendCommand(pid)
        if (!sent) return null

        return withTimeoutOrNull(timeoutMs) {
            while (isActive) {
                val current = getBufferContent()
                if (isMultiFrameComplete(current, pid)) {
                    val clean = cleanMultiFramePayload(current)
                    AppLogger.d("CAN-RX", "[$expectedHeader] $pid => $clean")
                    return@withTimeoutOrNull clean
                }
                delay(30)
            }
            AppLogger.w(TAG, "Timeout waiting for [$expectedHeader] $pid")
            null
        }
    }

    private fun clearBuffer() {
        synchronized(waitingLock) {
            responseBuffer.clear()
        }
    }

    private fun getBufferContent(): String {
        synchronized(waitingLock) {
            return responseBuffer.toString()
        }
    }

    private fun isMultiFrameComplete(buffer: String, expectedPid: String): Boolean {
        val lines = buffer.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return false

        // Check if ends with prompt > or has complete frames
        val last = lines.last()
        if (last == ">" || last.endsWith(">")) return true
        if (lines.any { it.contains("NO DATA") || it.contains("ERROR") || it.contains("UNABLE TO CONNECT") }) return true

        return lines.size >= 2
    }

    private fun cleanMultiFramePayload(rawBuffer: String): String {
        return try {
            val lines = rawBuffer.split("\n")
                .map { it.replace(">", "").trim() }
                .filter { it.isNotEmpty() && !it.startsWith("AT") && !it.contains("SEARCHING") && !it.contains("NODATA") }

            val hexOnly = StringBuilder()
            for (line in lines) {
                if (line.length >= 6 && (line.startsWith("7E") || line.startsWith("7A") || line.startsWith("7B"))) {
                    val payloadChunk = line.substring(3)
                    hexOnly.append(payloadChunk)
                } else {
                    hexOnly.append(line)
                }
            }
            hexOnly.toString()
        } catch (_: Exception) {
            ""
        }
    }
}
