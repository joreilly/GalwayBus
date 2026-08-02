package dev.johnoreilly.galwaybus

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.johnoreilly.galwaybus.location.LocationController
import dev.johnoreilly.galwaybus.scan.CameraController
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {

    // Bridges the one-shot permission dialog (an Activity concern) to the shared LocationProvider's
    // suspend request. Only one request is ever in flight at a time.
    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            pendingPermission?.complete(result.values.any { it })
            pendingPermission = null
        }

    // Same bridge for the CAMERA permission requested by the shared stop scanner.
    private var pendingCameraPermission: CompletableDeferred<Boolean>? = null
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingCameraPermission?.complete(granted)
            pendingCameraPermission = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        GalwayBusPrefs.appContext = applicationContext

        LocationController.appContext = applicationContext
        LocationController.permissionRequester = {
            val deferred = CompletableDeferred<Boolean>()
            pendingPermission = deferred
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            deferred.await()
        }

        CameraController.permissionRequester = {
            val deferred = CompletableDeferred<Boolean>()
            pendingCameraPermission = deferred
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            deferred.await()
        }

        setContent { App() }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
