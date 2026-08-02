package dev.johnoreilly.galwaybus.location

import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (onSuccess, onError) => {
        if (!navigator.geolocation) { onError(0); return; }
        navigator.geolocation.getCurrentPosition(
            (pos) => onSuccess(pos.coords.latitude, pos.coords.longitude),
            (err) => onError(err.code)
        );
    }
    """
)
private external fun requestGeolocation(onSuccess: (Double, Double) -> Unit, onError: (Int) -> Unit)

/** Browser Geolocation API. Error code 1 is `PERMISSION_DENIED`; anything else (no API, timeout,
 *  position unavailable) maps to [LocationResult.Unavailable]. */
actual class LocationProvider {
    actual suspend fun currentLocation(): LocationResult = suspendCancellableCoroutine { cont ->
        requestGeolocation(
            onSuccess = { lat, lon ->
                if (cont.isActive) cont.resume(LocationResult.Available(UserLocation(lat, lon)))
            },
            onError = { code ->
                if (cont.isActive) {
                    cont.resume(if (code == 1) LocationResult.PermissionDenied else LocationResult.Unavailable)
                }
            }
        )
    }
}
