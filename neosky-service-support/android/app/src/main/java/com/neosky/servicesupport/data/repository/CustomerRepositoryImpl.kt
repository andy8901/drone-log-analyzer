package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallUnit
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.data.remote.dto.FcmTokenRequestDto
import com.neosky.servicesupport.data.remote.dto.UpdateProfileRequestDto
import com.neosky.servicesupport.domain.model.Customer
import com.neosky.servicesupport.domain.model.DashboardSummary
import com.neosky.servicesupport.domain.repository.CustomerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : CustomerRepository {

    override suspend fun getProfile(): NetworkResult<Customer> {
        val result = safeApiCall { apiService.getProfile() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun updateProfile(
        fullName: String?,
        phone: String?,
        companyName: String?,
        billingAddress: String?,
        gstin: String?,
        fcmToken: String?,
    ): NetworkResult<Customer> {
        val result = safeApiCall {
            apiService.updateProfile(UpdateProfileRequestDto(fullName, phone, companyName, billingAddress, gstin, fcmToken))
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun getDashboard(): NetworkResult<DashboardSummary> {
        val result = safeApiCall { apiService.getDashboard() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun updateFcmToken(token: String): NetworkResult<Unit> =
        safeApiCallUnit { apiService.updateFcmToken(FcmTokenRequestDto(token)) }
}
