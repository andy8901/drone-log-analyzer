package com.neosky.servicesupport.core.util

import java.time.Duration
import java.time.Instant

/**
 * Pure, stateless flight-duration math shared by the presentation layer's live duration
 * preview (AddFlightLogScreen) and unit-tested directly (see FlightDurationCalculatorTest).
 * This is deliberately separate from [com.neosky.servicesupport.domain.usecase.CalculateFlightDurationUseCase],
 * which wraps this in the Result type the ViewModel layer consumes; this class holds the
 * arithmetic so it can also be reused by non-use-case call sites (e.g. quick inline previews).
 */
object FlightDurationCalculator {

    /** Returns whole/fractional minutes between two instants, or null if [end] is not after [start]. */
    fun minutesBetween(start: Instant, end: Instant): Double? {
        if (!end.isAfter(start)) return null
        val seconds = Duration.between(start, end).seconds
        return seconds / 60.0
    }

    fun isValidRange(start: Instant, end: Instant): Boolean = end.isAfter(start)
}
