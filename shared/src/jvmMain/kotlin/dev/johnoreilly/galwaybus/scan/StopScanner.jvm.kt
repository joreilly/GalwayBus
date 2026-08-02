package dev.johnoreilly.galwaybus.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Desktop has no camera/OCR pipeline, so stop scanning is unavailable and the tab is hidden. */
actual val isStopScanSupported: Boolean = false

@Composable
actual fun CameraTextScanner(
    onText: (String) -> Unit,
    modifier: Modifier
) {
    // Never shown: the scan tab is gated on isStopScanSupported.
    Box(modifier)
}
