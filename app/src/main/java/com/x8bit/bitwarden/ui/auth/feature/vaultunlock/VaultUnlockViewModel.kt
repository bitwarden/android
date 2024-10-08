package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.emptyInputDialogMessage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenErrorMessage
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.BackgroundEvent
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
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
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultUnlockViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepo: VaultRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val fido2CredentialManager: Fido2CredentialManager,
    environmentRepo: EnvironmentRepository,
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
            authRepository.logout()
        }
        VaultUnlockState(
            accountSummaries = accountSummaries,
            avatarColorString = activeAccountSummary.avatarColorHex,
            hideInput = hasNoMasterPassword && vaultUnlockType == VaultUnlockType.MASTER_PASSWORD,
            initials = activeAccountSummary.initials,
            email = activeAccountSummary.email,
            dialog = null,
            environmentUrl = environmentRepo.environment.label,
            input = "",
            isBiometricEnabled = activeAccount.isBiometricsEnabled,
            isBiometricsValid = isBiometricsValid,
            showAccountMenu = VaultUnlockArgs(savedStateHandle).unlockType == UnlockType.STANDARD,
            showBiometricInvalidatedMessage = false,
            vaultUnlockType = vaultUnlockType,
            userId = userState.activeUserId,
            // TODO: [PM-13075] Handle Fido2GetCredentialsRequest special circumstance
            fido2GetCredentialsRequest = null,
            // TODO: [PM-13076] Handle Fido2CredentialAssertionRequest special circumstance
            fido2CredentialAssertionRequest = null,
        )
    },
) {
    init {
        environmentRepo
            .environmentStateFlow
            .onEach { environment ->
                mutableStateFlow.update {
                    it.copy(environmentUrl = environment.label)
                }
            }
            .launchIn(viewModelScope)
        authRepository
            .userStateFlow
            .onEach {
                sendAction(VaultUnlockAction.Internal.UserStateUpdateReceive(userState = it))
            }
            .launchIn(viewModelScope)

        promptForBiometricsIfAvailable()
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
        }
    }

    private fun handleAddAccountClick() {
        authRepository.hasPendingAccountAddition = true
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
        when {
            state.fido2GetCredentialsRequest != null -> {
                sendEvent(VaultUnlockEvent.Fido2GetCredentialsError)
            }

            state.fido2CredentialAssertionRequest != null -> {
                sendEvent(VaultUnlockEvent.Fido2CredentialAssertionError)
            }

            else -> Unit
        }
    }

    private fun handleConfirmLogoutClick() {
        authRepository.logout()
    }

    private fun handleInputChanged(action: VaultUnlockAction.InputChanged) {
        mutableStateFlow.update {
            it.copy(input = action.input)
        }
    }

    private fun handleLockAccountClick(action: VaultUnlockAction.LockAccountClick) {
        vaultRepo.lockVault(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClick(action: VaultUnlockAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
    }

    private fun handleSwitchAccountClick(action: VaultUnlockAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleBiometricsLockOut() {
        authRepository.logout()
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
        if (!biometricsEncryptionManager.isBiometricIntegrityValid(activeUserId, action.cipher)) {
            mutableStateFlow.update { it.copy(isBiometricsValid = false) }
            return
        }
        mutableStateFlow.update { it.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading) }
        viewModelScope.launch {
            val vaultUnlockResult = vaultRepo.unlockVaultWithBiometrics()
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
                        title = R.string.an_error_has_occurred.asText(),
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
        fido2CredentialManager.isUserVerified =
            action.vaultUnlockResult is VaultUnlockResult.Success

        when (action.vaultUnlockResult) {
            is VaultUnlockResult.AuthenticationError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = if (action.isBiometricLogin) {
                                R.string.generic_error_message.asText()
                            } else {
                                state.vaultUnlockType.unlockScreenErrorMessage
                            },
                        ),
                    )
                }
            }

            VaultUnlockResult.GenericError,
            VaultUnlockResult.InvalidStateError,
                -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            VaultUnlockResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                if (state.isBiometricEnabled && !state.isBiometricsValid) {
                    biometricsEncryptionManager.setupBiometrics(action.userId)
                }
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

        // If the Vault is already unlocked, do nothing.
        if (userState.activeAccount.isVaultUnlocked) return
        // If the user state has changed to add a new account, do nothing.
        if (userState.hasPendingAccountAddition) return

        mutableStateFlow.update {
            val accountSummaries = userState.toAccountSummaries()
            val activeAccountSummary = userState.toActiveAccountSummary()
            it.copy(
                initials = activeAccountSummary.initials,
                avatarColorString = activeAccountSummary.avatarColorHex,
                accountSummaries = accountSummaries,
                email = activeAccountSummary.email,
                isBiometricEnabled = userState.activeAccount.isBiometricsEnabled,
                vaultUnlockType = userState.activeAccount.vaultUnlockType,
                input = "",
            )
        }

        // If the new account has biometrics available, automatically prompt for biometrics.
        promptForBiometricsIfAvailable()
    }

    private fun promptForBiometricsIfAvailable() {
        val cipher = biometricsEncryptionManager.getOrCreateCipher(state.userId)
        if (state.showBiometricLogin && cipher != null) {
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
    val fido2GetCredentialsRequest: Fido2GetCredentialsRequest? = null,
    val fido2CredentialAssertionRequest: Fido2CredentialAssertionRequest? = null,
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
        ) : VaultUnlockDialog()

        /**
         * Represents a loading state dialog.
         */
        @Parcelize
        data object Loading : VaultUnlockDialog()
    }
}

/**
 * Models events for the vault unlock screen.
 */
sealed class VaultUnlockEvent {
    /**
     * Displays a toast to the user.
     */
    data class ShowToast(
        val text: Text,
    ) : VaultUnlockEvent()

    /**
     * Prompts the user for biometrics unlock.
     */
    data class PromptForBiometrics(val cipher: Cipher) : BackgroundEvent, VaultUnlockEvent()

    /**
     * Completes the FIDO2 get credentials request with an error response.
     */
    data object Fido2GetCredentialsError : VaultUnlockEvent()

    /**
     * Completes the FIDO2 credential assertion request with an error response.
     */
    data object Fido2CredentialAssertionError : VaultUnlockEvent()
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
        val cipher: Cipher?,
    ) : VaultUnlockAction()

    /**
     * The user has attempted to login with biometrics too many times and has been locked out.
     */
    data object BiometricsLockOut : VaultUnlockAction()

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
