package com.neosky.servicesupport.domain.usecase

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.DashboardSummary
import com.neosky.servicesupport.domain.repository.CustomerRepository
import javax.inject.Inject

/** Thin today, but the natural seam for combining dashboard stats with a client-side
 * "needs attention" flag (warranty expiring OR maintenance due) that the UI highlights. */
class GetDashboardUseCase @Inject constructor(
    private val customerRepository: CustomerRepository,
) {
    suspend operator fun invoke(): NetworkResult<DashboardSummary> = customerRepository.getDashboard()
}
