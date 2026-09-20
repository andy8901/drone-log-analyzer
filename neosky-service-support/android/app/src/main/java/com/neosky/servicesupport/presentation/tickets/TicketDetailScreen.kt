package com.neosky.servicesupport.presentation.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neosky.servicesupport.core.util.DateTimeFormatters
import com.neosky.servicesupport.core.util.StatusColorMapper
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.model.TicketStatus
import com.neosky.servicesupport.domain.model.TicketTimelineEntry
import com.neosky.servicesupport.presentation.common.LoadingIndicator
import com.neosky.servicesupport.presentation.common.PrimaryButton
import com.neosky.servicesupport.presentation.common.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    viewModel: TicketDetailViewModel = hiltViewModel(),
) {
    val ticket by viewModel.ticket.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showCloseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ticket?.ticketNumber ?: "Ticket") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when {
            ticket == null && uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            ticket == null -> Text(
                uiState.error ?: "Ticket not found",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                    item { TicketHeader(ticket!!) }
                    item { Spacer(modifier = Modifier.height(16.dp)); Text("Timeline", style = MaterialTheme.typography.titleMedium) }
                    items(ticket!!.timeline) { entry -> TimelineRow(entry) }
                    if (ticket!!.attachments.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(16.dp)); Text("Attachments", style = MaterialTheme.typography.titleMedium) }
                        items(ticket!!.attachments) { attachment ->
                            Text("• ${attachment.fileName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Comments", style = MaterialTheme.typography.titleMedium)
                    }
                    items(ticket!!.comments) { comment ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(comment.authorName ?: "Comment", style = MaterialTheme.typography.labelMedium)
                                Text(comment.comment, style = MaterialTheme.typography.bodyMedium)
                                Text(DateTimeFormatters.formatDateTime(comment.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (ticket!!.status == TicketStatus.RESOLVED) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Confirm Resolution & Close",
                                onClick = { showCloseDialog = true },
                                isLoading = uiState.isClosing,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
                CommentInputBar(
                    text = uiState.commentText,
                    onTextChange = viewModel::onCommentChanged,
                    onSend = viewModel::submitComment,
                    isSending = uiState.isSubmittingComment,
                )
            }
        }
    }

    if (showCloseDialog) {
        var feedback by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Confirm resolution") },
            text = {
                Column {
                    Text("Are you satisfied that this issue has been resolved?")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        label = { Text("Feedback (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.confirmResolutionAndClose(feedback.ifBlank { null })
                    showCloseDialog = false
                }) { Text("Confirm & Close") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showCloseDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TicketHeader(ticket: Ticket) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(ticket.subject, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            StatusChip(label = ticket.status.displayName, tone = StatusColorMapper.toneFor(ticket.status))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${ticket.droneName ?: "Drone"} • ${ticket.category.displayName} • ${ticket.priority.displayName} priority",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
        ticket.location?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Location: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ticket.flightHoursAtIssue?.let {
            Text("Flight hours at issue: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimelineRow(entry: TicketTimelineEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .padding(top = 4.dp)
                .clip(CircleShape)
                .background(StatusColorMapper.colorFor(StatusColorMapper.toneFor(entry.statusTo))),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(entry.statusTo.displayName, style = MaterialTheme.typography.bodyMedium)
            entry.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(
                listOfNotNull(entry.actorName, DateTimeFormatters.formatDateTime(entry.at)).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommentInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, isSending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Add a comment…") },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isSending) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
            }
        }
    }
}
