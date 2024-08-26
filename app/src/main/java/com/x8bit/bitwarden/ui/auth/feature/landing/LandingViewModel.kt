package com.x8bit.bitwarden.ui.auth.feature.landing

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.isValidEmail
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial landing screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    private val featureFlagManager: FeatureFlagManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LandingState, LandingEvent, LandingAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LandingState(
            emailInput = authRepository.rememberedEmailAddress.orEmpty(),
            isContinueButtonEnabled = authRepository.rememberedEmailAddress != null,
            isRememberMeEnabled = authRepository.rememberedEmailAddress != null,
            selectedEnvironmentType = environmentRepository.environment.type,
            selectedEnvironmentLabel = environmentRepository.environment.label,
            dialog = null,
            accountSummaries = authRepository.userStateFlow.value?.toAccountSummaries().orEmpty(),
        ),
) {

    /**
     * Returns the [AccountSummary] from the current state that matches the current email input and
     * the the current environment, or `null` if there is no match.
     */
    private val matchingAccountSummary: AccountSummary?
        get() {
            val currentEmail = state.emailInput
            val currentEnvironmentLabel = state.selectedEnvironmentLabel
            val accountSummaries = state.accountSummaries
            return accountSummaries
                .find {
                    it.email == currentEmail &&
                        it.environmentLabel == currentEnvironmentLabel
                }
                ?.takeUnless { !it.isLoggedIn }
        }

    init {
        // As state updates:
        // - write to saved state handle
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
            }
            .launchIn(viewModelScope)

        // Listen for changes in environment triggered both by this VM and externally.
        environmentRepository
            .environmentStateFlow
            .onEach { environment ->
                sendAction(
                    LandingAction.Internal.UpdatedEnvironmentReceive(environment = environment),
                )
            }
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .map { userState ->
                userState?.activeAccount?.let(::mapToInternalActionOrNull)
            }
            .onEach { action ->
                action?.let(::handleAction)
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LandingAction) {
        when (action) {
            is LandingAction.LockAccountClick -> handleLockAccountClicked(action)
            is LandingAction.LogoutAccountClick -> handleLogoutAccountClicked(action)
            is LandingAction.SwitchAccountClick -> handleSwitchAccountClicked(action)
            is LandingAction.ConfirmSwitchToMatchingAccountClick -> {
                handleConfirmSwitchToMatchingAccountClicked(action)
            }

            is LandingAction.ContinueButtonClick -> handleContinueButtonClicked()
            LandingAction.CreateAccountClick -> handleCreateAccountClicked()
            is LandingAction.DialogDismiss -> handleDialogDismiss()
            is LandingAction.RememberMeToggle -> handleRememberMeToggled(action)
            is LandingAction.EmailInputChanged -> handleEmailInputChanged(action)
            is LandingAction.EnvironmentTypeSelect -> handleEnvironmentTypeSelect(action)
            is LandingAction.Internal -> handleInternalActions(action)
        }
    }

    private fun handleInternalActions(action: LandingAction.Internal) {
        when (action) {
            is LandingAction.Internal.UpdateEmailState -> handleInternalEmailStateUpdate(action)
            is LandingAction.Internal.UpdatedEnvironmentReceive -> {
                handleUpdatedEnvironmentReceive(action)
            }
        }
    }

    private fun handleLockAccountClicked(action: LandingAction.LockAccountClick) {
        vaultRepository.lockVault(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClicked(action: LandingAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
    }

    private fun handleSwitchAccountClicked(action: LandingAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleConfirmSwitchToMatchingAccountClicked(
        action: LandingAction.ConfirmSwitchToMatchingAccountClick,
    ) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleEmailInputChanged(action: LandingAction.EmailInputChanged) {
        updateEmailInput(action.input)
    }

    private fun handleInternalEmailStateUpdate(action: LandingAction.Internal.UpdateEmailState) {
        updateEmailInput(action.emailInput)
    }

    private fun updateEmailInput(updatedInput: String) {
        mutableStateFlow.update {
            it.copy(
                emailInput = updatedInput,
                isContinueButtonEnabled = updatedInput.isNotBlank(),
            )
        }
    }

    private fun handleContinueButtonClicked() {
        if (!state.emailInput.isValidEmail()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = LandingState.DialogState.Error(
                        message = R.string.invalid_email.asText(),
                    ),
                )
            }
            return
        }

        matchingAccountSummary?.let { accountSummary ->
            mutableStateFlow.update {
                it.copy(
                    dialog = LandingState.DialogState.AccountAlreadyAdded(
                        accountSummary = accountSummary,
                    ),
                )
            }
            return
        }

        val email = state.emailInput
        val isRememberMeEnabled = state.isRememberMeEnabled

        // Update the remembered email address
        authRepository.rememberedEmailAddress = email.takeUnless { !isRememberMeEnabled }

        sendEvent(LandingEvent.NavigateToLogin(email))
    }

    private fun handleCreateAccountClicked() {
        val navigationEvent =
            if (featureFlagManager.getFeatureFlag(key = FlagKey.EmailVerification)) {
                LandingEvent.NavigateToStartRegistration
            } else {
                LandingEvent.NavigateToCreateAccount
            }
        sendEvent(navigationEvent)
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleRememberMeToggled(action: LandingAction.RememberMeToggle) {
        mutableStateFlow.update { it.copy(isRememberMeEnabled = action.isChecked) }
    }

    private fun handleEnvironmentTypeSelect(action: LandingAction.EnvironmentTypeSelect) {
        val environment = when (action.environmentType) {
            Environment.Type.US -> Environment.Us
            Environment.Type.EU -> Environment.Eu
            Environment.Type.SELF_HOSTED -> {
                // Launch the self-hosted screen and select the full environment details there.
                sendEvent(LandingEvent.NavigateToEnvironment)
                return
            }
        }

        // Update the environment in the repo; the VM state will update accordingly because it is
        // listening for changes.
        environmentRepository.environment = environment
    }

    private fun handleUpdatedEnvironmentReceive(
        action: LandingAction.Internal.UpdatedEnvironmentReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                selectedEnvironmentType = action.environment.type,
                selectedEnvironmentLabel = action.environment.label,
            )
        }
    }

    /**
     * If the user state account is changed to an active but not "logged in" account we can
     * pre-populate the email field with this account.
     */
    private fun mapToInternalActionOrNull(
        activeAccount: UserState.Account,
    ): LandingAction.Internal.UpdateEmailState? {
        val activeUserNotLoggedIn = !activeAccount.isLoggedIn
        val noPendingAdditions = !authRepository.hasPendingAccountAddition
        return LandingAction.Internal.UpdateEmailState(activeAccount.email)
            .takeIf { activeUserNotLoggedIn && noPendingAdditions }
    }
}

/**
 * Models state of the landing screen.
 */
@Parcelize
data class LandingState(
    val emailInput: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
    val selectedEnvironmentType: Environment.Type,
    val selectedEnvironmentLabel: String,
    val dialog: DialogState?,
    val accountSummaries: List<AccountSummary>,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dialog indicating that the current email matches the existing
         * [accountSummary].
         */
        @Parcelize
        data class AccountAlreadyAdded(
            val accountSummary: AccountSummary,
        ) : DialogState()

        /**
         * Represents an error dialog with the given [message].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the landing screen.
 */
sealed class LandingEvent {
    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateToCreateAccount : LandingEvent()

    /**
     * Navigates to the Start Registration screen.
     */
    data object NavigateToStartRegistration : LandingEvent()

    /**
     * Navigates to the Login screen with the given email address and region label.
     */
    data class NavigateToLogin(
        val emailAddress: String,
    ) : LandingEvent()

    /**
     * Navigates to the self-hosted/custom environment screen.
     */
    data object NavigateToEnvironment : LandingEvent()
}

/**
 * Models actions for the landing screen.
 */
sealed class LandingAction {

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to lock
     * the associated account's vault.
     */
    data class LockAccountClick(
        val accountSummary: AccountSummary,
    ) : LandingAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to log out
     * of that account.
     */
    data class LogoutAccountClick(
        val accountSummary: AccountSummary,
    ) : LandingAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to switch
     * to it.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : LandingAction()

    /**
     * Indicates the user has confirmed they would like to switch to the existing [accountSummary].
     */
    data class ConfirmSwitchToMatchingAccountClick(
        val accountSummary: AccountSummary,
    ) : LandingAction()

    /**
     * Indicates that the continue button has been clicked and the app should navigate to Login.
     */
    data object ContinueButtonClick : LandingAction()

    /**
     * Indicates that the Create Account text was clicked.
     */
    data object CreateAccountClick : LandingAction()

    /**
     * Indicates that a dialog is attempting to be dismissed.
     */
    data object DialogDismiss : LandingAction()

    /**
     * Indicates that the Remember Me switch has been toggled.
     */
    data class RememberMeToggle(
        val isChecked: Boolean,
    ) : LandingAction()

    /**
     * Indicates that the input on the email field has changed.
     */
    data class EmailInputChanged(
        val input: String,
    ) : LandingAction()

    /**
     * Indicates that the selection from the region drop down has changed.
     */
    data class EnvironmentTypeSelect(
        val environmentType: Environment.Type,
    ) : LandingAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : LandingAction() {

        /**
         * Indicates that there has been a change in [environment].
         */
        data class UpdatedEnvironmentReceive(
            val environment: Environment,
        ) : Internal()

        /**
         * Internal action to update the email input state from a non-user action
         */
        data class UpdateEmailState(val emailInput: String) : Internal()
    }
}
