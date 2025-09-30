package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.hexToColor
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.account.util.initials
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.manager.AppResumeManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.util.toCreateCredentialRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toGetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.emptyInputDialogMessage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenErrorMessage
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.crypto.Cipher
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial vault unlock screen.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class VaultUnlockViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepo: VaultRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val bitwardenCredentialManager: BitwardenCredentialManager,
    private val appResumeManager: AppResumeManager,
    private val vaultLockManager: VaultLockManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultUnlockState, VaultUnlockEvent, VaultUnlockAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val activeAccount = userState.activeAccount
        val accountSummaries = userState.toAccountSummaries()
        val activeAccountSummary = userState.toActiveAccountSummary()
        val isBiometricsValid = biometricsEncryptionManager.isBiometricIntegrityValid(
            userId = userState.activeUserId,
            cipher = biometricsEncryptionManager.getOrCreateCipher(userState.activeUserId),
        )
        val vaultUnlockType = activeAccount.vaultUnlockType
        val hasNoMasterPassword = !activeAccount.hasMasterPassword
        if (!activeAccount.hasManualUnlockMechanism) {
            // There is no valid way to unlock this app.
            authRepository.logout(
                reason = LogoutReason.InvalidState(source = "VaultUnlockViewModel"),
            )
        }

        val specialCircumstance = specialCircumstanceManager.specialCircumstance

        val showAccountMenu =
            savedStateHandle.toVaultUnlockArgs().unlockType == UnlockType.STANDARD &&
                (specialCircumstance !is SpecialCircumstance.ProviderGetCredentials &&
                    specialCircumstance !is SpecialCircumstance.Fido2Assertion)
        VaultUnlockState(
            accountSummaries = accountSummaries,
            avatarColorString = activeAccountSummary.avatarColorHex,
            hideInput = hasNoMasterPassword && vaultUnlockType == VaultUnlockType.MASTER_PASSWORD,
            initials = activeAccountSummary.initials,
            email = activeAccountSummary.email,
            dialog = null,
            environmentUrl = activeAccount.environment.label,
            input = "",
            isBiometricEnabled = activeAccount.isBiometricsEnabled,
            isBiometricsValid = isBiometricsValid,
            showAccountMenu = showAccountMenu,
            showBiometricInvalidatedMessage = false,
            vaultUnlockType = vaultUnlockType,
            userId = userState.activeUserId,
            getCredentialsRequest = specialCircumstance?.toGetCredentialsRequestOrNull(),
            fido2CredentialAssertionRequest = specialCircumstance?.toFido2AssertionRequestOrNull(),
            createCredentialRequest = specialCircumstance?.toCreateCredentialRequestOrNull(),
            hasMasterPassword = activeAccount.hasMasterPassword,
            isFromLockFlow = vaultLockManager.isFromLockFlow,
        )
    },
) {
    init {
        authRepository
            .userStateFlow
            .onEach {
                sendAction(VaultUnlockAction.Internal.UserStateUpdateReceive(userState = it))
            }
            .launchIn(viewModelScope)

        promptForBiometricsIfAvailable()

        // only when navigating from vault to lock we should not display biometrics
        // subsequent views of the lock screen should display biometrics if available
        vaultLockManager.isFromLockFlow = false
        mutableStateFlow.update { it.copy(isFromLockFlow = false) }
    }

    override fun onCleared() {
        // TODO: This is required because there is an OS-level leak occurring that leaves the
        //   ViewModel in memory. We should remove this when that leak is fixed. (BIT-2287)
        mutableStateFlow.update { it.copy(input = "") }
        super.onCleared()
    }

    override fun handleAction(action: VaultUnlockAction) {
        when (action) {
            VaultUnlockAction.AddAccountClick -> handleAddAccountClick()
            VaultUnlockAction.DismissDialog -> handleDismissDialog()
            VaultUnlockAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
            is VaultUnlockAction.InputChanged -> handleInputChanged(action)
            is VaultUnlockAction.LockAccountClick -> handleLockAccountClick(action)
            is VaultUnlockAction.LogoutAccountClick -> handleLogoutAccountClick(action)
            is VaultUnlockAction.SwitchAccountClick -> handleSwitchAccountClick(action)
            VaultUnlockAction.BiometricsLockOut -> handleBiometricsLockOut()
            VaultUnlockAction.BiometricsUnlockClick -> handleBiometricsUnlockClick()
            is VaultUnlockAction.BiometricsUnlockSuccess -> handleBiometricsUnlockSuccess(action)
            VaultUnlockAction.UnlockClick -> handleUnlockClick()
            is VaultUnlockAction.Internal -> handleInternalAction(action)
            VaultUnlockAction.BiometricsNoLongerSupported -> {
                handleBiometricsNoLongerSupported()
            }

            VaultUnlockAction.DismissBiometricsNoLongerSupportedDialog -> {
                handleDismissBiometricsNoLongerSupportedDialog()
            }
        }
    }

    private fun handleBiometricsNoLongerSupported() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported,
            )
        }
    }

    private fun handleDismissBiometricsNoLongerSupportedDialog() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
        authRepository.logout(reason = LogoutReason.Biometrics.NoLongerSupported)
        authRepository.hasPendingAccountAddition = true
    }

    private fun handleAddAccountClick() {
        authRepository.hasPendingAccountAddition = true
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
        when {
            state.getCredentialsRequest != null -> {
                sendEvent(
                    VaultUnlockEvent.GetCredentialsError(
                        BitwardenString
                            .credential_operation_failed_because_user_could_not_be_verified
                            .asText(),
                    ),
                )
            }

            state.fido2CredentialAssertionRequest != null -> {
                sendEvent(
                    VaultUnlockEvent.Fido2CredentialAssertionError(
                        BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                            .asText(),
                    ),
                )
            }

            else -> Unit
        }
    }

    private fun handleConfirmLogoutClick() {
        authRepository.logout(reason = LogoutReason.Click(source = "VaultUnlockViewModel"))
    }

    private fun handleInputChanged(action: VaultUnlockAction.InputChanged) {
        mutableStateFlow.update {
            it.copy(input = action.input)
        }
    }

    private fun handleLockAccountClick(action: VaultUnlockAction.LockAccountClick) {
        vaultRepo.lockVault(userId = action.accountSummary.userId, isUserInitiated = true)
    }

    private fun handleLogoutAccountClick(action: VaultUnlockAction.LogoutAccountClick) {
        authRepository.logout(
            userId = action.accountSummary.userId,
            reason = LogoutReason.Click(source = "VaultUnlockViewModel"),
        )
    }

    private fun handleSwitchAccountClick(action: VaultUnlockAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleBiometricsLockOut() {
        authRepository.logout(reason = LogoutReason.Biometrics.Lockout)
    }

    private fun handleBiometricsUnlockClick() {
        val cipher = biometricsEncryptionManager.getOrCreateCipher(state.userId)
        if (cipher != null) {
            sendEvent(
                event = VaultUnlockEvent.PromptForBiometrics(
                    cipher = cipher,
                ),
            )
        } else {
            mutableStateFlow.update {
                it.copy(
                    isBiometricsValid = false,
                    showBiometricInvalidatedMessage = !biometricsEncryptionManager
                        .isAccountBiometricIntegrityValid(state.userId),
                )
            }
        }
    }

    private fun handleBiometricsUnlockSuccess(action: VaultUnlockAction.BiometricsUnlockSuccess) {
        val activeUserId = authRepository.activeUserId ?: return
        mutableStateFlow.update { it.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading) }
        viewModelScope.launch {
            val vaultUnlockResult = vaultRepo.unlockVaultWithBiometrics(cipher = action.cipher)
            sendAction(
                VaultUnlockAction.Internal.ReceiveVaultUnlockResult(
                    userId = activeUserId,
                    vaultUnlockResult = vaultUnlockResult,
                    isBiometricLogin = true,
                ),
            )
        }
    }

    private fun handleUnlockClick() {
        val activeUserId = authRepository.activeUserId ?: return

        if (state.input.isEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = VaultUnlockState.VaultUnlockDialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = it.vaultUnlockType.emptyInputDialogMessage,
                    ),
                )
            }
            return
        }

        mutableStateFlow.update { it.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading) }
        viewModelScope.launch {
            val vaultUnlockResult = when (state.vaultUnlockType) {
                VaultUnlockType.MASTER_PASSWORD -> {
                    vaultRepo.unlockVaultWithMasterPassword(
                        state.input,
                    )
                }

                VaultUnlockType.PIN -> {
                    vaultRepo.unlockVaultWithPin(
                        state.input,
                    )
                }
            }
            sendAction(
                VaultUnlockAction.Internal.ReceiveVaultUnlockResult(
                    userId = activeUserId,
                    vaultUnlockResult = vaultUnlockResult,
                    isBiometricLogin = false,
                ),
            )
        }
    }

    private fun handleInternalAction(action: VaultUnlockAction.Internal) {
        when (action) {
            is VaultUnlockAction.Internal.ReceiveVaultUnlockResult -> {
                handleReceiveVaultUnlockResult(action)
            }

            is VaultUnlockAction.Internal.UserStateUpdateReceive -> {
                handleUserStateUpdateReceive(action)
            }
        }
    }

    private fun handleReceiveVaultUnlockResult(
        action: VaultUnlockAction.Internal.ReceiveVaultUnlockResult,
    ) {
        if (action.userId != authRepository.activeUserId) {
            // The active user has automatically switched before receiving the event. Ignore any
            // results and just clear any loading dialog.
            mutableStateFlow.update { it.copy(dialog = null) }
            return
        }

        // Mark the user verified for this session if the unlock result is Success.
        bitwardenCredentialManager.isUserVerified =
            action.vaultUnlockResult is VaultUnlockResult.Success &&
                state.isUnlockingForCredentialManagerRequest

        when (val result = action.vaultUnlockResult) {
            is VaultUnlockResult.AuthenticationError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = if (action.isBiometricLogin) {
                                BitwardenString.generic_error_message.asText()
                            } else {
                                state.vaultUnlockType.unlockScreenErrorMessage
                            },
                            throwable = result.error?.takeIf { _ -> action.isBiometricLogin },
                        ),
                    )
                }
            }

            is VaultUnlockResult.BiometricDecodingError -> {
                biometricsEncryptionManager.clearBiometrics(userId = state.userId)
                mutableStateFlow.update {
                    it.copy(
                        isBiometricsValid = false,
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            title = BitwardenString.biometrics_failed.asText(),
                            message = BitwardenString.biometrics_decoding_failure.asText(),
                        ),
                    )
                }
            }

            is VaultUnlockResult.GenericError,
            is VaultUnlockResult.InvalidStateError,
                -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = (result as? VaultUnlockResult.InvalidStateError)?.error
                                ?: (result as? VaultUnlockResult.GenericError)?.error,
                        ),
                    )
                }
            }

            VaultUnlockResult.Success -> {
                if (specialCircumstanceManager.specialCircumstance == null) {
                    specialCircumstanceManager.specialCircumstance =
                        appResumeManager.getResumeSpecialCircumstance()
                }

                mutableStateFlow.update { it.copy(dialog = null) }
                // Don't do anything, we'll navigate to the right place.
            }
        }
    }

    private fun handleUserStateUpdateReceive(
        action: VaultUnlockAction.Internal.UserStateUpdateReceive,
    ) {
        // Leave the current data alone if there is no UserState; we are in the process of logging
        // out.
        val userState = action.userState ?: return

        // If the Vault is being unlocked for a FIDO 2 request, make sure we're unlocking the
        // correct Vault
        state.credentialManagerRequestUserId
            ?.let { fido2RequestUserId ->
                // If the current Vault is not the selected Vault, switch accounts.
                if (userState.activeUserId != fido2RequestUserId) {
                    authRepository.switchAccount(fido2RequestUserId)
                    return
                }
            }
        // If the Vault is already unlocked, do nothing.
        if (userState.activeAccount.isVaultUnlocked) return
        // If the user state has changed to add a new account, do nothing.
        if (userState.hasPendingAccountAddition) return

        mutableStateFlow.update {
            val accountSummaries = userState.toAccountSummaries()
            val activeAccountSummary = userState.toActiveAccountSummary()
            it.copy(
                userId = userState.activeUserId,
                initials = activeAccountSummary.initials,
                avatarColorString = activeAccountSummary.avatarColorHex,
                accountSummaries = accountSummaries,
                email = activeAccountSummary.email,
                isBiometricEnabled = userState.activeAccount.isBiometricsEnabled,
                vaultUnlockType = userState.activeAccount.vaultUnlockType,
                input = "",
                hasMasterPassword = userState.activeAccount.hasMasterPassword,
            )
        }

        // If the new account has biometrics available, automatically prompt for biometrics.
        promptForBiometricsIfAvailable()
    }

    private fun promptForBiometricsIfAvailable() {
        val cipher = biometricsEncryptionManager.getOrCreateCipher(state.userId)
        if (state.showBiometricLogin && cipher != null && !state.isFromLockFlow) {
            sendEvent(
                VaultUnlockEvent.PromptForBiometrics(
                    cipher = cipher,
                ),
            )
        }
    }
}

/**
 * Models state of the vault unlock screen.
 */
@Parcelize
data class VaultUnlockState(
    val accountSummaries: List<AccountSummary>,
    private val avatarColorString: String,
    val hideInput: Boolean,
    val initials: String,
    val email: String,
    val environmentUrl: String,
    val dialog: VaultUnlockDialog?,
    // We never want this saved since the input is sensitive data.
    @IgnoredOnParcel val input: String = "",
    val isBiometricsValid: Boolean,
    val isBiometricEnabled: Boolean,
    val showAccountMenu: Boolean,
    val showBiometricInvalidatedMessage: Boolean,
    val vaultUnlockType: VaultUnlockType,
    val userId: String,
    val getCredentialsRequest: GetCredentialsRequest? = null,
    val fido2CredentialAssertionRequest: Fido2CredentialAssertionRequest? = null,
    val createCredentialRequest: CreateCredentialRequest? = null,
    private val hasMasterPassword: Boolean,
    val isFromLockFlow: Boolean,
) : Parcelable {

    /**
     * The [Color] of the avatar.
     */
    val avatarColor: Color get() = avatarColorString.hexToColor()

    /**
     * Indicates if we should display the button login with biometrics.
     */
    val showBiometricLogin: Boolean get() = isBiometricEnabled && isBiometricsValid

    /**
     * Indicates if we want force focus on Master Password \ PIN input field and show keyboard.
     */
    val showKeyboard: Boolean get() = !showBiometricLogin && !hideInput

    /**
     * Returns the user ID present in the current CredentialManager request, or null when no request
     * is present.
     */
    val credentialManagerRequestUserId: String?
        get() = getCredentialsRequest?.userId ?: fido2CredentialAssertionRequest?.userId

    /**
     * Indicates if the Vault is being unlocked for a CredentialManager request.
     */
    val isUnlockingForCredentialManagerRequest: Boolean
        get() = getCredentialsRequest != null ||
            fido2CredentialAssertionRequest != null ||
            createCredentialRequest != null

    /**
     * If the user requires biometrics to be able to unlock the account.
     */
    val requiresBiometricsLogin: Boolean
        get() = when (vaultUnlockType) {
            VaultUnlockType.MASTER_PASSWORD -> !hasMasterPassword && isBiometricEnabled
            VaultUnlockType.PIN -> false
        }

    /**
     * Represents the various dialogs the vault unlock screen can display.
     */
    sealed class VaultUnlockDialog : Parcelable {
        /**
         * Represents an error dialog.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
            val throwable: Throwable? = null,
        ) : VaultUnlockDialog()

        /**
         * Represents a loading state dialog.
         */
        @Parcelize
        data object Loading : VaultUnlockDialog()

        /**
         * Show dialog for when biometrics the user has is no longer supported.
         */
        @Parcelize
        data object BiometricsNoLongerSupported : VaultUnlockDialog()
    }
}

/**
 * Models events for the vault unlock screen.
 */
sealed class VaultUnlockEvent {
    /**
     * Prompts the user for biometrics unlock.
     */
    data class PromptForBiometrics(val cipher: Cipher) : BackgroundEvent, VaultUnlockEvent()

    /**
     * Completes the get credentials request with an error response.
     */
    data class GetCredentialsError(val message: Text) : VaultUnlockEvent()

    /**
     * Completes the FIDO2 credential assertion request with an error response.
     */
    data class Fido2CredentialAssertionError(val message: Text) : VaultUnlockEvent()
}

/**
 * Models actions for the vault unlock screen.
 */
sealed class VaultUnlockAction {
    /**
     * The user has clicked the add account button.
     */
    data object AddAccountClick : VaultUnlockAction()

    /**
     * The user dismissed the currently displayed dialog.
     */
    data object DismissDialog : VaultUnlockAction()

    /**
     * The user has dismissed the biometrics not supported dialog
     */
    data object DismissBiometricsNoLongerSupportedDialog : VaultUnlockAction()

    /**
     * The user has clicked on the logout confirmation button.
     */
    data object ConfirmLogoutClick : VaultUnlockAction()

    /**
     * The user has modified the input.
     */
    data class InputChanged(
        val input: String,
    ) : VaultUnlockAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to lock
     * the associated account's vault.
     */
    data class LockAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultUnlockAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to log out
     * of that account.
     */
    data class LogoutAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultUnlockAction()

    /**
     * The user has clicked the an account to switch too.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultUnlockAction()

    /**
     * The user has clicked the biometrics button.
     */
    data object BiometricsUnlockClick : VaultUnlockAction()

    /**
     * The user has received a successful response from the biometrics call.
     */
    data class BiometricsUnlockSuccess(
        val cipher: Cipher,
    ) : VaultUnlockAction()

    /**
     * The user has attempted to login with biometrics too many times and has been locked out.
     */
    data object BiometricsLockOut : VaultUnlockAction()

    /**
     * The user has biometric unlock setup that is no longer valid.
     */
    data object BiometricsNoLongerSupported : VaultUnlockAction()

    /**
     * The user has clicked the unlock button.
     */
    data object UnlockClick : VaultUnlockAction()

    /**
     * Models actions that the [VaultUnlockViewModel] itself might send.
     */
    sealed class Internal : VaultUnlockAction() {
        /**
         * Indicates a vault unlock result has been received.
         */
        data class ReceiveVaultUnlockResult(
            val userId: String,
            val vaultUnlockResult: VaultUnlockResult,
            val isBiometricLogin: Boolean,
        ) : Internal()

        /**
         * Indicates a change in user state has been received.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
        ) : Internal()
    }
}
