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
    private val nowMs = now.toEpochMilliseconds()

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
    fun `stops ahead are labelled with the minutes until the bus reaches them`() {
        // Minutes, not clock times: the timeline beside the map counts in minutes, and the two
        // disagreeing on screen ("2′" next to "22:27" for one stop) read as two different things.
        val etas = stopEtasFor(busAt("C" to inMinutes(5), "D" to inMinutes(12)), stops, nowMs = nowMs)
        assertEquals("5′", etas.getValue("C").label)
        assertEquals("12′", etas.getValue("D").label)
        assertTrue(etas.getValue("C").upcoming)
    }

    @Test
    fun `a bus already there reads as Due`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(0)), stops, nowMs = nowMs)
        assertEquals("Due", etas.getValue("C").label)
    }

    @Test
    fun `only the next few stops are labelled`() {
        // Labelling the whole trip filled the map with times that collided with each other and
        // with the map's own place names.
        val bus = busAt("A" to inMinutes(1), "B" to inMinutes(3), "C" to inMinutes(6), "D" to inMinutes(9))
        val etas = stopEtasFor(bus, stops, nowMs = nowMs, labelledAhead = 3)
        assertEquals(listOf("A", "B", "C"), etas.filterValues { it.label.isNotBlank() }.keys.sorted())
        assertTrue(etas["D"] == null || etas.getValue("D").label.isBlank(), "The fourth stop ahead should be plain")
    }

    @Test
    fun `the stop being tracked keeps its time however far ahead it is`() {
        val bus = busAt("A" to inMinutes(1), "B" to inMinutes(3), "C" to inMinutes(6), "D" to inMinutes(20))
        val etas = stopEtasFor(bus, stops, trackedStopRef = "D", nowMs = nowMs, labelledAhead = 2)
        assertEquals("20′", etas.getValue("D").label, "The stop you are waiting at must always show its time")
    }

    @Test
    fun `stops the bus has passed are marked passed, not merely unknown`() {
        val etas = stopEtasFor(busAt("C" to inMinutes(5), "D" to inMinutes(12)), stops, nowMs = nowMs)
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
        val etas = stopEtasFor(busAt("C" to null, "D" to inMinutes(12)), stops, nowMs = nowMs)
        assertNull(etas["C"], "An ahead-but-untimed stop should not be labelled or muted")
        assertTrue(etas.getValue("D").upcoming)
    }

    @Test
    fun `with no bus in the feed nothing is annotated`() {
        assertTrue(stopEtasFor(null, stops, nowMs = nowMs).isEmpty())
    }
}
