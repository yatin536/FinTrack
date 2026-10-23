package com.example.fintrack.ui.components

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.CategorySpend
import com.example.fintrack.data.model.TrendPoint
import com.example.fintrack.theme.CyberCyan
import com.example.fintrack.theme.ElectricBlue
import com.example.fintrack.theme.EmeraldGreen
import com.example.fintrack.theme.NeonCoral
import java.util.Locale

/**
 * Pure Jetpack Compose dual-series trend chart:
 * - Sapphire & Cyan glowing bars for Income / Liquid Cash
 * - Coral smooth cubic bezier spline with gradient area fill for Expenses
 * - Encapsulated in a sleek glass container
 */
@Composable
fun TrendBarChart(
    trendPoints: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) {
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transaction data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val maxVal = trendPoints.maxOfOrNull { maxOf(it.expense, it.income) }?.coerceAtLeast(100.0) ?: 100.0

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cash Flow Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dual-series income bars & expense spline",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCoral)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Expense",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberCyan)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Income",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Canvas with Dual-Series: Bars for Income, Spline for Expense
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val chartHeight = size.height - 24.dp.toPx()
                val totalBars = trendPoints.size
                val groupWidth = size.width / totalBars
                val barWidth = 14.dp.toPx()

                // Baseline
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // 1. Draw Income Bars
                val barBrush = Brush.verticalGradient(
                    colors = listOf(CyberCyan, ElectricBlue)
                )

                val splineCoords = mutableListOf<Offset>()

                trendPoints.forEachIndexed { index, point ->
                    val groupCenter = index * groupWidth + (groupWidth / 2)

                    // Income Bar
                    val incHeight = ((point.income / maxVal) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                    if (incHeight > 0f) {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(groupCenter - (barWidth / 2), chartHeight - incHeight),
                            size = Size(barWidth, incHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Expense coordinate for spline
                    val expHeight = ((point.expense / maxVal) * chartHeight).toFloat().coerceIn(0f, chartHeight)
                    splineCoords.add(Offset(groupCenter, chartHeight - expHeight))
                }

                // 2. Draw Smooth Bezier Spline for Expense
                if (splineCoords.size > 1) {
                    val splinePath = Path()
                    val areaPath = Path()

                    splinePath.moveTo(splineCoords.first().x, splineCoords.first().y)
                    areaPath.moveTo(splineCoords.first().x, chartHeight)
                    areaPath.lineTo(splineCoords.first().x, splineCoords.first().y)

                    for (i in 0 until splineCoords.size - 1) {
                        val p0 = splineCoords[i]
                        val p1 = splineCoords[i + 1]
                        val controlPointX1 = p0.x + (p1.x - p0.x) / 2
                        val controlPointY1 = p0.y
                        val controlPointX2 = p0.x + (p1.x - p0.x) / 2
                        val controlPointY2 = p1.y

                        splinePath.cubicTo(
                            controlPointX1, controlPointY1,
                            controlPointX2, controlPointY2,
                            p1.x, p1.y
                        )
                        areaPath.cubicTo(
                            controlPointX1, controlPointY1,
                            controlPointX2, controlPointY2,
                            p1.x, p1.y
                        )
                    }

                    areaPath.lineTo(splineCoords.last().x, chartHeight)
                    areaPath.close()

                    // Draw translucent gradient under the spline
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCoral.copy(alpha = 0.28f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw the smooth spline stroke
                    drawPath(
                        path = splinePath,
                        color = NeonCoral,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw specular dots at spline nodes
                    splineCoords.forEach { coord ->
                        drawCircle(
                            color = NeonCoral,
                            radius = 4.dp.toPx(),
                            center = coord
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = coord
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // X-Axis Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                trendPoints.forEach { point ->
                    Text(
                        text = point.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Category breakdown list with color progress bars and rupee sums.
 */
@Composable
fun CategoryBreakdownView(
    categorySpends: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    if (categorySpends.isEmpty()) {
        Text(
            text = "No expenses recorded in this period",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 12.dp)
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categorySpends.forEach { spend ->
            val catColor = Color(spend.category.colorHex)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = spend.category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "${spend.totalAmountMoney.format()} (${String.format(Locale.getDefault(), "%.1f", spend.percentage)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (spend.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = catColor,
                    trackColor = catColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}
