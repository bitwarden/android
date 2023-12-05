package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.util.labelOrBaseUrlHost
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial vault unlock screen.
 */
@HiltViewModel
class VaultUnlockViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val vaultRepo: VaultRepository,
    environmentRepo: EnvironmentRepository,
) : BaseViewModel<VaultUnlockState, VaultUnlockEvent, VaultUnlockAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val accountSummaries = userState.toAccountSummaries()
        val activeAccountSummary = userState.toActiveAccountSummary()
        VaultUnlockState(
            accountSummaries = accountSummaries,
            avatarColorString = activeAccountSummary.avatarColorHex,
            initials = activeAccountSummary.initials,
            email = activeAccountSummary.email,
            dialog = null,
            environmentUrl = environmentRepo.environment.labelOrBaseUrlHost,
            passwordInput = "",
        )
    },
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        environmentRepo
            .environmentStateFlow
            .onEach { environment ->
                mutableStateFlow.update {
                    it.copy(environmentUrl = environment.labelOrBaseUrlHost)
                }
            }
            .launchIn(viewModelScope)
        authRepository
            .userStateFlow
            .onEach {
                sendAction(VaultUnlockAction.Internal.UserStateUpdateReceive(userState = it))
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultUnlockAction) {
        when (action) {
            VaultUnlockAction.AddAccountClick -> handleAddAccountClick()
            VaultUnlockAction.DismissDialog -> handleDismissDialog()
            VaultUnlockAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
            is VaultUnlockAction.PasswordInputChanged -> handlePasswordInputChanged(action)
            is VaultUnlockAction.SwitchAccountClick -> handleSwitchAccountClick(action)
            VaultUnlockAction.UnlockClick -> handleUnlockClick()
            is VaultUnlockAction.Internal.ReceiveVaultUnlockResult -> {
                handleReceiveVaultUnlockResult(action)
            }

            is VaultUnlockAction.Internal.UserStateUpdateReceive -> {
                handleUserStateUpdateReceive(action)
            }
        }
    }

    private fun handleAddAccountClick() {
        authRepository.specialCircumstance = UserState.SpecialCircumstance.PendingAccountAddition
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleConfirmLogoutClick() {
        authRepository.logout()
    }

    private fun handlePasswordInputChanged(action: VaultUnlockAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(passwordInput = action.passwordInput)
        }
    }

    private fun handleSwitchAccountClick(action: VaultUnlockAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleUnlockClick() {
        mutableStateFlow.update { it.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading) }
        viewModelScope.launch {
            val vaultUnlockResult = vaultRepo.unlockVaultAndSyncForCurrentUser(
                mutableStateFlow.value.passwordInput,
            )
            sendAction(VaultUnlockAction.Internal.ReceiveVaultUnlockResult(vaultUnlockResult))
        }
    }

    private fun handleReceiveVaultUnlockResult(
        action: VaultUnlockAction.Internal.ReceiveVaultUnlockResult,
    ) {
        when (action.vaultUnlockResult) {
            VaultUnlockResult.AuthenticationError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultUnlockState.VaultUnlockDialog.Error(
                            R.string.invalid_master_password.asText(),
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
                            R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            VaultUnlockResult.Success -> {
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

        mutableStateFlow.update {
            val accountSummaries = userState.toAccountSummaries()
            val activeAccountSummary = userState.toActiveAccountSummary()
            it.copy(
                initials = activeAccountSummary.initials,
                avatarColorString = activeAccountSummary.avatarColorHex,
                accountSummaries = accountSummaries,
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
    val initials: String,
    val email: String,
    val environmentUrl: Text,
    val dialog: VaultUnlockDialog?,
    val passwordInput: String,
) : Parcelable {

    /**
     * The [Color] of the avatar.
     */
    val avatarColor: Color get() = avatarColorString.hexToColor()

    /**
     * Represents the various dialogs the vault unlock screen can display.
     */
    sealed class VaultUnlockDialog : Parcelable {
        /**
         * Represents an error dialog.
         */
        @Parcelize
        data class Error(
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
     * The user has modified the password input.
     */
    data class PasswordInputChanged(
        val passwordInput: String,
    ) : VaultUnlockAction()

    /**
     * The user has clicked the an account to switch too.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultUnlockAction()

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
            val vaultUnlockResult: VaultUnlockResult,
        ) : Internal()

        /**
         * Indicates a change in user state has been received.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
        ) : Internal()
    }
}
