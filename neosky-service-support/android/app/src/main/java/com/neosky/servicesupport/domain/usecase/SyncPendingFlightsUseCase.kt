package com.neosky.servicesupport.domain.usecase

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.repository.FlightRepository
import javax.inject.Inject

/** Invoked by [com.neosky.servicesupport.data.sync.FlightSyncWorker] when connectivity returns. */
class SyncPendingFlightsUseCase @Inject constructor(
    private val flightRepository: FlightRepository,
) {
    suspend operator fun invoke(): NetworkResult<Unit> = flightRepository.syncPendingFlights()
}
