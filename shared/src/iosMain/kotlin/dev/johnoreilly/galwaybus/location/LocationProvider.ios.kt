package dev.johnoreilly.galwaybus.location

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class LocationProvider {
    // CLLocationManager.delegate is a weak reference, so both must be retained for the
    // lifetime of the request (the provider is held by the ViewModel).
    private var manager: CLLocationManager? = null
    private var delegate: LocationDelegate? = null

    actual suspend fun currentLocation(): LocationResult = suspendCancellableCoroutine { cont ->
        val locationManager = CLLocationManager()
        val locationDelegate = LocationDelegate { result ->
            if (cont.isActive) cont.resume(result)
        }
        locationManager.delegate = locationDelegate
        manager = locationManager
        delegate = locationDelegate

        cont.invokeOnCancellation { locationManager.delegate = null }

        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted ->
                if (cont.isActive) cont.resume(LocationResult.PermissionDenied)
            else -> locationManager.requestLocation()
        }
    }

    private class LocationDelegate(
        private val onResult: (LocationResult) -> Unit
    ) : NSObject(), CLLocationManagerDelegateProtocol {
        private var finished = false

        private fun finish(result: LocationResult) {
            if (finished) return
            finished = true
            onResult(result)
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            when (manager.authorizationStatus) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
                kCLAuthorizationStatusDenied,
                kCLAuthorizationStatusRestricted -> finish(LocationResult.PermissionDenied)
                else -> { /* still NotDetermined — wait for the user to answer the prompt */ }
            }
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation
            if (location != null) {
                val (lat, lon) = location.coordinate.useContents { latitude to longitude }
                finish(LocationResult.Available(UserLocation(lat, lon)))
            } else {
                finish(LocationResult.Unavailable)
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            finish(LocationResult.Unavailable)
        }
    }
}
