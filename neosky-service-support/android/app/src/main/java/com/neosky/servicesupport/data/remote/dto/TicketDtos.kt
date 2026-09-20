package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TicketCommentDto(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String? = null,
    val comment: String,
    val statusFrom: String? = null,
    val statusTo: String? = null,
    val isInternal: Boolean = false,
    val createdAt: String,
)

@Serializable
data class TicketAttachmentDto(
    val id: String,
    val ticketId: String,
    val fileUrl: String,
    val fileType: String,
    val fileName: String,
    val fileSizeBytes: Long? = null,
    val createdAt: String,
)

@Serializable
data class TicketTimelineEntryDto(
    val statusFrom: String? = null,
    val statusTo: String,
    val comment: String? = null,
    val actorName: String? = null,
    val at: String,
)

@Serializable
data class TicketDto(
    val id: String,
    val ticketNumber: String,
    val droneId: String,
    val droneName: String? = null,
    val category: String,
    val subCategory: String? = null,
    val priority: String,
    val subject: String,
    val description: String,
    val status: String,
    val issueDatetime: String? = null,
    val location: String? = null,
    val flightHoursAtIssue: Double? = null,
    val assignedEngineerId: String? = null,
    val assignedEngineerName: String? = null,
    val resolvedAt: String? = null,
    val closedAt: String? = null,
    val customerConfirmedResolution: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val timeline: List<TicketTimelineEntryDto> = emptyList(),
    val comments: List<TicketCommentDto> = emptyList(),
    val attachments: List<TicketAttachmentDto> = emptyList(),
)

@Serializable
data class CreateTicketRequestDto(
    val droneId: String,
    val category: String,
    val subCategory: String? = null,
    val priority: String,
    val subject: String,
    val description: String,
    val issueDatetime: String? = null,
    val location: String? = null,
    val flightHoursAtIssue: Double? = null,
)

@Serializable
data class AddCommentRequestDto(
    val comment: String,
)

@Serializable
data class CloseTicketRequestDto(
    val confirmed: Boolean,
    val feedback: String? = null,
)
