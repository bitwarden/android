package com.bitwarden.authenticator.ui.auth.unlock

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.theme.LocalBiometricsManager

/**
 * Top level composable for the unlock screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onUnlocked: () -> Unit,
) {

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources
    var showBiometricsPrompt by remember { mutableStateOf(true) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is UnlockEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            UnlockEvent.NavigateToItemListing -> onUnlocked()
        }
    }

    when (val dialog = state.dialog) {
        is UnlockState.Dialog.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = dialog.message,
            ),
            onDismissRequest = remember(viewModel) {
                {
                    viewModel.trySendAction(UnlockAction.DismissDialog)
                }
            },
        )

        UnlockState.Dialog.Loading -> BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(R.string.loading.asText()),
        )

        null -> Unit
    }

    val onBiometricsUnlock: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(UnlockAction.BiometricsUnlock) }
    }
    val onBiometricsLockOut: () -> Unit = remember(viewModel) {
        { viewModel.trySendAction(UnlockAction.BiometricsLockout) }
    }

    if (showBiometricsPrompt) {
        biometricsManager.promptBiometrics(
            onSuccess = {
                showBiometricsPrompt = false
                onBiometricsUnlock()
            },
            onCancel = {
                showBiometricsPrompt = false
            },
            onError = {
                showBiometricsPrompt = false
            },
            onLockOut = {
                showBiometricsPrompt = false
                onBiometricsLockOut()
            },
        )
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize(),
    ) { innerPadding ->
        Box {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(220.dp)
                        .height(74.dp)
                        .fillMaxWidth(),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    painter = painterResource(id = R.drawable.ic_logo_horizontal),
                    contentDescription = stringResource(R.string.bitwarden_authenticator),
                )
                Spacer(modifier = Modifier.height(32.dp))
                BitwardenFilledTonalButton(
                    label = stringResource(id = R.string.use_biometrics_to_unlock),
                    onClick = {
                        biometricsManager.promptBiometrics(
                            onSuccess = onBiometricsUnlock,
                            onCancel = {
                                // no-op
                            },
                            onError = {
                                // no-op
                            },
                            onLockOut = onBiometricsLockOut,
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
