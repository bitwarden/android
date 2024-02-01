package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val environmentRepository: EnvironmentRepository,
    private val policyManager: PolicyManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AccountSecurityState, AccountSecurityEvent, AccountSecurityAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "".asText(), // This will be filled in dynamically
            isApproveLoginRequestsEnabled = settingsRepository.isApprovePasswordlessLoginsEnabled,
            isUnlockWithBiometricsEnabled = settingsRepository.isUnlockWithBiometricsEnabled,
            isUnlockWithPinEnabled = settingsRepository.isUnlockWithPinEnabled,
            vaultTimeout = settingsRepository.vaultTimeout,
            vaultTimeoutAction = settingsRepository.vaultTimeoutAction,
            vaultTimeoutPolicyMinutes = null,
            vaultTimeoutPolicyAction = null,
        ),
) {
    private val webSettingsUrl: String
        get() {
            val baseUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            return "$baseUrl/#/settings"
        }

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
            .map { policies ->
                AccountSecurityAction.Internal.PolicyUpdateReceive(
                    vaultTimeoutPolicies = policies.mapNotNull {
                        it.policyInformation as? PolicyInformation.VaultTimeout
                    },
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            trySendAction(
                AccountSecurityAction.Internal.FingerprintResultReceive(
                    fingerprintResult = settingsRepository.getUserFingerprint(),
                ),
            )
        }
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
        AccountSecurityAction.LogoutClick -> handleLogoutClick()
        AccountSecurityAction.PendingLoginRequestsClick -> handlePendingLoginRequestsClick()
        is AccountSecurityAction.VaultTimeoutTypeSelect -> handleVaultTimeoutTypeSelect(action)
        is AccountSecurityAction.CustomVaultTimeoutSelect -> handleCustomVaultTimeoutSelect(action)
        is AccountSecurityAction.VaultTimeoutActionSelect -> handleVaultTimeoutActionSelect(action)
        AccountSecurityAction.TwoStepLoginClick -> handleTwoStepLoginClick()
        is AccountSecurityAction.UnlockWithBiometricToggle -> {
            handleUnlockWithBiometricToggle(action)
        }

        is AccountSecurityAction.UnlockWithPinToggle -> handleUnlockWithPinToggle(action)

        is AccountSecurityAction.ApprovePasswordlessLoginsToggle -> {
            handleApprovePasswordlessLoginsToggle(action)
        }

        is AccountSecurityAction.PushNotificationConfirm -> handlePushNotificationConfirm()
        is AccountSecurityAction.Internal -> handleInternalAction(action)
    }

    private fun handleAccountFingerprintPhraseClick() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
    }

    private fun handleBackClick() = sendEvent(AccountSecurityEvent.NavigateBack)

    private fun handleChangeMasterPasswordClick() {
        sendEvent(AccountSecurityEvent.NavigateToChangeMasterPassword(webSettingsUrl))
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

    private fun handleApprovePasswordlessLoginsToggle(
        action: AccountSecurityAction.ApprovePasswordlessLoginsToggle,
    ) {
        when (action) {
            AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled -> {
                settingsRepository.isApprovePasswordlessLoginsEnabled = false
                mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }
            }

            AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled -> {
                settingsRepository.isApprovePasswordlessLoginsEnabled = true
                mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = true) }
            }

            AccountSecurityAction.ApprovePasswordlessLoginsToggle.PendingEnabled -> {
                mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = true) }
            }
        }
    }

    private fun handlePushNotificationConfirm() {
        sendEvent(AccountSecurityEvent.NavigateToApplicationDataSettings)
    }

    private fun handleLogoutClick() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.ConfirmLogout) }
    }

    private fun handlePendingLoginRequestsClick() {
        sendEvent(AccountSecurityEvent.NavigateToPendingRequests)
    }

    private fun handleVaultTimeoutTypeSelect(action: AccountSecurityAction.VaultTimeoutTypeSelect) {
        val previousTimeout = state.vaultTimeout
        val vaultTimeout = when (action.vaultTimeoutType) {
            VaultTimeout.Type.IMMEDIATELY -> VaultTimeout.Immediately
            VaultTimeout.Type.ONE_MINUTE -> VaultTimeout.OneMinute
            VaultTimeout.Type.FIVE_MINUTES -> VaultTimeout.FiveMinutes
            VaultTimeout.Type.FIFTEEN_MINUTES -> VaultTimeout.FifteenMinutes
            VaultTimeout.Type.THIRTY_MINUTES -> VaultTimeout.ThirtyMinutes
            VaultTimeout.Type.ONE_HOUR -> VaultTimeout.OneHour
            VaultTimeout.Type.FOUR_HOURS -> VaultTimeout.FourHours
            VaultTimeout.Type.ON_APP_RESTART -> VaultTimeout.OnAppRestart
            VaultTimeout.Type.NEVER -> VaultTimeout.Never
            VaultTimeout.Type.CUSTOM -> {
                if (previousTimeout is VaultTimeout.Custom) {
                    previousTimeout
                } else {
                    VaultTimeout.Custom(vaultTimeoutInMinutes = 0)
                }
            }
        }
        handleVaultTimeoutSelect(vaultTimeout = vaultTimeout)
    }

    private fun handleCustomVaultTimeoutSelect(
        action: AccountSecurityAction.CustomVaultTimeoutSelect,
    ) {
        handleVaultTimeoutSelect(vaultTimeout = action.customVaultTimeout)
    }

    private fun handleVaultTimeoutSelect(vaultTimeout: VaultTimeout) {
        mutableStateFlow.update {
            it.copy(
                vaultTimeout = vaultTimeout,
            )
        }
        settingsRepository.vaultTimeout = vaultTimeout
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
    }

    private fun handleTwoStepLoginClick() {
        sendEvent(AccountSecurityEvent.NavigateToTwoStepLogin(webSettingsUrl))
    }

    private fun handleUnlockWithBiometricToggle(
        action: AccountSecurityAction.UnlockWithBiometricToggle,
    ) {
        if (action.enabled) {
            mutableStateFlow.update {
                it.copy(
                    dialog = AccountSecurityDialog.Loading(R.string.saving.asText()),
                    isUnlockWithBiometricsEnabled = true,
                )
            }
            viewModelScope.launch {
                val result = settingsRepository.setupBiometricsKey()
                sendAction(AccountSecurityAction.Internal.BiometricsKeyResultReceive(result))
            }
        } else {
            settingsRepository.clearBiometricsKey()
            mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = false) }
        }
    }

    private fun handleUnlockWithPinToggle(action: AccountSecurityAction.UnlockWithPinToggle) {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = action.isUnlockWithPinEnabled)
        }

        when (action) {
            AccountSecurityAction.UnlockWithPinToggle.PendingEnabled -> Unit
            AccountSecurityAction.UnlockWithPinToggle.Disabled -> {
                settingsRepository.clearUnlockPin()
            }

            is AccountSecurityAction.UnlockWithPinToggle.Enabled -> {
                settingsRepository.storeUnlockPin(
                    pin = action.pin,
                    shouldRequireMasterPasswordOnRestart =
                    action.shouldRequireMasterPasswordOnRestart,
                )
            }
        }
    }

    private fun handleInternalAction(action: AccountSecurityAction.Internal) {
        when (action) {
            is AccountSecurityAction.Internal.BiometricsKeyResultReceive -> {
                handleBiometricsKeyResultReceive(action)
            }

            is AccountSecurityAction.Internal.FingerprintResultReceive -> {
                handleFingerprintResultReceived(action)
            }

            is AccountSecurityAction.Internal.PolicyUpdateReceive -> {
                handlePolicyUpdateReceive(action)
            }
        }
    }

    private fun handleBiometricsKeyResultReceive(
        action: AccountSecurityAction.Internal.BiometricsKeyResultReceive,
    ) {
        when (action.result) {
            BiometricsKeyResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = false,
                    )
                }
            }

            BiometricsKeyResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = true,
                    )
                }
            }
        }
    }

    private fun handleFingerprintResultReceived(
        action: AccountSecurityAction.Internal.FingerprintResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                fingerprintPhrase = when (val result = action.fingerprintResult) {
                    is UserFingerprintResult.Success -> result.fingerprint.asText()
                    // This should never fail for an unlocked account.
                    is UserFingerprintResult.Error -> "".asText()
                },
            )
        }
    }

    private fun handlePolicyUpdateReceive(
        action: AccountSecurityAction.Internal.PolicyUpdateReceive,
    ) {
        // The vault timeout policy can only be implemented in organizations that have
        // the single organization policy, meaning that if this is enabled, the user is
        // only in one organization and hence there is only one result in the list.
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicyMinutes = action.vaultTimeoutPolicies?.firstOrNull()?.minutes,
                vaultTimeoutPolicyAction = action.vaultTimeoutPolicies?.firstOrNull()?.action,
            )
        }
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
    val vaultTimeout: VaultTimeout,
    val vaultTimeoutAction: VaultTimeoutAction,
    val vaultTimeoutPolicyMinutes: Int?,
    val vaultTimeoutPolicyAction: String?,
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

    /**
     * Displays a loading dialog.
     */
    @Parcelize
    data class Loading(
        val message: Text,
    ) : AccountSecurityDialog()
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
     * Navigate to the application's settings screen.
     */
    data object NavigateToApplicationDataSettings : AccountSecurityEvent()

    /**
     * Navigate to the delete account screen.
     */
    data object NavigateToDeleteAccount : AccountSecurityEvent()

    /**
     * Navigate to fingerprint phrase information.
     */
    data object NavigateToFingerprintPhrase : AccountSecurityEvent()

    /**
     * Navigate to the Pending Login Requests screen.
     */
    data object NavigateToPendingRequests : AccountSecurityEvent()

    /**
     * Navigate to the two step login screen.
     */
    data class NavigateToTwoStepLogin(val url: String) : AccountSecurityEvent()

    /**
     * Navigate to the change master password screen.
     */
    data class NavigateToChangeMasterPassword(val url: String) : AccountSecurityEvent()

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
     * User selected an updated [VaultTimeout.Custom].
     */
    data class CustomVaultTimeoutSelect(
        val customVaultTimeout: VaultTimeout.Custom,
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
     * User confirmed the push notification permission prompt.
     */
    data object PushNotificationConfirm : AccountSecurityAction()

    /**
     * User toggled the approve passwordless logins switch.
     */
    sealed class ApprovePasswordlessLoginsToggle : AccountSecurityAction() {
        /**
         * The toggle was enabled and confirmed.
         */
        data object Enabled : ApprovePasswordlessLoginsToggle()

        /**
         * The toggle was enabled but not yet confirmed.
         */
        data object PendingEnabled : ApprovePasswordlessLoginsToggle()

        /**
         * The toggle was disabled.
         */
        data object Disabled : ApprovePasswordlessLoginsToggle()
    }

    /**
     * User toggled the unlock with pin switch.
     */
    sealed class UnlockWithPinToggle : AccountSecurityAction() {
        /**
         * Whether or not the action represents PIN unlocking being enabled.
         */
        abstract val isUnlockWithPinEnabled: Boolean

        /**
         * The toggle was disabled.
         */
        data object Disabled : UnlockWithPinToggle() {
            override val isUnlockWithPinEnabled: Boolean get() = false
        }

        /**
         * The toggle was enabled but the behavior is pending confirmation.
         */
        data object PendingEnabled : UnlockWithPinToggle() {
            override val isUnlockWithPinEnabled: Boolean get() = true
        }

        /**
         * The toggle was enabled and the user's [pin] and [shouldRequireMasterPasswordOnRestart]
         * preference were confirmed.
         */
        data class Enabled(
            val pin: String,
            val shouldRequireMasterPasswordOnRestart: Boolean,
        ) : UnlockWithPinToggle() {
            override val isUnlockWithPinEnabled: Boolean get() = true
        }
    }

    /**
     * Models actions that can be sent by the view model itself.
     */
    sealed class Internal : AccountSecurityAction() {
        /**
         * A biometrics key result has been received.
         */
        data class BiometricsKeyResultReceive(
            val result: BiometricsKeyResult,
        ) : Internal()

        /**
         * A fingerprint has been received.
         */
        data class FingerprintResultReceive(
            val fingerprintResult: UserFingerprintResult,
        ) : Internal()

        /**
         * A policy update has been received.
         */
        data class PolicyUpdateReceive(
            val vaultTimeoutPolicies: List<PolicyInformation.VaultTimeout>?,
        ) : Internal()
    }
}
