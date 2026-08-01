package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation

/**
 * Decides which bus positions to show, smoothing over the /bus.json feed's transient empties.
 *
 * The backend intermittently returns an empty `bus` map (HTTP 200) even during active service,
 * which would otherwise blank the map every few polls. So when a poll comes back empty we keep
 * showing the last known buses until [msSinceLastNonEmpty] exceeds [graceMs] — after which a
 * sustained empty is treated as genuine "no service" and the map clears.
 *
 * @param fetched positions from the latest poll (possibly empty).
 * @param current positions currently on the map.
 * @param msSinceLastNonEmpty time since the last non-empty poll.
 * @param graceMs how long to retain stale positions through empty polls.
 */
internal fun busPositionsForDisplay(
    fetched: List<BusLocation>,
    current: List<BusLocation>,
    msSinceLastNonEmpty: Long,
    graceMs: Long
): List<BusLocation> = when {
    fetched.isNotEmpty() -> fetched
    current.isNotEmpty() && msSinceLastNonEmpty < graceMs -> current
    else -> emptyList()
}
