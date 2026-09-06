package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.model.StopPrediction
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

    private val stops = listOf(stop("A"), stop("B"), stop("C"), stop("D"))

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
    fun `stops ahead are labelled with their predicted time`() {
        val etas = stopEtasFor(busAt("C" to "2026-09-06T17:05:00Z", "D" to "2026-09-06T17:12:00Z"), stops)
        assertTrue(etas.getValue("C").upcoming)
        assertTrue(etas.getValue("D").upcoming)
        // Formatted as a local clock time; the exact hour depends on the test machine's zone, so
        // assert the shape rather than the value.
        assertTrue(etas.getValue("C").label.matches(Regex("""\d{2}:\d{2}""")), "Got '${etas.getValue("C").label}'")
    }

    @Test
    fun `stops the bus has passed are marked passed, not merely unknown`() {
        val etas = stopEtasFor(busAt("C" to "2026-09-06T17:05:00Z", "D" to "2026-09-06T17:12:00Z"), stops)
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
        val etas = stopEtasFor(busAt("C" to null, "D" to "2026-09-06T17:12:00Z"), stops)
        assertNull(etas["C"], "An ahead-but-untimed stop should not be labelled or muted")
        assertTrue(etas.getValue("D").upcoming)
    }

    @Test
    fun `with no bus in the feed nothing is annotated`() {
        assertTrue(stopEtasFor(null, stops).isEmpty())
    }
}
