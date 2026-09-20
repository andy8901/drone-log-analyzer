package com.neosky.servicesupport.data.local.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.neosky.servicesupport.data.local.entity.DroneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DroneDao {
    @Query("SELECT * FROM drones ORDER BY droneName ASC")
    fun observeAll(): Flow<List<DroneEntity>>

    @Query("SELECT * FROM drones WHERE id = :droneId")
    fun observeById(droneId: String): Flow<DroneEntity?>

    @Query("SELECT * FROM drones WHERE id = :droneId")
    suspend fun getById(droneId: String): DroneEntity?

    @Upsert
    suspend fun upsertAll(drones: List<DroneEntity>)

    @Upsert
    suspend fun upsert(drone: DroneEntity)

    @Query("DELETE FROM drones")
    suspend fun clearAll()

    @Query("DELETE FROM drones WHERE id NOT IN (:keepIds)")
    suspend fun pruneExcept(keepIds: List<String>)
}
