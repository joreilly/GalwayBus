package dev.johnoreilly.galwaybus.ui.utils

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

@ExperimentalCoroutinesApi
class FusedLocationWrapper(private val fusedLocation: FusedLocationProviderClient) {

    @RequiresPermission(anyOf = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    ))
    fun lastLocation(): Flow<Location> = flow {
        emit(fusedLocation.lastLocation.await())
    }

    @RequiresPermission(anyOf = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    ))
    fun requestLocationUpdates(
            context: Context,
            request: LocationRequest
    ): Flow<List<Location>> = fusedLocation.locationFlow(request, context.mainLooper)

    @RequiresPermission(anyOf = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    ))
    private fun FusedLocationProviderClient.locationFlow(request: LocationRequest, looper: android.os.Looper) = callbackFlow<List<Location>> {
        // code based on ktx codelab: https://codelabs.developers.google.com/codelabs/building-kotlin-extensions-library
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                result ?: return
                try {
                    offer(result.locations) // pass the locations directly from the API without modification
                } catch (throwable: Throwable) {
                    // channel was closed (possibly by cause)
                }
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
fun AppCompatActivity.fusedLocationWrapper()
        = FusedLocationWrapper(LocationServices.getFusedLocationProviderClient(this))

