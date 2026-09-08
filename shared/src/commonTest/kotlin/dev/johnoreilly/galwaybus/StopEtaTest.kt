package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.model.StopPrediction
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Times drawn beside the stops on the tracked trip's line. The distinction that matters is between
 * a stop the bus is still coming to and one it has already left: the feed only predicts the former,
 * so the latter must be shown as passed rather than as "no data".
 */
class StopEtaTest {

    private fun stop(ref: String) = Stop(
        stop_ref = ref, stop_id = ref.takeLast(6), long_name = ref,
        short_name = ref, latitude = 53.27, longitude = -9.05
    )

    private val stops = listOf(stop("A"), stop("B"), stop("C"), stop("D"), stop("E"), stop("F"))

    private val now = Instant.parse("2026-09-06T17:00:00Z")
    /** Pinned: clock labels are rendered in a zone, and CI runs in UTC while a dev Mac does not. */
    private val zone = TimeZone.UTC

    /** An ISO timestamp [minutes] from the fixed "now" these tests run at. */
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
    fun `stops ahead are labelled with the predicted clock time`() {
        // Clock times, not a countdown: this is the number you compare against a printed
        // timetable, and the timeline gutter beside the map renders the same string.
        val etas = stopEtasFor(busAt("C" to inMinutes(5), "D" to inMinutes(12)), stops, now = now, zone = zone)
        assertEquals("17:05", etas.getValue("C").label)
        assertEquals("17:12", etas.getValue("D").label)
        assertTrue(etas.getValue("C").upcoming)
    }

    @Test
    fun `an imminent stop still shows its time`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(0)), stops, now = now, zone = zone)
        assertEquals("17:00", etas.getValue("C").label)
    }

    @Test
    fun `the clock label rolls over the hour correctly`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(65)), stops, now = now, zone = zone)
        assertEquals("18:05", etas.getValue("C").label, "17:00 + 65 min is 18:05, not 17:65")
    }

    @Test
    fun `only the next few stops are labelled`() {
        // Labelling the whole trip filled the map with times that collided with each other and
        // with the map's own place names.
        val bus = busAt("A" to inMinutes(1), "B" to inMinutes(3), "C" to inMinutes(6), "D" to inMinutes(9))
        val etas = stopEtasFor(bus, stops, labelledAhead = 3, now = now, zone = zone)
        assertEquals(listOf("A", "B", "C"), etas.filterValues { it.label.isNotBlank() }.keys.sorted())
        assertTrue(etas["D"] == null || etas.getValue("D").label.isBlank(), "The fourth stop ahead should be plain")
    }

    @Test
    fun `the stop being tracked keeps its time however far ahead it is`() {
        val bus = busAt("A" to inMinutes(1), "B" to inMinutes(3), "C" to inMinutes(6), "D" to inMinutes(20))
        val etas = stopEtasFor(bus, stops, trackedStopRef = "D", labelledAhead = 2, now = now, zone = zone)
        assertEquals("17:20", etas.getValue("D").label, "The stop you are waiting at must always show its time")
    }

    @Test
    fun `stops the bus has passed are marked passed, not merely unknown`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(5), "D" to inMinutes(12)), stops, now = now, zone = zone)
        listOf("A", "B").forEach { ref ->
            val eta = etas[ref]
            assertTrue(eta != null, "Stop $ref should be marked as passed")
            assertTrue(!eta.upcoming, "Stop $ref should not be upcoming")
            assertEquals("", eta.label, "A passed stop has no prediction to show")
        }
    }

    @Test
    fun `an upcoming stop the feed hasn't timed yet is left plain`() {
        // NTA's predictions are sparse; a stop that is ahead but untimed gets no marker treatment,
        // rather than being wrongly greyed out as passed.
        val etas = stopEtasFor(busAt("C" to null, "D" to inMinutes(12)), stops, now = now, zone = zone)
        assertNull(etas["C"], "An ahead-but-untimed stop should not be labelled or muted")
        assertTrue(etas.getValue("D").upcoming)
    }

    @Test
    fun `with no bus in the feed nothing is annotated`() {
        assertTrue(stopEtasFor(null, stops, zone = zone).isEmpty())
    }

    // --- a predicted time that has already ticked into the past ---
    //
    // The feed keeps a stop in the ahead-list for a short grace period after its predicted moment,
    // so the marker doesn't blink out the instant the bus is due. On closely spaced stops and a bus
    // running meaningfully ahead of schedule, several of those grace windows can overlap: without
    // this, all of them would keep showing their (by-then past) clock time as if still imminent.

    @Test
    fun `a predicted time already behind now is muted like a passed stop`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(-2)), stops, now = now, zone = zone)
        val eta = etas["C"]
        assertTrue(eta != null, "Still in the ahead-list, so still worth a marker")
        assertTrue(!eta.upcoming, "But its moment has passed, so it should read as passed")
        assertEquals("", eta.label, "A stale clock time is worse than none")
    }

    @Test
    fun `several closely spaced stops overdue at once are all muted, not all shown stale`() {
        // The reported case: a bus running ahead of schedule through a tight cluster of stops can
        // have several of them overdue in the same poll.
        val bus = busAt("A" to inMinutes(-3), "B" to inMinutes(-1), "C" to inMinutes(2))
        val etas = stopEtasFor(bus, stops, now = now, zone = zone)
        assertEquals("", etas.getValue("A").label)
        assertEquals("", etas.getValue("B").label)
        assertEquals("17:02", etas.getValue("C").label, "The one stop still ahead keeps its real time")
    }
}
