package com.neosky.servicesupport.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.neosky.servicesupport.domain.model.DroneStatus
import com.neosky.servicesupport.domain.model.MaintenanceStatus
import com.neosky.servicesupport.domain.model.PaymentStatus
import com.neosky.servicesupport.domain.model.TicketPriority
import com.neosky.servicesupport.domain.model.TicketStatus
import com.neosky.servicesupport.domain.model.WarrantyClaimStatus
import com.neosky.servicesupport.domain.model.WarrantyStatus
import com.neosky.servicesupport.presentation.theme.StatusColors

/**
 * Single source of truth mapping every status/priority enum in the app to a semantic color
 * bucket (Positive / Info / Warning / Negative / Neutral). Every screen that renders a status
 * chip — tickets, drones, warranty, maintenance, invoices — goes through this object so the
 * same status always renders the same color everywhere, as required by the design spec.
 *
 * The actual ARGB values live in [StatusColors] (theme-aware, light/dark), this file only
 * decides *which* bucket a given status falls into.
 */
enum class StatusTone { POSITIVE, INFO, WARNING, NEGATIVE, NEUTRAL }

object StatusColorMapper {

    fun toneFor(status: TicketStatus): StatusTone = when (status) {
        TicketStatus.NEW -> StatusTone.INFO
        TicketStatus.ASSIGNED -> StatusTone.INFO
        TicketStatus.IN_PROGRESS -> StatusTone.INFO
        TicketStatus.WAITING_CUSTOMER -> StatusTone.WARNING
        TicketStatus.WAITING_PARTS -> StatusTone.WARNING
        TicketStatus.SERVICE -> StatusTone.WARNING
        TicketStatus.QC -> StatusTone.WARNING
        TicketStatus.RESOLVED -> StatusTone.POSITIVE
        TicketStatus.CLOSED -> StatusTone.NEUTRAL
        TicketStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(priority: TicketPriority): StatusTone = when (priority) {
        TicketPriority.LOW -> StatusTone.POSITIVE
        TicketPriority.MEDIUM -> StatusTone.INFO
        TicketPriority.HIGH -> StatusTone.WARNING
        TicketPriority.CRITICAL -> StatusTone.NEGATIVE
        TicketPriority.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(status: DroneStatus): StatusTone = when (status) {
        DroneStatus.ACTIVE -> StatusTone.POSITIVE
        DroneStatus.UNDER_SERVICE -> StatusTone.WARNING
        DroneStatus.GROUNDED -> StatusTone.NEGATIVE
        DroneStatus.RETIRED -> StatusTone.NEUTRAL
        DroneStatus.LOST_STOLEN -> StatusTone.NEGATIVE
        DroneStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(status: MaintenanceStatus): StatusTone = when (status) {
        MaintenanceStatus.UPCOMING -> StatusTone.POSITIVE
        MaintenanceStatus.DUE_SOON -> StatusTone.WARNING
        MaintenanceStatus.DUE -> StatusTone.WARNING
        MaintenanceStatus.OVERDUE -> StatusTone.NEGATIVE
        MaintenanceStatus.COMPLETED -> StatusTone.NEUTRAL
        MaintenanceStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(status: WarrantyStatus): StatusTone = when (status) {
        WarrantyStatus.ACTIVE -> StatusTone.POSITIVE
        WarrantyStatus.EXPIRING_SOON -> StatusTone.WARNING
        WarrantyStatus.EXPIRED -> StatusTone.NEGATIVE
        WarrantyStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(status: WarrantyClaimStatus): StatusTone = when (status) {
        WarrantyClaimStatus.SUBMITTED -> StatusTone.INFO
        WarrantyClaimStatus.APPROVED -> StatusTone.POSITIVE
        WarrantyClaimStatus.REJECTED -> StatusTone.NEGATIVE
        WarrantyClaimStatus.COMPLETED -> StatusTone.POSITIVE
        WarrantyClaimStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    fun toneFor(status: PaymentStatus): StatusTone = when (status) {
        PaymentStatus.PAID -> StatusTone.POSITIVE
        PaymentStatus.PENDING -> StatusTone.WARNING
        PaymentStatus.PARTIALLY_PAID -> StatusTone.WARNING
        PaymentStatus.CANCELLED -> StatusTone.NEGATIVE
        PaymentStatus.UNKNOWN -> StatusTone.NEUTRAL
    }

    @Composable
    fun colorFor(tone: StatusTone): Color = when (tone) {
        StatusTone.POSITIVE -> StatusColors.positive
        StatusTone.INFO -> StatusColors.info
        StatusTone.WARNING -> StatusColors.warning
        StatusTone.NEGATIVE -> StatusColors.negative
        StatusTone.NEUTRAL -> StatusColors.neutral
    }
}
