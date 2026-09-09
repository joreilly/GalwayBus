package dev.johnoreilly.galwaybus

import dev.johnoreilly.galwaybus.location.distanceMeters
import dev.johnoreilly.galwaybus.model.BusLocation
import dev.johnoreilly.galwaybus.model.Stop

/**
 * Index into [stops] of where [bus] currently is along the route, for the tracking timeline's
 * passed/next markers. Returns -1 when it can't be determined.
 *
 * The real-time `next_stop_ref` is normally the best signal, but it can point to a stop far from
 * the vehicle's actual position — e.g. when the feed reports a later leg of the vehicle's block
 * (after the terminus it turns and heads back), which would otherwise reverse the whole timeline.
 * So we anchor on the GPS-nearest stop and only trust `next_stop_ref` when it agrees with it.
 */
internal fun resolveBusIndex(stops: List<Stop>, bus: BusLocation?): Int {
    if (bus == null || stops.isEmpty()) return -1

    // Real metres, not squared degrees: at Galway's latitude a degree of longitude is only about
    // 0.6 of a degree of latitude on the ground, so comparing raw degrees over-weights east-west
    // separation by ~1.7x — enough to pick the wrong anchor stop, which reverses the whole timeline.
    val nearestIndex = stops.indices.minByOrNull { i ->
        distanceMeters(bus.latitude, bus.longitude, stops[i].latitude, stops[i].longitude)
    } ?: -1

    val rtIndex = bus.next_stop_ref
        ?.let { ref -> stops.indexOfFirst { it.stop_ref == ref } }
        ?.takeIf { it >= 0 }

    return when {
        rtIndex == null -> nearestIndex
        nearestIndex < 0 -> rtIndex
        // Consistent with the physical position (at, just before, or a couple of stops ahead of
        // the nearest stop) → trust the more precise real-time value.
        rtIndex in (nearestIndex - 1)..(nearestIndex + 2) -> rtIndex
        // Otherwise next_stop_ref contradicts the GPS fix; the position is ground truth.
        else -> nearestIndex
    }
}
