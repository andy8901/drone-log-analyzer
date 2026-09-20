package com.neosky.servicesupport.domain.usecase

/**
 * This module intentionally does not wrap every repository method in a use case: screens that
 * only need a straight passthrough (e.g. DroneListScreen calling DroneRepository.refreshDrones)
 * call the repository interface directly through their ViewModel, per the project's guidance to
 * avoid manufacturing no-op wrapper use cases. Use cases exist here only where there is real
 * logic: validation, calculation, orchestration across repositories, or platform side-effects
 * (file I/O). See:
 *  - CalculateFlightDurationUseCase (live duration calculation)
 *  - GetDashboardUseCase (seam for future cross-repository aggregation)
 *  - SubmitTicketUseCase (client-side validation before network round-trip)
 *  - SyncPendingFlightsUseCase (invoked by FlightSyncWorker)
 *  - LoginUseCase (field validation/trimming)
 *  - DownloadInvoicePdfUseCase (MediaStore file I/O side effect)
 */
