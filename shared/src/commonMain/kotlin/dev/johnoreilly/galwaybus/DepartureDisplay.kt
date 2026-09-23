package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.DepartureTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * From how far out a departure is shown as a clock time ("18:42") rather than a countdown.
 *
 * A countdown is what you want for the bus you're about to run for; for one an hour away, "1h 12min"
 * is harder to plan with than the time itself, and a clock time is what you compare against the
 * printed timetable at the stop.
 */
internal const val CLOCK_TIME_FROM_MINUTES = 20

/** How a departure's time reads: imminent, a countdown, or a clock time further out. */
internal sealed interface DepartureWhen {
    data object Due : DepartureWhen
    data class Minutes(val minutes: Int) : DepartureWhen
    data class ClockTime(val time: String) : DepartureWhen
    data object Unknown : DepartureWhen
}

/**
 * Whole minutes from [now] until [at], rounded to the nearest minute rather than down. Rounding
 * down made "Due" cover the whole last minute — up to 59 seconds early — and "1 min" anything up
 * to 1m59s. Rounded, "Due" (0 or less) is the last 30 seconds and "1 min" is 30–89 seconds.
 */
internal fun minutesUntil(at: Instant, now: Instant): Int {
    val seconds = (at - now).inWholeSeconds
    // Truncating division, so anything already past stays at 0 or below and still reads as due.
    return ((seconds + 30) / 60).toInt()
}

internal fun departureWhen(departTimestamp: String?, now: Instant, zone: TimeZone): DepartureWhen {
    val at = departTimestamp?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return DepartureWhen.Unknown
    val minutes = minutesUntil(at, now)
    return when {
        minutes <= 0 -> DepartureWhen.Due
        minutes < CLOCK_TIME_FROM_MINUTES -> DepartureWhen.Minutes(minutes)
        else -> {
            val local = at.toLocalDateTime(zone)
            DepartureWhen.ClockTime("${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}")
        }
    }
}

/**
 * Whether this time comes from real-time tracking rather than the timetable alone. The backend sets
 * [DepartureTime.delaySeconds] only when NTA said something about this departure; null means the
 * time is just the schedule, and the list shouldn't present it as though a bus were being tracked.
 */
internal val DepartureTime.isLive: Boolean get() = delaySeconds != null
