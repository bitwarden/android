package com.x8bit.bitwarden

import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.util.getCompleteRegistrationDataIntentOrNull
import com.x8bit.bitwarden.data.auth.util.getPasswordlessRequestDataIntentOrNull
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2CredentialRequestOrNull
import com.x8bit.bitwarden.data.autofill.fido2.util.getFido2GetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.util.getAutofillSaveItemOrNull
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.isMyVaultShortcut
import com.x8bit.bitwarden.ui.platform.util.isPasswordGeneratorShortcut
import com.x8bit.bitwarden.ui.vault.util.getTotpDataOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val SPECIAL_CIRCUMSTANCE_KEY = "special-circumstance"

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MainViewModel @Inject constructor(
    accessibilitySelectionManager: AccessibilitySelectionManager,
    autofillSelectionManager: AutofillSelectionManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val garbageCollectionManager: GarbageCollectionManager,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val intentManager: IntentManager,
    settingsRepository: SettingsRepository,
    vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : BaseViewModel<MainState, MainEvent, MainAction>(
    initialState = MainState(
        theme = settingsRepository.appTheme,
        isScreenCaptureAllowed = settingsRepository.isScreenCaptureAllowed,
    ),
) {
    private var specialCircumstance: SpecialCircumstance?
        get() = savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY]
        set(value) {
            savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY] = value
        }

    init {
        // Immediately restore the special circumstance if we have one and then listen for changes
        specialCircumstanceManager.specialCircumstance = specialCircumstance

        specialCircumstanceManager
            .specialCircumstanceStateFlow
            .onEach { specialCircumstance = it }
            .launchIn(viewModelScope)

        accessibilitySelectionManager
            .accessibilitySelectionFlow
            .map { MainAction.Internal.AccessibilitySelectionReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        autofillSelectionManager
            .autofillSelectionFlow
            .onEach { trySendAction(MainAction.Internal.AutofillSelectionReceive(it)) }
            .launchIn(viewModelScope)

        settingsRepository
            .appThemeStateFlow
            .onEach { trySendAction(MainAction.Internal.ThemeUpdate(it)) }
            .launchIn(viewModelScope)

        settingsRepository
            .isScreenCaptureAllowedStateFlow
            .map { MainAction.Internal.ScreenCaptureUpdate(it) }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .drop(count = 1)
            // Trigger an action whenever the current user changes or we go into/out of a pending
            // account state (which acts like switching to a temporary user).
            .map { it?.activeUserId to it?.hasPendingAccountAddition }
            .distinctUntilChanged()
            .onEach {
                // Switching between account states often involves some kind of animation (ex:
                // account switcher) that we might want to give time to finish before triggering
                // a refresh.
                @Suppress("MagicNumber")
                delay(500)
                trySendAction(MainAction.Internal.CurrentUserStateChange)
            }
            .launchIn(viewModelScope)

        vaultRepository
            .vaultStateEventFlow
            .onEach {
                when (it) {
                    is VaultStateEvent.Locked -> {
                        // Similar to account switching, triggering this action too soon can
                        // interfere with animations or navigation logic, so we will delay slightly.
                        @Suppress("MagicNumber")
                        delay(500)
                        trySendAction(MainAction.Internal.VaultUnlockStateChange)
                    }

                    is VaultStateEvent.Unlocked -> Unit
                }
            }
            .launchIn(viewModelScope)

        // On app launch, mark all active users as having previously logged in.
        // This covers any users who are active prior to this value being recorded.
        viewModelScope.launch {
            val userState = authRepository
                .userStateFlow
                .first()
            userState
                ?.accounts
                ?.forEach {
                    settingsRepository.storeUserHasLoggedInValue(it.userId)
                }
        }
    }

    override fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.Internal.AccessibilitySelectionReceive -> {
                handleAccessibilitySelectionReceive(action)
            }

            is MainAction.Internal.AutofillSelectionReceive -> {
                handleAutofillSelectionReceive(action)
            }

            is MainAction.Internal.CurrentUserStateChange -> handleCurrentUserStateChange()
            is MainAction.Internal.ScreenCaptureUpdate -> handleScreenCaptureUpdate(action)
            is MainAction.Internal.ThemeUpdate -> handleAppThemeUpdated(action)
            is MainAction.Internal.VaultUnlockStateChange -> handleVaultUnlockStateChange()
            is MainAction.ReceiveFirstIntent -> handleFirstIntentReceived(action)
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(action)
            MainAction.OpenDebugMenu -> handleOpenDebugMenu()
        }
    }

    private fun handleOpenDebugMenu() {
        sendEvent(MainEvent.NavigateToDebugMenu)
    }

    private fun handleAccessibilitySelectionReceive(
        action: MainAction.Internal.AccessibilitySelectionReceive,
    ) {
        sendEvent(MainEvent.CompleteAccessibilityAutofill(cipherView = action.cipherView))
    }

    private fun handleAutofillSelectionReceive(
        action: MainAction.Internal.AutofillSelectionReceive,
    ) {
        sendEvent(MainEvent.CompleteAutofill(cipherView = action.cipherView))
    }

    private fun handleCurrentUserStateChange() {
        recreateUiAndGarbageCollect()
    }

    private fun handleScreenCaptureUpdate(action: MainAction.Internal.ScreenCaptureUpdate) {
        mutableStateFlow.update { it.copy(isScreenCaptureAllowed = action.isScreenCaptureEnabled) }
    }

    private fun handleAppThemeUpdated(action: MainAction.Internal.ThemeUpdate) {
        mutableStateFlow.update { it.copy(theme = action.theme) }
    }

    private fun handleVaultUnlockStateChange() {
        recreateUiAndGarbageCollect()
    }

    private fun handleFirstIntentReceived(action: MainAction.ReceiveFirstIntent) {
        handleIntent(
            intent = action.intent,
            isFirstIntent = true,
        )
    }

    private fun handleNewIntentReceived(action: MainAction.ReceiveNewIntent) {
        handleIntent(
            intent = action.intent,
            isFirstIntent = false,
        )
    }

    @Suppress("LongMethod")
    private fun handleIntent(
        intent: Intent,
        isFirstIntent: Boolean,
    ) {
        val passwordlessRequestData = intent.getPasswordlessRequestDataIntentOrNull()
        val autofillSaveItem = intent.getAutofillSaveItemOrNull()
        val autofillSelectionData = intent.getAutofillSelectionDataOrNull()
        val shareData = intentManager.getShareDataFromIntent(intent)
        val totpData = intent.getTotpDataOrNull()
        val hasGeneratorShortcut = intent.isPasswordGeneratorShortcut
        val hasVaultShortcut = intent.isMyVaultShortcut
        val fido2CredentialRequestData = intent.getFido2CredentialRequestOrNull()
        val completeRegistrationData = intent.getCompleteRegistrationDataIntentOrNull()
        val fido2CredentialAssertionRequest = intent.getFido2AssertionRequestOrNull()
        val fido2GetCredentialsRequest = intent.getFido2GetCredentialsRequestOrNull()
        when {
            passwordlessRequestData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.PasswordlessRequest(
                        passwordlessRequestData = passwordlessRequestData,
                        // Allow users back into the already-running app when completing the
                        // autofill task when this is not the first intent.
                        shouldFinishWhenComplete = isFirstIntent,
                    )
            }

            completeRegistrationData != null -> {
                handleCompleteRegistrationData(completeRegistrationData)
            }

            autofillSaveItem != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AutofillSave(
                        autofillSaveItem = autofillSaveItem,
                    )
            }

            autofillSelectionData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AutofillSelection(
                        autofillSelectionData = autofillSelectionData,
                        // Allow users back into the already-running app when completing the
                        // autofill task when this is not the first intent.
                        shouldFinishWhenComplete = isFirstIntent,
                    )
            }

            totpData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AddTotpLoginItem(data = totpData)
            }

            shareData != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ShareNewSend(
                        data = shareData,
                        // Allow users back into the already-running app when completing the
                        // Send task when this is not the first intent.
                        shouldFinishWhenComplete = isFirstIntent,
                    )
            }

            fido2CredentialRequestData != null -> {
                // Set the user's verification status when a new FIDO 2 request is received to force
                // explicit verification if the user's vault is unlocked when the request is
                // received.
                fido2CredentialManager.isUserVerified = false
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.Fido2Save(
                        fido2CredentialRequest = fido2CredentialRequestData,
                    )

                // Switch accounts if the selected user is not the active user.
                if (authRepository.activeUserId != null &&
                    authRepository.activeUserId != fido2CredentialRequestData.userId
                ) {
                    authRepository.switchAccount(fido2CredentialRequestData.userId)
                }
            }

            fido2CredentialAssertionRequest != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.Fido2Assertion(
                        fido2AssertionRequest = fido2CredentialAssertionRequest,
                    )
            }

            fido2GetCredentialsRequest != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.Fido2GetCredentials(
                        fido2GetCredentialsRequest = fido2GetCredentialsRequest,
                    )
            }

            hasGeneratorShortcut -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.GeneratorShortcut
            }

            hasVaultShortcut -> {
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.VaultShortcut
            }
        }
    }

    private fun recreateUiAndGarbageCollect() {
        sendEvent(MainEvent.Recreate)
        garbageCollectionManager.tryCollect()
    }

    private fun handleCompleteRegistrationData(data: CompleteRegistrationData) {
        viewModelScope.launch {
            // Attempt to load the environment for the user if they have a pre-auth environment
            // saved.
            environmentRepository.loadEnvironmentForEmail(userEmail = data.email)
            // Determine if the token is still valid.
            val emailTokenResult = authRepository.validateEmailToken(
                email = data.email,
                token = data.verificationToken,
            )
            when (emailTokenResult) {
                is EmailTokenResult.Error -> {
                    sendEvent(
                        MainEvent.ShowToast(
                            message = emailTokenResult
                                .message
                                ?.asText()
                                ?: R.string.there_was_an_issue_validating_the_registration_token
                                    .asText(),
                        ),
                    )
                }

                EmailTokenResult.Expired -> {
                    specialCircumstanceManager.specialCircumstance = SpecialCircumstance
                        .RegistrationEvent
                        .ExpiredRegistrationLink
                }

                EmailTokenResult.Success -> {
                    if (authRepository.activeUserId != null) {
                        authRepository.hasPendingAccountAddition = true
                    }
                    specialCircumstanceManager.specialCircumstance =
                        SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                            completeRegistrationData = data,
                            timestamp = clock.millis(),
                        )
                }
            }
        }
    }
}

/**
 * Models state for the [MainActivity].
 */
@Parcelize
data class MainState(
    val theme: AppTheme,
    val isScreenCaptureAllowed: Boolean,
) : Parcelable

/**
 * Models actions for the [MainActivity].
 */
sealed class MainAction {
    /**
     * Receive first Intent by the application.
     */
    data class ReceiveFirstIntent(val intent: Intent) : MainAction()

    /**
     * Receive Intent by the application.
     */
    data class ReceiveNewIntent(val intent: Intent) : MainAction()

    /**
     * Receive event to open the debug menu.
     */
    data object OpenDebugMenu : MainAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : MainAction() {
        /**
         * Indicates the user has manually selected the given [cipherView] for accessibility
         * autofill.
         */
        data class AccessibilitySelectionReceive(
            val cipherView: CipherView,
        ) : Internal()

        /**
         * Indicates the user has manually selected the given [cipherView] for autofill.
         */
        data class AutofillSelectionReceive(
            val cipherView: CipherView,
        ) : Internal()

        /**
         * Indicates a relevant change in the current user state.
         */
        data object CurrentUserStateChange : Internal()

        /**
         * Indicates that the screen capture state has changed.
         */
        data class ScreenCaptureUpdate(
            val isScreenCaptureEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates that the app theme has changed.
         */
        data class ThemeUpdate(
            val theme: AppTheme,
        ) : Internal()

        /**
         * Indicates a relevant change in the current vault lock state.
         */
        data object VaultUnlockStateChange : Internal()
    }
}

/**
 * Represents events that are emitted by the [MainViewModel].
 */
sealed class MainEvent {
    /**
     * Event indicating that the user has chosen the given [cipherView] for accessibility autofill
     * and that the process is ready to complete.
     */
    data class CompleteAccessibilityAutofill(val cipherView: CipherView) : MainEvent()

    /**
     * Event indicating that the user has chosen the given [cipherView] for autofill and that the
     * process is ready to complete.
     */
    data class CompleteAutofill(val cipherView: CipherView) : MainEvent()

    /**
     * Event indicating that the UI should recreate itself.
     */
    data object Recreate : MainEvent()

    /**
     * Navigate to the debug menu.
     */
    data object NavigateToDebugMenu : MainEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : MainEvent()
}
