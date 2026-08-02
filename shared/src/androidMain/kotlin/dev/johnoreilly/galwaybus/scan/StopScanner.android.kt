package dev.johnoreilly.galwaybus.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * Wiring for Android camera access. [MainActivity] populates [permissionRequester] at startup so the
 * shared scanner can trigger the Activity-scoped runtime-permission prompt (which needs an Activity
 * we can't reach from shared code). Mirrors `LocationController`.
 */
object CameraController {
    /** Requests the CAMERA permission, returning whether it was granted. */
    @Volatile
    var permissionRequester: (suspend () -> Boolean)? = null
}

actual val isStopScanSupported: Boolean = true

@Composable
actual fun CameraTextScanner(
    onText: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var denied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val granted = CameraController.permissionRequester?.invoke() ?: false
            hasPermission = granted
            denied = !granted
        }
    }

    when {
        hasPermission -> CameraPreviewWithAnalysis(onText, modifier)
        denied -> ScanMessage(
            title = "Camera permission needed",
            body = "Allow camera access to scan a stop's number.",
            modifier = modifier
        )
        // Still waiting for the permission dialog to be answered.
        else -> Box(modifier)
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun CameraPreviewWithAnalysis(
    onText: (String) -> Unit,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // Keep the analyzer calling the latest onText without rebinding the camera each recomposition.
    val currentOnText by rememberUpdatedState(onText)
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, TextAnalyzer(recognizer) { currentOnText(it) }) }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Runs ML Kit text recognition on each analysed frame. With STRATEGY_KEEP_ONLY_LATEST the proxy is
 * held (and newer frames dropped) until recognition completes, so only one request is ever in flight.
 */
private class TextAnalyzer(
    private val recognizer: TextRecognizer,
    private val onText: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { result -> if (result.text.isNotBlank()) onText(result.text) }
            .addOnCompleteListener { imageProxy.close() }
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
