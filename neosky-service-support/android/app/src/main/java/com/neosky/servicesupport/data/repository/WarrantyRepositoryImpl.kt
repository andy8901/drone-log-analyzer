package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.Warranty
import com.neosky.servicesupport.domain.repository.WarrantyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarrantyRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : WarrantyRepository {
    override suspend fun getWarranty(droneId: String): NetworkResult<Warranty> {
        val result = safeApiCall { apiService.getWarranty(droneId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
