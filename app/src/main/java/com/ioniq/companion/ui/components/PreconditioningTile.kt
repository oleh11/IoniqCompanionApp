package com.ioniq.companion.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Thermostat
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
import com.ioniq.companion.data.model.BatteryData
import com.ioniq.companion.data.model.PreconditioningData
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.HyundaiBlue
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * Battery Preconditioning Readiness ETA Tile
 * Modeled after gburlingame/ioniq-app live "Time to 70°F / 21°C" fast-charge readiness tracker.
 */
@Composable
fun PreconditioningTile(
    preconditioning: PreconditioningData,
    battery: BatteryData,
    modifier: Modifier = Modifier
) {
    val isReady = battery.minTempC >= 21.0f
    val isWarming = preconditioning.isActive || (battery.minTempC < 21.0f && battery.powerKw < -10f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarming) PowerAmber.copy(alpha = 0.08f) else SurfaceCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isWarming) PowerAmber.copy(alpha = 0.4f) else CardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (isWarming) PowerAmber else if (isReady) EcoGreen else IoniqCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRECONDITIONING READINESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                // Status Capsule Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isWarming) PowerAmber.copy(alpha = 0.15f)
                            else if (isReady) EcoGreen.copy(alpha = 0.15f)
                            else HyundaiBlue.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isWarming) PowerAmber.copy(alpha = 0.4f)
                            else if (isReady) EcoGreen.copy(alpha = 0.4f)
                            else HyundaiBlue.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isWarming) "WARMING ACTIVE" else if (isReady) "800V DC READY" else "NORMAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWarming) PowerAmber else if (isReady) EcoGreen else IoniqCyan,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isReady) "Optimal for DC Fast Charge"
                        else if (preconditioning.timeToTargetMin > 0) "~${preconditioning.timeToTargetMin} min to 21°C (70°F)"
                        else "Battery at ${battery.avgTempC.toInt()}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Target 21°C (70°F) for maximum 235+ kW charge curve",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (preconditioning.timeToTargetMin > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = PowerAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${preconditioning.timeToTargetMin}m",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = PowerAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (preconditioning.heaterPowerKw > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PTC Battery Heater Draw",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "${String.format("%.1f", preconditioning.heaterPowerKw)} kW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PowerAmber,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
