package dev.johnoreilly.galwaybus.ui.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resumeWithException

@ExperimentalCoroutinesApi
@SuppressLint("MissingPermission")
class FusedLocationWrapper(private val fusedLocation: FusedLocationProviderClient) {

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun lastLocation(): Flow<Location> = flow {
        fusedLocation.lastLocation.await()?.let { location ->
            emit(location)
        }
    }


    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    suspend fun awaitLastLocation(): Location =
        suspendCancellableCoroutine { continuation ->
            fusedLocation.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location, onCancellation = {})
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
        }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun requestLocationUpdates(
            context: Context,
            request: LocationRequest
    ): Flow<Location> = fusedLocation.locationFlow(request, context.mainLooper)

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun FusedLocationProviderClient.locationFlow(request: LocationRequest, looper: android.os.Looper) = callbackFlow<Location> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                try { trySend(result.lastLocation) } catch(e: Exception) {}
            }
        }

        requestLocationUpdates(
                request,
                callback,
                looper
        ).addOnFailureListener { e ->
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            removeLocationUpdates(callback) // clean up when Flow collection ends
        }
    }
}

@ExperimentalCoroutinesApi
fun ComponentActivity.fusedLocationWrapper()
        = FusedLocationWrapper(LocationServices.getFusedLocationProviderClient(this))

