package com.neosky.servicesupport.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Central date/time/duration formatting so every screen renders the same way. */
object DateTimeFormatters {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    fun formatDate(date: LocalDate?): String = date?.format(dateFormatter) ?: "—"

    fun formatDate(instant: Instant?): String = instant?.let {
        LocalDate.ofInstant(it, zone).format(dateFormatter)
    } ?: "—"

    fun formatDateTime(instant: Instant?): String = instant?.atZone(zone)?.format(dateTimeFormatter) ?: "—"

    fun formatTime(instant: Instant?): String = instant?.atZone(zone)?.format(timeFormatter) ?: "—"

    /** Parses a `yyyy-MM` API month bucket (e.g. flight stats) into a short "MMM yyyy" label. */
    fun formatMonthBucket(yearMonth: String): String = runCatching {
        val parts = yearMonth.split("-")
        val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
        date.format(monthYearFormatter)
    }.getOrDefault(yearMonth)

    /** Minutes -> "1h 32m" / "32m" for compact display. */
    fun formatDurationMinutes(minutes: Double): String {
        val totalMinutes = minutes.toLong()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /** Hours (as used by flight-hours totals) -> "126.5 hrs". */
    fun formatHours(hours: Double): String = String.format(Locale.getDefault(), "%.1f hrs", hours)

    /** Relative "3 days remaining" / "Expired 4 days ago" style label for warranty/maintenance countdowns. */
    fun formatDaysRemaining(days: Int): String = when {
        days > 1 -> "$days days remaining"
        days == 1 -> "1 day remaining"
        days == 0 -> "Due today"
        days == -1 -> "1 day overdue"
        else -> "${-days} days overdue"
    }
}
