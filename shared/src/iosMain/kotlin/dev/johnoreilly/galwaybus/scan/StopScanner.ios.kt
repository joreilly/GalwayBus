package dev.johnoreilly.galwaybus.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.readValue
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.UIKit.UIView
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelFast
import platform.darwin.NSObject
import platform.darwin.NSUInteger
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

actual val isStopScanSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraTextScanner(
    onText: (String) -> Unit,
    modifier: Modifier
) {
    var authorized by remember { mutableStateOf(cameraAuthorized()) }
    var denied by remember { mutableStateOf(cameraDenied()) }

    LaunchedEffect(Unit) {
        if (!authorized && !denied) {
            val granted = requestCameraAccess()
            authorized = granted
            denied = !granted
        }
    }

    when {
        authorized -> CameraPreview(onText, modifier)
        denied -> ScanMessage(
            title = "Camera permission needed",
            body = "Allow camera access to scan a stop's number.",
            modifier = modifier
        )
        // Still waiting for the permission dialog to be answered.
        else -> Box(modifier)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun CameraPreview(onText: (String) -> Unit, modifier: Modifier) {
    // Keep the delegate calling the latest onText without recreating the session.
    val currentOnText by rememberUpdatedState(onText)
    val controller = remember { ScannerController { currentOnText(it) } }

    DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    UIKitView(
        factory = { controller.createView() },
        modifier = modifier.fillMaxSize()
    )
}

private fun cameraAuthorized(): Boolean =
    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized

private fun cameraDenied(): Boolean {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return status == AVAuthorizationStatusDenied || status == AVAuthorizationStatusRestricted
}

private suspend fun requestCameraAccess(): Boolean = suspendCancellableCoroutine { cont ->
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        if (cont.isActive) cont.resume(granted)
    }
}

/**
 * A [UIView] that stretches its [AVCaptureVideoPreviewLayer] to fill its bounds on every layout
 * pass (the preview layer isn't autoresized by UIKit).
 */
@OptIn(ExperimentalForeignApi::class)
private class ScannerUIView : UIView(frame = CGRectZero.readValue()) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.setFrame(bounds)
    }
}

/**
 * Owns the AVCaptureSession that feeds camera frames to Vision text recognition. Retained across
 * recompositions (via `remember`) so the delegate — which the capture output holds only weakly —
 * outlives each frame. [stop] tears the session down when the scan screen leaves composition.
 */
@OptIn(ExperimentalForeignApi::class)
private class ScannerController(onText: (String) -> Unit) {
    private val session = AVCaptureSession()
    private val queue = dispatch_queue_create("dev.johnoreilly.galwaybus.scanner", null)
    private val delegate = SampleDelegate(onText)
    private var configured = false

    fun createView(): UIView {
        configureIfNeeded()
        val view = ScannerUIView()
        val layer = AVCaptureVideoPreviewLayer(session = session).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
            @Suppress("DEPRECATION")
            connection?.let { conn ->
                if (conn.isVideoOrientationSupported()) conn.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }
        view.previewLayer = layer
        view.layer.addSublayer(layer)
        dispatch_async(queue) { session.startRunning() }
        return view
    }

    private fun configureIfNeeded() {
        if (configured) return
        configured = true
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetHigh

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device != null) {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
            if (input != null && session.canAddInput(input)) session.addInput(input)
        }

        val output = AVCaptureVideoDataOutput().apply { alwaysDiscardsLateVideoFrames = true }
        output.setSampleBufferDelegate(delegate, queue)
        if (session.canAddOutput(output)) session.addOutput(output)

        session.commitConfiguration()
    }

    fun stop() {
        dispatch_async(queue) { session.stopRunning() }
    }
}

/**
 * Capture-output delegate that runs one Vision text-recognition pass per delivered frame. Frames
 * arriving while a pass is in flight are dropped by the serial queue + `alwaysDiscardsLateVideoFrames`.
 */
@OptIn(ExperimentalForeignApi::class)
private class SampleDelegate(
    private val onText: (String) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private val request = VNRecognizeTextRequest(completionHandler = { req, error ->
        if (error == null) {
            val text = buildString {
                req?.results?.forEach { observation ->
                    (observation as? VNRecognizedTextObservation)
                        ?.topCandidates(1.convert<NSUInteger>())
                        ?.firstOrNull()
                        ?.let { (it as VNRecognizedText).string }
                        ?.let { append(it).append('\n') }
                }
            }
            if (text.isNotBlank()) dispatch_async(dispatch_get_main_queue()) { onText(text) }
        }
    }).apply {
        recognitionLevel = VNRequestTextRecognitionLevelFast
        usesLanguageCorrection = false
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        val handler = VNImageRequestHandler(
            cVPixelBuffer = pixelBuffer,
            orientation = kCGImagePropertyOrientationRight,
            options = emptyMap<Any?, Any?>()
        )
        handler.performRequests(listOf(request), null)
    }
}

@Composable
private fun ScanMessage(title: String, body: String, modifier: Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
