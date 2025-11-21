package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.util.isPortrait
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.StatusBarsAppearanceAffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.camera.CameraPreview
import com.bitwarden.ui.platform.components.camera.QrCodeSquare
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.composition.LocalQrCodeAnalyzer
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.LocalBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme

/**
 * The screen to scan QR codes for the application.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: QrCodeScanViewModel = hiltViewModel(),
    qrCodeAnalyzer: QrCodeAnalyzer = LocalQrCodeAnalyzer.current,
    onNavigateToManualCodeEntryScreen: () -> Unit,
) {
    qrCodeAnalyzer.onQrCodeScanned = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(it)) }
    }
    val onEnterCodeManuallyClick = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.ManualEntryTextClick) }
    }
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is QrCodeScanEvent.NavigateBack -> {
                onNavigateBack.invoke()
            }

            is QrCodeScanEvent.NavigateToManualCodeEntry -> {
                onNavigateToManualCodeEntryScreen.invoke()
            }
        }
    }

    // This screen should always look like it's in dark mode
    CompositionLocalProvider(LocalBitwardenColorScheme provides darkBitwardenColorScheme) {
        StatusBarsAppearanceAffect(isLightStatusBars = false)
        QrCodeScanDialogs(
            dialogState = state.dialog,
            onSaveHereClick = remember(viewModel) {
                { viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(it)) }
            },
            onTakeMeToBitwardenClick = remember(viewModel) {
                { viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(it)) }
            },
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenErrorDismiss) }
            },
        )

        BitwardenScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                BitwardenTopAppBar(
                    title = stringResource(id = BitwardenString.scan_qr_code),
                    navigationIcon = painterResource(id = BitwardenDrawable.ic_close),
                    navigationIconContentDescription = stringResource(id = BitwardenString.close),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(QrCodeScanAction.CloseClick) }
                    },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                )
            },
        ) {
            CameraPreview(
                cameraErrorReceive = remember(viewModel) {
                    { viewModel.trySendAction(QrCodeScanAction.CameraSetupErrorReceive) }
                },
                qrCodeAnalyzer = qrCodeAnalyzer,
                modifier = Modifier.fillMaxSize(),
            )

            if (LocalConfiguration.current.isPortrait) {
                PortraitQRCodeContent(
                    onEnterCodeManuallyClick = onEnterCodeManuallyClick,
                )
            } else {
                LandscapeQRCodeContent(
                    onEnterCodeManuallyClick = onEnterCodeManuallyClick,
                )
            }
        }
    }
}

@Composable
private fun QrCodeScanDialogs(
    dialogState: QrCodeScanState.DialogState?,
    onSaveHereClick: (Boolean) -> Unit,
    onTakeMeToBitwardenClick: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        QrCodeScanState.DialogState.ChooseSaveLocation -> {
            ChooseSaveLocationDialog(
                onSaveHereClick = onSaveHereClick,
                onTakeMeToBitwardenClick = onTakeMeToBitwardenClick,
            )
        }

        QrCodeScanState.DialogState.SaveToBitwardenError -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.something_went_wrong),
                message = stringResource(id = BitwardenString.please_try_again),
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
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
                .standardHorizontalMargin()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = BitwardenString.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = BitwardenTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            BottomClickableText(
                onEnterCodeManuallyClick = onEnterCodeManuallyClick,
                modifier = Modifier.padding(horizontal = 16.dp),
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
                .standardHorizontalMargin()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = BitwardenString.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = BitwardenTheme.typography.bodySmall,
            )

            BottomClickableText(
                onEnterCodeManuallyClick = onEnterCodeManuallyClick,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun BottomClickableText(
    onEnterCodeManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enterKeyText = stringResource(id = BitwardenString.enter_key_manually)
    Text(
        text = annotatedStringResource(
            id = BitwardenString.cannot_scan_qr_code_enter_key_manually,
            linkHighlightStyle = spanStyleOf(
                color = BitwardenTheme.colorScheme.text.interaction,
                textStyle = BitwardenTheme.typography.bodyMedium,
            ),
            style = spanStyleOf(
                color = Color.White,
                textStyle = BitwardenTheme.typography.bodyMedium,
            ),
            onAnnotationClick = {
                when (it) {
                    "enterKeyManually" -> onEnterCodeManuallyClick()
                }
            },
        ),
        modifier = modifier.semantics {
            CustomAccessibilityAction(
                label = enterKeyText,
                action = {
                    onEnterCodeManuallyClick()
                    true
                },
            )
        },
    )
}
