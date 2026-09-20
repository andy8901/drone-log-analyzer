package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Ticket
import kotlinx.coroutines.flow.Flow
import java.io.File

interface TicketRepository {

    fun observeTickets(): Flow<List<Ticket>>

    suspend fun refreshTickets(status: String? = null, droneId: String? = null, page: Int = 1): NetworkResult<List<Ticket>>

    fun observeTicket(ticketId: String): Flow<Ticket?>

    suspend fun refreshTicket(ticketId: String): NetworkResult<Ticket>

    suspend fun createTicket(
        droneId: String,
        category: String,
        subCategory: String?,
        priority: String,
        subject: String,
        description: String,
        issueDatetimeIso: String?,
        location: String?,
        flightHoursAtIssue: Double?,
    ): NetworkResult<Ticket>

    suspend fun addComment(ticketId: String, comment: String): NetworkResult<Unit>

    suspend fun addAttachment(ticketId: String, file: File, fileType: String): NetworkResult<Unit>

    suspend fun closeTicket(ticketId: String, feedback: String?): NetworkResult<Unit>
}
