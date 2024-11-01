package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import android.os.Build
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.util.policyInformation
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.crypto.Cipher
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the account security screen.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val featureFlagManager: FeatureFlagManager,
    private val firstTimeActionManager: FirstTimeActionManager,
    policyManager: PolicyManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AccountSecurityState, AccountSecurityEvent, AccountSecurityAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
        val isBiometricsValid = biometricsEncryptionManager.isBiometricIntegrityValid(
            userId = userId,
            cipher = biometricsEncryptionManager.getOrCreateCipher(userId),
        )
        AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "".asText(), // This will be filled in dynamically
            isAuthenticatorSyncChecked = settingsRepository.isAuthenticatorSyncEnabled,
            isUnlockWithBiometricsEnabled = settingsRepository.isUnlockWithBiometricsEnabled &&
                isBiometricsValid,
            isUnlockWithPasswordEnabled = authRepository
                .userStateFlow
                .value
                ?.activeAccount
                ?.hasMasterPassword != false,
            isUnlockWithPinEnabled = settingsRepository.isUnlockWithPinEnabled,
            shouldShowEnableAuthenticatorSync =
            featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) &&
                !isBuildVersionBelow(Build.VERSION_CODES.S),
            userId = userId,
            vaultTimeout = settingsRepository.vaultTimeout,
            vaultTimeoutAction = settingsRepository.vaultTimeoutAction,
            vaultTimeoutPolicyMinutes = null,
            vaultTimeoutPolicyAction = null,
            shouldShowUnlockActionCard = false,
        )
    },
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

        featureFlagManager
            .getFeatureFlagFlow(FlagKey.AuthenticatorSync)
            .onEach {
                trySendAction(AccountSecurityAction.Internal.AuthenticatorSyncFeatureFlagUpdate(it))
            }
            .launchIn(viewModelScope)

        firstTimeActionManager
            .firstTimeStateFlow
            .map {
                AccountSecurityAction.Internal.ShowUnlockBadgeUpdated(it.showSetupUnlockCard)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isUnlockWithBiometricsEnabledFlow
            .map {
                AccountSecurityAction.Internal.BiometricLockUpdate(
                    isBiometricEnabled = it,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isUnlockWithPinEnabledFlow
            .map {
                AccountSecurityAction.Internal.PinProtectedLockUpdate(
                    isPinProtected = it,
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
        is AccountSecurityAction.AuthenticatorSyncToggle -> handleAuthenticatorSyncToggle(action)
        AccountSecurityAction.BackClick -> handleBackClick()
        AccountSecurityAction.ChangeMasterPasswordClick -> handleChangeMasterPasswordClick()
        AccountSecurityAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
        AccountSecurityAction.DeleteAccountClick -> handleDeleteAccountClick()
        AccountSecurityAction.DismissDialog -> handleDismissDialog()
        AccountSecurityAction.EnableBiometricsClick -> handleEnableBiometricsClick()
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
        is AccountSecurityAction.PushNotificationConfirm -> handlePushNotificationConfirm()
        is AccountSecurityAction.Internal -> handleInternalAction(action)
        AccountSecurityAction.UnlockActionCardCtaClick -> handleUnlockCardCtaClick()
        AccountSecurityAction.UnlockActionCardDismiss -> handleUnlockCardDismiss()
    }

    private fun handleUnlockCardDismiss() {
        dismissUnlockNotificationBadge()
    }

    private fun handleUnlockCardCtaClick() {
        sendEvent(AccountSecurityEvent.NavigateToSetupUnlockScreen)
    }

    private fun handleAccountFingerprintPhraseClick() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
    }

    private fun handleAuthenticatorSyncToggle(
        action: AccountSecurityAction.AuthenticatorSyncToggle,
    ) {
        settingsRepository.isAuthenticatorSyncEnabled = action.enabled
        mutableStateFlow.update { it.copy(isAuthenticatorSyncChecked = action.enabled) }
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

    private fun handleEnableBiometricsClick() {
        biometricsEncryptionManager
            .createCipherOrNull(userId = state.userId)
            ?.let {
                sendEvent(
                    AccountSecurityEvent.ShowBiometricsPrompt(
                        // Generate a new key in case the previous one was invalidated
                        cipher = it,
                    ),
                )
            }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        dialog = AccountSecurityDialog.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }
        dismissUnlockNotificationBadge()
    }

    private fun handleFingerPrintLearnMoreClick() {
        sendEvent(AccountSecurityEvent.NavigateToFingerprintPhrase)
    }

    private fun handleLockNowClick() {
        vaultRepository.lockVaultForCurrentUser()
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
        setVaultTimeoutAction(action.vaultTimeoutAction)
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
            validateVaultTimeoutAction()
        }
    }

    private fun handleUnlockWithPinToggle(action: AccountSecurityAction.UnlockWithPinToggle) {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = action.unlockWithPinState.isUnlockWithPinEnabled)
        }

        when (val state = action.unlockWithPinState) {
            UnlockWithPinState.PendingEnabled -> Unit
            UnlockWithPinState.Disabled -> {
                settingsRepository.clearUnlockPin()
                validateVaultTimeoutAction()
            }

            is UnlockWithPinState.Enabled -> {
                settingsRepository.storeUnlockPin(
                    pin = state.pin,
                    shouldRequireMasterPasswordOnRestart =
                    state.shouldRequireMasterPasswordOnRestart,
                )
            }
        }
        dismissUnlockNotificationBadge()
    }

    private fun handleInternalAction(action: AccountSecurityAction.Internal) {
        when (action) {
            is AccountSecurityAction.Internal.AuthenticatorSyncFeatureFlagUpdate -> {
                handleAuthenticatorSyncFeatureFlagUpdate(action)
            }

            is AccountSecurityAction.Internal.BiometricsKeyResultReceive -> {
                handleBiometricsKeyResultReceive(action)
            }

            is AccountSecurityAction.Internal.FingerprintResultReceive -> {
                handleFingerprintResultReceived(action)
            }

            is AccountSecurityAction.Internal.PolicyUpdateReceive -> {
                handlePolicyUpdateReceive(action)
            }

            is AccountSecurityAction.Internal.ShowUnlockBadgeUpdated -> {
                handleShowUnlockBadgeUpdated(action)
            }

            is AccountSecurityAction.Internal.BiometricLockUpdate -> {
                hanleBiometricUnlockUpdate(action)
            }

            is AccountSecurityAction.Internal.PinProtectedLockUpdate -> {
                handlePinProtectedLockUpdate(action)
            }
        }
    }

    private fun handlePinProtectedLockUpdate(
        action: AccountSecurityAction.Internal.PinProtectedLockUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                isUnlockWithPinEnabled = action.isPinProtected,
            )
        }
    }

    private fun hanleBiometricUnlockUpdate(
        action: AccountSecurityAction.Internal.BiometricLockUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = action.isBiometricEnabled,
            )
        }
    }

    private fun handleShowUnlockBadgeUpdated(
        action: AccountSecurityAction.Internal.ShowUnlockBadgeUpdated,
    ) {
        mutableStateFlow.update {
            it.copy(
                shouldShowUnlockActionCard = action.showUnlockBadge,
            )
        }
    }

    private fun handleAuthenticatorSyncFeatureFlagUpdate(
        action: AccountSecurityAction.Internal.AuthenticatorSyncFeatureFlagUpdate,
    ) {
        val shouldShowAuthenticatorSync =
            action.isEnabled && !isBuildVersionBelow(Build.VERSION_CODES.S)
        mutableStateFlow.update {
            it.copy(
                shouldShowEnableAuthenticatorSync = shouldShowAuthenticatorSync,
            )
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

    private fun validateVaultTimeoutAction() {
        if (!state.hasUnlockMechanism) {
            setVaultTimeoutAction(VaultTimeoutAction.LOGOUT)
        }
    }

    private fun setVaultTimeoutAction(vaultTimeoutAction: VaultTimeoutAction) {
        mutableStateFlow.update { it.copy(vaultTimeoutAction = vaultTimeoutAction) }
        settingsRepository.vaultTimeoutAction = vaultTimeoutAction
    }

    private fun dismissUnlockNotificationBadge() {
        if (!state.shouldShowUnlockActionCard) return
        firstTimeActionManager.storeShowUnlockSettingBadge(
            showBadge = false,
        )
    }
}

/**
 * Models state for the Account Security screen.
 */
@Parcelize
data class AccountSecurityState(
    val dialog: AccountSecurityDialog?,
    val fingerprintPhrase: Text,
    val isAuthenticatorSyncChecked: Boolean,
    val isUnlockWithBiometricsEnabled: Boolean,
    val isUnlockWithPasswordEnabled: Boolean,
    val isUnlockWithPinEnabled: Boolean,
    val shouldShowEnableAuthenticatorSync: Boolean,
    val userId: String,
    val vaultTimeout: VaultTimeout,
    val vaultTimeoutAction: VaultTimeoutAction,
    val vaultTimeoutPolicyMinutes: Int?,
    val vaultTimeoutPolicyAction: String?,
    val shouldShowUnlockActionCard: Boolean,
) : Parcelable {
    /**
     * Indicates that there is a mechanism for unlocking your vault in place.
     */
    val hasUnlockMechanism: Boolean
        get() = isUnlockWithPasswordEnabled ||
            isUnlockWithPinEnabled ||
            isUnlockWithBiometricsEnabled
}

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

    /**
     * Displays an error dialog with a title and message.
     */
    @Parcelize
    data class Error(
        val title: Text,
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
     * Shows the prompt for biometrics using with the given [cipher].
     */
    data class ShowBiometricsPrompt(
        val cipher: Cipher,
    ) : AccountSecurityEvent()

    /**
     * Displays a toast with the given [Text].
     */
    data class ShowToast(
        val text: Text,
    ) : AccountSecurityEvent()

    /**
     * Navigate to the setup unlock screen.
     */
    data object NavigateToSetupUnlockScreen : AccountSecurityEvent()
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
     * User clicked the authenticator sync toggle.
     */
    data class AuthenticatorSyncToggle(
        val enabled: Boolean,
    ) : AccountSecurityAction()

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
     * The user clicked to enable biometrics.
     */
    data object EnableBiometricsClick : AccountSecurityAction()

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
     * User toggled the unlock with pin switch.
     */
    data class UnlockWithPinToggle(
        val unlockWithPinState: UnlockWithPinState,
    ) : AccountSecurityAction()

    /**
     * User has dismissed the unlock action card.
     */
    data object UnlockActionCardDismiss : AccountSecurityAction()

    /**
     * User has clicked the CTA on the unlock action card.
     */
    data object UnlockActionCardCtaClick : AccountSecurityAction()

    /**
     * Models actions that can be sent by the view model itself.
     */
    sealed class Internal : AccountSecurityAction() {

        /**
         * The feature flag value for authenticator syncing was updated.
         */
        data class AuthenticatorSyncFeatureFlagUpdate(
            val isEnabled: Boolean,
        ) : Internal()

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

        /**
         * The show unlock badge update has been received.
         */
        data class ShowUnlockBadgeUpdated(val showUnlockBadge: Boolean) : Internal()

        /**
         * The user's biometric unlock status has been updated.
         */
        data class BiometricLockUpdate(
            val isBiometricEnabled: Boolean,
        ) : Internal()

        /**
         * The user's pin unlock status has been updated.
         */
        data class PinProtectedLockUpdate(
            val isPinProtected: Boolean,
        ) : Internal()
    }
}
