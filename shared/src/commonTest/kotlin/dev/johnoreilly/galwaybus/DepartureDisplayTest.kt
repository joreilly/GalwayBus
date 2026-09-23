package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.DepartureTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DepartureDisplayTest {

    private val now = Instant.parse("2026-09-23T17:00:00Z")
    /** Pinned: clock times are rendered in a zone, and CI runs in UTC while a dev Mac does not. */
    private val zone = TimeZone.UTC

    private fun whenIn(offsetSeconds: Long) =
        departureWhen((now + offsetSeconds.seconds).toString(), now, zone)

    @Test
    fun `an imminent departure is due`() {
        assertEquals(DepartureWhen.Due, whenIn(30))
        assertEquals(DepartureWhen.Due, whenIn(-45), "Just gone but still listed reads as due, not negative")
    }

    @Test
    fun `a nearby departure counts down in minutes`() {
        assertEquals(DepartureWhen.Minutes(1), whenIn(60))
        assertEquals(DepartureWhen.Minutes(19), whenIn(19.minutes.inWholeSeconds + 59))
    }

    @Test
    fun `from twenty minutes out it is a clock time`() {
        assertEquals(DepartureWhen.ClockTime("17:20"), whenIn(20.minutes.inWholeSeconds))
        assertEquals(DepartureWhen.ClockTime("18:12"), whenIn(72.minutes.inWholeSeconds))
    }

    @Test
    fun `the clock time is the rider's local time`() {
        val dublin = TimeZone.of("Europe/Dublin")
        assertEquals(DepartureWhen.ClockTime("18:42"), departureWhen("2026-09-23T17:42:00Z", now, dublin))
    }

    @Test
    fun `a missing or unreadable time is unknown`() {
        assertEquals(DepartureWhen.Unknown, departureWhen(null, now, zone))
        assertEquals(DepartureWhen.Unknown, departureWhen("soon", now, zone))
    }

    @Test
    fun `only a departure the feed said something about is live`() {
        fun dep(delay: Int?) = DepartureTime(display_name = "City", timetable_id = "401", delaySeconds = delay)
        assertTrue(dep(0).isLive, "On time according to the feed is still live")
        assertTrue(dep(240).isLive)
        assertFalse(dep(null).isLive, "No delay reading means timetable only")
    }
}
