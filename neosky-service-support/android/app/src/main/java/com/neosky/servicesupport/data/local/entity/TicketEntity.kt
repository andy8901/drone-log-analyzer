package com.neosky.servicesupport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Read-through cache of a customer's own tickets, for offline viewing. */
@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val ticketNumber: String,
    val droneId: String,
    val droneName: String?,
    val category: String,
    val subCategory: String?,
    val priority: String,
    val subject: String,
    val description: String,
    val status: String,
    val issueDatetime: String?,
    val location: String?,
    val flightHoursAtIssue: Double?,
    val assignedEngineerId: String?,
    val assignedEngineerName: String?,
    val resolvedAt: String?,
    val closedAt: String?,
    val customerConfirmedResolution: Boolean,
    val createdAt: String,
    val updatedAt: String,
    /** JSON-encoded List<TicketTimelineEntry>/List<TicketComment>/List<TicketAttachment>. */
    val timelineJson: String = "[]",
    val commentsJson: String = "[]",
    val attachmentsJson: String = "[]",
    val cachedAt: Long = System.currentTimeMillis(),
)
