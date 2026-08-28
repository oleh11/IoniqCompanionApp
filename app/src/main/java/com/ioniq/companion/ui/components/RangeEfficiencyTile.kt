package com.ioniq.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.EfficiencyData
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * Range & 15-Minute Rolling Efficiency Tile
 * Modeled directly after gburlingame/ioniq-app (EV Dashboard) mathematical specification:
 * - Steady long-term range projection
 * - Real-time mi/kWh
 * - 15-minute rolling average dashed reference line
 * - Green (better than 15-min average) vs Amber (below 15-min average) dynamic trendline
 */
@Composable
fun RangeEfficiencyTile(
    estRangeMiles: Float,
    estRangeKm: Float,
    efficiency: EfficiencyData,
    isMilesUnit: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayRange = if (isMilesUnit) "${estRangeMiles.toInt()} mi" else "${estRangeKm.toInt()} km"
    val displayEfficiency = if (isMilesUnit) "${String.format("%.1f", efficiency.currentMiPerKwh)} mi/kWh" else "${String.format("%.1f", efficiency.currentKmPerKwh)} km/kWh"
    val displayAvg = if (isMilesUnit) "${String.format("%.1f", efficiency.rollingAverageMiPerKwh)} avg" else "${String.format("%.1f", efficiency.rollingAverageMiPerKwh * 1.609f)} avg"
    val isBetter = efficiency.isBetterThanAverage
    val dotColor = if (isBetter) EcoGreen else PowerAmber

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Range & Current Efficiency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = IoniqCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RANGE & EFFICIENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }

                // Green/Amber Dot + Present Efficiency
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = displayEfficiency,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = dotColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Projected Range + 15-min Average subtext
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = displayRange,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Est. based on rolling consumption",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayAvg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "15-min avg",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 15-Minute Trend Sparkline Chart with Dashed Moving Average Line
            EfficiencyTrendCanvas(
                samples = efficiency.trendHistory,
                rollingAverage = efficiency.rollingAverageMiPerKwh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun EfficiencyTrendCanvas(
    samples: List<Float>,
    rollingAverage: Float,
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Collecting 15-minute driving pattern...",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        return
    }

    val minVal = (samples.minOrNull() ?: 2.5f).coerceAtMost(rollingAverage - 0.5f)
    val maxVal = (samples.maxOrNull() ?: 4.5f).coerceAtLeast(rollingAverage + 0.5f)
    val range = (maxVal - minVal).coerceAtLeast(0.5f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Draw dashed moving-average reference line
        val avgY = height - (((rollingAverage - minVal) / range) * height).coerceIn(4f, height - 4f)
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, avgY),
            end = Offset(width, avgY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = pathEffect
        )

        // 2. Draw segments with Green (above avg) / Amber (below avg)
        val stepX = width / (samples.size - 1)
        for (i in 0 until samples.size - 1) {
            val v1 = samples[i]
            val v2 = samples[i + 1]

            val x1 = i * stepX
            val y1 = height - (((v1 - minVal) / range) * height).coerceIn(4f, height - 4f)
            val x2 = (i + 1) * stepX
            val y2 = height - (((v2 - minVal) / range) * height).coerceIn(4f, height - 4f)

            val segmentAvg = (v1 + v2) / 2f
            val segmentColor = if (segmentAvg >= rollingAverage) EcoGreen else PowerAmber

            drawLine(
                color = segmentColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Draw dot at latest sample
        val latestVal = samples.last()
        val latestX = width
        val latestY = height - (((latestVal - minVal) / range) * height).coerceIn(4f, height - 4f)
        val dotColor = if (latestVal >= rollingAverage) EcoGreen else PowerAmber

        drawCircle(
            color = dotColor,
            radius = 4.dp.toPx(),
            center = Offset(latestX, latestY)
        )
    }
}
