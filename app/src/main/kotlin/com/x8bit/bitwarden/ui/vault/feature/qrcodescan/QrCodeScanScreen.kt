package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.StatusBarsAppearanceAffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.camera.CameraPreview
import com.bitwarden.ui.platform.components.camera.QrCodeSquare
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalQrCodeAnalyzer
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import com.bitwarden.ui.platform.model.WindowSize
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.platform.theme.LocalBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme
import com.bitwarden.ui.platform.util.rememberWindowSize

/**
 * The screen to scan QR codes for the application.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    viewModel: QrCodeScanViewModel = hiltViewModel(),
    qrCodeAnalyzer: QrCodeAnalyzer = LocalQrCodeAnalyzer.current,
) {
    qrCodeAnalyzer.onQrCodeScanned = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.QrCodeScanReceive(it)) }
    }

    val onEnterKeyManuallyClick = remember(viewModel) {
        { viewModel.trySendAction(QrCodeScanAction.ManualEntryTextClick) }
    }

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
        BitwardenScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                BitwardenTopAppBar(
                    title = stringResource(id = BitwardenString.scan_qr_code),
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                    navigationIconContentDescription = stringResource(id = BitwardenString.close),
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(QrCodeScanAction.CloseClick) }
                    },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                        state = rememberTopAppBarState(),
                    ),
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
            when (rememberWindowSize()) {
                WindowSize.Compact -> {
                    QrCodeContentCompact(
                        onEnterKeyManuallyClick = onEnterKeyManuallyClick,
                    )
                }

                WindowSize.Medium -> {
                    QrCodeContentMedium(
                        onEnterKeyManuallyClick = onEnterKeyManuallyClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCodeContentCompact(
    onEnterKeyManuallyClick: () -> Unit,
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
                .background(color = BitwardenTheme.colorScheme.background.scrim)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = BitwardenString.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = BitwardenTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            EnterKeyManuallyText(
                onEnterKeyManuallyClick = onEnterKeyManuallyClick,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun QrCodeContentMedium(
    onEnterKeyManuallyClick: () -> Unit,
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
                .background(color = BitwardenTheme.colorScheme.background.scrim)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(id = BitwardenString.point_your_camera_at_the_qr_code),
                textAlign = TextAlign.Center,
                color = Color.White,
                style = BitwardenTheme.typography.bodySmall,
            )

            EnterKeyManuallyText(
                onEnterKeyManuallyClick = onEnterKeyManuallyClick,
            )
        }
    }
}

@Composable
private fun EnterKeyManuallyText(
    onEnterKeyManuallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enterKeyManuallyString = stringResource(BitwardenString.enter_key_manually)
    Text(
        text = annotatedStringResource(
            id = BitwardenString.cannot_scan_qr_code_enter_key_manually,
            onAnnotationClick = {
                when (it) {
                    "enterKeyManually" -> onEnterKeyManuallyClick()
                }
            },
        ),
        style = BitwardenTheme.typography.bodySmall,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = enterKeyManuallyString,
                        action = {
                            onEnterKeyManuallyClick()
                            true
                        },
                    ),
                )
            },
    )
}
