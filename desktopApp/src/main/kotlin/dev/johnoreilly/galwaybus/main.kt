package dev.johnoreilly.galwaybus

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.util.prefs.Preferences

private val windowPrefs: Preferences =
    Preferences.userRoot().node("dev/johnoreilly/galwaybus/window")

fun main() = application {
    val state = rememberWindowState(
        width = windowPrefs.getInt("width", 1000).dp,
        height = windowPrefs.getInt("height", 720).dp,
        position = windowPrefs.getInt("x", Int.MIN_VALUE).let { x ->
            if (x == Int.MIN_VALUE) WindowPosition.PlatformDefault
            else WindowPosition(x.dp, windowPrefs.getInt("y", 0).dp)
        }
    )

    Window(
        onCloseRequest = {
            windowPrefs.putInt("width", state.size.width.value.toInt())
            windowPrefs.putInt("height", state.size.height.value.toInt())
            (state.position as? WindowPosition.Absolute)?.let {
                windowPrefs.putInt("x", it.x.value.toInt())
                windowPrefs.putInt("y", it.y.value.toInt())
            }
            exitApplication()
        },
        title = "GalwayBus",
        state = state,
    ) {
        // Below this the two-pane layout degenerates; keep the window usable.
        LaunchedEffect(Unit) { window.minimumSize = Dimension(480, 480) }
        App()
    }
}
