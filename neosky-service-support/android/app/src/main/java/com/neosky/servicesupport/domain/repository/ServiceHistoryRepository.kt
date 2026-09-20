package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.ServiceRecord

interface ServiceHistoryRepository {
    suspend fun getServiceHistory(droneId: String): NetworkResult<List<ServiceRecord>>
}
