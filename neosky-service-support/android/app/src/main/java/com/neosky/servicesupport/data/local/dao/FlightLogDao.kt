package com.neosky.servicesupport.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.neosky.servicesupport.data.local.entity.FlightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightLogDao {
    @Query("SELECT * FROM flight_logs ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FlightLogEntity>>

    @Query("SELECT * FROM flight_logs WHERE droneId = :droneId ORDER BY startTime DESC")
    fun observeForDrone(droneId: String): Flow<List<FlightLogEntity>>

    @Query("SELECT * FROM flight_logs WHERE syncStatus = 'pending_sync'")
    suspend fun getPendingSync(): List<FlightLogEntity>

    @Upsert
    suspend fun upsertAll(flights: List<FlightLogEntity>)

    @Upsert
    suspend fun upsert(flight: FlightLogEntity)

    @Query("DELETE FROM flight_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM flight_logs")
    suspend fun clearAll()
}
