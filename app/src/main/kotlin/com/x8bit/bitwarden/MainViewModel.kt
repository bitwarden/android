package com.x8bit.bitwarden

import android.content.Intent
import android.os.Parcelable
import androidx.browser.auth.AuthTabIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.cxf.util.getProviderImportCredentialsRequest
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.share.ShareManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.manager.AddTotpItemFromAuthenticatorManager
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.EmailTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getDuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.getWebAuthResult
import com.x8bit.bitwarden.data.auth.util.getCompleteRegistrationDataIntentOrNull
import com.x8bit.bitwarden.data.auth.util.getPasswordlessRequestDataIntentOrNull
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.util.getAutofillSaveItemOrNull
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.util.getCreateCredentialRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getGetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.credentials.util.getProviderGetPasswordRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.AppResumeManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.garbage.GarbageCollectionManager
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.util.isAddTotpLoginItemFromAuthenticator
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.model.FeatureFlagsState
import com.x8bit.bitwarden.ui.platform.util.isAccountSecurityShortcut
import com.x8bit.bitwarden.ui.platform.util.isMyVaultShortcut
import com.x8bit.bitwarden.ui.platform.util.isPasswordGeneratorShortcut
import com.x8bit.bitwarden.ui.vault.util.getTotpDataOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val SPECIAL_CIRCUMSTANCE_KEY = "special-circumstance"
private const val ANIMATION_DEBOUNCE_DELAY_MS = 500L

/**
 * A view model that helps launch actions for the [MainActivity].
 */
@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MainViewModel @Inject constructor(
    accessibilitySelectionManager: AccessibilitySelectionManager,
    autofillSelectionManager: AutofillSelectionManager,
    private val addTotpItemFromAuthenticatorManager: AddTotpItemFromAuthenticatorManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val garbageCollectionManager: GarbageCollectionManager,
    private val bitwardenCredentialManager: BitwardenCredentialManager,
    private val shareManager: ShareManager,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    private val savedStateHandle: SavedStateHandle,
    private val appResumeManager: AppResumeManager,
    private val clock: Clock,
    private val toastManager: ToastManager,
) : BaseViewModel<MainState, MainEvent, MainAction>(
    initialState = MainState(
        theme = settingsRepository.appTheme,
        isScreenCaptureAllowed = settingsRepository.isScreenCaptureAllowed,
        isDynamicColorsEnabled = settingsRepository.isDynamicColorsEnabled,
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
            .appLanguageStateFlow
            .map { MainEvent.UpdateAppLocale(it.localeName) }
            .onEach(::sendEvent)
            .launchIn(viewModelScope)

        settingsRepository
            .isScreenCaptureAllowedStateFlow
            .map { MainAction.Internal.ScreenCaptureUpdate(it) }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isDynamicColorsEnabledFlow
            .map { MainAction.Internal.DynamicColorsUpdate(it) }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)

        merge(
            authRepository
                .userStateFlow
                .drop(count = 1)
                // Trigger an action whenever the current user changes or we go into/out of a
                // pending account state (which acts like switching to a temporary user).
                .map { it?.activeUserId to it?.hasPendingAccountAddition }
                .distinctUntilChanged(),
            vaultRepository
                .vaultStateEventFlow
                .filter { it is VaultStateEvent.Locked },
        )
            // This debounce ensure we do not emit multiple times rapidly and also acts as a short
            // delay to give animations time to finish (ex: account switcher).
            .debounce(timeoutMillis = ANIMATION_DEBOUNCE_DELAY_MS)
            .map { MainAction.Internal.CurrentUserOrVaultStateChange }
            .onEach(::sendAction)
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
            is MainAction.ReceiveFirstIntent -> handleFirstIntentReceived(action)
            is MainAction.ReceiveNewIntent -> handleNewIntentReceived(action)
            MainAction.OpenDebugMenu -> handleOpenDebugMenu()
            is MainAction.ResumeScreenDataReceived -> handleAppResumeDataUpdated(action)
            is MainAction.AppSpecificLanguageUpdate -> handleAppSpecificLanguageUpdate(action)
            is MainAction.DuoResult -> handleDuoResult(action)
            is MainAction.SsoResult -> handleSsoResult(action)
            is MainAction.WebAuthnResult -> handleWebAuthnResult(action)
            is MainAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: MainAction.Internal) {
        when (action) {
            is MainAction.Internal.AccessibilitySelectionReceive -> {
                handleAccessibilitySelectionReceive(action)
            }

            is MainAction.Internal.AutofillSelectionReceive -> {
                handleAutofillSelectionReceive(action)
            }

            is MainAction.Internal.CurrentUserOrVaultStateChange -> {
                handleCurrentUserOrVaultStateChange()
            }

            is MainAction.Internal.ScreenCaptureUpdate -> handleScreenCaptureUpdate(action)
            is MainAction.Internal.ThemeUpdate -> handleAppThemeUpdated(action)
            is MainAction.Internal.DynamicColorsUpdate -> handleDynamicColorsUpdate(action)
        }
    }

    private fun handleAppSpecificLanguageUpdate(action: MainAction.AppSpecificLanguageUpdate) {
        settingsRepository.appLanguage = action.appLanguage
    }

    private fun handleDuoResult(action: MainAction.DuoResult) {
        authRepository.setDuoCallbackTokenResult(
            tokenResult = action.authResult.getDuoCallbackTokenResult(),
        )
    }

    private fun handleSsoResult(action: MainAction.SsoResult) {
        authRepository.setSsoCallbackResult(result = action.authResult.getSsoCallbackResult())
    }

    private fun handleWebAuthnResult(action: MainAction.WebAuthnResult) {
        authRepository.setWebAuthResult(webAuthResult = action.authResult.getWebAuthResult())
    }

    private fun handleAppResumeDataUpdated(action: MainAction.ResumeScreenDataReceived) {
        when (val data = action.screenResumeData) {
            null -> appResumeManager.clearResumeScreen()
            else -> appResumeManager.setResumeScreen(data)
        }
    }

    private fun handleOpenDebugMenu() {
        sendEvent(MainEvent.NavigateToDebugMenu)
    }

    private fun handleAccessibilitySelectionReceive(
        action: MainAction.Internal.AccessibilitySelectionReceive,
    ) {
        specialCircumstanceManager.specialCircumstance = null
        sendEvent(MainEvent.CompleteAccessibilityAutofill(cipherView = action.cipherView))
    }

    private fun handleAutofillSelectionReceive(
        action: MainAction.Internal.AutofillSelectionReceive,
    ) {
        specialCircumstanceManager.specialCircumstance = null
        sendEvent(MainEvent.CompleteAutofill(cipherView = action.cipherView))
    }

    private fun handleCurrentUserOrVaultStateChange() {
        sendEvent(MainEvent.Recreate)
        garbageCollectionManager.tryCollect()
    }

    private fun handleScreenCaptureUpdate(action: MainAction.Internal.ScreenCaptureUpdate) {
        mutableStateFlow.update { it.copy(isScreenCaptureAllowed = action.isScreenCaptureEnabled) }
    }

    private fun handleAppThemeUpdated(action: MainAction.Internal.ThemeUpdate) {
        mutableStateFlow.update { it.copy(theme = action.theme) }
        sendEvent(MainEvent.UpdateAppTheme(osTheme = action.theme.osValue))
    }

    private fun handleDynamicColorsUpdate(action: MainAction.Internal.DynamicColorsUpdate) {
        mutableStateFlow.update { it.copy(isDynamicColorsEnabled = action.isDynamicColorsEnabled) }
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun handleIntent(
        intent: Intent,
        isFirstIntent: Boolean,
    ) {
        val passwordlessRequestData = intent.getPasswordlessRequestDataIntentOrNull()
        val autofillSaveItem = intent.getAutofillSaveItemOrNull()
        val autofillSelectionData = intent.getAutofillSelectionDataOrNull()
        val shareData = shareManager.getShareDataOrNull(intent = intent)
        val totpData: TotpData? =
            // First grab TOTP URI directly from the intent data:
            intent.getTotpDataOrNull()
                ?: run {
                    // Then check to see if the intent is coming from the Authenticator app:
                    if (intent.isAddTotpLoginItemFromAuthenticator()) {
                        addTotpItemFromAuthenticatorManager.pendingAddTotpLoginItemData.also {
                            // Clear pending add TOTP data so it is only handled once:
                            addTotpItemFromAuthenticatorManager.pendingAddTotpLoginItemData = null
                        }
                    } else {
                        null
                    }
                }
        val hasGeneratorShortcut = intent.isPasswordGeneratorShortcut
        val hasVaultShortcut = intent.isMyVaultShortcut
        val hasAccountSecurityShortcut = intent.isAccountSecurityShortcut
        val completeRegistrationData = intent.getCompleteRegistrationDataIntentOrNull()
        val createCredentialRequest = intent.getCreateCredentialRequestOrNull()
        val getCredentialsRequest = intent.getGetCredentialsRequestOrNull()
        val fido2AssertCredentialRequest = intent.getFido2AssertionRequestOrNull()
        val providerGetPasswordRequest = intent.getProviderGetPasswordRequestOrNull()
        val importCredentialsRequest = intent.getProviderImportCredentialsRequest()
        when {
            passwordlessRequestData != null -> {
                authRepository.activeUserId?.let {
                    if (it != passwordlessRequestData.userId &&
                        !vaultRepository.isVaultUnlocked(it)
                    ) {
                        // We only switch the account here if the current user's vault is not
                        // unlocked, otherwise prompt the user to allow us to change the account
                        // in the LoginApprovalScreen
                        authRepository.switchAccount(passwordlessRequestData.userId)
                    }
                }
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

            createCredentialRequest != null -> {
                // Set the user's verification status when a new FIDO 2 request is received to force
                // explicit verification if the user's vault is unlocked when the request is
                // received.
                bitwardenCredentialManager.isUserVerified =
                    createCredentialRequest.isUserPreVerified

                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderCreateCredential(
                        createCredentialRequest = createCredentialRequest,
                    )

                // Switch accounts if the selected user is not the active user.
                if (authRepository.activeUserId != null &&
                    authRepository.activeUserId != createCredentialRequest.userId
                ) {
                    authRepository.switchAccount(createCredentialRequest.userId)
                }
            }

            fido2AssertCredentialRequest != null -> {
                // Set the user's verification status when a new FIDO 2 request is received to force
                // explicit verification if the user's vault is unlocked when the request is
                // received.
                bitwardenCredentialManager.isUserVerified =
                    fido2AssertCredentialRequest.isUserPreVerified

                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.Fido2Assertion(
                        fido2AssertionRequest = fido2AssertCredentialRequest,
                    )
            }

            providerGetPasswordRequest != null -> {
                // Set the user's verification status when a new GetPassword request is
                // received to force explicit verification if the user's vault is
                // unlocked when the request is received.
                bitwardenCredentialManager.isUserVerified =
                    providerGetPasswordRequest.isUserPreVerified

                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderGetPasswordRequest(
                        passwordGetRequest = providerGetPasswordRequest,
                    )
            }

            getCredentialsRequest != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.ProviderGetCredentials(
                        getCredentialsRequest = getCredentialsRequest,
                    )
            }

            hasGeneratorShortcut -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.GeneratorShortcut
            }

            hasVaultShortcut -> {
                specialCircumstanceManager.specialCircumstance = SpecialCircumstance.VaultShortcut
            }

            hasAccountSecurityShortcut -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.AccountSecurityShortcut
            }

            importCredentialsRequest != null -> {
                specialCircumstanceManager.specialCircumstance =
                    SpecialCircumstance.CredentialExchangeExport(
                        data = ImportCredentialsRequestData(
                            uri = importCredentialsRequest.uri,
                            requestJson = importCredentialsRequest.request.requestJson,
                        ),
                    )
            }
        }
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
                    emailTokenResult
                        .message
                        ?.let { toastManager.show(message = it) }
                        ?: run {
                            toastManager.show(
                                messageId = BitwardenString
                                    .there_was_an_issue_validating_the_registration_token,
                            )
                        }
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
    val isDynamicColorsEnabled: Boolean,
) : Parcelable {
    /**
     * Contains all feature flags that are available to the UI.
     */
    val featureFlagsState: FeatureFlagsState
        get() = FeatureFlagsState
}

/**
 * Models actions for the [MainActivity].
 */
sealed class MainAction {
    /**
     * Receive the result from the Duo login flow.
     */
    data class DuoResult(val authResult: AuthTabIntent.AuthResult) : MainAction()

    /**
     * Receive the result from the SSO login flow.
     */
    data class SsoResult(val authResult: AuthTabIntent.AuthResult) : MainAction()

    /**
     * Receive the result from the WebAuthn login flow.
     */
    data class WebAuthnResult(val authResult: AuthTabIntent.AuthResult) : MainAction()

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
     * Receive event to save the app resume screen
     */
    data class ResumeScreenDataReceived(val screenResumeData: AppResumeScreenData?) : MainAction()

    /**
     * Receive if there is an app specific locale selection made by user
     * in the device's settings.
     */
    data class AppSpecificLanguageUpdate(val appLanguage: AppLanguage) : MainAction()

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
         * Indicates a relevant change in the current user state or vault locked state.
         */
        data object CurrentUserOrVaultStateChange : Internal()

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
         * Indicates that the dynamic colors state has changed.
         */
        data class DynamicColorsUpdate(
            val isDynamicColorsEnabled: Boolean,
        ) : Internal()
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
     * Indicates that the app language has been updated.
     */
    data class UpdateAppLocale(
        val localeName: String?,
    ) : MainEvent()

    /**
     * Indicates that the app theme has been updated.
     */
    data class UpdateAppTheme(
        val osTheme: Int,
    ) : MainEvent()
}
