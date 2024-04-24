package com.bitwarden.authenticator.ui.platform.feature.splash

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

@HiltViewModel
class SplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
) : BaseViewModel<SplashState, SplashEvent, SplashAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val showBiometricsPrompt = settingsRepository.isUnlockWithBiometricsEnabled
            && biometricsEncryptionManager.isBiometricIntegrityValid()
        SplashState(
            isBiometricEnabled = settingsRepository.isUnlockWithBiometricsEnabled,
            isBiometricsValid = biometricsEncryptionManager.isBiometricIntegrityValid(),
            viewState = SplashState.ViewState.Locked(showBiometricsPrompt = showBiometricsPrompt),
            dialog = null,
        )
    }
) {

    override fun handleAction(action: SplashAction) {
        when (action) {
            SplashAction.DismissDialog -> handleDismissDialog()
            is SplashAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleInternalAction(action: SplashAction.Internal) {
        when (action) {
            is SplashAction.Internal.UnlockResultReceived -> handleUnlockResultReceive(action)
        }
    }

    private fun handleUnlockResultReceive(action: SplashAction.Internal.UnlockResultReceived) {
        when (action.unlockResult) {
            UnlockResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SplashState.ViewState.Locked(showBiometricsPrompt = false),
                        dialog = SplashState.Dialog.Error(R.string.generic_error_message.asText())
                    )
                }
                sendEvent(SplashEvent.ExitApplication)
            }

            UnlockResult.LockOut,
            UnlockResult.Cancel,
            -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SplashState.ViewState.Locked(
                            showBiometricsPrompt = false
                        )
                    )
                }
                sendEvent(SplashEvent.ExitApplication)
            }

            UnlockResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SplashState.ViewState.Unlocked,
                        dialog = null,
                    )
                }
                if (state.isBiometricEnabled && !state.isBiometricsValid) {
                    biometricsEncryptionManager.setupBiometrics()
                }
                sendEvent(SplashEvent.NavigateToAuthenticator)
            }
        }
    }
}

data class SplashState(
    val isBiometricEnabled: Boolean,
    val isBiometricsValid: Boolean,
    val viewState: ViewState,
    val dialog: Dialog?,
) {
    sealed class ViewState {
        data object Unlocked : ViewState()

        data class Locked(
            val showBiometricsPrompt: Boolean,
        ) : ViewState()
    }

    sealed class Dialog : Parcelable {

        @Parcelize
        data class Error(
            val message: Text,
        ) : Dialog()

        @Parcelize
        data object Loading : Dialog()
    }
}

sealed class SplashEvent {
    data object NavigateToAuthenticator : SplashEvent()

    data class ShowToast(val message: Text) : SplashEvent()

    data object ExitApplication : SplashEvent()
}

sealed class SplashAction {
    data object DismissDialog : SplashAction()

    sealed class Internal : SplashAction() {
        data class UnlockResultReceived(
            val unlockResult: UnlockResult,
        ) : Internal()
    }
}

sealed class UnlockResult {
    data object Success : UnlockResult()

    data object Cancel : UnlockResult()

    data object LockOut : UnlockResult()

    data object Error : UnlockResult()
}
