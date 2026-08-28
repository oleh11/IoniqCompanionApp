package com.ioniq.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioniq.companion.data.model.CellData
import com.ioniq.companion.ui.theme.CardBorder
import com.ioniq.companion.ui.theme.CardBorderSubtle
import com.ioniq.companion.ui.theme.CellCool
import com.ioniq.companion.ui.theme.CellHot
import com.ioniq.companion.ui.theme.CellNominal
import com.ioniq.companion.ui.theme.CellWarm
import com.ioniq.companion.ui.theme.EcoGreen
import com.ioniq.companion.ui.theme.IoniqCyan
import com.ioniq.companion.ui.theme.SurfaceCard
import com.ioniq.companion.ui.theme.SurfaceCardElevated
import com.ioniq.companion.ui.theme.SurfaceCardSecondary
import com.ioniq.companion.ui.theme.TextMuted
import com.ioniq.companion.ui.theme.TextPrimary
import com.ioniq.companion.ui.theme.TextSecondary

/**
 * Hyundai E-GMP 192-Cell Battery Pack Architectural Heatmap Matrix
 * Organizes 32 Physical Modules (6 cells/module) with individual voltage balance visualization.
 */
@Composable
fun CellVoltageHeatmap(
    cellData: CellData,
    modifier: Modifier = Modifier
) {
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }
    val hasCells = cellData.cellVoltages.isNotEmpty()
    val totalCells = if (hasCells) cellData.cellVoltages.size else 192

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BATTERY CELL MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.0.sp
                    )
                    Text(
                        text = if (hasCells) "E-GMP Skateboard Pack (32 Modules • 192 Cells)" else "Standby (Awaiting OBD Connection)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                val deltaColor = when {
                    !hasCells -> TextMuted
                    cellData.deltaVoltageMv < 25f -> EcoGreen
                    cellData.deltaVoltageMv < 40f -> CellWarm
                    else -> CellHot
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(deltaColor.copy(alpha = 0.12f))
                        .border(1.dp, deltaColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hasCells) "ΔV ${cellData.deltaVoltageMv.toInt()} mV" else "ΔV --",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cell Summary Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCardSecondary)
                    .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MIN CELL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (hasCells) "${String.format("%.3f", cellData.minCellVoltage)} V" else "--",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCells) CellCool else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = if (hasCells) "#${cellData.minCellIndex + 1}" else "--", fontSize = 9.sp, color = TextMuted)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AVG CELL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    val avgV = if (hasCells) cellData.cellVoltages.average().toFloat() else 0f
                    Text(
                        text = if (hasCells) "${String.format("%.3f", avgV)} V" else "--",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCells) TextPrimary else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = if (hasCells) "Nominal" else "Disconnected", fontSize = 9.sp, color = TextMuted)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MAX CELL", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (hasCells) "${String.format("%.3f", cellData.maxCellVoltage)} V" else "--",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCells) CellWarm else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(text = if (hasCells) "#${cellData.maxCellIndex + 1}" else "--", fontSize = 9.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 192 Cells Matrix (16 columns x 12 rows)
            LazyVerticalGrid(
                columns = GridCells.Fixed(16),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(totalCells) { index ->
                    val color = if (hasCells) {
                        val voltage = cellData.cellVoltages.getOrElse(index) { 3.75f }
                        calculateCellColor(voltage, cellData.minCellVoltage, cellData.maxCellVoltage)
                    } else {
                        SurfaceCardElevated
                    }

                    val isSelected = selectedCellIndex == index
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable(enabled = hasCells) {
                                selectedCellIndex = if (selectedCellIndex == index) null else index
                            }
                    )
                }
            }

            // Cell Inspection Pill
            if (selectedCellIndex != null && hasCells) {
                val idx = selectedCellIndex!!
                val volt = cellData.cellVoltages.getOrElse(idx) { 3.75f }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCardSecondary)
                        .border(1.dp, IoniqCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cell #${idx + 1} (Module ${(idx / 6) + 1})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = IoniqCyan
                    )
                    Text(
                        text = "${String.format("%.4f", volt)} V",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun calculateCellColor(voltage: Float, minV: Float, maxV: Float): Color {
    val span = (maxV - minV).coerceAtLeast(0.005f)
    val fraction = ((voltage - minV) / span).coerceIn(0f, 1f)

    return when {
        fraction < 0.25f -> CellCool
        fraction < 0.70f -> CellNominal
        fraction < 0.90f -> CellWarm
        else -> CellHot
    }
}
