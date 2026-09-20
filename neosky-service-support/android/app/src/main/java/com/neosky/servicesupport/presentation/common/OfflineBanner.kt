package com.neosky.servicesupport.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neosky.servicesupport.presentation.theme.StatusColors

/**
 * App-wide banner shown whenever [com.neosky.servicesupport.core.util.ConnectivityObserver]
 * reports no connectivity. Hosted once at the NavHost scaffold level (see MainActivity) so
 * every screen gets it for free instead of each screen wiring its own connectivity check.
 */
@Composable
fun OfflineBanner(isOffline: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = StatusColors.warning.copy(alpha = 0.16f),
            contentColor = StatusColors.warning,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = "You're offline. Showing cached data — changes will sync automatically.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
