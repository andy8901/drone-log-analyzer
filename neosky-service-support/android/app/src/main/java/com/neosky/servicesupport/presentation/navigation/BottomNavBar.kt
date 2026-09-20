package com.neosky.servicesupport.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class NavIcon(val selected: ImageVector, val unselected: ImageVector)

private fun iconFor(destination: BottomNavDestination): NavIcon = when (destination) {
    BottomNavDestination.HOME -> NavIcon(Icons.Filled.Home, Icons.Outlined.Home)
    BottomNavDestination.MY_DRONES -> NavIcon(Icons.Filled.FlightTakeoff, Icons.Outlined.FlightTakeoff)
    BottomNavDestination.TICKETS -> NavIcon(Icons.Filled.Assignment, Icons.Outlined.Assignment)
    BottomNavDestination.FLIGHT_LOG -> NavIcon(Icons.Filled.History, Icons.Outlined.History)
    BottomNavDestination.PROFILE -> NavIcon(Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun NeoSkyBottomNavBar(
    currentRoute: String?,
    onDestinationSelected: (BottomNavDestination) -> Unit,
) {
    NavigationBar {
        BottomNavDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.screen.route
            val icon = iconFor(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) icon.selected else icon.unselected,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
