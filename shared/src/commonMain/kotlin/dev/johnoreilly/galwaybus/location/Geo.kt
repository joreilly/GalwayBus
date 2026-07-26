package dev.johnoreilly.galwaybus.location

import dev.johnoreilly.galwaybus.model.Stop
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle (haversine) distance in metres between two WGS84 points. */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2).pow(2) +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(min(1.0, sqrt(a)))
}

private fun Double.toRadians() = this / 180.0 * PI

/** A stop paired with its distance (metres) from a reference location. */
data class NearbyStop(val stop: Stop, val distanceMeters: Double)

/** The [limit] stops closest to [origin], nearest first. */
fun List<Stop>.nearestTo(origin: UserLocation, limit: Int = 20): List<NearbyStop> =
    map { NearbyStop(it, distanceMeters(origin.lat, origin.lon, it.latitude, it.longitude)) }
        .sortedBy { it.distanceMeters }
        .take(limit)
