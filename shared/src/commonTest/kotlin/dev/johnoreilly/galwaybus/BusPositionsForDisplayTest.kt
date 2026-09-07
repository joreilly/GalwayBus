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

    // --- once the backend says whether an empty response is an outage ---

    @Test
    fun staleFeedWithPositionsStillShowsThem() {
        // The backend serves its own last known positions for a few minutes. They are the best
        // guess available, so they stay on the map — the UI labels them rather than hiding them.
        assertEquals(fetched, busPositionsForDisplay(fetched, current, 0, graceMs, feedStale = true))
    }

    @Test
    fun staleFeedWithNothingLeftClearsImmediately() {
        // The backend has given up, so there is nothing to smooth over. Holding the old markers
        // for another two minutes would be showing buses no one can vouch for; the banner
        // explains the empty map instead.
        assertTrue(busPositionsForDisplay(emptyList(), current, 0, graceMs, feedStale = true).isEmpty())
    }

    @Test
    fun staleFlagBeatsTheGraceTimer() {
        // Well inside the grace window, which alone would have retained `current`.
        assertTrue(
            busPositionsForDisplay(emptyList(), current, 1_000, graceMs, feedStale = true).isEmpty(),
            "An explained outage should not fall back to the timer's guess"
        )
    }

    @Test
    fun aHealthyEmptyStillUsesTheGraceTimer() {
        // The old-backend path: no flag, so an empty response is still ambiguous and smoothed.
        assertEquals(
            current,
            busPositionsForDisplay(emptyList(), current, 1_000, graceMs, feedStale = false)
        )
    }
}
