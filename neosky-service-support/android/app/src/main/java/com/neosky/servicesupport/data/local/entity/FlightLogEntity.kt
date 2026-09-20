package com.neosky.servicesupport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A flight log row. [id] is the server-assigned UUID once synced; for a flight created offline
 * and not yet uploaded, [id] is temporarily set equal to [clientUuid] so the row has a stable
 * primary key from the moment it's saved. [clientUuid] is always the client-generated id and is
 * what makes the eventual upload idempotent (the server keys off it — see API_SPEC.md § 6).
 */
@Entity(tableName = "flight_logs")
data class FlightLogEntity(
    @PrimaryKey val id: String,
    val clientUuid: String,
    val droneId: String,
    val droneName: String?,
    val pilotId: String,
    val pilotName: String?,
    val flightDate: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Double,
    val location: String?,
    val maxAltitudeM: Double?,
    val distanceTravelledKm: Double?,
    val missionType: String?,
    val payloadUsed: String?,
    val batteryUsed: String?,
    val batteryCycle: Int?,
    val weather: String?,
    val flightResult: String,
    val remarks: String?,
    val incidentFlag: Boolean,
    /** "synced" | "pending_sync" — see [com.neosky.servicesupport.domain.model.SyncStatus]. */
    val syncStatus: String,
)
