package com.neosky.servicesupport.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.usecase.SyncPendingFlightsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Uploads every PENDING_SYNC flight log row once connectivity returns, using each row's
 * client_uuid for idempotency (see docs/API_SPEC.md § 6). Enqueued by
 * [com.neosky.servicesupport.data.repository.FlightRepositoryImpl] every time a flight is
 * logged while offline, and again by [com.neosky.servicesupport.core.util.ConnectivityObserver]
 * consumers whenever connectivity is regained.
 */
@HiltWorker
class FlightSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncPendingFlightsUseCase: SyncPendingFlightsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = syncPendingFlightsUseCase()) {
            is NetworkResult.Success -> Result.success()
            is NetworkResult.Error -> if (result.apiException.isNetworkError || result.apiException.isTimeout) {
                Result.retry()
            } else {
                // A non-network failure (e.g. validation) on one row would otherwise retry forever;
                // surface it as a failure so WorkManager doesn't spin, the row stays PENDING_SYNC
                // and is retried on the next explicit sync trigger instead.
                Result.failure()
            }
            NetworkResult.Loading -> Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "flight_sync_work"
    }
}
