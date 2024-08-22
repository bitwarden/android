package com.x8bit.bitwarden.ui.autofill.fido2

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.autofill.fido2.handlers.Fido2DialogHandlers
import com.x8bit.bitwarden.ui.autofill.fido2.handlers.Fido2UserVerificationHandlers
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenPinDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.composition.LocalBiometricsManager
import com.x8bit.bitwarden.ui.platform.composition.LocalFido2CompletionManager
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager

/**
 * FIDO 2 request handling screen.
 */
@Suppress("LongMethod")
@Composable
fun Fido2Screen(
    viewModel: Fido2ViewModel = hiltViewModel(),
    fido2CompletionManager: Fido2CompletionManager = LocalFido2CompletionManager.current,
    biometricsManager: BiometricsManager = LocalBiometricsManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val userVerificationHandlers = remember(viewModel) {
        Fido2UserVerificationHandlers.create(viewModel = viewModel)
    }
    val dialogHandlers = remember(viewModel) {
        Fido2DialogHandlers.create(viewModel = viewModel)
    }

    EventsEffect(viewModel = viewModel) { event ->
        Log.d("PASSKEY", "Fido2Screen: event received with: event = $event")
        when (event) {
            is Fido2Event.CompleteFido2GetCredentialsRequest -> {
                fido2CompletionManager.completeFido2GetCredentialRequest(event.result)
            }

            is Fido2Event.Fido2UserVerification -> {
                biometricsManager.promptUserVerification(
                    onSuccess = {
                        userVerificationHandlers
                            .onUserVerificationSuccess(
                                event.selectedCipher,
                            )
                    },
                    onCancel = userVerificationHandlers.onUserVerificationCancelled,
                    onLockOut = userVerificationHandlers.onUserVerificationLockOut,
                    onError = userVerificationHandlers.onUserVerificationFail,
                    onNotSupported = {
                        userVerificationHandlers
                            .onUserVerificationNotSupported(
                                event.selectedCipher.id,
                            )
                    },
                )
            }

            is Fido2Event.CompleteFido2Assertion -> {
                fido2CompletionManager.completeFido2Assertion(event.result)
            }
        }
    }

    Fido2Dialogs(
        dialogState = state.dialog,
        onDismissDialogClick = dialogHandlers.onDismissDialogClick,
        onDismissUserVerification = dialogHandlers.onDismissUserVerification,
        onSubmitMasterPasswordFido2Verification = { password, selectedCipherId ->
            dialogHandlers.onSubmitMasterPasswordFido2Verification(password, selectedCipherId)
        },
        onSubmitPinSetUpFido2Verification = { pin, selectedCipherId ->
            dialogHandlers.onSubmitPinSetUpFido2Verification(pin, selectedCipherId)
        },
        onSubmitPinFido2Verification = { pin, selectedCipherId ->
            dialogHandlers.onSubmitPinFido2Verification(pin, selectedCipherId)
        },
        onRetryFido2PasswordVerification = { selectedCipherId ->
            dialogHandlers.onRetryFido2PasswordVerification(selectedCipherId)
        },
        onRetryFido2PinVerification = { selectedCipherId ->
            dialogHandlers.onRetryFido2PinVerification(selectedCipherId)
        },
        onRetryPinSetUpFido2Verification = { selectedCipherId ->
            dialogHandlers.onRetryPinSetUpFido2Verification(selectedCipherId)
        },
    )
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        content = { },
    )
}

@Suppress("LongMethod")
@Composable
private fun Fido2Dialogs(
    dialogState: Fido2State.DialogState?,
    onDismissDialogClick: () -> Unit,
    onDismissUserVerification: () -> Unit,
    onSubmitMasterPasswordFido2Verification: (password: String, selectedCipherId: String) -> Unit,
    onSubmitPinFido2Verification: (pin: String, selectedCipherId: String) -> Unit,
    onSubmitPinSetUpFido2Verification: (pin: String, selectedCipherId: String) -> Unit,
    onRetryFido2PasswordVerification: (selectedCipherId: String) -> Unit,
    onRetryFido2PinVerification: (selectedCipherId: String) -> Unit,
    onRetryPinSetUpFido2Verification: (selectedCipherId: String) -> Unit,
) {
    when (dialogState) {
        Fido2State.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(R.string.loading.asText()),
            )
        }

        is Fido2State.DialogState.Error -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(dialogState.title, dialogState.message),
                onDismissRequest = onDismissDialogClick,
            )
        }

        is Fido2State.DialogState.Fido2MasterPasswordPrompt -> {
            BitwardenMasterPasswordDialog(
                onConfirmClick = { password ->
                    onSubmitMasterPasswordFido2Verification(
                        password,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissUserVerification,
            )
        }

        is Fido2State.DialogState.Fido2PinPrompt -> {
            BitwardenPinDialog(
                onConfirmClick = { pin ->
                    onSubmitPinFido2Verification(
                        pin,
                        dialogState.selectedCipherId,
                    )
                },
                onDismissRequest = onDismissUserVerification,
            )
        }

        is Fido2State.DialogState.Fido2PinSetUpPrompt -> {
            PinInputDialog(
                onSubmitClick = { pin ->
                    onSubmitPinSetUpFido2Verification(pin, dialogState.selectedCipherId)
                },
                onCancelClick = onDismissUserVerification,
                onDismissRequest = onDismissUserVerification,
            )
        }

        is Fido2State.DialogState.Fido2MasterPasswordError -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = {
                    onRetryFido2PasswordVerification(dialogState.selectedCipherId)
                },
            )
        }

        is Fido2State.DialogState.Fido2PinError -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = {
                    onRetryFido2PinVerification(dialogState.selectedCipherId)
                },
            )
        }

        is Fido2State.DialogState.Fido2PinSetUpError -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = dialogState.title,
                message = dialogState.message,
            ),
            onDismissRequest = {
                onRetryPinSetUpFido2Verification(dialogState.selectedCipherId)
            },
        )

        null -> Unit
    }
}
