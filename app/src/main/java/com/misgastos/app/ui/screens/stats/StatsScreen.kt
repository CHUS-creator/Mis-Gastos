package com.misgastos.app.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.data.dao.CategoryTotal
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.util.categoryLabel
import com.misgastos.app.viewmodel.MisGastosViewModel

private val chartColors = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFFC62828),
    Color(0xFFFFA000),
    Color(0xFF6A1B9A),
    Color(0xFF00838F),
    Color(0xFFEF6C00),
    Color(0xFF455A64),
    Color(0xFFAD1457),
    Color(0xFF558B2F),
    Color(0xFF283593),
    Color(0xFF4E342E),
    Color(0xFF37474F),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: MisGastosViewModel) {
    val categories by viewModel.monthExpensesByCategory.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_by_category, dashboard.monthLabel),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.stats_total, formatMoney(dashboard.monthExpenses)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            if (categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                val maxTotal = categories.maxOf { it.total }.coerceAtLeast(0.0)
                val totalSum = categories.sumOf { it.total }.coerceAtLeast(0.0)
                CategoryPieChart(
                    categories = categories,
                    totalSum = totalSum,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { item ->
                        CategoryBar(
                            category = item.category,
                            total = item.total,
                            fraction = if (maxTotal > 0) (item.total / maxTotal).toFloat() else 0f,
                            color = colorForCategory(item, categories),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPieChart(
    categories: List<CategoryTotal>,
    totalSum: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_pie_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        drawPieChart(categories, totalSum)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.take(6).forEachIndexed { index, item ->
                        LegendItem(
                            color = chartColors[index % chartColors.size],
                            label = categoryLabel(item.category),
                            value = item.total,
                        )
                    }
                    if (categories.size > 6) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPieChart(
    categories: List<CategoryTotal>,
    totalSum: Double,
) {
    if (totalSum <= 0.0) return
    val sweep = size.minDimension
    val topLeft = Offset(
        x = (size.width - sweep) / 2f,
        y = (size.height - sweep) / 2f,
    )
    val arcSize = Size(sweep, sweep)
    var startAngle = -90f
    categories.forEachIndexed { index, item ->
        val fraction = (item.total / totalSum).toFloat()
        val angle = fraction * 360f
        drawArc(
            color = chartColors[index % chartColors.size],
            startAngle = startAngle,
            sweepAngle = angle,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
        startAngle += angle
    }
    drawArc(
        color = Color.White,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = true,
        topLeft = Offset(topLeft.x + sweep * 0.3f, topLeft.y + sweep * 0.3f),
        size = Size(sweep * 0.4f, sweep * 0.4f),
    )
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: Double,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMoney(value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun colorForCategory(item: CategoryTotal, categories: List<CategoryTotal>): Color {
    val index = categories.indexOf(item)
    return chartColors[index % chartColors.size]
}

@Composable
private fun CategoryBar(
    category: String,
    total: Double,
    fraction: Float,
    color: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = categoryLabel(category), fontWeight = FontWeight.Medium)
                Text(text = formatMoney(total), fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(12.dp)
                        .background(color, MaterialTheme.shapes.small),
                )
            }
        }
    }
}
