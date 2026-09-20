package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: String,
    val customerCode: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val companyName: String? = null,
    val billingAddress: String? = null,
    val gstin: String? = null,
)

@Serializable
data class UpdateProfileRequestDto(
    val fullName: String? = null,
    val phone: String? = null,
    val companyName: String? = null,
    val billingAddress: String? = null,
    val gstin: String? = null,
    val fcmToken: String? = null,
)

@Serializable
data class FcmTokenRequestDto(
    val fcmToken: String,
)

@Serializable
data class DashboardRecentFlightDto(
    val id: String,
    val droneName: String,
    val flightDate: String,
    val durationMinutes: Double,
)

@Serializable
data class DashboardRecentTicketDto(
    val id: String,
    val ticketNumber: String,
    val subject: String,
    val status: String,
)

@Serializable
data class DashboardRecentInvoiceDto(
    val id: String,
    val invoiceNumber: String,
    val totalAmount: Double,
    val paymentStatus: String,
)

@Serializable
data class DashboardDto(
    val registeredDrones: Int,
    val activeWarrantyDrones: Int,
    val warrantyExpiringSoon: Int,
    val openTickets: Int,
    val pendingServiceRequests: Int,
    val upcomingMaintenance: Int,
    val totalFlightHours: Double,
    val recentFlight: DashboardRecentFlightDto? = null,
    val recentTicket: DashboardRecentTicketDto? = null,
    val recentInvoice: DashboardRecentInvoiceDto? = null,
)
