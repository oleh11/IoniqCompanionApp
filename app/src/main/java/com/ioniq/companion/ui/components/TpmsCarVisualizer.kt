package com.ioniq.companion.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.TpmsData
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.PowerAmber
import com.ioniq.companion.ui.theme.PowerRed
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.SurfaceCardElevated
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * 4-Wheel TPMS Visualizer with E-GMP skateboard chassis schematic and per-wheel metrics
 */
@Composable
fun TpmsCarVisualizer(
    tpms: TpmsData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TIRE PRESSURE & TEMPERATURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Door Placard Cold Target: ${tpms.recommendedPsi.toInt()} PSI",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (tpms.isWarningActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PowerRed.copy(alpha = 0.15f))
                            .border(1.dp, PowerRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LOW PRESSURE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PowerRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chassis Visualizer with 4 Tire Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Front Left & Rear Left
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TireCard(
                        position = "FRONT LEFT",
                        psi = tpms.frontLeftPsi,
                        tempC = tpms.frontLeftTempC,
                        recommendedPsi = tpms.recommendedPsi
                    )
                    TireCard(
                        position = "REAR LEFT",
                        psi = tpms.rearLeftPsi,
                        tempC = tpms.rearLeftTempC,
                        recommendedPsi = tpms.recommendedPsi
                    )
                }

                // Center: Architectural Vehicle Silhouette Canvas
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 160.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Skateboard chassis body
                        drawRoundRect(
                            color = SurfaceCardSecondary,
                            topLeft = Offset(w * 0.20f, h * 0.10f),
                            size = Size(w * 0.60f, h * 0.80f),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                        )
                        drawRoundRect(
                            color = CardBorder,
                            topLeft = Offset(w * 0.20f, h * 0.10f),
                            size = Size(w * 0.60f, h * 0.80f),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Battery Pack outline in center of skateboard
                        drawRoundRect(
                            color = SurfaceCardElevated,
                            topLeft = Offset(w * 0.26f, h * 0.28f),
                            size = Size(w * 0.48f, h * 0.44f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Windshield indicators
                        drawLine(
                            color = IoniqCyan.copy(alpha = 0.4f),
                            start = Offset(w * 0.28f, h * 0.32f),
                            end = Offset(w * 0.72f, h * 0.32f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = IoniqCyan.copy(alpha = 0.4f),
                            start = Offset(w * 0.28f, h * 0.68f),
                            end = Offset(w * 0.72f, h * 0.68f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // 4 Wheel blocks
                        val wheelW = w * 0.14f
                        val wheelH = h * 0.20f
                        val wheelColor = Color(0xFF2A3446)

                        // FL & FR
                        drawRoundRect(wheelColor, Offset(w * 0.04f, h * 0.16f), Size(wheelW, wheelH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                        drawRoundRect(wheelColor, Offset(w * 0.82f, h * 0.16f), Size(wheelW, wheelH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                        // RL & RR
                        drawRoundRect(wheelColor, Offset(w * 0.04f, h * 0.64f), Size(wheelW, wheelH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                        drawRoundRect(wheelColor, Offset(w * 0.82f, h * 0.64f), Size(wheelW, wheelH), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                    }
                }

                // Right Column: Front Right & Rear Right
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TireCard(
                        position = "FRONT RIGHT",
                        psi = tpms.frontRightPsi,
                        tempC = tpms.frontRightTempC,
                        recommendedPsi = tpms.recommendedPsi
                    )
                    TireCard(
                        position = "REAR RIGHT",
                        psi = tpms.rearRightPsi,
                        tempC = tpms.rearRightTempC,
                        recommendedPsi = tpms.recommendedPsi
                    )
                }
            }
        }
    }
}

@Composable
fun TireCard(
    position: String,
    psi: Float,
    tempC: Float,
    recommendedPsi: Float
) {
    val isConnected = psi > 0f
    val isLow = isConnected && psi < (recommendedPsi - 4f)
    val color = when {
        !isConnected -> TextMuted
        isLow -> PowerAmber
        else -> EcoGreen
    }

    Box(
        modifier = Modifier
            .size(width = 95.dp, height = 62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardSecondary)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = position, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(
                text = if (isConnected) "${String.format("%.1f", psi)} PSI" else "-- PSI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isConnected) "${tempC.toInt()} °C" else "-- °C",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
