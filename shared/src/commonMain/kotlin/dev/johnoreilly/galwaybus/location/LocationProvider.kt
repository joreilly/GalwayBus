package dev.johnoreilly.galwaybus.location

/** A device location fix (WGS84 latitude/longitude, in degrees). */
data class UserLocation(val lat: Double, val lon: Double)

/** Outcome of a one-shot location request. */
sealed interface LocationResult {
    /** A fix was obtained. */
    data class Available(val location: UserLocation) : LocationResult
    /** Permission was refused or restricted; the UI should offer to re-request. */
    data object PermissionDenied : LocationResult
    /** No fix available: no hardware (e.g. desktop), disabled, or the request failed/timed out. */
    data object Unavailable : LocationResult
}

/**
 * One-shot device location, requesting runtime permission if needed.
 *
 * Platform actuals wrap `FusedLocationProviderClient` (Android) and `CLLocationManager`
 * (iOS); the JVM/desktop actual has no GPS and always reports [LocationResult.Unavailable].
 */
expect class LocationProvider() {
    suspend fun currentLocation(): LocationResult
}
