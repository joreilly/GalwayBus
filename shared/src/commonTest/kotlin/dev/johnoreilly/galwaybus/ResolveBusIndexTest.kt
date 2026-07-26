package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveBusIndexTest {

    // A short linear route running roughly west→east (increasing longitude).
    private val stops = listOf(
        stop("A", 53.26, -9.10),
        stop("B", 53.26, -9.08),
        stop("C", 53.26, -9.06),
        stop("D", 53.26, -9.04),
        stop("E", 53.26, -9.02),
    )

    private fun stop(ref: String, lat: Double, lon: Double) =
        Stop(stop_ref = ref, stop_id = ref, long_name = ref, short_name = ref, latitude = lat, longitude = lon)

    private fun bus(lat: Double, lon: Double, nextStopRef: String? = null) =
        BusLocation(
            latitude = lat, longitude = lon, modified_timestamp = "", trip_duid = "t",
            next_stop_ref = nextStopRef
        )

    @Test
    fun nullBusIsUnknown() {
        assertEquals(-1, resolveBusIndex(stops, null))
    }

    @Test
    fun usesNearestStopWhenNoRealtime() {
        // Sitting on top of stop C, no next_stop_ref.
        assertEquals(2, resolveBusIndex(stops, bus(53.26, -9.06)))
    }

    @Test
    fun trustsRealtimeWhenConsistentWithPosition() {
        // Between C and D, next stop reported as D — position agrees, so use the RT value.
        assertEquals(3, resolveBusIndex(stops, bus(53.26, -9.05, nextStopRef = "D")))
    }

    @Test
    fun ignoresRealtimeWhenItContradictsPosition() {
        // The reported bug: the bus is physically at E (end of the line) but the feed says its
        // next stop is B (near the start, a later leg of its block). Trust the GPS position.
        assertEquals(4, resolveBusIndex(stops, bus(53.26, -9.02, nextStopRef = "B")))
    }

    @Test
    fun unknownRealtimeRefFallsBackToPosition() {
        // next_stop_ref not present in this direction's stop list at all.
        assertEquals(1, resolveBusIndex(stops, bus(53.26, -9.08, nextStopRef = "ZZZ")))
    }
}
