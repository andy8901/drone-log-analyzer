package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Customer
import com.neosky.servicesupport.domain.model.DashboardSummary

/**
 * Not explicitly named in the architecture spec's repository list, but required to back the
 * Profile and Dashboard screens against `/api/customer/*` and `/api/dashboard` cleanly,
 * rather than bolting profile/dashboard calls onto AuthRepository.
 */
interface CustomerRepository {
    suspend fun getProfile(): NetworkResult<Customer>

    suspend fun updateProfile(
        fullName: String? = null,
        phone: String? = null,
        companyName: String? = null,
        billingAddress: String? = null,
        gstin: String? = null,
        fcmToken: String? = null,
    ): NetworkResult<Customer>

    suspend fun getDashboard(): NetworkResult<DashboardSummary>

    suspend fun updateFcmToken(token: String): NetworkResult<Unit>
}
