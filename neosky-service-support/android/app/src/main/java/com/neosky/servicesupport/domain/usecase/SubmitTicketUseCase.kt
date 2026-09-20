package com.neosky.servicesupport.domain.usecase

import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.repository.TicketRepository
import javax.inject.Inject

/**
 * Validates a new ticket's required fields before calling the network, so the API never
 * has to round-trip a request that's obviously incomplete (empty subject/description, or
 * no drone selected).
 */
class SubmitTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    suspend operator fun invoke(
        droneId: String,
        category: String,
        subCategory: String?,
        priority: String,
        subject: String,
        description: String,
        issueDatetimeIso: String?,
        location: String?,
        flightHoursAtIssue: Double?,
    ): NetworkResult<Ticket> {
        if (droneId.isBlank()) {
            return NetworkResult.Error(
                ApiException(422, "VALIDATION_ERROR", "Please select a drone", mapOf("drone_id" to "required")),
            )
        }
        if (subject.isBlank()) {
            return NetworkResult.Error(
                ApiException(422, "VALIDATION_ERROR", "Subject is required", mapOf("subject" to "required")),
            )
        }
        if (description.isBlank()) {
            return NetworkResult.Error(
                ApiException(422, "VALIDATION_ERROR", "Description is required", mapOf("description" to "required")),
            )
        }
        return ticketRepository.createTicket(
            droneId = droneId,
            category = category,
            subCategory = subCategory,
            priority = priority,
            subject = subject.trim(),
            description = description.trim(),
            issueDatetimeIso = issueDatetimeIso,
            location = location,
            flightHoursAtIssue = flightHoursAtIssue,
        )
    }
}
