package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

private const val KEY_STATE = "state"

/**
 * ViewModel for the VerifyPassword screen.
 *
 * This view model does not assume password verification is requested for the active user. Switching
 * to the provided account is deferred until password verification is explicitly requested. This is
 * done to reduce the number of times account switching is performed, since it can be a costly
 * operation.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VerifyPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val policyManager: PolicyManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VerifyPasswordState, VerifyPasswordEvent, VerifyPasswordAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toVerifyPasswordArgs()
            val account = authRepository
                .userStateFlow
                .value
                ?.accounts
                ?.firstOrNull { it.userId == args.userId }
                ?: throw IllegalStateException("Account not found")

            val singleAccount = !args.hasOtherAccounts

            val restrictedItemPolicyOrgIds = policyManager
                .getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
                .filter { it.isEnabled }
                .map { it.organizationId }

            VerifyPasswordState(
                title = if (account.hasMasterPassword) {
                    BitwardenString.verify_your_master_password.asText()
                } else {
                    BitwardenString.verify_your_account_email_address.asText()
                },
                subtext = BitwardenString
                    .enter_the_6_digit_code_that_was_emailed_to_the_address_below
                    .asText()
                    .takeUnless { account.hasMasterPassword },
                accountSummaryListItem = AccountSelectionListItem(
                    userId = args.userId,
                    avatarColorHex = account.avatarColorHex,
                    email = account.email,
                    initials = account.initials,
                    isItemRestricted = account
                        .organizations
                        .any { it.id in restrictedItemPolicyOrgIds },
                ),
                showResendCodeButton = !account.hasMasterPassword,
                hasOtherAccounts = !singleAccount,
            )
        },
) {

    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        if (stateFlow.value.showResendCodeButton) {
            viewModelScope.launch {
                sendAction(
                    VerifyPasswordAction.Internal.SendOtpCodeResultReceive(
                        result = authRepository.requestOneTimePasscode(),
                    ),
                )
            }
        }
    }

    override fun onCleared() {
        // TODO: This is required because there is an OS-level leak occurring that leaves the
        //   ViewModel in memory. We should remove this when that leak is fixed. (BIT-2287)
        mutableStateFlow.update { it.copy(input = "") }
        super.onCleared()
    }

    override fun handleAction(action: VerifyPasswordAction) {
        when (action) {
            VerifyPasswordAction.NavigateBackClick -> {
                handleNavigateBackClick()
            }

            VerifyPasswordAction.ContinueClick -> {
                handleContinueClick()
            }

            is VerifyPasswordAction.PasswordInputChangeReceive -> {
                handlePasswordInputChange(action)
            }

            VerifyPasswordAction.DismissDialog -> {
                handleDismissDialog()
            }

            VerifyPasswordAction.ResendCodeClick -> {
                handleResendCodeClick()
            }

            is VerifyPasswordAction.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleNavigateBackClick() {
        if (state.hasOtherAccounts) {
            sendEvent(VerifyPasswordEvent.NavigateBack)
        } else {
            sendEvent(VerifyPasswordEvent.CancelExport)
        }
    }

    private fun handleContinueClick() {
        if (state.input.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = VerifyPasswordState.DialogState.General(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.validation_field_required.asText(
                            BitwardenString.master_password.asText(),
                        ),
                    ),
                )
            }
            return
        }

        mutableStateFlow.update {
            it.copy(
                dialog = VerifyPasswordState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
        }

        if (authRepository.activeUserId != state.accountSummaryListItem.userId) {
            switchAccountAndVerifyPassword()
        } else {
            validatePassword()
        }
    }

    private fun handlePasswordInputChange(
        action: VerifyPasswordAction.PasswordInputChangeReceive,
    ) {
        mutableStateFlow.update { it.copy(input = action.input) }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleResendCodeClick() {
        mutableStateFlow.update {
            it.copy(
                dialog = VerifyPasswordState.DialogState.Loading(
                    message = BitwardenString.sending.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                VerifyPasswordAction.Internal.SendOtpCodeResultReceive(
                    result = authRepository.requestOneTimePasscode(),
                ),
            )
        }
    }

    private fun handleInternalAction(action: VerifyPasswordAction.Internal) {
        when (action) {
            is VerifyPasswordAction.Internal.ValidatePasswordResultReceive -> {
                handleValidatePasswordResultReceive(action)
            }

            is VerifyPasswordAction.Internal.UnlockVaultResultReceive -> {
                handleUnlockVaultResultReceive(action)
            }

            is VerifyPasswordAction.Internal.SendOtpCodeResultReceive -> {
                handleSendOtpCodeResultReceive(action)
            }

            is VerifyPasswordAction.Internal.VerifyOtpResultReceive -> {
                handleVerifyOtpResultReceive(action)
            }
        }
    }

    private fun handleValidatePasswordResultReceive(
        action: VerifyPasswordAction.Internal.ValidatePasswordResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (action.result) {
            is ValidatePasswordResult.Success -> {
                if (action.result.isValid) {
                    clearInputs()
                    sendEvent(
                        VerifyPasswordEvent.PasswordVerified(
                            state.accountSummaryListItem.userId,
                        ),
                    )
                } else {
                    showInvalidMasterPasswordDialog()
                }
            }

            is ValidatePasswordResult.Error -> {
                showGenericErrorDialog(throwable = action.result.error)
            }
        }
    }

    private fun handleUnlockVaultResultReceive(
        action: VerifyPasswordAction.Internal.UnlockVaultResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (action.vaultUnlockResult) {
            VaultUnlockResult.Success -> {
                // A successful unlock result means the provided password is correct so we can
                // consider the password verified and send the event.
                clearInputs()
                sendEvent(
                    VerifyPasswordEvent.PasswordVerified(
                        state.accountSummaryListItem.userId,
                    ),
                )
            }

            is VaultUnlockResult.AuthenticationError -> {
                showInvalidMasterPasswordDialog(
                    throwable = action.vaultUnlockResult.error,
                )
            }

            is VaultUnlockResult.InvalidStateError,
            is VaultUnlockResult.BiometricDecodingError,
            is VaultUnlockResult.GenericError,
                -> {
                showGenericErrorDialog(throwable = action.vaultUnlockResult.error)
            }
        }
    }

    private fun handleSendOtpCodeResultReceive(
        action: VerifyPasswordAction.Internal.SendOtpCodeResultReceive,
    ) {
        when (val result = action.result) {
            is RequestOtpResult.Error -> {
                val message = result.message?.asText()
                    ?: BitwardenString.generic_error_message.asText()
                mutableStateFlow.update {
                    it.copy(
                        dialog = VerifyPasswordState.DialogState.General(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = message,
                        ),
                    )
                }
            }

            RequestOtpResult.Success -> {
                mutableStateFlow.update {
                    it.copy(dialog = null)
                }
                sendEvent(
                    VerifyPasswordEvent.ShowSnackbar(BitwardenString.code_sent.asText()),
                )
            }
        }
    }

    private fun handleVerifyOtpResultReceive(
        action: VerifyPasswordAction.Internal.VerifyOtpResultReceive,
    ) {
        when (action.result) {
            is VerifyOtpResult.Verified -> {
                mutableStateFlow.update { it.copy(input = "", dialog = null) }
                sendEvent(
                    VerifyPasswordEvent.PasswordVerified(
                        state.accountSummaryListItem.userId,
                    ),
                )
            }

            is VerifyOtpResult.NotVerified -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VerifyPasswordState.DialogState.General(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.invalid_verification_code.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun switchAccountAndVerifyPassword() {
        val switchAccountResult = authRepository
            .switchAccount(userId = state.accountSummaryListItem.userId)

        when (switchAccountResult) {
            SwitchAccountResult.AccountSwitched -> validatePassword()
            SwitchAccountResult.NoChange -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VerifyPasswordState.DialogState.General(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun validatePassword() {
        val userId = state.accountSummaryListItem.userId

        viewModelScope.launch {
            if (state.showResendCodeButton) {
                sendAction(
                    VerifyPasswordAction.Internal.VerifyOtpResultReceive(
                        result = authRepository.verifyOneTimePasscode(
                            oneTimePasscode = state.input,
                        ),
                    ),
                )
            } else if (vaultRepository.isVaultUnlocked(userId)) {
                // If the vault is already unlocked, validate the password directly.
                sendAction(
                    VerifyPasswordAction.Internal.ValidatePasswordResultReceive(
                        authRepository.validatePassword(password = state.input),
                    ),
                )
            } else {
                // Otherwise, unlock the vault with the provided password. The unlock result will
                // indicate whether the password is correct.
                sendAction(
                    VerifyPasswordAction.Internal.UnlockVaultResultReceive(
                        vaultRepository
                            .unlockVaultWithMasterPassword(masterPassword = state.input),
                    ),
                )
            }
        }
    }

    private fun showInvalidMasterPasswordDialog(
        throwable: Throwable? = null,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = VerifyPasswordState.DialogState.General(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.invalid_master_password.asText(),
                    error = throwable,
                ),
            )
        }
    }

    private fun showGenericErrorDialog(throwable: Throwable?) {
        mutableStateFlow.update {
            it.copy(
                dialog = VerifyPasswordState.DialogState.General(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    error = throwable,
                ),
            )
        }
    }

    private fun clearInputs() {
        mutableStateFlow.update { it.copy(input = "") }
    }
}

/**
 * Represents the state of the VerifyPassword screen.
 * @param accountSummaryListItem The account summary to display.
 * @param input The current password input.
 * @param dialog The current dialog state, or null if no dialog is shown.
 * @param showResendCodeButton Whether to show the send code button.
 */
@Parcelize
data class VerifyPasswordState(
    val accountSummaryListItem: AccountSelectionListItem,
    val title: Text,
    val subtext: Text?,
    val hasOtherAccounts: Boolean,
    // We never want this saved since the input is sensitive data.
    @IgnoredOnParcel
    val input: String = "",
    val dialog: DialogState? = null,
    val showResendCodeButton: Boolean = false,
) : Parcelable {

    /**
     * Whether the unlock button should be enabled.
     */
    val isContinueButtonEnabled: Boolean
        get() = input.isNotBlank() && dialog !is DialogState.Loading

    /**
     * Represents the state of a dialog.
     */
    @Parcelize
    sealed class DialogState : Parcelable {
        /**
         * Represents a general dialog with a title, message, and optional error.
         * @param title The dialog title.
         * @param message The dialog message.
         * @param error An optional error associated with the dialog.
         */
        data class General(
            val title: Text,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with a message.
         * @param message The loading message.
         */
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Represents events that can be emitted from the VerifyPasswordViewModel.
 */
sealed class VerifyPasswordEvent {
    /**
     * Indicates a request to navigate back.
     */
    data object NavigateBack : VerifyPasswordEvent()

    /**
     * Indicates that the password has been successfully verified.
     * @param userId The ID of the user whose password was verified.
     */
    data class PasswordVerified(val userId: String) : VerifyPasswordEvent()

    /**
     * Cancel the export request.
     */
    data object CancelExport : VerifyPasswordEvent()

    /**
     * Show a snackbar with the given data.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : VerifyPasswordEvent() {
        constructor(
            message: Text,
            messageHeader: Text? = null,
            actionLabel: Text? = null,
            withDismissAction: Boolean = false,
        ) : this(
            data = BitwardenSnackbarData(
                message = message,
                messageHeader = messageHeader,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            ),
        )
    }
}

/**
 * Represents actions that can be handled by the VerifyPasswordViewModel.
 */
sealed class VerifyPasswordAction {
    /**
     * Represents a click on the navigate back button.
     */
    data object NavigateBackClick : VerifyPasswordAction()

    /**
     * Represents a click on the Continue button.
     */
    data object ContinueClick : VerifyPasswordAction()

    /**
     * Dismiss the current dialog.
     */
    data object DismissDialog : VerifyPasswordAction()

    /**
     * Represents a click on the resend code button.
     */
    data object ResendCodeClick : VerifyPasswordAction()

    /**
     * Represents a change in the password input.
     * @param input The new password input.
     */
    data class PasswordInputChangeReceive(val input: String) : VerifyPasswordAction()

    /**
     * Represents internal actions that the VerifyPasswordViewModel itself may send.
     */
    sealed class Internal : VerifyPasswordAction() {

        /**
         * Represents a result of validating the password.
         * @param result The result of validating the password.
         */
        data class ValidatePasswordResultReceive(
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Represents a result of unlocking the vault.
         * @param vaultUnlockResult The result of unlocking the vault.
         */
        data class UnlockVaultResultReceive(
            val vaultUnlockResult: VaultUnlockResult,
        ) : Internal()

        /**
         * Represents a result of requesting an OTP code.
         * @param result The result of requesting an OTP code.
         */
        data class SendOtpCodeResultReceive(val result: RequestOtpResult) : Internal()

        /**
         * Represents a result of verifying an OTP code.
         * @param result The result of verifying an OTP code.
         */
        data class VerifyOtpResultReceive(val result: VerifyOtpResult) : Internal()
    }
}
