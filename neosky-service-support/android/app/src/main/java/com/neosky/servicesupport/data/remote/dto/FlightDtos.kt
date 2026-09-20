package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FlightLogDto(
    val id: String,
    val clientUuid: String? = null,
    val droneId: String,
    val droneName: String? = null,
    val pilotId: String,
    val pilotName: String? = null,
    val flightDate: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Double,
    val location: String? = null,
    val maxAltitudeM: Double? = null,
    val distanceTravelledKm: Double? = null,
    val missionType: String? = null,
    val payloadUsed: String? = null,
    val batteryUsed: String? = null,
    val batteryCycle: Int? = null,
    val weather: String? = null,
    val flightResult: String = "successful",
    val remarks: String? = null,
    val incidentFlag: Boolean = false,
    val syncStatus: String = "synced",
)

@Serializable
data class CreateFlightRequestDto(
    val clientUuid: String,
    val droneId: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Double? = null,
    val location: String? = null,
    val maxAltitudeM: Double? = null,
    val distanceTravelledKm: Double? = null,
    val missionType: String? = null,
    val payloadUsed: String? = null,
    val batteryUsed: String? = null,
    val batteryCycle: Int? = null,
    val weather: String? = null,
    val flightResult: String = "successful",
    val remarks: String? = null,
    val incidentFlag: Boolean = false,
)

@Serializable
data class MonthlyFlightHoursDto(val month: String, val hours: Double)

@Serializable
data class DroneFlightHoursDto(val droneId: String, val droneName: String, val hours: Double)

@Serializable
data class PilotFlightHoursDto(val pilotId: String, val pilotName: String, val hours: Double)

@Serializable
data class FlightStatsDto(
    val totalFlights: Int,
    val totalFlightHours: Double,
    val monthlyFlightHours: List<MonthlyFlightHoursDto> = emptyList(),
    val averageDurationMinutes: Double = 0.0,
    val hoursPerDrone: List<DroneFlightHoursDto> = emptyList(),
    val hoursPerPilot: List<PilotFlightHoursDto> = emptyList(),
)
