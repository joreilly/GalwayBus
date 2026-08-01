package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusPositionsForDisplayTest {

    private val graceMs = 120_000L

    private fun bus(id: String) =
        BusLocation(latitude = 53.27, longitude = -9.05, modified_timestamp = "", trip_duid = id)

    private val fetched = listOf(bus("a"), bus("b"))
    private val current = listOf(bus("x"))

    @Test
    fun nonEmptyFetchIsShownAsIs() {
        assertEquals(fetched, busPositionsForDisplay(fetched, current, 0, graceMs))
    }

    @Test
    fun transientEmptyWithinGraceRetainsCurrent() {
        assertEquals(
            current,
            busPositionsForDisplay(emptyList(), current, graceMs - 1, graceMs)
        )
    }

    @Test
    fun sustainedEmptyPastGraceClears() {
        assertTrue(busPositionsForDisplay(emptyList(), current, graceMs, graceMs).isEmpty())
        assertTrue(busPositionsForDisplay(emptyList(), current, graceMs + 5_000, graceMs).isEmpty())
    }

    @Test
    fun emptyWithNothingToRetainStaysEmpty() {
        assertTrue(busPositionsForDisplay(emptyList(), emptyList(), 0, graceMs).isEmpty())
    }
}
