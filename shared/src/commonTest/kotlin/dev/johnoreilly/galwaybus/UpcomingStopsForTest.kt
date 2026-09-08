package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.model.StopPrediction
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The floating card's list of stops a tracked bus is still due at — the same "already passed"
 * judgement as [StopEtaTest], but as a plain list rather than per-stop map labels.
 */
class UpcomingStopsForTest {

    private fun stop(ref: String) = Stop(
        stop_ref = ref, stop_id = ref.takeLast(6), long_name = ref,
        short_name = ref, latitude = 53.27, longitude = -9.05
    )

    private val stops = listOf(stop("A"), stop("B"), stop("C"), stop("D"))

    private val now = Instant.parse("2026-09-06T17:00:00Z")

    private fun inMinutes(minutes: Int) = (now + minutes.minutes).toString()

    private fun busAt(vararg ahead: Pair<String, String?>) = BusLocation(
        latitude = 53.27,
        longitude = -9.05,
        modified_timestamp = "2026-09-06T17:00:00Z",
        trip_duid = "trip-1",
        next_stops = ahead.mapIndexed { i, (ref, time) ->
            StopPrediction(stop_ref = ref, stop_sequence = i, departure_timestamp = time)
        }
    )

    @Test
    fun `upcoming stops keep their minutes-away count, nearest first`() {
        val result = upcomingStopsFor(busAt("A" to inMinutes(2), "B" to inMinutes(9)), stops, now)
        assertEquals(listOf("A" to 2, "B" to 9), result.map { (s, m) -> s.stop_ref to m })
    }

    @Test
    fun `a prediction only briefly behind now, within ordinary latency, is not dropped`() {
        // 60s behind is plain fetch/poll/render latency, not evidence the bus has passed the stop.
        val result = upcomingStopsFor(busAt("A" to inMinutes(-1), "B" to inMinutes(5)), stops, now)
        assertEquals(listOf("A", "B"), result.map { it.first.stop_ref })
    }

    @Test
    fun `a prediction genuinely behind now is dropped, not shown as due`() {
        val result = upcomingStopsFor(busAt("A" to inMinutes(-3), "B" to inMinutes(5)), stops, now)
        assertEquals(listOf("B"), result.map { it.first.stop_ref }, "A's moment is well past, so it should not appear at all")
    }

    @Test
    fun `several closely spaced overdue stops are all dropped, not all shown as due`() {
        // The reported case: a bus running ahead of schedule through a tight cluster of stops.
        val bus = busAt("A" to inMinutes(-4), "B" to inMinutes(-3), "C" to inMinutes(0), "D" to inMinutes(2))
        val result = upcomingStopsFor(bus, stops, now)
        assertEquals(listOf("C", "D"), result.map { it.first.stop_ref })
    }

    @Test
    fun `a prediction exactly at now is still shown, not dropped`() {
        val result = upcomingStopsFor(busAt("A" to inMinutes(0)), stops, now)
        assertEquals(listOf("A" to 0), result.map { (s, m) -> s.stop_ref to m })
    }

    @Test
    fun `an untimed stop still appears with no minutes`() {
        val result = upcomingStopsFor(busAt("A" to null), stops, now)
        assertEquals(1, result.size)
        assertNull(result.first().second)
    }

    @Test
    fun `a stop ref not on this trip's stop list is skipped`() {
        val result = upcomingStopsFor(busAt("Z" to inMinutes(1), "A" to inMinutes(2)), stops, now)
        assertEquals(listOf("A"), result.map { it.first.stop_ref })
    }

    @Test
    fun `the list is capped at the given limit`() {
        val bus = busAt("A" to inMinutes(1), "B" to inMinutes(2), "C" to inMinutes(3), "D" to inMinutes(4))
        assertEquals(2, upcomingStopsFor(bus, stops, now, limit = 2).size)
        assertTrue(upcomingStopsFor(bus, stops, now).size <= 4, "Default limit is 4")
    }
}
