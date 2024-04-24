package com.bitwarden.authenticator.ui.platform.feature.splash

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.EventsEffect
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.components.dialog.BasicDialogState
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.authenticator.ui.platform.components.dialog.LoadingDialogState
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.theme.LocalBiometricsManager

/**
 * Splash screen with empty composable content so that the Activity window background is shown.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
    onNavigateToAuthenticator: () -> Unit,
    onExitApplication: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel) { event ->
        when (event) {
            is SplashEvent.NavigateToAuthenticator -> onNavigateToAuthenticator()

            is SplashEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            is SplashEvent.ExitApplication -> onExitApplication()
        }
    }

    when (val dialog = state.dialog) {
        is SplashState.Dialog.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = R.string.an_error_has_occurred.asText(),
                    message = dialog.message
                ),
                onDismissRequest = remember(viewModel) {
                    {
                        viewModel.trySendAction(SplashAction.DismissDialog)
                    }
                },
            )
        }

        SplashState.Dialog.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(
                    text = R.string.loading.asText()
                ),
            )
        }

        null -> Unit
    }

    Box(modifier = Modifier.fillMaxSize())

    when (val viewState = state.viewState) {
        is SplashState.ViewState.Locked -> {
            if (viewState.showBiometricsPrompt) {
                biometricsManager.promptBiometrics(
                    onSuccess = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                SplashAction.Internal.UnlockResultReceived(UnlockResult.Success)
                            )
                        }
                    },
                    onCancel = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                SplashAction.Internal.UnlockResultReceived(UnlockResult.Cancel)
                            )
                        }
                    },
                    onError = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                SplashAction.Internal.UnlockResultReceived(UnlockResult.Error),
                            )
                        }
                    },
                    onLockOut = remember(viewModel) {
                        {
                            viewModel.trySendAction(
                                SplashAction.Internal.UnlockResultReceived(UnlockResult.LockOut),
                            )
                        }
                    },
                )
            } else {
                viewModel.trySendAction(
                    SplashAction.Internal.UnlockResultReceived(UnlockResult.Success)
                )
            }
        }

        SplashState.ViewState.Unlocked -> Unit
    }
}
