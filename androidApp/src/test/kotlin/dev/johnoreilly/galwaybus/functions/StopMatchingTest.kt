package dev.johnoreilly.galwaybus.functions

import dev.johnoreilly.galwaybus.model.DepartureTime
import dev.johnoreilly.galwaybus.model.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StopMatchingTest {

    private val eyreSquare = Stop(
        stop_ref = "8460B5243701",
        stop_id = "524371",
        long_name = "Eyre Square",
        long_name_ga = "An Fhaiche Mhór",
        short_name = "Eyre Square",
        latitude = 53.2749,
        longitude = -9.0491,
        routes = listOf("404", "405"),
        direction = "Rahoon",
    )

    @Test
    fun `matches a name ignoring case and the words models add`() {
        assertTrue(eyreSquare.matchesName("eyre square"))
        assertTrue(eyreSquare.matchesName("the Eyre Square stop"))
        assertTrue(eyreSquare.matchesName("Eyre Sq"))
        assertFalse(eyreSquare.matchesName("Salthill"))
    }

    @Test
    fun `matches the Irish name without accents`() {
        assertTrue(eyreSquare.matchesName("Fhaiche Mhor"))
    }

    @Test
    fun `matches the stop number`() {
        assertTrue(eyreSquare.matchesName("524371"))
        assertFalse(eyreSquare.matchesName("52437"))
    }

    @Test
    fun `every word of the query has to match`() {
        assertFalse(eyreSquare.matchesName("Eyre Road"))
        assertFalse("Dr. Mannix Road".matchesText(""))
        assertTrue("Dr. Mannix Road".matchesText("mannix"))
    }

    @Test
    fun `route filter`() {
        assertTrue(eyreSquare.servesRoute(null))
        assertTrue(eyreSquare.servesRoute(" 405 "))
        assertFalse(eyreSquare.servesRoute("401"))
    }

    @Test
    fun `departure in local time, rounded minutes and delay`() {
        val now = Instant.parse("2026-11-28T11:00:00Z")
        val live = DepartureTime(
            display_name = "Rahoon",
            timetable_id = "405",
            depart_timestamp = "2026-11-28T11:02:39Z",
            delaySeconds = -141,
        ).toResponse(eyreSquare, now)!!
        assertEquals("11:02", live.departs) // Irish winter time is UTC
        assertEquals(3, live.minutesAway)
        assertEquals(-2, live.minutesLate)
        assertEquals("524371", live.stopNumber)

        val timetabled = DepartureTime("Newcastle", "404", "2026-07-01T11:18:30Z").toResponse(eyreSquare, now)!!
        assertEquals("12:18", timetabled.departs) // summer time is UTC+1
        assertNull(timetabled.minutesLate)
    }

    @Test
    fun `departure without a time is dropped`() {
        assertNull(DepartureTime("Rahoon", "405").toResponse(eyreSquare, Instant.now()))
    }
}
