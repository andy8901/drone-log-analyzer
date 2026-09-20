package com.neosky.servicesupport.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.DashboardSummary
import com.neosky.servicesupport.presentation.common.EmptyState
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.QuickActionButton
import com.neosky.servicesupport.presentation.common.SectionHeader
import com.neosky.servicesupport.presentation.common.StatCard
import com.neosky.servicesupport.presentation.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDrones: () -> Unit,
    onNavigateToTickets: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onRaiseTicket: () -> Unit,
    onLogFlight: () -> Unit,
    onNavigateToWarrantyOrMaintenance: () -> Unit,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
    onTicketClick: (String) -> Unit,
    onInvoiceClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NeoSky Service & Support") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                    IconButton(onClick = onNotifications) {
                        if (unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text(unreadCount.coerceAtMost(99).toString()) } }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                            }
                        } else {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.summary == null -> LoadingIndicator(modifier = Modifier.padding(padding))
            state.summary == null -> EmptyState(
                title = "Couldn't load your dashboard",
                subtitle = state.error ?: "Pull to refresh once you're back online.",
                modifier = Modifier.padding(padding),
            )
            else -> DashboardContent(
                summary = state.summary!!,
                modifier = Modifier.padding(padding),
                onNavigateToDrones = onNavigateToDrones,
                onNavigateToTickets = onNavigateToTickets,
                onNavigateToFlights = onNavigateToFlights,
                onNavigateToInvoices = onNavigateToInvoices,
                onRaiseTicket = onRaiseTicket,
                onLogFlight = onLogFlight,
                onNavigateToWarrantyOrMaintenance = onNavigateToWarrantyOrMaintenance,
                onTicketClick = onTicketClick,
                onInvoiceClick = onInvoiceClick,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    modifier: Modifier = Modifier,
    onNavigateToDrones: () -> Unit,
    onNavigateToTickets: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onRaiseTicket: () -> Unit,
    onLogFlight: () -> Unit,
    onNavigateToWarrantyOrMaintenance: () -> Unit,
    onTicketClick: (String) -> Unit,
    onInvoiceClick: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeader("Overview")
        StatsGrid(summary, onNavigateToDrones, onNavigateToTickets, onNavigateToFlights, onNavigateToWarrantyOrMaintenance)

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Quick Actions")
        QuickActionsRow(onRaiseTicket, onLogFlight, onNavigateToDrones, onNavigateToWarrantyOrMaintenance, onNavigateToInvoices)

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Recent Activity")
        RecentActivitySection(summary, onTicketClick, onInvoiceClick)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatsGrid(
    summary: DashboardSummary,
    onNavigateToDrones: () -> Unit,
    onNavigateToTickets: () -> Unit,
    onNavigateToFlights: () -> Unit,
    onNavigateToWarrantyOrMaintenance: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Registered Drones",
                value = summary.registeredDrones.toString(),
                icon = Icons.Filled.FlightTakeoff,
                onClick = onNavigateToDrones,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Open Tickets",
                value = summary.openTickets.toString(),
                icon = Icons.Filled.Assignment,
                onClick = onNavigateToTickets,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Total Flight Hours",
                value = DateTimeFormatters.formatHours(summary.totalFlightHours),
                icon = Icons.Filled.Timer,
                onClick = onNavigateToFlights,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Warranty Expiring Soon",
                value = summary.warrantyExpiringSoon.toString(),
                icon = Icons.Filled.Shield,
                accentColor = if (summary.warrantyExpiringSoon > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                onClick = onNavigateToWarrantyOrMaintenance,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Maintenance Due",
                value = summary.upcomingMaintenance.toString(),
                icon = Icons.Filled.Build,
                onClick = onNavigateToWarrantyOrMaintenance,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Pending Service",
                value = summary.pendingServiceRequests.toString(),
                icon = Icons.Filled.Schedule,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onRaiseTicket: () -> Unit,
    onLogFlight: () -> Unit,
    onMyDrones: () -> Unit,
    onWarrantyOrMaintenance: () -> Unit,
    onInvoices: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            listOf(
                Triple("Raise Ticket", Icons.Filled.Assignment, onRaiseTicket),
                Triple("Log Flight", Icons.Filled.FlightTakeoff, onLogFlight),
                Triple("My Drones", Icons.Filled.FlightTakeoff, onMyDrones),
                Triple("Warranty", Icons.Filled.Shield, onWarrantyOrMaintenance),
                Triple("Invoices", Icons.Filled.Receipt, onInvoices),
                Triple("Maintenance", Icons.Filled.Build, onWarrantyOrMaintenance),
            ),
        ) { (label, icon, onClick) ->
            QuickActionButton(label = label, icon = icon, onClick = onClick)
        }
    }
}

@Composable
private fun RecentActivitySection(
    summary: DashboardSummary,
    onTicketClick: (String) -> Unit,
    onInvoiceClick: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        summary.recentFlight?.let { flight ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Flight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${flight.droneName} • ${DateTimeFormatters.formatDate(flight.flightDate)}", style = MaterialTheme.typography.bodyLarge)
                    Text(DateTimeFormatters.formatDurationMinutes(flight.durationMinutes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        summary.recentTicket?.let { ticket ->
            Card(
                onClick = { onTicketClick(ticket.id) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Ticket", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${ticket.ticketNumber} • ${ticket.subject}", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusChip(label = ticket.status.displayName, tone = StatusColorMapper.toneFor(ticket.status))
                }
            }
        }
        summary.recentInvoice?.let { invoice ->
            Card(
                onClick = { onInvoiceClick(invoice.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Invoice", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(invoice.invoiceNumber, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusChip(label = invoice.paymentStatus.displayName, tone = StatusColorMapper.toneFor(invoice.paymentStatus))
                }
            }
        }
        if (summary.recentFlight == null && summary.recentTicket == null && summary.recentInvoice == null) {
            Text(
                "No recent activity yet — log a flight or raise a ticket to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
