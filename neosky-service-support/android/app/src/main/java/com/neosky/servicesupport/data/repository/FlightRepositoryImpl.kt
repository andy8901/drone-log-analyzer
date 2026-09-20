package com.neosky.servicesupport.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neosky.servicesupport.core.datastore.TokenManager
import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallUnit
import com.neosky.servicesupport.core.util.FlightDurationCalculator
import com.neosky.servicesupport.data.local.dao.FlightLogDao
import com.neosky.servicesupport.data.local.entity.FlightLogEntity
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.data.remote.dto.CreateFlightRequestDto
import com.neosky.servicesupport.data.sync.FlightSyncWorker
import com.neosky.servicesupport.domain.model.FlightLog
import com.neosky.servicesupport.domain.model.FlightStats
import com.neosky.servicesupport.domain.model.SyncStatus
import com.neosky.servicesupport.domain.repository.FlightRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val flightLogDao: FlightLogDao,
    private val tokenManager: TokenManager,
) : FlightRepository {

    override fun observeFlights(droneId: String?): Flow<List<FlightLog>> {
        val flow = if (droneId != null) flightLogDao.observeForDrone(droneId) else flightLogDao.observeAll()
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshFlights(droneId: String?, from: String?, to: String?): NetworkResult<List<FlightLog>> {
        val result = safeApiCall { apiService.getFlights(droneId = droneId, from = from, to = to) }
        return when (result) {
            is NetworkResult.Success -> {
                val dtos = result.data.items
                flightLogDao.upsertAll(dtos.map { it.toEntity() })
                NetworkResult.Success(dtos.map { it.toDomain() })
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun getStats(): NetworkResult<FlightStats> {
        val result = safeApiCall { apiService.getFlightStats() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun logFlight(
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
    ): String {
        val clientUuid = UUID.randomUUID().toString()
        val startInstant = Instant.parse(startTimeIso)
        val endInstant = Instant.parse(endTimeIso)
        val computedDuration = durationMinutes ?: FlightDurationCalculator.minutesBetween(startInstant, endInstant) ?: 0.0
        val flightDate = LocalDate.ofInstant(startInstant, ZoneId.systemDefault()).toString()

        val entity = FlightLogEntity(
            id = clientUuid,
            clientUuid = clientUuid,
            droneId = droneId,
            droneName = null,
            pilotId = tokenManager.getLoggedInUserId().orEmpty(),
            pilotName = null,
            flightDate = flightDate,
            startTime = startTimeIso,
            endTime = endTimeIso,
            durationMinutes = computedDuration,
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
            syncStatus = SyncStatus.PENDING_SYNC.wireValue,
        )
        flightLogDao.upsert(entity)
        enqueueSync()
        return clientUuid
    }

    override suspend fun syncPendingFlights(): NetworkResult<Unit> {
        val pending = flightLogDao.getPendingSync()
        if (pending.isEmpty()) return NetworkResult.Success(Unit)

        var firstError: ApiException? = null
        for (row in pending) {
            val request = CreateFlightRequestDto(
                clientUuid = row.clientUuid,
                droneId = row.droneId,
                startTime = row.startTime,
                endTime = row.endTime,
                durationMinutes = row.durationMinutes,
                location = row.location,
                maxAltitudeM = row.maxAltitudeM,
                distanceTravelledKm = row.distanceTravelledKm,
                missionType = row.missionType,
                payloadUsed = row.payloadUsed,
                batteryUsed = row.batteryUsed,
                batteryCycle = row.batteryCycle,
                weather = row.weather,
                flightResult = row.flightResult,
                remarks = row.remarks,
                incidentFlag = row.incidentFlag,
            )
            when (val result = safeApiCall { apiService.createFlight(request) }) {
                is NetworkResult.Success -> {
                    flightLogDao.deleteById(row.id)
                    flightLogDao.upsert(result.data.toEntity())
                }
                is NetworkResult.Error -> {
                    if (firstError == null) firstError = result.apiException
                }
                NetworkResult.Loading -> Unit
            }
        }
        return firstError?.let { NetworkResult.Error(it) } ?: NetworkResult.Success(Unit)
    }

    override suspend fun deleteFlight(flightId: String): NetworkResult<Unit> {
        val result = safeApiCallUnit { apiService.deleteFlight(flightId) }
        if (result is NetworkResult.Success) {
            flightLogDao.deleteById(flightId)
        }
        return result
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<FlightSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(FlightSyncWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
