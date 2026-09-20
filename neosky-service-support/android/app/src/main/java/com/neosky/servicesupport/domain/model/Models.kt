package com.neosky.servicesupport.domain.model

import java.time.Instant
import java.time.LocalDate

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: UserRole,
)

data class Customer(
    val id: String,
    val customerCode: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val companyName: String?,
    val billingAddress: String?,
    val gstin: String?,
)

data class DroneComponent(
    val id: String,
    val droneId: String,
    val componentType: ComponentType,
    val name: String,
    val serialNumber: String?,
    val cycleCount: Int,
    val installedDate: LocalDate?,
    val status: String,
)

data class Drone(
    val id: String,
    val droneName: String,
    val model: String,
    val serialNumber: String,
    val uin: String?,
    val purchaseDate: LocalDate?,
    val deliveryDate: LocalDate?,
    val invoiceNumber: String?,
    val invoiceDate: LocalDate?,
    val warrantyStartDate: LocalDate?,
    val warrantyEndDate: LocalDate?,
    val totalFlightHours: Double,
    val totalFlights: Int,
    val lastFlightAt: Instant?,
    val lastServiceAt: Instant?,
    val nextMaintenanceDueHours: Double?,
    val nextMaintenanceDueDate: LocalDate?,
    val firmwareVersion: String?,
    val status: DroneStatus,
    val components: List<DroneComponent> = emptyList(),
)

data class TicketComment(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String?,
    val comment: String,
    val statusFrom: TicketStatus?,
    val statusTo: TicketStatus?,
    val isInternal: Boolean,
    val createdAt: Instant,
)

data class TicketAttachment(
    val id: String,
    val ticketId: String,
    val fileUrl: String,
    val fileType: AttachmentType,
    val fileName: String,
    val fileSizeBytes: Long?,
    val createdAt: Instant,
)

/** A single entry in a ticket's status timeline, derived server-side from [TicketComment] status transitions. */
data class TicketTimelineEntry(
    val statusFrom: TicketStatus?,
    val statusTo: TicketStatus,
    val comment: String?,
    val actorName: String?,
    val at: Instant,
)

data class Ticket(
    val id: String,
    val ticketNumber: String,
    val droneId: String,
    val droneName: String? = null,
    val category: TicketCategory,
    val subCategory: String?,
    val priority: TicketPriority,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val issueDatetime: Instant?,
    val location: String?,
    val flightHoursAtIssue: Double?,
    val assignedEngineerId: String?,
    val assignedEngineerName: String? = null,
    val resolvedAt: Instant?,
    val closedAt: Instant?,
    val customerConfirmedResolution: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val timeline: List<TicketTimelineEntry> = emptyList(),
    val comments: List<TicketComment> = emptyList(),
    val attachments: List<TicketAttachment> = emptyList(),
)

data class FlightLog(
    val id: String,
    val clientUuid: String,
    val droneId: String,
    val droneName: String? = null,
    val pilotId: String,
    val pilotName: String? = null,
    val flightDate: LocalDate,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Double,
    val location: String?,
    val maxAltitudeM: Double?,
    val distanceTravelledKm: Double?,
    val missionType: String?,
    val payloadUsed: String?,
    val batteryUsed: String?,
    val batteryCycle: Int?,
    val weather: String?,
    val flightResult: FlightResult,
    val remarks: String?,
    val incidentFlag: Boolean,
    val syncStatus: SyncStatus,
)

data class MonthlyFlightHours(val month: String, val hours: Double)
data class DroneFlightHours(val droneId: String, val droneName: String, val hours: Double)
data class PilotFlightHours(val pilotId: String, val pilotName: String, val hours: Double)

data class FlightStats(
    val totalFlights: Int,
    val totalFlightHours: Double,
    val monthlyFlightHours: List<MonthlyFlightHours>,
    val averageDurationMinutes: Double,
    val hoursPerDrone: List<DroneFlightHours>,
    val hoursPerPilot: List<PilotFlightHours>,
)

data class WarrantyClaim(
    val id: String,
    val claimNumber: String,
    val ticketId: String,
    val description: String?,
    val status: WarrantyClaimStatus,
    val claimedAt: Instant,
    val resolvedAt: Instant?,
)

data class Warranty(
    val droneId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysRemaining: Int,
    val status: WarrantyStatus,
    val coveredItems: List<String>,
    val excludedItems: List<String>,
    val claims: List<WarrantyClaim>,
)

data class MaintenanceSchedule(
    val intervalFlightHours: Double?,
    val intervalCalendarDays: Int?,
    val lastMaintenanceDate: LocalDate?,
    val currentFlightHours: Double,
    val nextDueHours: Double?,
    val remainingHours: Double?,
    val nextDueDate: LocalDate?,
    val status: MaintenanceStatus,
)

data class MaintenanceRecord(
    val id: String,
    val maintenanceType: String,
    val performedAt: Instant,
    val flightHoursAtService: Double?,
    val description: String?,
    val partsReplaced: List<String>,
    val status: String,
)

data class MaintenanceInfo(
    val schedule: MaintenanceSchedule?,
    val records: List<MaintenanceRecord>,
)

data class InvoiceItem(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
)

data class Payment(
    val id: String,
    val amount: Double,
    val paymentDate: Instant,
    val paymentMethod: String?,
    val referenceNumber: String?,
)

data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val droneId: String?,
    val droneName: String? = null,
    val invoiceDate: LocalDate,
    val productService: String,
    val subtotalAmount: Double,
    val gstAmount: Double,
    val totalAmount: Double,
    val paymentStatus: PaymentStatus,
    val warrantyType: WarrantyType,
    val pdfUrl: String?,
    val items: List<InvoiceItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
)

data class ServiceRecord(
    val id: String,
    val droneId: String,
    val ticketId: String?,
    val serviceDate: LocalDate,
    val issueSummary: String?,
    val actionTaken: String?,
    val performedByName: String?,
    val status: String,
)

data class Document(
    val id: String,
    val documentType: DocumentType,
    val customerId: String?,
    val droneId: String?,
    val droneName: String? = null,
    val ticketId: String?,
    val ticketNumber: String? = null,
    val serviceRecordId: String?,
    val fileName: String,
    val fileUrl: String,
    val createdAt: Instant,
)

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val relatedEntityType: String?,
    val relatedEntityId: String?,
    val isRead: Boolean,
    val createdAt: Instant,
)

data class DashboardRecentFlight(val id: String, val droneName: String, val flightDate: LocalDate, val durationMinutes: Double)
data class DashboardRecentTicket(val id: String, val ticketNumber: String, val subject: String, val status: TicketStatus)
data class DashboardRecentInvoice(val id: String, val invoiceNumber: String, val totalAmount: Double, val paymentStatus: PaymentStatus)

data class DashboardSummary(
    val registeredDrones: Int,
    val activeWarrantyDrones: Int,
    val warrantyExpiringSoon: Int,
    val openTickets: Int,
    val pendingServiceRequests: Int,
    val upcomingMaintenance: Int,
    val totalFlightHours: Double,
    val recentFlight: DashboardRecentFlight?,
    val recentTicket: DashboardRecentTicket?,
    val recentInvoice: DashboardRecentInvoice?,
)

data class SearchResults(
    val drones: List<Drone>,
    val tickets: List<Ticket>,
    val invoices: List<Invoice>,
    val serviceRecords: List<ServiceRecord>,
)

/** A generic page envelope, mirroring the API's `{items, page, page_size, total, total_pages}` shape. */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: User,
)
