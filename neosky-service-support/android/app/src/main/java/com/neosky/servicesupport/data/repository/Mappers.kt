package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.util.DateParsing.parseInstant
import com.neosky.servicesupport.core.util.DateParsing.parseInstantOrEpoch
import com.neosky.servicesupport.core.util.DateParsing.parseLocalDate
import com.neosky.servicesupport.core.util.DateParsing.parseLocalDateOrEpoch
import com.neosky.servicesupport.data.local.entity.DroneEntity
import com.neosky.servicesupport.data.local.entity.FlightLogEntity
import com.neosky.servicesupport.data.local.entity.NotificationEntity
import com.neosky.servicesupport.data.local.entity.TicketEntity
import com.neosky.servicesupport.data.remote.dto.CustomerDto
import com.neosky.servicesupport.data.remote.dto.DashboardDto
import com.neosky.servicesupport.data.remote.dto.DocumentDto
import com.neosky.servicesupport.data.remote.dto.DroneComponentDto
import com.neosky.servicesupport.data.remote.dto.DroneDto
import com.neosky.servicesupport.data.remote.dto.FlightLogDto
import com.neosky.servicesupport.data.remote.dto.FlightStatsDto
import com.neosky.servicesupport.data.remote.dto.InvoiceDto
import com.neosky.servicesupport.data.remote.dto.MaintenanceInfoDto
import com.neosky.servicesupport.data.remote.dto.MaintenanceRecordDto
import com.neosky.servicesupport.data.remote.dto.MaintenanceScheduleDto
import com.neosky.servicesupport.data.remote.dto.NotificationDto
import com.neosky.servicesupport.data.remote.dto.SearchResultsDto
import com.neosky.servicesupport.data.remote.dto.ServiceRecordDto
import com.neosky.servicesupport.data.remote.dto.TicketAttachmentDto
import com.neosky.servicesupport.data.remote.dto.TicketCommentDto
import com.neosky.servicesupport.data.remote.dto.TicketDto
import com.neosky.servicesupport.data.remote.dto.TicketTimelineEntryDto
import com.neosky.servicesupport.data.remote.dto.UserDto
import com.neosky.servicesupport.data.remote.dto.WarrantyDto
import com.neosky.servicesupport.domain.model.AttachmentType
import com.neosky.servicesupport.domain.model.ComponentType
import com.neosky.servicesupport.domain.model.Customer
import com.neosky.servicesupport.domain.model.DashboardRecentFlight
import com.neosky.servicesupport.domain.model.DashboardRecentInvoice
import com.neosky.servicesupport.domain.model.DashboardRecentTicket
import com.neosky.servicesupport.domain.model.DashboardSummary
import com.neosky.servicesupport.domain.model.Document
import com.neosky.servicesupport.domain.model.DocumentType
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.DroneComponent
import com.neosky.servicesupport.domain.model.DroneFlightHours
import com.neosky.servicesupport.domain.model.DroneStatus
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.FlightResult
import com.neosky.servicesupport.domain.model.FlightStats
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.domain.model.InvoiceItem
import com.neosky.servicesupport.domain.model.MaintenanceInfo
import com.neosky.servicesupport.domain.model.MaintenanceRecord
import com.neosky.servicesupport.domain.model.MaintenanceSchedule
import com.neosky.servicesupport.domain.model.MaintenanceStatus
import com.neosky.servicesupport.domain.model.MonthlyFlightHours
import com.neosky.servicesupport.domain.model.Notification
import com.neosky.servicesupport.domain.model.NotificationType
import com.neosky.servicesupport.domain.model.Payment
import com.neosky.servicesupport.domain.model.PaymentStatus
import com.neosky.servicesupport.domain.model.PilotFlightHours
import com.neosky.servicesupport.domain.model.SearchResults
import com.neosky.servicesupport.domain.model.ServiceRecord
import com.neosky.servicesupport.domain.model.SyncStatus
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.model.TicketAttachment
import com.neosky.servicesupport.domain.model.TicketCategory
import com.neosky.servicesupport.domain.model.TicketComment
import com.neosky.servicesupport.domain.model.TicketPriority
import com.neosky.servicesupport.domain.model.TicketStatus
import com.neosky.servicesupport.domain.model.TicketTimelineEntry
import com.neosky.servicesupport.domain.model.User
import com.neosky.servicesupport.domain.model.UserRole
import com.neosky.servicesupport.domain.model.Warranty
import com.neosky.servicesupport.domain.model.WarrantyClaim
import com.neosky.servicesupport.domain.model.WarrantyClaimStatus
import com.neosky.servicesupport.domain.model.WarrantyStatus
import com.neosky.servicesupport.domain.model.WarrantyType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val mapperJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

// ----------------------------------------------------------------- Auth / Customer

fun UserDto.toDomain() = User(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    role = UserRole.fromWire(role),
)

fun CustomerDto.toDomain() = Customer(
    id = id,
    customerCode = customerCode,
    fullName = fullName,
    email = email,
    phone = phone,
    companyName = companyName,
    billingAddress = billingAddress,
    gstin = gstin,
)

fun DashboardDto.toDomain() = DashboardSummary(
    registeredDrones = registeredDrones,
    activeWarrantyDrones = activeWarrantyDrones,
    warrantyExpiringSoon = warrantyExpiringSoon,
    openTickets = openTickets,
    pendingServiceRequests = pendingServiceRequests,
    upcomingMaintenance = upcomingMaintenance,
    totalFlightHours = totalFlightHours,
    recentFlight = recentFlight?.let {
        DashboardRecentFlight(it.id, it.droneName, parseLocalDateOrEpoch(it.flightDate), it.durationMinutes)
    },
    recentTicket = recentTicket?.let {
        DashboardRecentTicket(it.id, it.ticketNumber, it.subject, TicketStatus.fromWire(it.status))
    },
    recentInvoice = recentInvoice?.let {
        DashboardRecentInvoice(it.id, it.invoiceNumber, it.totalAmount, PaymentStatus.fromWire(it.paymentStatus))
    },
)

// ----------------------------------------------------------------------- Drones

fun DroneComponentDto.toDomain() = DroneComponent(
    id = id,
    droneId = droneId,
    componentType = ComponentType.fromWire(componentType),
    name = name,
    serialNumber = serialNumber,
    cycleCount = cycleCount,
    installedDate = parseLocalDate(installedDate),
    status = status,
)

fun DroneDto.toDomain() = Drone(
    id = id,
    droneName = droneName,
    model = model,
    serialNumber = serialNumber,
    uin = uin,
    purchaseDate = parseLocalDate(purchaseDate),
    deliveryDate = parseLocalDate(deliveryDate),
    invoiceNumber = invoiceNumber,
    invoiceDate = parseLocalDate(invoiceDate),
    warrantyStartDate = parseLocalDate(warrantyStartDate),
    warrantyEndDate = parseLocalDate(warrantyEndDate),
    totalFlightHours = totalFlightHours,
    totalFlights = totalFlights,
    lastFlightAt = parseInstant(lastFlightAt),
    lastServiceAt = parseInstant(lastServiceAt),
    nextMaintenanceDueHours = nextMaintenanceDueHours,
    nextMaintenanceDueDate = parseLocalDate(nextMaintenanceDueDate),
    firmwareVersion = firmwareVersion,
    status = DroneStatus.fromWire(status),
    components = components.map { it.toDomain() },
)

fun DroneDto.toEntity() = DroneEntity(
    id = id,
    droneName = droneName,
    model = model,
    serialNumber = serialNumber,
    uin = uin,
    purchaseDate = purchaseDate,
    deliveryDate = deliveryDate,
    invoiceNumber = invoiceNumber,
    invoiceDate = invoiceDate,
    warrantyStartDate = warrantyStartDate,
    warrantyEndDate = warrantyEndDate,
    totalFlightHours = totalFlightHours,
    totalFlights = totalFlights,
    lastFlightAt = lastFlightAt,
    lastServiceAt = lastServiceAt,
    nextMaintenanceDueHours = nextMaintenanceDueHours,
    nextMaintenanceDueDate = nextMaintenanceDueDate,
    firmwareVersion = firmwareVersion,
    status = status,
    componentsJson = mapperJson.encodeToString(components),
)

fun DroneEntity.toDomain(): Drone {
    val componentDtos = runCatching { mapperJson.decodeFromString<List<DroneComponentDto>>(componentsJson) }.getOrDefault(emptyList())
    return Drone(
        id = id,
        droneName = droneName,
        model = model,
        serialNumber = serialNumber,
        uin = uin,
        purchaseDate = parseLocalDate(purchaseDate),
        deliveryDate = parseLocalDate(deliveryDate),
        invoiceNumber = invoiceNumber,
        invoiceDate = parseLocalDate(invoiceDate),
        warrantyStartDate = parseLocalDate(warrantyStartDate),
        warrantyEndDate = parseLocalDate(warrantyEndDate),
        totalFlightHours = totalFlightHours,
        totalFlights = totalFlights,
        lastFlightAt = parseInstant(lastFlightAt),
        lastServiceAt = parseInstant(lastServiceAt),
        nextMaintenanceDueHours = nextMaintenanceDueHours,
        nextMaintenanceDueDate = parseLocalDate(nextMaintenanceDueDate),
        firmwareVersion = firmwareVersion,
        status = DroneStatus.fromWire(status),
        components = componentDtos.map { it.toDomain() },
    )
}

// ---------------------------------------------------------------------- Tickets

fun TicketCommentDto.toDomain() = TicketComment(
    id = id,
    ticketId = ticketId,
    authorId = authorId,
    authorName = authorName,
    comment = comment,
    statusFrom = statusFrom?.let { TicketStatus.fromWire(it) },
    statusTo = statusTo?.let { TicketStatus.fromWire(it) },
    isInternal = isInternal,
    createdAt = parseInstantOrEpoch(createdAt),
)

fun TicketAttachmentDto.toDomain() = TicketAttachment(
    id = id,
    ticketId = ticketId,
    fileUrl = fileUrl,
    fileType = AttachmentType.fromWire(fileType),
    fileName = fileName,
    fileSizeBytes = fileSizeBytes,
    createdAt = parseInstantOrEpoch(createdAt),
)

fun TicketTimelineEntryDto.toDomain() = TicketTimelineEntry(
    statusFrom = statusFrom?.let { TicketStatus.fromWire(it) },
    statusTo = TicketStatus.fromWire(statusTo),
    comment = comment,
    actorName = actorName,
    at = parseInstantOrEpoch(at),
)

fun TicketDto.toDomain() = Ticket(
    id = id,
    ticketNumber = ticketNumber,
    droneId = droneId,
    droneName = droneName,
    category = TicketCategory.fromWire(category),
    subCategory = subCategory,
    priority = TicketPriority.fromWire(priority),
    subject = subject,
    description = description,
    status = TicketStatus.fromWire(status),
    issueDatetime = parseInstant(issueDatetime),
    location = location,
    flightHoursAtIssue = flightHoursAtIssue,
    assignedEngineerId = assignedEngineerId,
    assignedEngineerName = assignedEngineerName,
    resolvedAt = parseInstant(resolvedAt),
    closedAt = parseInstant(closedAt),
    customerConfirmedResolution = customerConfirmedResolution,
    createdAt = parseInstantOrEpoch(createdAt),
    updatedAt = parseInstantOrEpoch(updatedAt),
    timeline = timeline.map { it.toDomain() },
    comments = comments.map { it.toDomain() },
    attachments = attachments.map { it.toDomain() },
)

fun TicketDto.toEntity() = TicketEntity(
    id = id,
    ticketNumber = ticketNumber,
    droneId = droneId,
    droneName = droneName,
    category = category,
    subCategory = subCategory,
    priority = priority,
    subject = subject,
    description = description,
    status = status,
    issueDatetime = issueDatetime,
    location = location,
    flightHoursAtIssue = flightHoursAtIssue,
    assignedEngineerId = assignedEngineerId,
    assignedEngineerName = assignedEngineerName,
    resolvedAt = resolvedAt,
    closedAt = closedAt,
    customerConfirmedResolution = customerConfirmedResolution,
    createdAt = createdAt,
    updatedAt = updatedAt,
    timelineJson = mapperJson.encodeToString(timeline),
    commentsJson = mapperJson.encodeToString(comments),
    attachmentsJson = mapperJson.encodeToString(attachments),
)

fun TicketEntity.toDomain(): Ticket {
    val timelineDtos = runCatching { mapperJson.decodeFromString<List<TicketTimelineEntryDto>>(timelineJson) }.getOrDefault(emptyList())
    val commentDtos = runCatching { mapperJson.decodeFromString<List<TicketCommentDto>>(commentsJson) }.getOrDefault(emptyList())
    val attachmentDtos = runCatching { mapperJson.decodeFromString<List<TicketAttachmentDto>>(attachmentsJson) }.getOrDefault(emptyList())
    return Ticket(
        id = id,
        ticketNumber = ticketNumber,
        droneId = droneId,
        droneName = droneName,
        category = TicketCategory.fromWire(category),
        subCategory = subCategory,
        priority = TicketPriority.fromWire(priority),
        subject = subject,
        description = description,
        status = TicketStatus.fromWire(status),
        issueDatetime = parseInstant(issueDatetime),
        location = location,
        flightHoursAtIssue = flightHoursAtIssue,
        assignedEngineerId = assignedEngineerId,
        assignedEngineerName = assignedEngineerName,
        resolvedAt = parseInstant(resolvedAt),
        closedAt = parseInstant(closedAt),
        customerConfirmedResolution = customerConfirmedResolution,
        createdAt = parseInstantOrEpoch(createdAt),
        updatedAt = parseInstantOrEpoch(updatedAt),
        timeline = timelineDtos.map { it.toDomain() },
        comments = commentDtos.map { it.toDomain() },
        attachments = attachmentDtos.map { it.toDomain() },
    )
}

// ---------------------------------------------------------------------- Flights

fun FlightLogDto.toDomain() = FlightLog(
    id = id,
    clientUuid = clientUuid ?: id,
    droneId = droneId,
    droneName = droneName,
    pilotId = pilotId,
    pilotName = pilotName,
    flightDate = parseLocalDateOrEpoch(flightDate),
    startTime = parseInstantOrEpoch(startTime),
    endTime = parseInstantOrEpoch(endTime),
    durationMinutes = durationMinutes,
    location = location,
    maxAltitudeM = maxAltitudeM,
    distanceTravelledKm = distanceTravelledKm,
    missionType = missionType,
    payloadUsed = payloadUsed,
    batteryUsed = batteryUsed,
    batteryCycle = batteryCycle,
    weather = weather,
    flightResult = FlightResult.fromWire(flightResult),
    remarks = remarks,
    incidentFlag = incidentFlag,
    syncStatus = SyncStatus.fromWire(syncStatus),
)

fun FlightLogDto.toEntity() = FlightLogEntity(
    id = id,
    clientUuid = clientUuid ?: id,
    droneId = droneId,
    droneName = droneName,
    pilotId = pilotId,
    pilotName = pilotName,
    flightDate = flightDate,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    location = location,
    maxAltitudeM = maxAltitudeM,
    distanceTravelledKm = distanceTravelledKm,
    missionType = missionType,
    payloadUsed = payloadUsed,
    batteryUsed = batteryUsed,
    batteryCycle = batteryCycle,
    weather = weather,
    flightResult = flightResult,
    remarks = remarks,
    incidentFlag = incidentFlag,
    syncStatus = syncStatus,
)

fun FlightLogEntity.toDomain() = FlightLog(
    id = id,
    clientUuid = clientUuid,
    droneId = droneId,
    droneName = droneName,
    pilotId = pilotId,
    pilotName = pilotName,
    flightDate = parseLocalDateOrEpoch(flightDate),
    startTime = parseInstantOrEpoch(startTime),
    endTime = parseInstantOrEpoch(endTime),
    durationMinutes = durationMinutes,
    location = location,
    maxAltitudeM = maxAltitudeM,
    distanceTravelledKm = distanceTravelledKm,
    missionType = missionType,
    payloadUsed = payloadUsed,
    batteryUsed = batteryUsed,
    batteryCycle = batteryCycle,
    weather = weather,
    flightResult = FlightResult.fromWire(flightResult),
    remarks = remarks,
    incidentFlag = incidentFlag,
    syncStatus = SyncStatus.fromWire(syncStatus),
)

fun FlightStatsDto.toDomain() = FlightStats(
    totalFlights = totalFlights,
    totalFlightHours = totalFlightHours,
    monthlyFlightHours = monthlyFlightHours.map { MonthlyFlightHours(it.month, it.hours) },
    averageDurationMinutes = averageDurationMinutes,
    hoursPerDrone = hoursPerDrone.map { DroneFlightHours(it.droneId, it.droneName, it.hours) },
    hoursPerPilot = hoursPerPilot.map { PilotFlightHours(it.pilotId, it.pilotName, it.hours) },
)

// --------------------------------------------------------------------- Warranty

fun WarrantyDto.toDomain() = Warranty(
    droneId = droneId,
    startDate = parseLocalDateOrEpoch(startDate),
    endDate = parseLocalDateOrEpoch(endDate),
    daysRemaining = daysRemaining,
    status = WarrantyStatus.fromWire(status),
    coveredItems = coveredItems,
    excludedItems = excludedItems,
    claims = claims.map {
        WarrantyClaim(
            id = it.id,
            claimNumber = it.claimNumber,
            ticketId = it.ticketId,
            description = it.description,
            status = WarrantyClaimStatus.fromWire(it.status),
            claimedAt = parseInstantOrEpoch(it.claimedAt),
            resolvedAt = parseInstant(it.resolvedAt),
        )
    },
)

// ----------------------------------------------------------------- Maintenance

fun MaintenanceScheduleDto.toDomain() = MaintenanceSchedule(
    intervalFlightHours = intervalFlightHours,
    intervalCalendarDays = intervalCalendarDays,
    lastMaintenanceDate = parseLocalDate(lastMaintenanceDate),
    currentFlightHours = currentFlightHours,
    nextDueHours = nextDueHours,
    remainingHours = remainingHours,
    nextDueDate = parseLocalDate(nextDueDate),
    status = MaintenanceStatus.fromWire(status),
)

fun MaintenanceRecordDto.toDomain() = MaintenanceRecord(
    id = id,
    maintenanceType = maintenanceType,
    performedAt = parseInstantOrEpoch(performedAt),
    flightHoursAtService = flightHoursAtService,
    description = description,
    partsReplaced = partsReplaced,
    status = status,
)

fun MaintenanceInfoDto.toDomain() = MaintenanceInfo(
    schedule = schedule?.toDomain(),
    records = records.map { it.toDomain() },
)

// -------------------------------------------------------------------- Invoices

fun InvoiceDto.toDomain() = Invoice(
    id = id,
    invoiceNumber = invoiceNumber,
    droneId = droneId,
    droneName = droneName,
    invoiceDate = parseLocalDateOrEpoch(invoiceDate),
    productService = productService,
    subtotalAmount = subtotalAmount,
    gstAmount = gstAmount,
    totalAmount = totalAmount,
    paymentStatus = PaymentStatus.fromWire(paymentStatus),
    warrantyType = WarrantyType.fromWire(warrantyType),
    pdfUrl = pdfUrl,
    items = invoiceItems.map { InvoiceItem(it.id, it.description, it.quantity, it.unitPrice, it.amount) },
    payments = payments.map { Payment(it.id, it.amount, parseInstantOrEpoch(it.paymentDate), it.paymentMethod, it.referenceNumber) },
)

// --------------------------------------------------------------- Service history

fun ServiceRecordDto.toDomain() = ServiceRecord(
    id = id,
    droneId = droneId,
    ticketId = ticketId,
    serviceDate = parseLocalDateOrEpoch(serviceDate),
    issueSummary = issueSummary,
    actionTaken = actionTaken,
    performedByName = performedByName,
    status = status,
)

// -------------------------------------------------------------------- Documents

fun DocumentDto.toDomain() = Document(
    id = id,
    documentType = DocumentType.fromWire(documentType),
    customerId = customerId,
    droneId = droneId,
    droneName = droneName,
    ticketId = ticketId,
    ticketNumber = ticketNumber,
    serviceRecordId = serviceRecordId,
    fileName = fileName,
    fileUrl = fileUrl,
    createdAt = parseInstantOrEpoch(createdAt),
)

// ---------------------------------------------------------------- Notifications

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = NotificationType.fromWire(type),
    title = title,
    body = body,
    relatedEntityType = relatedEntityType,
    relatedEntityId = relatedEntityId,
    isRead = isRead,
    createdAt = parseInstantOrEpoch(createdAt),
)

fun NotificationDto.toEntity() = NotificationEntity(
    id = id,
    type = type,
    title = title,
    body = body,
    relatedEntityType = relatedEntityType,
    relatedEntityId = relatedEntityId,
    isRead = isRead,
    createdAt = createdAt,
)

fun NotificationEntity.toDomain() = Notification(
    id = id,
    type = NotificationType.fromWire(type),
    title = title,
    body = body,
    relatedEntityType = relatedEntityType,
    relatedEntityId = relatedEntityId,
    isRead = isRead,
    createdAt = parseInstantOrEpoch(createdAt),
)

// ------------------------------------------------------------------------ Search

fun SearchResultsDto.toDomain() = SearchResults(
    drones = drones.map { it.toDomain() },
    tickets = tickets.map { it.toDomain() },
    invoices = invoices.map { it.toDomain() },
    serviceRecords = serviceRecords.map { it.toDomain() },
)
