package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.local.dao.DroneDao
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.DroneComponent
import com.neosky.servicesupport.domain.repository.DroneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DroneRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val droneDao: DroneDao,
) : DroneRepository {

    override fun observeDrones(): Flow<List<Drone>> = droneDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshDrones(status: String?): NetworkResult<List<Drone>> {
        val result = safeApiCall { apiService.getDrones(status) }
        return when (result) {
            is NetworkResult.Success -> {
                val dtos = result.data.items
                droneDao.upsertAll(dtos.map { it.toEntity() })
                NetworkResult.Success(dtos.map { it.toDomain() })
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override fun observeDrone(droneId: String): Flow<Drone?> = droneDao.observeById(droneId).map { it?.toDomain() }

    override suspend fun refreshDrone(droneId: String): NetworkResult<Drone> {
        val result = safeApiCall { apiService.getDrone(droneId) }
        return when (result) {
            is NetworkResult.Success -> {
                droneDao.upsert(result.data.toEntity())
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun getComponents(droneId: String): NetworkResult<List<DroneComponent>> {
        val result = safeApiCall { apiService.getDroneComponents(droneId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun searchDrones(query: String): NetworkResult<List<Drone>> {
        val result = safeApiCall { apiService.searchDrones(query) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
