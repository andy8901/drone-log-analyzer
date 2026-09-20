package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.FlightStats
import kotlinx.coroutines.flow.Flow

interface FlightRepository {

    fun observeFlights(droneId: String? = null): Flow<List<FlightLog>>

    suspend fun refreshFlights(droneId: String? = null, from: String? = null, to: String? = null): NetworkResult<List<FlightLog>>

    suspend fun getStats(): NetworkResult<FlightStats>

    /**
     * Saves a flight log locally (marked PENDING_SYNC) immediately, and enqueues a
     * [com.neosky.servicesupport.data.sync.FlightSyncWorker] to upload it once connectivity
     * returns. Returns the client-generated UUID used to track it.
     */
    suspend fun logFlight(
        droneId: String,
        startTimeIso: String,
        endTimeIso: String,
        durationMinutes: Double?,
        location: String?,
        maxAltitudeM: Double?,
        distanceTravelledKm: Double?,
        missionType: String?,
        payloadUsed: String?,
        batteryUsed: String?,
        batteryCycle: Int?,
        weather: String?,
        flightResult: String,
        remarks: String?,
        incidentFlag: Boolean,
    ): String

    /** Uploads every PENDING_SYNC row using its client_uuid for idempotency. Called by the sync worker. */
    suspend fun syncPendingFlights(): NetworkResult<Unit>

    suspend fun deleteFlight(flightId: String): NetworkResult<Unit>
}
