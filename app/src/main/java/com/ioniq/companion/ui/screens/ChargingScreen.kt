package com.ioniq.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.VehicleTelemetry
import com.ioniq.companion.ui.components.StatCard
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CockpitBg
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.HyundaiBlue
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * 800V DC Fast Charging Telemetry Screen
 * Built according to Hyundai ccNC & Material Design 3 guidelines.
 */
@Composable
fun ChargingScreen(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val isCharging = telemetry.charging.isCharging || telemetry.battery.powerKw > 0.5f
    val chargingKw = if (telemetry.charging.isCharging) telemetry.charging.chargingPowerKw else abs(telemetry.battery.powerKw)
    val isConnected = telemetry.isConnected || telemetry.isDemoMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Charging Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHARGING TELEMETRY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IoniqCyan,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = if (isCharging) "800V DC Fast Charging Active" else if (isConnected) "Port Connected (Standby)" else "EVSE Disconnected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCharging) EcoGreen.copy(alpha = 0.12f) else TextMuted.copy(alpha = 0.12f))
                    .border(1.dp, if (isCharging) EcoGreen.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isCharging) "CHARGING" else "STANDBY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCharging) EcoGreen else TextSecondary,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Primary 240kW Charging Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INSTANTANEOUS CHARGING POWER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isCharging) String.format("%.1f", chargingKw) else "0.0",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCharging) EcoGreen else TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " kW",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "PACK VOLTAGE", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isConnected) "${telemetry.battery.voltage.toInt()} V" else "-- V",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "CURRENT DELIVERED", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isCharging) "${String.format("%.1f", abs(telemetry.battery.current))} A" else "0.0 A",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TIME TO 80%", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isCharging) "${telemetry.charging.estMinutesTo80} min" else "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Charging Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Session Added",
                value = if (isCharging) String.format("%.2f", telemetry.charging.sessionKwhDelivered) else "0.00",
                unit = "kWh",
                subValue = if (isCharging) "Started at ${telemetry.battery.socDisplay.toInt()}%" else "No active session",
                icon = Icons.Default.ElectricBolt,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Station Limit",
                value = if (isCharging) "${telemetry.charging.evseMaxPowerKw.toInt()}" else "--",
                unit = "kW",
                subValue = if (isCharging) "${telemetry.charging.evseMaxCurrentA.toInt()} A EVSE Max" else "EVSE Standby",
                icon = Icons.Default.EvStation,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Battery Temp",
                value = if (isConnected) String.format("%.1f", telemetry.battery.avgTempC) else "--",
                unit = "°C",
                subValue = if (isConnected) "Optimal 21.0°C" else "Standby",
                icon = Icons.Default.Thermostat,
                accentColor = if (telemetry.battery.avgTempC >= 20f) EcoGreen else PowerAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Time Remaining",
                value = if (isCharging) "${telemetry.charging.estMinutesTo100}" else "--",
                unit = "min",
                subValue = if (isCharging) "Target 100% SOC" else "Standby",
                icon = Icons.Default.AvTimer,
                accentColor = IoniqCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // EVSE & BMS Pilot Control Signal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BMS PILOT & PROTOCOL TELEMETRY",
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
                        Text(text = "REQUESTED CURRENT", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isCharging) "${telemetry.charging.requestedCurrentA.toInt()} A" else "-- A",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "PILOT DUTY CYCLE", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isCharging) "${telemetry.charging.pilotDutyCyclePercent.toInt()}%" else "--%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "PLUG STATE", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = if (isCharging) "DC Fast (CCS)" else "Disconnected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCharging) EcoGreen else TextMuted
                        )
                    }
                }
            }
        }
    }
}
