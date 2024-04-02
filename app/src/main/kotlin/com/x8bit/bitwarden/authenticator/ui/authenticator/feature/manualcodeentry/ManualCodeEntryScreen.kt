package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.authenticator.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.authenticator.ui.platform.theme.LocalIntentManager
import com.x8bit.bitwarden.authenticator.ui.platform.theme.LocalPermissionsManager

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
                intent.data = Uri.parse("package:" + context.packageName)

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
            message = stringResource(id = R.string.enable_camer_permission_to_use_the_scanner),
            confirmButtonText = stringResource(id = R.string.settings),
            dismissButtonText = stringResource(id = R.string.no_thanks),
            onConfirmClick = remember(viewModel) {
                { viewModel.trySendAction(ManualCodeEntryAction.SettingsClick) }
            },
            onDismissClick = { shouldShowPermissionDialog = false },
            onDismissRequest = { shouldShowPermissionDialog = false },
            title = null,
        )
    }

    BitwardenScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.authenticator_key_scanner),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ManualCodeEntryAction.CloseClick) }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            Text(
                text = stringResource(id = R.string.enter_key_manually),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                singleLine = false,
                label = stringResource(id = R.string.authenticator_key_scanner),
                value = state.code,
                onValueChange = remember(viewModel) {
                    {
                        viewModel.trySendAction(
                            ManualCodeEntryAction.CodeTextChange(it),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            BitwardenFilledTonalButton(
                label = stringResource(id = R.string.add_totp),
                onClick = remember(viewModel) {
                    { viewModel.trySendAction(ManualCodeEntryAction.CodeSubmit) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Text(
                text = stringResource(id = R.string.once_the_key_is_successfully_entered),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 16.dp,
                        horizontal = 16.dp,
                    ),
            )

            Text(
                text = stringResource(id = R.string.cannot_add_authenticator_key),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp,
                        horizontal = 16.dp,
                    ),
            )

            ClickableText(
                text = stringResource(id = R.string.scan_qr_code).toAnnotatedString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                onClick = remember(viewModel) {
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
}
