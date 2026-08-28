package com.ioniq.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.IoniqCyan

/**
 * Real-time scrolling telemetry line chart with gradient area fill
 */
@Composable
fun TelemetryLineChart(
    dataPoints: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    lineColor: Color = IoniqCyan,
    minY: Float = -60f,
    maxY: Float = 240f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Horizontal zero line
            val zeroY = h - ((0f - minY) / (maxY - minY)) * h
            drawLine(
                color = CardBorderSubtle,
                start = Offset(0f, zeroY),
                end = Offset(w, zeroY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            if (dataPoints.size < 2) return@Canvas

            val stepX = w / (dataPoints.size - 1)
            val path = Path()
            val fillPath = Path()

            dataPoints.forEachIndexed { i, point ->
                val x = i * stepX
                val clampedVal = point.second.coerceIn(minY, maxY)
                val y = h - ((clampedVal - minY) / (maxY - minY)) * h

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            // Area Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.15f),
                        lineColor.copy(alpha = 0.01f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // Line Stroke
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
