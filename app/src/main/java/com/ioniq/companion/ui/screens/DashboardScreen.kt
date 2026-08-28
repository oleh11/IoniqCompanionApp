package com.ioniq.companion.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.ioniq.companion.ui.components.BatterySocRing
import com.ioniq.companion.ui.components.PowerDialGauge
import com.ioniq.companion.ui.components.PreconditioningTile
import com.ioniq.companion.ui.components.RangeEfficiencyTile
import com.ioniq.companion.ui.components.StatCard
import com.ioniq.companion.ui.components.TelemetryLineChart
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
import kotlin.math.abs

/**
 * Main Cockpit Driving Dashboard Screen
 * Modeled after gburlingame/ioniq-app (EV Dashboard) and Hyundai ccNC guidelines.
 */
@Composable
fun DashboardScreen(
    telemetry: VehicleTelemetry,
    powerHistory: List<Pair<Long, Float>>,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vehicle Profile & Telemetry Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = telemetry.vehicleModel.displayName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IoniqCyan,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = if (telemetry.isConnected) "Live CAN Telemetry" else if (telemetry.isDemoMode) "Simulation Active" else "Adapter Disconnected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            when {
                telemetry.isDemoMode -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(IoniqCyan.copy(alpha = 0.12f))
                            .border(1.dp, IoniqCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SIMULATOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IoniqCyan,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                telemetry.isConnected -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(EcoGreen.copy(alpha = 0.12f))
                            .border(1.dp, EcoGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CONNECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PowerAmber.copy(alpha = 0.10f))
                            .border(1.dp, PowerAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DISCONNECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PowerAmber,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }

        // Connection Action Card (when disconnected and not in demo)
        if (!telemetry.isConnected && !telemetry.isDemoMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(IoniqCyan.copy(alpha = 0.3f)))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = IoniqCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Text(
                                text = "Veepeak Adapter Disconnected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap to pair Veepeak OBDCheck or switch to Demo Mode",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = IoniqCyan, contentColor = CockpitBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Twin Digital Instrument Cluster Dials (Power Meter & Battery SOC Ring)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerDialGauge(
                powerKw = telemetry.battery.powerKw,
                gear = telemetry.drive.gear,
                speedKmh = telemetry.drive.speedKmh,
                size = 175.dp
            )

            BatterySocRing(
                battery = telemetry.battery,
                size = 175.dp
            )
        }

        // Range & 15-Minute Efficiency Tile (gburlingame/ioniq-app Model)
        RangeEfficiencyTile(
            estRangeMiles = telemetry.battery.estRangeMiles,
            estRangeKm = telemetry.battery.estRangeKm,
            efficiency = telemetry.efficiency,
            isMilesUnit = true
        )

        // Preconditioning Readiness & Battery Warmer ETA Tile
        PreconditioningTile(
            preconditioning = telemetry.preconditioning,
            battery = telemetry.battery
        )

        // Live Power Dynamic Flow Chart
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
                        text = "POWER FLOW CURVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (telemetry.isConnected || telemetry.isDemoMode) "${String.format("%.1f", abs(telemetry.battery.powerKw))} kW" else "0.0 kW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (telemetry.battery.powerKw > 0) EcoGreen else IoniqCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TelemetryLineChart(
                    dataPoints = powerHistory,
                    lineColor = if (telemetry.battery.powerKw > 0) EcoGreen else IoniqCyan
                )
            }
        }

        // Primary Telemetry Grid (Voltage, Battery Temps, Motor RPM, 12V Aux Battery)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Pack Voltage",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.1f", telemetry.battery.voltage) else "--",
                unit = "V",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Nominal 720V" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = IoniqCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Battery Temp",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.1f", telemetry.battery.avgTempC) else "--",
                unit = "°C",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Min ${telemetry.battery.minTempC.toInt()}° • Max ${telemetry.battery.maxTempC.toInt()}°" else "Standby",
                icon = Icons.Default.Thermostat,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Motor RPM",
                value = if (telemetry.isConnected || telemetry.isDemoMode) "${telemetry.drive.frontMotorRpm}" else "--",
                unit = "rpm",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Motor ${telemetry.drive.motorTempC.toInt()}°C" else "Standby",
                icon = Icons.Default.Speed,
                accentColor = IoniqCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Aux 12V Battery",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.2f", telemetry.auxBattery.voltage) else "--",
                unit = "V",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "SOC ${telemetry.auxBattery.soc.toInt()}% • LDC Active" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Subsystem Power Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ENERGY CONSUMPTION BREAKDOWN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                PowerRow(
                    label = "Drivetrain (Front & Rear Motors)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.drive.motorPowerDrawKw else 0f,
                    color = IoniqCyan
                )
                PowerRow(
                    label = "Climate Control (HVAC / Heat Pump)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.climate.hvacPowerDrawKw else 0f,
                    color = HyundaiBlue
                )
                PowerRow(
                    label = "High-Voltage Battery Heater",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.preconditioning.heaterPowerKw else 0f,
                    color = PowerAmber
                )
                PowerRow(
                    label = "Auxiliary Electronics (12V LDC)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) 0.35f else 0f,
                    color = EcoGreen
                )
            }
        }
    }
}

@Composable
fun PowerRow(
    label: String,
    kw: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
            Text(
                text = "  $label",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "${String.format("%.2f", kw)} kW",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
