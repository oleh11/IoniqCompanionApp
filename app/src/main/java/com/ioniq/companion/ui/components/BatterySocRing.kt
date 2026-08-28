package com.ioniq.companion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.BatteryData
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.PowerRed
import com.ioniq.companion.ui.theme.SurfaceCardElevated
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * Hyundai E-GMP State-of-Charge & High-Voltage Battery Telemetry Widget
 */
@Composable
fun BatterySocRing(
    battery: BatteryData,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animatedSoc by animateFloatAsState(
        targetValue = battery.socDisplay,
        animationSpec = tween(durationMillis = 350),
        label = "socAnimation"
    )

    val isConnected = battery.voltage > 0f

    val ringColor = when {
        battery.socDisplay > 20f -> IoniqCyan
        battery.socDisplay > 10f -> PowerAmber
        else -> PowerRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val center = Offset(w / 2f, h / 2f)
            val strokeWidth = 10.dp.toPx()
            val radius = (w - strokeWidth - 20.dp.toPx()) / 2f

            // 1. Background Arc Track (-90 deg top, 360 deg)
            drawArc(
                color = SurfaceCardElevated,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active SOC Arc
            if (isConnected && animatedSoc > 0f) {
                val sweep = (animatedSoc / 100f) * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to IoniqCyan.copy(alpha = 0.5f),
                        0.5f to ringColor,
                        1.0f to ringColor,
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BATTERY SOC",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.2.sp
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isConnected) "${animatedSoc.toInt()}" else "--",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isConnected) TextPrimary else TextMuted
                )
                Text(
                    text = "%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) ringColor else TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Text(
                text = if (isConnected) "BMS: ${String.format("%.1f", battery.socBms)}%" else "Disconnected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Sub-metrics (Pack Voltage & Remaining Energy)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isConnected) "${battery.voltage.toInt()} V" else "-- V",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IoniqCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "•", fontSize = 10.sp, color = TextMuted)
                Text(
                    text = if (isConnected) "${String.format("%.1f", battery.remainingEnergyKwh)} kWh" else "-- kWh",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EcoGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
