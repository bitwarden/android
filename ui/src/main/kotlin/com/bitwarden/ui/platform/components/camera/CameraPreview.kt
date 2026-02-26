package com.bitwarden.ui.platform.components.camera

import android.content.Context
import android.os.Build
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executors

/**
 * A composable for displaying the camera preview.
 *
 * @param qrCodeAnalyzer The [QrCodeAnalyzer].
 * @param cameraErrorReceive A callback invoked when an error occurs.
 * @param modifier The [Modifier] for this composable.
 * @param context The local context.
 * @param lifecycleOwner The current lifecycle owner.
 */
@Composable
fun CameraPreview(
    qrCodeAnalyzer: QrCodeAnalyzer,
    cameraErrorReceive: (Exception) -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    // We simply do not draw anything when running in tests to avoid flaky HardwareRenderer issues
    if ("robolectric" == Build.FINGERPRINT) return
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }
    val preview = rememberPreview { surfaceRequests.value = it }
    val imageAnalyzer = rememberImageAnalyzer(qrCodeAnalyzer = qrCodeAnalyzer)
    LaunchedEffect(Unit) {
        try {
            val provider = ProcessCameraProvider.awaitInstance(context = context)
            provider.unbindAll()
            if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                provider.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                    useCases = arrayOf(preview, imageAnalyzer),
                )
            } else {
                cameraErrorReceive(IllegalStateException("Missing Back Camera"))
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            cameraErrorReceive(e)
        }
    }
    val surfaceRequest by surfaceRequests.collectAsState()
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = modifier,
        )
    }
}

@Composable
private fun rememberImageAnalyzer(
    qrCodeAnalyzer: QrCodeAnalyzer,
): ImageAnalysis = remember(qrCodeAnalyzer) {
    ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(Executors.newSingleThreadExecutor(), qrCodeAnalyzer) }
}

@Composable
private fun rememberPreview(
    surfaceCallback: (SurfaceRequest) -> Unit,
): Preview = remember {
    Preview.Builder().build().apply {
        setSurfaceProvider { request -> surfaceCallback(request) }
    }
}
