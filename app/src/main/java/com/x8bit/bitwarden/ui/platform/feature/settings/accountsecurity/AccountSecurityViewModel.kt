package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the account security screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AccountSecurityState, AccountSecurityEvent, AccountSecurityAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "fingerprint-placeholder".asText(),
            isApproveLoginRequestsEnabled = false,
            isUnlockWithBiometricsEnabled = false,
            isUnlockWithPinEnabled = false,
            vaultTimeoutType = settingsRepository.vaultTimeout.type,
            vaultTimeoutAction = settingsRepository.vaultTimeoutAction,
        ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AccountSecurityAction): Unit = when (action) {
        AccountSecurityAction.AccountFingerprintPhraseClick -> handleAccountFingerprintPhraseClick()
        AccountSecurityAction.BackClick -> handleBackClick()
        AccountSecurityAction.ChangeMasterPasswordClick -> handleChangeMasterPasswordClick()
        AccountSecurityAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
        AccountSecurityAction.DeleteAccountClick -> handleDeleteAccountClick()
        AccountSecurityAction.DismissDialog -> handleDismissDialog()
        AccountSecurityAction.FingerPrintLearnMoreClick -> handleFingerPrintLearnMoreClick()
        AccountSecurityAction.LockNowClick -> handleLockNowClick()
        is AccountSecurityAction.LoginRequestToggle -> handleLoginRequestToggle(action)
        AccountSecurityAction.LogoutClick -> handleLogoutClick()
        AccountSecurityAction.PendingLoginRequestsClick -> handlePendingLoginRequestsClick()
        is AccountSecurityAction.VaultTimeoutTypeSelect -> handleVaultTimeoutTypeSelect(action)
        is AccountSecurityAction.VaultTimeoutActionSelect -> {
            handleVaultTimeoutActionSelect(action)
        }

        AccountSecurityAction.TwoStepLoginClick -> handleTwoStepLoginClick()
        is AccountSecurityAction.UnlockWithBiometricToggle -> {
            handleUnlockWithBiometricToggled(action)
        }

        is AccountSecurityAction.UnlockWithPinToggle -> handleUnlockWithPinToggle(action)
    }

    private fun handleAccountFingerprintPhraseClick() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
    }

    private fun handleBackClick() = sendEvent(AccountSecurityEvent.NavigateBack)

    private fun handleChangeMasterPasswordClick() {
        // TODO BIT-971: Add Leaving app Dialog
        sendEvent(AccountSecurityEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleConfirmLogoutClick() {
        mutableStateFlow.update { it.copy(dialog = null) }
        authRepository.logout()
    }

    private fun handleDeleteAccountClick() {
        sendEvent(AccountSecurityEvent.NavigateToDeleteAccount)
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleFingerPrintLearnMoreClick() {
        sendEvent(AccountSecurityEvent.NavigateToFingerprintPhrase)
    }

    private fun handleLockNowClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleLoginRequestToggle(action: AccountSecurityAction.LoginRequestToggle) {
        // TODO BIT-466: Persist pending login requests state
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = action.enabled) }
        sendEvent(AccountSecurityEvent.ShowToast("Handle Login requests on this device.".asText()))
    }

    private fun handleLogoutClick() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.ConfirmLogout) }
    }

    private fun handlePendingLoginRequestsClick() {
        // TODO BIT-466: Implement pending login requests UI
        sendEvent(AccountSecurityEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleVaultTimeoutTypeSelect(action: AccountSecurityAction.VaultTimeoutTypeSelect) {
        val vaultTimeoutType = action.vaultTimeoutType
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutType = action.vaultTimeoutType,
            )
        }
        val vaultTimeout = when (vaultTimeoutType) {
            VaultTimeout.Type.IMMEDIATELY -> VaultTimeout.Immediately
            VaultTimeout.Type.ONE_MINUTE -> VaultTimeout.OneMinute
            VaultTimeout.Type.FIVE_MINUTES -> VaultTimeout.FiveMinutes
            VaultTimeout.Type.FIFTEEN_MINUTES -> VaultTimeout.FifteenMinutes
            VaultTimeout.Type.THIRTY_MINUTES -> VaultTimeout.ThirtyMinutes
            VaultTimeout.Type.ONE_HOUR -> VaultTimeout.OneHour
            VaultTimeout.Type.FOUR_HOURS -> VaultTimeout.FourHours
            VaultTimeout.Type.ON_APP_RESTART -> VaultTimeout.OnAppRestart
            VaultTimeout.Type.NEVER -> VaultTimeout.Never
            VaultTimeout.Type.CUSTOM -> VaultTimeout.Custom(vaultTimeoutInMinutes = 0)
        }
        settingsRepository.vaultTimeout = vaultTimeout

        // TODO: Finish implementing vault timeouts (BIT-1120)
        sendEvent(AccountSecurityEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleVaultTimeoutActionSelect(
        action: AccountSecurityAction.VaultTimeoutActionSelect,
    ) {
        val vaultTimeoutAction = action.vaultTimeoutAction
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutAction = action.vaultTimeoutAction,
            )
        }
        settingsRepository.vaultTimeoutAction = vaultTimeoutAction

        // TODO BIT-746: Finish implementing session timeout action
        sendEvent(AccountSecurityEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleTwoStepLoginClick() {
        // TODO BIT-468: Implement two-step login
        sendEvent(AccountSecurityEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleUnlockWithBiometricToggled(
        action: AccountSecurityAction.UnlockWithBiometricToggle,
    ) {
        // TODO Display alert
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = action.enabled) }
        sendEvent(AccountSecurityEvent.ShowToast("Handle unlock with biometrics.".asText()))
    }

    private fun handleUnlockWithPinToggle(action: AccountSecurityAction.UnlockWithPinToggle) {
        // TODO BIT-974: Display alert
        mutableStateFlow.update { it.copy(isUnlockWithPinEnabled = action.enabled) }
        sendEvent(AccountSecurityEvent.ShowToast("Handle unlock with pin.".asText()))
    }
}

/**
 * Models state for the Account Security screen.
 */
@Parcelize
data class AccountSecurityState(
    val dialog: AccountSecurityDialog?,
    val fingerprintPhrase: Text,
    val isApproveLoginRequestsEnabled: Boolean,
    val isUnlockWithBiometricsEnabled: Boolean,
    val isUnlockWithPinEnabled: Boolean,
    val vaultTimeoutType: VaultTimeout.Type,
    val vaultTimeoutAction: VaultTimeoutAction,
) : Parcelable

/**
 * Representation of the dialogs that can be displayed on account security screen.
 */
sealed class AccountSecurityDialog : Parcelable {
    /**
     * Allows the user to confirm that they want to logout.
     */
    @Parcelize
    data object ConfirmLogout : AccountSecurityDialog()

    /**
     * Allows the user to view their fingerprint phrase.
     */
    @Parcelize
    data object FingerprintPhrase : AccountSecurityDialog()
}

/**
 * A representation of the Session timeout action.
 */
enum class SessionTimeoutAction(val text: Text) {
    LOCK(text = R.string.lock.asText()),
    LOG_OUT(text = R.string.log_out.asText()),
}

/**
 * Models events for the account security screen.
 */
sealed class AccountSecurityEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AccountSecurityEvent()

    /**
     * Navigate to the delete account screen.
     */
    data object NavigateToDeleteAccount : AccountSecurityEvent()

    /**
     * Navigate to fingerprint phrase information.
     */
    data object NavigateToFingerprintPhrase : AccountSecurityEvent()

    /**
     * Displays a toast with the given [Text].
     */
    data class ShowToast(
        val text: Text,
    ) : AccountSecurityEvent()
}

/**
 * Models actions for the account security screen.
 */
sealed class AccountSecurityAction {

    /**
     * User clicked account fingerprint phrase.
     */
    data object AccountFingerprintPhraseClick : AccountSecurityAction()

    /**
     * User clicked back button.
     */
    data object BackClick : AccountSecurityAction()

    /**
     * User clicked change master password.
     */
    data object ChangeMasterPasswordClick : AccountSecurityAction()

    /**
     * User confirmed they want to logout.
     */
    data object ConfirmLogoutClick : AccountSecurityAction()

    /**
     * User clicked delete account.
     */
    data object DeleteAccountClick : AccountSecurityAction()

    /**
     * User dismissed the currently displayed dialog.
     */
    data object DismissDialog : AccountSecurityAction()

    /**
     * User clicked fingerprint phrase.
     */
    data object FingerPrintLearnMoreClick : AccountSecurityAction()

    /**
     * User clicked lock now.
     */
    data object LockNowClick : AccountSecurityAction()

    /**
     * User toggled the login request switch.
     */
    data class LoginRequestToggle(
        val enabled: Boolean,
    ) : AccountSecurityAction()

    /**
     * User clicked log out.
     */
    data object LogoutClick : AccountSecurityAction()

    /**
     * User clicked pending login requests.
     */
    data object PendingLoginRequestsClick : AccountSecurityAction()

    /**
     * User selected a [vaultTimeoutType].
     */
    data class VaultTimeoutTypeSelect(
        val vaultTimeoutType: VaultTimeout.Type,
    ) : AccountSecurityAction()

    /**
     * User selected a [VaultTimeoutAction].
     */
    data class VaultTimeoutActionSelect(
        val vaultTimeoutAction: VaultTimeoutAction,
    ) : AccountSecurityAction()

    /**
     * User clicked two-step login.
     */
    data object TwoStepLoginClick : AccountSecurityAction()

    /**
     * User toggled the unlock with biometrics switch.
     */
    data class UnlockWithBiometricToggle(
        val enabled: Boolean,
    ) : AccountSecurityAction()

    /**
     * User toggled the unlock with pin switch.
     */
    data class UnlockWithPinToggle(
        val enabled: Boolean,
    ) : AccountSecurityAction()
}
