package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.DroneComponent
import kotlinx.coroutines.flow.Flow

interface DroneRepository {

    /** Read-through cache: emits locally cached drones immediately, refreshed from the network. */
    fun observeDrones(): Flow<List<Drone>>

    suspend fun refreshDrones(status: String? = null): NetworkResult<List<Drone>>

    fun observeDrone(droneId: String): Flow<Drone?>

    suspend fun refreshDrone(droneId: String): NetworkResult<Drone>

    suspend fun getComponents(droneId: String): NetworkResult<List<DroneComponent>>

    suspend fun searchDrones(query: String): NetworkResult<List<Drone>>
}
