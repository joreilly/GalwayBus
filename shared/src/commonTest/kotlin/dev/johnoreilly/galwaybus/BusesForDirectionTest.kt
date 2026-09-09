package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.StopPrediction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Placing a live bus in one of a route's two directions. This used to be headsign-string equality
 * against a headsign taken from one representative trip in the snapshot, which silently dropped
 * every bus running a short working or a variant destination.
 */
class BusesForDirectionTest {

    private val outbound = setOf("out1", "out2", "out3", "shared")
    private val inbound = setOf("in1", "in2", "in3", "shared")
    private val refs = listOf(outbound, inbound)
    private val headsigns = listOf("Parkmore", "Salthill")

    private fun bus(
        trip: String,
        headsign: String? = null,
        ahead: List<String> = emptyList(),
        nextStop: String? = null
    ) = BusLocation(
        latitude = 53.27, longitude = -9.05, modified_timestamp = "", trip_duid = trip,
        headsign = headsign, next_stop_ref = nextStop,
        next_stops = ahead.takeIf { it.isNotEmpty() }
            ?.mapIndexed { i, ref -> StopPrediction(stop_ref = ref, stop_sequence = i) }
    )

    @Test
    fun `the stops a bus is heading for decide its direction`() {
        assertEquals(0, directionOfBus(bus("t", ahead = listOf("out2", "out3")), refs, headsigns))
        assertEquals(1, directionOfBus(bus("t", ahead = listOf("in1", "in2")), refs, headsigns))
    }

    @Test
    fun `a variant headsign no longer hides a bus`() {
        // The regression this replaces: a short working whose destination string matches neither
        // direction's representative headsign, but whose remaining stops are unambiguous.
        val shortWorking = bus("t", headsign = "GMIT only", ahead = listOf("out2", "out3"))
        assertEquals(0, directionOfBus(shortWorking, refs, headsigns))
        assertTrue(shortWorking in busesForDirection(listOf(shortWorking), refs, headsigns, direction = 0))
        assertTrue(shortWorking !in busesForDirection(listOf(shortWorking), refs, headsigns, direction = 1))
    }

    @Test
    fun `headsign is the fallback when a bus has no predictions`() {
        assertEquals(1, directionOfBus(bus("t", headsign = "Salthill"), refs, headsigns))
    }

    @Test
    fun `a bus only on a shared stop is not forced into a direction`() {
        assertNull(directionOfBus(bus("t", ahead = listOf("shared")), refs, headsigns))
    }

    @Test
    fun `an undecidable bus is shown in every direction rather than vanishing`() {
        val unknown = bus("t", headsign = "Depot")
        assertTrue(unknown in busesForDirection(listOf(unknown), refs, headsigns, direction = 0))
        assertTrue(unknown in busesForDirection(listOf(unknown), refs, headsigns, direction = 1))
    }

    @Test
    fun `next_stop_ref is used when the full prediction list is absent`() {
        assertEquals(0, directionOfBus(bus("t", nextStop = "out1"), refs, headsigns))
    }

    @Test
    fun `a route with one direction filters nothing`() {
        val buses = listOf(bus("a", headsign = "x"), bus("b", headsign = "y"))
        assertEquals(buses, busesForDirection(buses, listOf(outbound), listOf("Parkmore"), direction = 0))
    }
}
