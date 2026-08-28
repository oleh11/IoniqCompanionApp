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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
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
import com.ioniq.companion.ui.components.StatCard
import com.ioniq.companion.ui.components.TpmsCarVisualizer
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CockpitBg
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.HyundaiBlue
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * Vehicle Telemetry & TPMS Diagnostics Screen (Motors, Inverter, Climate, Tires)
 * Built according to Hyundai ccNC & Material Design 3 guidelines.
 */
@Composable
fun VehicleTpmsScreen(
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
            text = "VEHICLE SYSTEMS & TPMS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = IoniqCyan,
            letterSpacing = 1.0.sp
        )

        // 4-Wheel TPMS Visualizer Card
        TpmsCarVisualizer(tpms = telemetry.tpms)

        // Drivetrain & Motor Thermals
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DUAL MOTOR & INVERTER TELEMETRY",
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
                        Text(text = "Front Motor RPM", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.frontMotorRpm}" else "--",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = if (isConnected) "Temp ${telemetry.drive.motorTempC.toInt()}°C" else "Standby", fontSize = 10.sp, color = TextMuted)
                    }

                    Column {
                        Text(text = "Rear Motor RPM", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.rearMotorRpm}" else "--",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = if (isConnected) "Temp ${telemetry.drive.motorTempC.toInt()}°C" else "Standby", fontSize = 10.sp, color = TextMuted)
                    }

                    Column {
                        Text(text = "Inverter Temp", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.inverterTempC.toInt()} °C" else "-- °C",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.drive.inverterTempC > 65f) PowerAmber else EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = "SiC Power Stage", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        // Climate & Thermal Control Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Cabin Temp",
                value = if (isConnected) String.format("%.1f", telemetry.climate.cabinTempC) else "--",
                unit = "°C",
                subValue = if (isConnected) "Outside: ${telemetry.climate.ambientTempC.toInt()}°C" else "Standby",
                icon = Icons.Default.Thermostat,
                accentColor = IoniqCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "HVAC Power",
                value = if (isConnected) String.format("%.2f", telemetry.climate.hvacPowerDrawKw) else "0.00",
                unit = "kW",
                subValue = if (isConnected) "Heat Pump Active" else "Off",
                icon = Icons.Default.AcUnit,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Vent Temp",
                value = if (isConnected) "${telemetry.climate.driverVentTempC.toInt()}" else "--",
                unit = "°C",
                subValue = if (isConnected) "Blower Stage 3" else "Standby",
                icon = Icons.Default.Air,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Battery Warmer",
                value = if (isConnected) String.format("%.2f", telemetry.climate.batteryWarmerPowerKw) else "0.00",
                unit = "kW",
                subValue = if (telemetry.climate.batteryWarmerPowerKw > 0) "Pre-heating" else "Standby",
                icon = Icons.Default.Speed,
                accentColor = PowerAmber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
