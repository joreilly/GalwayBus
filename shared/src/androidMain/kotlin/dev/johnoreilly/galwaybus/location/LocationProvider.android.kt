package dev.johnoreilly.galwaybus.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wiring for Android location access. The Android entry point ([MainActivity]) populates
 * both fields at startup: [appContext] for the fused-location client and [permissionRequester]
 * for the Activity-scoped runtime-permission prompt (which needs an Activity we can't reach
 * from shared code).
 */
object LocationController {
    @Volatile
    var appContext: Context? = null

    /** Requests the fine/coarse location permission, returning whether it was granted. */
    @Volatile
    var permissionRequester: (suspend () -> Boolean)? = null
}

actual class LocationProvider {
    actual suspend fun currentLocation(): LocationResult {
        val context = LocationController.appContext ?: return LocationResult.Unavailable

        if (!hasLocationPermission(context)) {
            val granted = LocationController.permissionRequester?.invoke() ?: false
            if (!granted) return LocationResult.PermissionDenied
        }

        return try {
            awaitCurrentLocation(context)
        } catch (_: SecurityException) {
            LocationResult.PermissionDenied
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        fun granted(permission: String) =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private suspend fun awaitCurrentLocation(context: Context): LocationResult =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build()
            val cancellation = CancellationTokenSource()
            cont.invokeOnCancellation { cancellation.cancel() }

            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    if (!cont.isActive) return@addOnSuccessListener
                    cont.resume(
                        if (location != null) {
                            LocationResult.Available(UserLocation(location.latitude, location.longitude))
                        } else {
                            LocationResult.Unavailable
                        }
                    )
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(LocationResult.Unavailable)
                }
        }
}
