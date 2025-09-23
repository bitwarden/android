package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorTopAppBar
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.authenticator.ui.platform.components.field.BitwardenTextField
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.composition.LocalPermissionsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

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

    val context = LocalContext.current

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ManualCodeEntryEvent.NavigateToAppSettings -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = "package:${context.packageName}".toUri()

                intentManager.startActivity(intent = intent)
            }

            is ManualCodeEntryEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message.invoke(context.resources), Toast.LENGTH_SHORT)
                    .show()
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
            AuthenticatorTopAppBar(
                title = stringResource(id = BitwardenString.create_verification_code),
                navigationIcon = painterResource(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ManualCodeEntryAction.CloseClick) }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
            )
        },
    ) { paddingValues ->
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
            modifier = Modifier.padding(paddingValues = paddingValues),
        )
    }
}

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
        Text(
            text = stringResource(id = BitwardenString.enter_key_manually),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenTextField(
            label = stringResource(id = BitwardenString.name),
            value = state.issuer,
            onValueChange = onNameChange,
            modifier = Modifier
                .testTag(tag = "NameTextField")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenPasswordField(
            singleLine = false,
            label = stringResource(id = BitwardenString.key),
            value = state.code,
            onValueChange = onKeyChange,
            capitalization = KeyboardCapitalization.Characters,
            modifier = Modifier
                .testTag(tag = "KeyTextField")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        SaveManualCodeButtons(
            state = state.buttonState,
            onSaveLocallyClick = onSaveLocallyClick,
            onSaveToBitwardenClick = onSaveToBitwardenClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        Text(
            text = stringResource(id = BitwardenString.cannot_add_authenticator_key),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        ScanQrCodeText(
            onClick = onScanQrCodeClick,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ScanQrCodeText(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilityString = stringResource(id = BitwardenString.scan_qr_code)
    Text(
        text = annotatedStringResource(
            id = BitwardenString.scan_qr_code,
            emphasisHighlightStyle = spanStyleOf(
                color = MaterialTheme.colorScheme.primary,
                textStyle = MaterialTheme.typography.bodyMedium,
            ),
            onAnnotationClick = {
                when (it) {
                    "scanQrCode" -> onClick()
                }
            },
        ),
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(
                    label = accessibilityString,
                    action = {
                        onClick()
                        true
                    },
                ),
            )
        },
    )
}

@Composable
private fun ManualCodeEntryDialogs(
    dialog: ManualCodeEntryState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (val dialogString = dialog) {
        is ManualCodeEntryState.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogString.title,
                    message = dialogString.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        is ManualCodeEntryState.DialogState.Loading -> {
            BitwardenLoadingDialog(visibilityState = LoadingDialogState.Shown(dialog.message))
        }

        null -> Unit
    }
}
