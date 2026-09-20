package com.neosky.servicesupport.domain.usecase

import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Computes a flight's duration in whole minutes from its start/end instants, mirroring the
 * server-side computation the backend performs when `duration_minutes` is sent as null
 * (see docs/API_SPEC.md § 6 Flight Logs). Used by AddFlightLogScreen to live-update the
 * duration field as the user picks start/end times, before the manual-override field is touched.
 */
class CalculateFlightDurationUseCase @Inject constructor() {

    operator fun invoke(startTime: Instant, endTime: Instant): Result {
        if (!endTime.isAfter(startTime)) {
            return Result.Invalid("End time must be after start time")
        }
        val seconds = Duration.between(startTime, endTime).seconds
        val minutes = (seconds / 60.0 * 100).roundToLong() / 100.0
        return Result.Valid(minutes)
    }

    sealed class Result {
        data class Valid(val minutes: Double) : Result()
        data class Invalid(val reason: String) : Result()
    }
}
