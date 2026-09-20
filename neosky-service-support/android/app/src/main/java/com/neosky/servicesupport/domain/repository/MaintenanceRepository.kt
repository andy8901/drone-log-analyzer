package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.MaintenanceInfo

interface MaintenanceRepository {
    suspend fun getMaintenance(droneId: String): NetworkResult<MaintenanceInfo>
}
