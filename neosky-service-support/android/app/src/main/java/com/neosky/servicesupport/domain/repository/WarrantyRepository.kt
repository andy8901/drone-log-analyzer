package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Warranty

interface WarrantyRepository {
    suspend fun getWarranty(droneId: String): NetworkResult<Warranty>
}
