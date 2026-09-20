package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MaintenanceScheduleDto(
    val intervalFlightHours: Double? = null,
    val intervalCalendarDays: Int? = null,
    val lastMaintenanceDate: String? = null,
    val currentFlightHours: Double = 0.0,
    val nextDueHours: Double? = null,
    val remainingHours: Double? = null,
    val nextDueDate: String? = null,
    val status: String,
)

@Serializable
data class MaintenanceRecordDto(
    val id: String,
    val maintenanceType: String,
    val performedAt: String,
    val flightHoursAtService: Double? = null,
    val description: String? = null,
    val partsReplaced: List<String> = emptyList(),
    val status: String = "completed",
)

@Serializable
data class MaintenanceInfoDto(
    val schedule: MaintenanceScheduleDto? = null,
    val records: List<MaintenanceRecordDto> = emptyList(),
)
