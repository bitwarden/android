package com.bitwarden.authenticator.ui.auth.unlock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.platform.composition.LocalBiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

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
    var showBiometricsPrompt by remember { mutableStateOf(true) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            UnlockEvent.NavigateToItemListing -> onUnlocked()
        }
    }

    when (val dialog = state.dialog) {
        is UnlockState.Dialog.Error -> BitwardenBasicDialog(
            title = stringResource(id = BitwardenString.an_error_has_occurred),
            message = dialog.message(),
            onDismissRequest = remember(viewModel) {
                {
                    viewModel.trySendAction(UnlockAction.DismissDialog)
                }
            },
        )

        UnlockState.Dialog.Loading -> BitwardenLoadingDialog(
            text = stringResource(id = BitwardenString.loading),
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(220.dp)
                    .height(74.dp)
                    .fillMaxWidth(),
                painter = rememberVectorPainter(id = BitwardenDrawable.logo_authenticator),
                contentDescription = stringResource(BitwardenString.bitwarden_authenticator),
            )
            Spacer(modifier = Modifier.height(32.dp))
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.unlock),
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

            Spacer(modifier = Modifier.height(height = 12.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
