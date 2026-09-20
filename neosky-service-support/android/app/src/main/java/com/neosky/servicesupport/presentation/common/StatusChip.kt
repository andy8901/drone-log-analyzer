package com.neosky.servicesupport.presentation.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.core.util.StatusTone

/**
 * The single rendering of a status/priority badge, used everywhere a ticket/drone/warranty/
 * maintenance/payment status appears, so a given status always looks the same across the app.
 * Callers resolve [tone] via [StatusColorMapper.toneFor] overloads for their specific enum.
 */
@Composable
fun StatusChip(label: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val color = StatusColorMapper.colorFor(tone)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
