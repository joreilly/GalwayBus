package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.Stop
import dev.johnoreilly.galwaybus.scan.StopMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StopMatcherTest {

    private val stops = listOf(
        stop("8460B5234401", "523441"),
        stop("8460B5234501", "523451"),
        stop("8460B6355601", "635561"),
    )

    private fun stop(ref: String, code: String) =
        Stop(stop_ref = ref, stop_id = code, long_name = ref, short_name = ref, latitude = 0.0, longitude = 0.0)

    @Test
    fun matchesExactCode() {
        assertEquals("523441", StopMatcher.match("523441", stops)?.stop_id)
    }

    @Test
    fun matchesCodeEmbeddedInNoisyOcrText() {
        // Real OCR of a plate often includes route/operator noise around the code.
        assertEquals("635561", StopMatcher.match("Bus Éireann\n635561\nRoute 401", stops)?.stop_id)
    }

    @Test
    fun matchesCodeSplitByDashOrSpace() {
        assertEquals("523451", StopMatcher.match("5234-51", stops)?.stop_id)
        assertEquals("523451", StopMatcher.match("5234 51", stops)?.stop_id)
    }

    @Test
    fun ignoresShortNumbersLikeRouteNumbers() {
        // "401" is a route number, not a 6-digit stop code — must not match anything.
        assertNull(StopMatcher.match("Route 401 city direct", stops))
    }

    @Test
    fun returnsNullWhenSixDigitCodeIsUnknown() {
        assertNull(StopMatcher.match("999999", stops))
    }

    @Test
    fun returnsNullForBlankOrEmpty() {
        assertNull(StopMatcher.match("", stops))
        assertNull(StopMatcher.match("   ", stops))
        assertNull(StopMatcher.match("523441", emptyList()))
    }
}
