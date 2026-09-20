package com.neosky.servicesupport.presentation.flights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A minimal, dependency-free bar chart drawn on a single [Canvas], used for the flight-hours /
 * flights-per-month and hours-per-drone breakdowns on FlightLogListScreen. Deliberately does not
 * pull in a third-party charting library, per the architecture guidance.
 */
@Composable
fun SimpleBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Float) -> String = { it.toString() },
) {
    if (data.isEmpty()) return
    val maxValue = (data.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(0.01f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 4.dp),
        ) {
            val barCount = data.size
            val spacing = size.width * 0.02f
            val barWidth = (size.width - spacing * (barCount + 1)) / barCount
            data.forEachIndexed { index, (_, value) ->
                val barHeight = (value / maxValue) * size.height
                val left = spacing + index * (barWidth + spacing)
                val top = size.height - barHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            data.forEach { (label, value) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
