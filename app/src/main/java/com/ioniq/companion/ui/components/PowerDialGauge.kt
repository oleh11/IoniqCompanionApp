package com.ioniq.companion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.Gear
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.PowerRed
import com.ioniq.companion.ui.theme.SurfaceCardElevated
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hyundai ccNC / E-GMP Digital Instrument Cluster Power Meter
 * Bi-directional Power & Charge Gauge with OEM tick marks, segmented power arc, and gear indicator.
 */
@Composable
fun PowerDialGauge(
    powerKw: Float,
    gear: Gear,
    speedKmh: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val animatedPower by animateFloatAsState(
        targetValue = powerKw,
        animationSpec = tween(durationMillis = 180),
        label = "powerDialAnimation"
    )

    val isRegen = animatedPower > 0.5f
    val isDraw = animatedPower < -0.5f
    val absPower = abs(animatedPower)

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

            // Total arc: Starts at 140° and sweeps 260° (to 400°). Top 0 kW is at 270°.
            val startAngle = 140f
            val totalSweep = 260f
            val zeroAngle = 270f

            // 1. Background Arc Track
            drawArc(
                color = SurfaceCardElevated,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Precision Tick Marks (Every 20 degrees along the perimeter)
            val numTicks = 14
            for (i in 0..numTicks) {
                val fraction = i.toFloat() / numTicks
                val angleDeg = startAngle + (fraction * totalSweep)
                val angleRad = (angleDeg * PI / 180.0).toFloat()

                val isZeroTick = abs(angleDeg - zeroAngle) < 2f
                val isChargeTick = angleDeg < zeroAngle
                val tickLen = if (isZeroTick) 10.dp.toPx() else 5.dp.toPx()
                val tickColor = when {
                    isZeroTick -> TextPrimary
                    isChargeTick -> EcoGreen.copy(alpha = 0.5f)
                    else -> IoniqCyan.copy(alpha = 0.5f)
                }

                val innerR = radius + (strokeWidth / 2f) + 3.dp.toPx()
                val outerR = innerR + tickLen

                val p1 = center + Offset(cos(angleRad) * innerR, sin(angleRad) * innerR)
                val p2 = center + Offset(cos(angleRad) * outerR, sin(angleRad) * outerR)

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isZeroTick) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. Dynamic Active Power/Charge Arc
            if (isRegen) {
                // Regen Arc (Sweeps counter-clockwise from 270° towards 140°, max -80 kW)
                val regenFraction = (absPower / 80f).coerceIn(0f, 1f)
                val regenSweep = -regenFraction * (zeroAngle - startAngle)
                drawArc(
                    brush = Brush.sweepGradient(
                        0.25f to EcoGreen.copy(alpha = 0.3f),
                        0.75f to EcoGreen,
                        center = center
                    ),
                    startAngle = zeroAngle,
                    sweepAngle = regenSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else if (isDraw) {
                // Power Draw Arc (Sweeps clockwise from 270° towards 400°, max +220 kW)
                val maxDrawSweep = (startAngle + totalSweep) - zeroAngle
                val drawFraction = (absPower / 220f).coerceIn(0f, 1f)
                val drawSweep = drawFraction * maxDrawSweep
                val color = if (absPower > 140f) PowerRed else if (absPower > 70f) PowerAmber else IoniqCyan

                drawArc(
                    brush = Brush.sweepGradient(
                        0.75f to IoniqCyan,
                        1.0f to color,
                        center = center
                    ),
                    startAngle = zeroAngle,
                    sweepAngle = drawSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Readout Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            // Status Tag
            Text(
                text = when {
                    isRegen -> "CHARGE"
                    isDraw -> "POWER"
                    else -> "READY"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isRegen -> EcoGreen
                    isDraw -> if (absPower > 140f) PowerRed else PowerAmber
                    else -> IoniqCyan
                },
                letterSpacing = 1.2.sp
            )

            // Primary Numerical Readout
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", absPower),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = " kW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hyundai Digital Cluster Gear Indicator (P R N D)
            HyundaiGearCluster(activeGear = gear)
        }
    }
}

@Composable
fun HyundaiGearCluster(
    activeGear: Gear
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardSecondary)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE).forEach { g ->
            val isActive = g == activeGear
            Text(
                text = g.label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isActive) IoniqCyan else TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
