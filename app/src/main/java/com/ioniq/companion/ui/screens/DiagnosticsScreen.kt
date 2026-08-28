package com.ioniq.companion.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioniq.companion.data.model.DtcItem
import com.ioniq.companion.data.model.DtcSeverity
import com.ioniq.companion.data.model.VehicleTelemetry
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.CockpitBg
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.HyundaiBlue
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.PowerRed
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary
import com.ioniq.companion.util.AppLogger
import com.ioniq.companion.util.LogEntry
import com.ioniq.companion.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multi-ECU Bus Scanner, DTC Diagnostics, CSV Exporter, On-Device Crash Viewer, and Live Terminal Logger
 */
@Composable
fun DiagnosticsScreen(
    telemetry: VehicleTelemetry,
    onExportCsv: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    val logs by AppLogger.logs.collectAsStateWithLifecycle()

    // Read stored crash log from SharedPreferences
    val prefs = remember { context.getSharedPreferences("iskra_crash_store", Context.MODE_PRIVATE) }
    var lastCrashTrace by remember { mutableStateOf(prefs.getString("last_crash_trace", null)) }
    var lastCrashMsg by remember { mutableStateOf(prefs.getString("last_crash_message", null)) }
    val lastCrashTime by remember { mutableStateOf(prefs.getLong("last_crash_time", 0L)) }

    val filteredLogs = remember(logs, selectedTagFilter) {
        if (selectedTagFilter == null) logs else logs.filter { it.tag == selectedTagFilter }
    }

    val mockModules = listOf(
        Pair("BMS (Battery Management System)", "0x7E4 • Responding (OK)"),
        Pair("VMCU (Vehicle & Motor Control Unit)", "0x7E2 • Responding (OK)"),
        Pair("ICCU (Integrated Central Control Unit)", "0x7E5 • Responding (OK)"),
        Pair("TPMS (Tire Pressure Monitoring)", "0x7A0 • Responding (OK)"),
        Pair("FATC (Full Auto Temperature Control)", "0x7B3 • Responding (OK)"),
        Pair("LDC (Low Voltage DC-DC Converter)", "0x7E4 • Responding (OK)")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "VEHICLE BUS & DTC DIAGNOSTICS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = IoniqCyan,
            letterSpacing = 1.0.sp
        )

        // Previous On-Device Crash Banner (if any)
        if (!lastCrashTrace.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PowerRed))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PowerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "SAVED CRASH REPORT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PowerRed,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear Saved Crash",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    prefs.edit().clear().apply()
                                    lastCrashTrace = null
                                    lastCrashMsg = null
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val timeStr = if (lastCrashTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(lastCrashTime)) else ""
                    Text(
                        text = "Recorded at $timeStr: ${lastCrashMsg ?: "Unexpected Error"}",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val fullText = buildString {
                                append("=== ISKRA ON-DEVICE CRASH LOG ===\n")
                                append("Time: $timeStr\n")
                                append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n")
                                append("Error: $lastCrashMsg\n\n")
                                append("Stacktrace:\n")
                                append(lastCrashTrace)
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Iskra Crash Log", fullText))
                            Toast.makeText(context, "✓ Crash report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PowerRed, contentColor = CockpitBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Copy Full Crash Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Diagnostic Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    AppLogger.i("DIAGNOSTICS", "Manual ECU Bus Scan Triggered")
                },
                colors = ButtonDefaults.buttonColors(containerColor = IoniqCyan, contentColor = CockpitBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Scan ECUs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val csv = onExportCsv()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, csv)
                        type = "text/csv"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Telemetry CSV"))
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = IoniqCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export CSV", fontSize = 12.sp, color = TextPrimary)
            }
        }

        // Live Diagnostic Terminal Console Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = IoniqCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "LIVE DIAGNOSTIC CONSOLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Copy & Share Log Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Logs",
                            tint = IoniqCyan,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val text = AppLogger.exportFormattedLogs()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Iskra Logs", text))
                                    Toast.makeText(context, "Copied ${logs.size} log lines to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Logs",
                            tint = IoniqCyan,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val text = AppLogger.exportFormattedLogs()
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Iskra Logs"))
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    AppLogger.clear()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tag Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(null to "ALL", "BLUETOOTH" to "BT", "ELM327" to "ELM", "CAN-RX" to "CAN").forEach { (tag, label) ->
                        val isSelected = selectedTagFilter == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) IoniqCyan else SurfaceCardSecondary)
                                .clickable { selectedTagFilter = tag }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CockpitBg else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Log Output Terminal Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitBg)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Text(
                            text = "Console ready. Connect adapter to view handshake and CAN stream...",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(filteredLogs.size) {
                            listState.animateScrollToItem(filteredLogs.size - 1)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredLogs) { entry ->
                                LogLineView(entry = entry)
                            }
                        }
                    }
                }
            }
        }

        // Active Diagnostic Trouble Codes (DTC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIAGNOSTIC TROUBLE CODES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )

                    val dtcCount = telemetry.diagnostics.activeDtcs.size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (dtcCount == 0) EcoGreen.copy(alpha = 0.12f) else PowerRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (dtcCount == 0) "NO FAULTS" else "$dtcCount DTC FOUND",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dtcCount == 0) EcoGreen else PowerRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (telemetry.diagnostics.activeDtcs.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardSecondary)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Text(
                                text = "All Powertrain & BMS Systems Normal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "No active DTCs detected across CAN 11-bit ECUs",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    telemetry.diagnostics.activeDtcs.forEach { dtc ->
                        DtcItemRow(dtc = dtc)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Responding E-GMP Control Modules List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "RESPONDING CAN ECUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                mockModules.forEach { (name, address) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name, fontSize = 12.sp, color = TextPrimary)
                        Text(
                            text = address,
                            fontSize = 11.sp,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogLineView(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.DEBUG -> TextMuted
        LogLevel.INFO -> IoniqCyan
        LogLevel.WARN -> PowerAmber
        LogLevel.ERROR -> PowerRed
    }

    Text(
        text = "[${entry.formattedTime}] [${entry.tag}] ${entry.message}",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = color,
        lineHeight = 14.sp
    )
}

@Composable
fun DtcItemRow(dtc: DtcItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardSecondary)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = if (dtc.severity == DtcSeverity.CRITICAL) PowerRed else PowerAmber,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${dtc.code} • ${dtc.module}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(text = dtc.description, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
