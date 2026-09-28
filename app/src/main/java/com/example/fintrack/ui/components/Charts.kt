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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.CategorySpend
import com.example.fintrack.data.model.TrendPoint
import java.util.Locale

/**
 * Pure Jetpack Compose hardware-accelerated dual-bar trend chart.
 * Displays Income vs Expense side-by-side with labels and value scaling.
 */
@Composable
fun TrendBarChart(
    trendPoints: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transaction data for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = trendPoints.maxOfOrNull { maxOf(it.expense, it.income) }?.coerceAtLeast(100.0) ?: 100.0
    val expenseColor = Color(0xFFEF4444) // Vibrant Red
    val incomeColor = Color(0xFF10B981) // Vibrant Green

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Flow Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(expenseColor))
                    Spacer(Modifier.width(4.dp))
                    Text("Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(incomeColor))
                    Spacer(Modifier.width(4.dp))
                    Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val chartHeight = size.height - 30.dp.toPx()
                val totalBars = trendPoints.size
                val groupWidth = size.width / totalBars
                val barWidth = 10.dp.toPx()

                // Baseline
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                trendPoints.forEachIndexed { index, point ->
                    val groupCenter = index * groupWidth + (groupWidth / 2)

                    // Expense Bar
                    val expBarHeight = ((point.expense / maxVal) * chartHeight).toFloat().coerceIn(4f, chartHeight)
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = Offset(groupCenter - barWidth - 2.dp.toPx(), chartHeight - expBarHeight),
                        size = Size(barWidth, expBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Income Bar
                    val incBarHeight = ((point.income / maxVal) * chartHeight).toFloat().coerceIn(4f, chartHeight)
                    drawRoundRect(
                        color = incomeColor,
                        topLeft = Offset(groupCenter + 2.dp.toPx(), chartHeight - incBarHeight),
                        size = Size(barWidth, incBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                trendPoints.forEach { point ->
                    Text(
                        text = point.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
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
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", spend.totalAmount)}  (${String.format(Locale.getDefault(), "%.1f", spend.percentage)}%)",
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
