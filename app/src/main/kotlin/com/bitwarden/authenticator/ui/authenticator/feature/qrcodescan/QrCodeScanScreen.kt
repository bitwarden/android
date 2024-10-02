package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import android.content.res.Configuration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.util.QrCodeAnalyzer
import com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan.util.QrCodeAnalyzerImpl
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.theme.LocalNonMaterialColors
import com.bitwarden.authenticator.ui.platform.theme.clickableSpanStyle
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The screen to scan QR codes for the application.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: QrCodeScanViewModel = hiltViewModel(),
    qrCodeAnalyzer: QrCodeAnalyzer = QrCodeAnalyzerImpl(),
    onNavigateToManualCodeEntryScreen: () -> Unit,
) {
    qrCodeAnalyzer.onQrCodeScanned = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(it)) }
    }

    val orientation = LocalConfiguration.current.orientation

    val context = LocalContext.current

    val onEnterCodeManuallyClick = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.ManualEntryTextClick) }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is QrCodeScanEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message.invoke(context.resources), Toast.LENGTH_SHORT)
                    .show()
            }

            is QrCodeScanEvent.NavigateBack -> {
                onNavigateBack.invoke()
            }

            is QrCodeScanEvent.NavigateToManualCodeEntry -> {
                onNavigateToManualCodeEntryScreen.invoke()
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.scan_qr_code),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(QrCodeScanAction.CloseClick) }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
            )
        },
    ) { innerPadding ->
        CameraPreview(
            cameraErrorReceive = remember(viewModel) {
                { viewModel.trySendAction(QrCodeScanAction.CameraSetupErrorReceive) }
            },
            qrCodeAnalyzer = qrCodeAnalyzer,
            modifier = Modifier.padding(innerPadding),
        )

        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LandscapeQRCodeContent(
                    onEnterCodeManuallyClick = onEnterCodeManuallyClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                PortraitQRCodeContent(
                    onEnterCodeManuallyClick = onEnterCodeManuallyClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun PortraitQRCodeContent(
    onEnterCodeManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        QrCodeSquare(
            squareOutlineSize = 250.dp,
            modifier = Modifier.weight(2f),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = .4f))
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = R.string.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            BottomClickableText(
                onEnterCodeManuallyClick = onEnterCodeManuallyClick,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LandscapeQRCodeContent(
    onEnterCodeManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        QrCodeSquare(
            squareOutlineSize = 200.dp,
            modifier = Modifier.weight(2f),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = .4f))
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = R.string.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )

            BottomClickableText(
                onEnterCodeManuallyClick = onEnterCodeManuallyClick,
            )
        }
    }
}

@Suppress("LongMethod", "TooGenericExceptionCaught")
@Composable
private fun CameraPreview(
    cameraErrorReceive: () -> Unit,
    qrCodeAnalyzer: QrCodeAnalyzer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT,
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val imageAnalyzer = remember(qrCodeAnalyzer) {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    qrCodeAnalyzer,
                )
            }
    }

    val preview = Preview.Builder()
        .build()
        .apply { setSurfaceProvider(previewView.surfaceProvider) }

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
                        { continuation.resume(future.get()) },
                        Executors.newSingleThreadExecutor(),
                    )
                }
            }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer,
            )
        } catch (e: Exception) {
            cameraErrorReceive()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

/**
 * UI for the blue QR code square that is drawn onto the screen.
 */
@Suppress("MagicNumber", "LongMethod")
@Composable
private fun QrCodeSquare(
    modifier: Modifier = Modifier,
    squareOutlineSize: Dp,
) {
    val color = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Canvas(
            modifier = Modifier
                .size(squareOutlineSize)
                .padding(8.dp),
        ) {
            val strokeWidth = 3.dp.toPx()

            val squareSize = size.width
            val strokeOffset = strokeWidth / 2
            val sideLength = (1f / 6) * squareSize

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.apply {
                    // Draw upper top left.
                    drawLine(
                        color = color,
                        start = Offset(0f, strokeOffset),
                        end = Offset(sideLength, strokeOffset),
                        strokeWidth = strokeWidth,
                    )

                    // Draw lower top left.
                    drawLine(
                        color = color,
                        start = Offset(strokeOffset, strokeOffset),
                        end = Offset(strokeOffset, sideLength),
                        strokeWidth = strokeWidth,
                    )

                    // Draw upper top right.
                    drawLine(
                        color = color,
                        start = Offset(squareSize - sideLength, strokeOffset),
                        end = Offset(squareSize - strokeOffset, strokeOffset),
                        strokeWidth = strokeWidth,
                    )

                    // Draw lower top right.
                    drawLine(
                        color = color,
                        start = Offset(squareSize - strokeOffset, 0f),
                        end = Offset(squareSize - strokeOffset, sideLength),
                        strokeWidth = strokeWidth,
                    )

                    // Draw upper bottom right.
                    drawLine(
                        color = color,
                        start = Offset(squareSize - strokeOffset, squareSize),
                        end = Offset(squareSize - strokeOffset, squareSize - sideLength),
                        strokeWidth = strokeWidth,
                    )

                    // Draw lower bottom right.
                    drawLine(
                        color = color,
                        start = Offset(squareSize - strokeOffset, squareSize - strokeOffset),
                        end = Offset(squareSize - sideLength, squareSize - strokeOffset),
                        strokeWidth = strokeWidth,
                    )

                    // Draw upper bottom left.
                    drawLine(
                        color = color,
                        start = Offset(strokeOffset, squareSize),
                        end = Offset(strokeOffset, squareSize - sideLength),
                        strokeWidth = strokeWidth,
                    )

                    // Draw lower bottom left.
                    drawLine(
                        color = color,
                        start = Offset(0f, squareSize - strokeOffset),
                        end = Offset(sideLength, squareSize - strokeOffset),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomClickableText(
    onEnterCodeManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cannotScanText = stringResource(id = R.string.cannot_scan_qr_code)
    val enterKeyText = stringResource(id = R.string.enter_key_manually)
    val clickableStyle = clickableSpanStyle()
    val manualTextColor = LocalNonMaterialColors.current.qrCodeClickableText

    val customTitleLineBreak = LineBreak(
        strategy = LineBreak.Strategy.Balanced,
        strictness = LineBreak.Strictness.Strict,
        wordBreak = LineBreak.WordBreak.Phrase,
    )

    val annotatedString = remember {
        buildAnnotatedString {
            withStyle(style = clickableStyle.copy(color = Color.White)) {
                pushStringAnnotation(
                    tag = cannotScanText,
                    annotation = cannotScanText,
                )
                append(cannotScanText)
            }

            append("  ")

            withStyle(style = clickableStyle.copy(color = manualTextColor)) {
                pushStringAnnotation(tag = enterKeyText, annotation = enterKeyText)
                append(enterKeyText)
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            lineBreak = customTitleLineBreak,
        ),
        onClick = { offset ->
            annotatedString
                .getStringAnnotations(
                    tag = enterKeyText,
                    start = offset,
                    end = offset,
                )
                .firstOrNull()
                ?.let { onEnterCodeManuallyClick.invoke() }
        },
        modifier = modifier
            .semantics {
                CustomAccessibilityAction(
                    label = enterKeyText,
                    action = {
                        onEnterCodeManuallyClick.invoke()
                        true
                    },
                )
            },
    )
}
