package dev.johnoreilly.galwaybus.location

/**
 * Desktop has no location hardware, so there is never a device fix. The nearby-stops UI
 * treats [LocationResult.Unavailable] by falling back to Galway city centre.
 */
actual class LocationProvider {
    actual suspend fun currentLocation(): LocationResult = LocationResult.Unavailable
}
