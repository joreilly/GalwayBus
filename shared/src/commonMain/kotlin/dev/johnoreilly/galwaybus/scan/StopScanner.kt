package dev.johnoreilly.galwaybus.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Whether this platform can scan a stop plate through the camera. True on Android and iOS;
 * false on Desktop (JVM) and Web, which have no camera/OCR implementation — the UI hides the
 * scan entry point where this is false.
 */
expect val isStopScanSupported: Boolean

/**
 * A full-bleed live camera preview that runs on-device text recognition on each frame and reports
 * the recognised text via [onText]. Each platform provides the camera + OCR:
 *  - Android → CameraX preview + ML Kit text recognition
 *  - iOS → AVCaptureSession + Vision (VNRecognizeTextRequest)
 *  - Desktop/Web → a "not available" placeholder ([isStopScanSupported] is false there)
 *
 * Callers debounce/match the raw text themselves (see [StopMatcher]); this composable just streams
 * whatever the recogniser reads. It requests camera permission on first use where the platform
 * requires it.
 *
 * @param onText invoked (on the main thread) with each block of recognised text.
 */
@Composable
expect fun CameraTextScanner(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier
)
