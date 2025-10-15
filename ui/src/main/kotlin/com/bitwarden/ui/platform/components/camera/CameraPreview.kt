package com.bitwarden.ui.platform.components.camera

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(value = null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageAnalyzer = remember(qrCodeAnalyzer) {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(Executors.newSingleThreadExecutor(), qrCodeAnalyzer) }
    }
    val preview = Preview.Builder()
        .build()
        .apply { surfaceProvider = previewView.surfaceProvider }

    // Unbind from the camera provider when we leave the screen.
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    // Set up the camera provider on a background thread. This is necessary because
    // ProcessCameraProvider.getInstance returns a ListenableFuture. For an example see
    // https://github.com/JetBrains/compose-multiplatform/blob/1c7154b975b79901f40f28278895183e476ed36d/examples/imageviewer/shared/src/androidMain/kotlin/example/imageviewer/view/CameraView.android.kt#L85
    LaunchedEffect(imageAnalyzer) {
        try {
            cameraProvider = suspendCoroutine { continuation ->
                ProcessCameraProvider.getInstance(context).also { future ->
                    future.addListener(
                        { continuation.resume(value = future.get()) },
                        ContextCompat.getMainExecutor(context),
                    )
                }
            }

            cameraProvider
                ?.let {
                    it.unbindAll()
                    if (it.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        it.bindToLifecycle(
                            lifecycleOwner = lifecycleOwner,
                            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                            useCases = arrayOf(preview, imageAnalyzer),
                        )
                    } else {
                        cameraErrorReceive(IllegalStateException("Missing Back Camera"))
                    }
                }
                ?: cameraErrorReceive(IllegalStateException("Missing Camera Provider"))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            cameraErrorReceive(e)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
