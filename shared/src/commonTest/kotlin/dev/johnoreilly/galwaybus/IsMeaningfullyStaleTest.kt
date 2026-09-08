package dev.johnoreilly.galwaybus

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsMeaningfullyStaleTest {

    private val threshold = 90L

    @Test
    fun liveDataIsNeverFlagged() {
        assertFalse(isMeaningfullyStale(stale = false, staleSeconds = null, thresholdSeconds = threshold))
        // A live response should never carry an age at all, but even if it did, `stale = false`
        // is the authority — it should still not flag.
        assertFalse(isMeaningfullyStale(stale = false, staleSeconds = 500L, thresholdSeconds = threshold))
    }

    @Test
    fun aFreshFallbackUnderThresholdIsNotFlagged() {
        // One missed poll cycle: stale, but barely older than a live response would have been —
        // this is the routine-rate-limiting case the threshold exists to quiet down.
        assertFalse(isMeaningfullyStale(stale = true, staleSeconds = 10L, thresholdSeconds = threshold))
        assertFalse(isMeaningfullyStale(stale = true, staleSeconds = threshold - 1, thresholdSeconds = threshold))
    }

    @Test
    fun aFallbackAtOrPastThresholdIsFlagged() {
        assertTrue(isMeaningfullyStale(stale = true, staleSeconds = threshold, thresholdSeconds = threshold))
        assertTrue(isMeaningfullyStale(stale = true, staleSeconds = threshold + 120, thresholdSeconds = threshold))
    }

    @Test
    fun anUnknownAgeIsTreatedAsWorthFlagging() {
        // No fallback existed at all to measure an age from — a harder failure than a merely-old
        // one, so it must not read as "fresher than the threshold".
        assertTrue(isMeaningfullyStale(stale = true, staleSeconds = null, thresholdSeconds = threshold))
    }
}
