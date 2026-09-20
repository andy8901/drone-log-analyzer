package com.neosky.servicesupport.core.util

import java.time.Instant
import java.time.LocalDate

/**
 * Lenient parsers for the ISO-8601 date/timestamp strings the API sends (see API_SPEC.md
 * "Conventions": dates are `YYYY-MM-DD`, timestamps are `YYYY-MM-DDTHH:MM:SSZ`). Every DTO ->
 * domain mapper goes through these so a malformed/missing date never crashes a screen.
 */
object DateParsing {
    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }

    fun parseInstantOrEpoch(value: String?): Instant = parseInstant(value) ?: Instant.EPOCH

    fun parseLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    fun parseLocalDateOrEpoch(value: String?): LocalDate = parseLocalDate(value) ?: LocalDate.EPOCH
}
