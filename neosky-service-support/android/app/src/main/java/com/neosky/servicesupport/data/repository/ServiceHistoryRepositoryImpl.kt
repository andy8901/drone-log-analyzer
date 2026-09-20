package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.ServiceRecord
import com.neosky.servicesupport.domain.repository.ServiceHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceHistoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : ServiceHistoryRepository {
    override suspend fun getServiceHistory(droneId: String): NetworkResult<List<ServiceRecord>> {
        val result = safeApiCall { apiService.getServiceHistory(droneId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
