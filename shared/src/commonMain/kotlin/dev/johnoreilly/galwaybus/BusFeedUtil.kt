package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation

/**
 * Decides which bus positions to show when a poll comes back empty.
 *
 * An empty `/bus.json` has two very different meanings — "nothing is running" and "we cannot
 * reach the live feed" — and the app used to be unable to tell them apart, so it guessed with a
 * timer: keep the last known buses for [graceMs], then clear. That guess is wrong both ways. It
 * hides a real outage behind markers that look live, and then clears the map with no explanation.
 *
 * The backend now says which it is, so the guess is only the fallback:
 *  - Positions came back → show them. [feedStale] tells the UI whether to label them as not live;
 *    the backend serves its own last-known data for a few minutes before giving up.
 *  - Nothing came back and the backend says it is stale → it has given up, so we clear rather
 *    than freezing old buses on screen forever. The UI explains why, which is the whole point.
 *  - Nothing came back and the backend says it is fine → trust the timer as before. This is the
 *    path for a backend too old to send the flag, where empty is still ambiguous.
 *
 * @param fetched positions from the latest poll (possibly empty).
 * @param current positions currently on the map.
 * @param msSinceLastNonEmpty time since the last non-empty poll.
 * @param graceMs how long to retain positions through unexplained empty polls.
 * @param feedStale whether the backend reported this response as not live.
 */
internal fun busPositionsForDisplay(
    fetched: List<BusLocation>,
    current: List<BusLocation>,
    msSinceLastNonEmpty: Long,
    graceMs: Long,
    feedStale: Boolean = false
): List<BusLocation> = when {
    fetched.isNotEmpty() -> fetched
    feedStale -> emptyList()
    current.isNotEmpty() && msSinceLastNonEmpty < graceMs -> current
    else -> emptyList()
}
