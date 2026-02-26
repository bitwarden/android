package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.composition.LocalPermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The screen to manually add a totp code.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCodeEntryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScreen: () -> Unit,
    viewModel: ManualCodeEntryViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
) {
    var shouldShowPermissionDialog by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val launcher = permissionsManager.getLauncher { isGranted ->
        if (isGranted) {
            viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
        } else {
            shouldShowPermissionDialog = true
        }
    }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ManualCodeEntryEvent.NavigateToAppSettings -> {
                intentManager.startAppSettingsActivity()
            }

            is ManualCodeEntryEvent.NavigateToQrCodeScreen -> {
                onNavigateToQrCodeScreen.invoke()
            }

            is ManualCodeEntryEvent.NavigateBack -> {
                onNavigateBack.invoke()
            }
        }
    }

    if (shouldShowPermissionDialog) {
        BitwardenTwoButtonDialog(
            message = stringResource(
                id = BitwardenString.enable_camera_permission_to_use_the_scanner,
            ),
            confirmButtonText = stringResource(id = BitwardenString.settings),
            dismissButtonText = stringResource(id = BitwardenString.no_thanks),
            onConfirmClick = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.SettingsClick) }
            },
            onDismissClick = { shouldShowPermissionDialog = false },
            onDismissRequest = { shouldShowPermissionDialog = false },
            title = null,
        )
    }

    ManualCodeEntryDialogs(
        dialog = state.dialog,
        onDismissRequest = remember(state) {
            { viewModel.trySendAction(ManualCodeEntryAction.DismissDialog) }
        },
    )

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.create_verification_code),
                navigationIcon = painterResource(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ManualCodeEntryAction.CloseClick) }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
            )
        },
    ) {
        ManualCodeEntryContent(
            state = state,
            onNameChange = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.IssuerTextChange(it)) }
            },
            onKeyChange = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.CodeTextChange(it)) }
            },
            onSaveLocallyClick = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.SaveLocallyClick) }
            },
            onSaveToBitwardenClick = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.SaveToBitwardenClick) }
            },
            onScanQrCodeClick = remember(viewModel) {
                {
                    if (permissionsManager.checkPermission(Manifest.permission.CAMERA)) {
                        viewModel.trySendAction(ManualCodeEntryAction.ScanQrCodeTextClick)
                    } else {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ManualCodeEntryContent(
    state: ManualCodeEntryState,
    onNameChange: (name: String) -> Unit,
    onKeyChange: (key: String) -> Unit,
    onSaveLocallyClick: () -> Unit,
    onSaveToBitwardenClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = BitwardenString.enter_key_manually),
            style = BitwardenTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.name),
            value = state.issuer,
            onValueChange = onNameChange,
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .testTag(tag = "NameTextField")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenPasswordField(
            singleLine = false,
            label = stringResource(id = BitwardenString.key),
            value = state.code,
            onValueChange = onKeyChange,
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .testTag(tag = "KeyTextField")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        SaveManualCodeButtons(
            state = state.buttonState,
            onSaveLocallyClick = onSaveLocallyClick,
            onSaveToBitwardenClick = onSaveToBitwardenClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 24.dp))
        Text(
            text = stringResource(id = BitwardenString.cannot_add_authenticator_key),
            color = BitwardenTheme.colorScheme.text.secondary,
            style = BitwardenTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        BitwardenTextButton(
            label = stringResource(id = BitwardenString.scan_qr_code),
            onClick = onScanQrCodeClick,
            modifier = Modifier
                .wrapContentWidth()
                .align(alignment = Alignment.CenterHorizontally)
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ManualCodeEntryDialogs(
    dialog: ManualCodeEntryState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialog) {
        is ManualCodeEntryState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = dialog.title?.invoke(),
                message = dialog.message(),
                onDismissRequest = onDismissRequest,
            )
        }

        is ManualCodeEntryState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }
}
