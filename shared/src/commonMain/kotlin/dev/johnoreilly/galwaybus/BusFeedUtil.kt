package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.model.BusLocation

/**
 * Whether the "not live" chip is worth showing: the backend polls NTA about as often as its rate
 * limit allows, so [stale] alone flags every routine missed poll along with genuine outages —
 * flapping the chip on and off for data that's barely older than a live response would be. Only
 * once the served data's age crosses [thresholdSeconds] (a real, sustained outage) does it read as
 * worth telling someone. An unknown age (no fallback existed at all to measure — a harder failure
 * than a merely-old one) is treated as exceeding the threshold rather than as fresher than it is.
 */
internal fun isMeaningfullyStale(
    stale: Boolean,
    staleSeconds: Long?,
    thresholdSeconds: Long = 90L
): Boolean = stale && (staleSeconds == null || staleSeconds >= thresholdSeconds)

/**
 * Which of a route's directions a bus is running, or null if it can't be told.
 *
 * The route map shows one direction at a time, so each live bus has to be placed in one. Matching
 * the bus's headsign against the direction's headsign looks like the obvious way and is not: the
 * direction's headsign comes from one representative trip picked when the GTFS snapshot was
 * generated, so any trip running a short working or a variant destination carries a different
 * string and matches neither direction — and so disappeared from the map entirely.
 *
 * The stops a bus says it is still heading for are a far better signal, because the two directions
 * of a Galway route barely share any: opposite sides of the same road are different stop refs. So
 * the direction is whichever one's stop list covers most of the bus's remaining stops, provided
 * one of them wins outright. Headsign equality is kept as the fallback for a bus with no
 * predictions at all (no real-time entry for its trip), and null means neither could decide.
 */
internal fun directionOfBus(
    bus: BusLocation,
    stopRefsByDirection: List<Set<String>>,
    headsignsByDirection: List<String>
): Int? {
    val ahead = bus.next_stops?.map { it.stop_ref }?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(bus.next_stop_ref)

    if (ahead.isNotEmpty()) {
        val overlaps = stopRefsByDirection.map { refs -> ahead.count { it in refs } }
        val best = overlaps.maxOrNull() ?: 0
        // A tie says the stops don't distinguish the directions here (a shared city-centre leg,
        // or a bus with a single prediction on a stop both directions serve) — not an answer.
        if (best > 0 && overlaps.count { it == best } == 1) return overlaps.indexOf(best)
    }

    return headsignsByDirection
        .indexOfFirst { it.isNotEmpty() && it == bus.headsign }
        .takeIf { it >= 0 }
}

/**
 * The buses to draw on the map for [direction]. A bus whose direction can't be determined is shown
 * rather than hidden: a marker in the wrong half of a route is a smaller lie than a bus the rider
 * can see out the window but not on the map.
 */
internal fun busesForDirection(
    buses: List<BusLocation>,
    stopRefsByDirection: List<Set<String>>,
    headsignsByDirection: List<String>,
    direction: Int
): List<BusLocation> {
    if (stopRefsByDirection.size < 2) return buses
    return buses.filter { bus ->
        val resolved = directionOfBus(bus, stopRefsByDirection, headsignsByDirection)
        resolved == null || resolved == direction
    }
}
