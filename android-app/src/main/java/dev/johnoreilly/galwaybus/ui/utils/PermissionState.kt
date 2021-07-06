package dev.johnoreilly.galwaybus.ui.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull

class PermissionState(
        val permission: String,
        val hasPermission: StateFlow<Boolean>,
        private val launcher: ActivityResultLauncher<String>
) {
    fun launchPermissionRequest() = launcher.launch(permission)
}

@ExperimentalCoroutinesApi
private class PermissionResultCall(
    key: String,
    private val activity: ComponentActivity,
    private val permission: String
) {

    // defer this to allow construction before onCreate
    private val hasPermission =  MutableStateFlow<Boolean>(false)

    // Don't do this in onCreate because compose setContent may be called in Activity usage before
    // onCreate is dispatched to this lifecycle observer (as a result, need to manually unregister)
    private var call = activity.activityResultRegistry.register(
            "LocationPermissions#($key)",
            ActivityResultContracts.RequestPermission()
    ) { result ->
        onPermissionResult(result)
    }

    /**
     * Call this after [Activity.onCreate] to perform the initial permissions checks
     */
    fun initialCheck() {
        hasPermission.value = checkPermission()
    }

    fun unregister() {
        call.unregister()
    }

    fun checkSelfPermission(): PermissionState {
        return PermissionState(permission, hasPermission, call)
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionResult(result: Boolean) {
        hasPermission.value = result
    }
}


/**
 * Instantiate and manage it in composition like this
 */
@ExperimentalComposeApi
@Composable
fun checkSelfPermissionState(activity: ComponentActivity, permission: String): PermissionState {
    val key = "1" //currentComposer.currentCompoundKeyHash.toString()
    val call = remember(activity, permission) {
        PermissionResultCall(key, activity, permission)
    }

    DisposableEffect(call) {
        call.initialCheck()
        onDispose {
            call.unregister()
        }
    }

    return call.checkSelfPermission()
}
