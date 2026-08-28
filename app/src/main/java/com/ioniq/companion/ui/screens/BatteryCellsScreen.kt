package com.ioniq.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.VehicleTelemetry
import com.ioniq.companion.ui.components.CellVoltageHeatmap
import com.ioniq.companion.ui.components.StatCard
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CockpitBg
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.HyundaiBlue
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * In-depth Battery Health & 192-Cell Heatmap Diagnostic Screen
 * Built according to Hyundai ccNC & Material Design 3 guidelines.
 */
@Composable
fun BatteryCellsScreen(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val isConnected = telemetry.isConnected || telemetry.isDemoMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "HIGH VOLTAGE BATTERY SYSTEM",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = IoniqCyan,
            letterSpacing = 1.0.sp
        )

        // 192-Cell Voltage Heatmap Matrix (32 Modules)
        CellVoltageHeatmap(cellData = telemetry.cells)

        // Battery Health Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "State of Health",
                value = if (isConnected) String.format("%.1f", telemetry.battery.soh) else "--",
                unit = "%",
                subValue = if (isConnected) "Nominal 100%" else "Standby",
                icon = Icons.Default.HealthAndSafety,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Isolation Resist.",
                value = if (isConnected) "${telemetry.battery.isolationResistanceKohm}" else "--",
                unit = "kΩ",
                subValue = if (isConnected) "Safe > 500 kΩ" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        // Energy Throughput Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "LIFETIME ENERGY THROUGHPUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TOTAL CHARGED", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${String.format("%.1f", telemetry.battery.cumulativeEnergyChargedKwh)} kWh" else "-- kWh",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "TOTAL DISCHARGED", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${String.format("%.1f", telemetry.battery.cumulativeEnergyDischargedKwh)} kWh" else "-- kWh",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "EFFICIENCY", fontSize = 10.sp, color = TextMuted)
                        val eff = if (isConnected && telemetry.battery.cumulativeEnergyChargedKwh > 0) {
                            (telemetry.battery.cumulativeEnergyDischargedKwh / telemetry.battery.cumulativeEnergyChargedKwh) * 100f
                        } else 94.5f
                        Text(
                            text = if (isConnected) "${String.format("%.1f", eff)}%" else "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Auxiliary 12V Battery & LDC Subsystem
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "12V AUXILIARY & LDC CONVERTER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "AUX VOLTAGE", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${String.format("%.2f", telemetry.auxBattery.voltage)} V" else "-- V",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "LDC CURRENT", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${String.format("%.1f", telemetry.auxBattery.current)} A" else "-- A",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "AUX TEMP", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${telemetry.auxBattery.temperatureC.toInt()} °C" else "-- °C",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
