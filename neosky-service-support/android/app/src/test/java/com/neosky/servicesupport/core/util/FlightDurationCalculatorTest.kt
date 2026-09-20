package com.neosky.servicesupport.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class FlightDurationCalculatorTest {

    @Test
    fun `minutesBetween returns correct minutes for a simple range`() {
        val start = Instant.parse("2026-09-20T09:00:00Z")
        val end = Instant.parse("2026-09-20T09:32:00Z")

        val result = FlightDurationCalculator.minutesBetween(start, end)

        assertThat(result).isEqualTo(32.0)
    }

    @Test
    fun `minutesBetween handles fractional minutes`() {
        val start = Instant.parse("2026-09-20T09:00:00Z")
        val end = Instant.parse("2026-09-20T09:00:30Z")

        val result = FlightDurationCalculator.minutesBetween(start, end)

        assertThat(result).isEqualTo(0.5)
    }

    @Test
    fun `minutesBetween returns null when end is before start`() {
        val start = Instant.parse("2026-09-20T09:32:00Z")
        val end = Instant.parse("2026-09-20T09:00:00Z")

        val result = FlightDurationCalculator.minutesBetween(start, end)

        assertThat(result).isNull()
    }

    @Test
    fun `minutesBetween returns null when end equals start`() {
        val instant = Instant.parse("2026-09-20T09:00:00Z")

        val result = FlightDurationCalculator.minutesBetween(instant, instant)

        assertThat(result).isNull()
    }

    @Test
    fun `isValidRange is true only when end is strictly after start`() {
        val start = Instant.parse("2026-09-20T09:00:00Z")
        val end = Instant.parse("2026-09-20T09:32:00Z")

        assertThat(FlightDurationCalculator.isValidRange(start, end)).isTrue()
        assertThat(FlightDurationCalculator.isValidRange(end, start)).isFalse()
        assertThat(FlightDurationCalculator.isValidRange(start, start)).isFalse()
    }
}
