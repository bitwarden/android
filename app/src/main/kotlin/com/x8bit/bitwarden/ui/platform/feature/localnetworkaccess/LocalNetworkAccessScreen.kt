package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.composition.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.handlers.LocalNetworkAccessHandler
import com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess.handlers.rememberLocalNetworkAccessHandler
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager

@SuppressLint("InlinedApi")
private const val LOCAL_NETWORK_PERMISSION: String = Manifest.permission.ACCESS_LOCAL_NETWORK

/**
 * Top-level composable for the Local Network Access screen.
 */
@Composable
fun LocalNetworkAccessScreen(
    onDismiss: () -> Unit,
    viewModel: LocalNetworkAccessViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            LocalNetworkAccessEvent.NavigateBack -> onDismiss()
            LocalNetworkAccessEvent.NavigateToSettings -> intentManager.startAppSettingsActivity()
        }
    }
    val handler = rememberLocalNetworkAccessHandler(viewModel)
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                handler.onResumed(permissionsManager.checkPermission(LOCAL_NETWORK_PERMISSION))
            }

            else -> Unit
        }
    }
    BackHandler(onBack = handler.onCloseClick)

    BitwardenScaffold(
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    ) {
        LocalNetworkAccessContent(
            permissionsManager = permissionsManager,
            handler = handler,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun LocalNetworkAccessContent(
    permissionsManager: PermissionsManager,
    handler: LocalNetworkAccessHandler,
    modifier: Modifier = Modifier,
) {
    var shouldShowPermissionDialog by rememberSaveable { mutableStateOf(value = false) }
    val localNetworkAccessPermissionLauncher = permissionsManager.getLauncher { isGranted ->
        if (isGranted) {
            handler.onCloseClick()
        } else if (
            !permissionsManager.shouldShowRequestPermissionRationale(LOCAL_NETWORK_PERMISSION)
        ) {
            // "shouldShowRequestPermissionRationale" will only be 'true' after you have declined
            // the first OS prompt but have not seen the second prompt attempt. We do not want
            // to display the dialog after the first time we were declined, but we do after that.
            shouldShowPermissionDialog = true
        }
    }
    if (shouldShowPermissionDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.local_network_access_required),
            message = stringResource(
                id = BitwardenString
                    .without_this_permission_bitwarden_wont_be_able_to_sync_with_your_server,
            ),
            confirmButtonText = stringResource(id = BitwardenString.go_to_settings),
            dismissButtonText = stringResource(id = BitwardenString.no_thanks),
            onConfirmClick = {
                shouldShowPermissionDialog = false
                handler.onSettingsClick()
            },
            onDismissClick = { shouldShowPermissionDialog = false },
            onDismissRequest = { shouldShowPermissionDialog = false },
        )
    }
    Column(
        modifier = modifier.verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(height = 32.dp))

        Image(
            painter = rememberVectorPainter(id = BitwardenDrawable.ill_sso_cookie_sync),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(size = 100.dp)
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        Text(
            text = stringResource(id = BitwardenString.access_your_local_network),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        Text(
            text = stringResource(
                id = BitwardenString.bitwarden_needs_local_network_access_to_sync_with_your_server,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.enable_local_network_access),
            onClick = {
                localNetworkAccessPermissionLauncher.launch(input = LOCAL_NETWORK_PERMISSION)
            },
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.ask_again_later),
            onClick = handler.onContinueWithoutPermissionClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
