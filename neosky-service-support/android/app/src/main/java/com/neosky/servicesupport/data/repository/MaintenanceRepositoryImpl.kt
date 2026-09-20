package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.MaintenanceInfo
import com.neosky.servicesupport.domain.repository.MaintenanceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : MaintenanceRepository {
    override suspend fun getMaintenance(droneId: String): NetworkResult<MaintenanceInfo> {
        val result = safeApiCall { apiService.getMaintenance(droneId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
