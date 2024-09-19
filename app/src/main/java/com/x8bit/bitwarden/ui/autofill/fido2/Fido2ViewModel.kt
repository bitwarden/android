package com.x8bit.bitwarden.ui.autofill.fido2

import androidx.lifecycle.viewModelScope
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.autofill.fido2.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.util.toFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toFido2GetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.BackgroundEvent
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Models logic for [Fido2Screen].
 */
@Suppress("LargeClass", "TooManyFunctions")
@HiltViewModel
class Fido2ViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<Fido2State, Fido2Event, Fido2Action>(
    initialState = run {
        val specialCircumstance = specialCircumstanceManager.specialCircumstance
        val fido2GetCredentialsRequest = specialCircumstance?.toFido2GetCredentialsRequestOrNull()
        val fido2AssertCredentialRequest = specialCircumstance?.toFido2AssertionRequestOrNull()
        val requestUserId = fido2GetCredentialsRequest?.userId
            ?: fido2AssertCredentialRequest?.userId
        Fido2State(
            requestUserId = requireNotNull(requestUserId),
            fido2GetCredentialsRequest = fido2GetCredentialsRequest,
            fido2AssertCredentialRequest = fido2AssertCredentialRequest,
            dialog = null,
        )
    },
) {

    init {
        vaultRepository.vaultDataStateFlow
            .map { Fido2Action.Internal.VaultDataStateChangeReceive(it) }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)

        authRepository.userStateFlow
            .map { Fido2Action.Internal.UserStateChangeReceive(it) }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: Fido2Action) {
        when (action) {
            Fido2Action.DismissErrorDialogClick -> {
                clearDialogState()
                sendEvent(
                    Fido2Event.CompleteFido2GetCredentialsRequest(
                        result = Fido2GetCredentialsResult.Error,
                    ),
                )
            }

            Fido2Action.DeviceUserVerificationFail,
            Fido2Action.DeviceUserVerificationLockOut,
                -> handleDeviceUserVerificationFail()

            is Fido2Action.DeviceUserVerificationSuccess -> {
                handleUserVerificationSuccess(action)
            }

            is Fido2Action.DeviceUserVerificationNotSupported -> {
                handleDeviceUserVerificationNotSupported(action)
            }

            Fido2Action.DeviceUserVerificationCancelled,
            Fido2Action.DismissBitwardenUserVerification,
                -> handleUserVerificationCancelled()

            is Fido2Action.MasterPasswordFido2VerificationSubmit -> {
                handleMasterPasswordSubmit(action)
            }

            is Fido2Action.PinFido2SetUpSubmit -> {
                handlePinSetUpSubmit(action)
            }

            is Fido2Action.PinFido2VerificationSubmit -> {
                handlePinSubmit(action)
            }

            is Fido2Action.RetryFido2PinSetUpClick -> {
                handleRetryFido2PinSetUpClick(action)
            }

            is Fido2Action.RetryFido2PasswordVerificationClick -> {
                handleRetryFido2PasswordVerificationClick(action)
            }

            is Fido2Action.RetryFido2PinVerificationClick -> {
                handleRetryFido2PinVerificationClick(action)
            }

            is Fido2Action.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleInternalAction(action: Fido2Action.Internal) {
        when (action) {
            is Fido2Action.Internal.VaultDataStateChangeReceive -> {
                handleVaultDataStateChangeReceive(action.vaultData)
            }

            is Fido2Action.Internal.GetCredentialsResultReceive -> {
                handleGetCredentialsResultReceive(action)
            }

            is Fido2Action.Internal.AuthenticateCredentialResultReceive -> {
                handleAuthenticateCredentialResultReceive(action)
            }

            is Fido2Action.Internal.ValidateFido2PinResultReceive -> {
                handleValidateFido2PinResultReceive(action)
            }

            is Fido2Action.Internal.UserStateChangeReceive -> {
                handleUserStateChangeReceive(action)
            }
        }
    }

    private fun handleUserStateChangeReceive(action: Fido2Action.Internal.UserStateChangeReceive) {
        val activeUserId = action.userState?.activeUserId ?: return
        val requestUserId = state.requestUserId
        if (requestUserId != activeUserId) {
            authRepository.switchAccount(requestUserId)
        }
    }

    private fun handleVaultDataStateChangeReceive(vaultDataState: DataState<VaultData>) {
        when (vaultDataState) {
            is DataState.Error -> mutableStateFlow.update {
                it.copy(
                    dialog = Fido2State.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }

            is DataState.Loaded -> handleVaultDataLoaded(vaultDataState.data)
            DataState.Loading -> handleVaultDataLoading()
            is DataState.NoNetwork -> handleNoNetwork()
            is DataState.Pending -> clearDialogState()
        }
    }

    private fun handleVaultDataLoaded(data: VaultData) {
        clearDialogState()
        if (authRepository.activeUserId != state.requestUserId) {
            // Ignore vault data when we are waiting for the account to switch
            return
        }

        viewModelScope.launch {
            state
                .fido2GetCredentialsRequest
                ?.let { getCredentialsRequest ->
                    getFido2CredentialAutofillViewsForSelection(getCredentialsRequest, data)
                }
                ?: state
                    .fido2AssertCredentialRequest
                    ?.let { assertCredentialRequest ->
                        startFido2CredentialAssertion(assertCredentialRequest, data)
                    }
                ?: showFido2ErrorDialog(
                    title = R.string.generic_error_message.asText(),
                    message = R.string.an_error_has_occurred.asText(),
                )
        }
    }

    private suspend fun getFido2CredentialAutofillViewsForSelection(
        fido2GetCredentialsRequest: Fido2GetCredentialsRequest,
        data: VaultData,
    ) {
        val relyingPartyId = fido2CredentialManager
            .getPasskeyAssertionOptionsOrNull(
                requestJson = fido2GetCredentialsRequest.option.requestJson,
            )
            ?.relyingPartyId
            ?: run {
                sendAction(Fido2Action.Internal.GetCredentialsResultReceive.Error)
                return
            }
        val credentials: Map<String, Fido2CredentialAutofillView> = data
            .toFido2CredentialAutofillViews()
            .filter { it.rpId == relyingPartyId }
            .associate {
                val cipherName = data
                    .cipherViewList
                    .find { cipher -> cipher.id == it.cipherId }
                    ?.name
                    // This should never happen, but we display "Bitwarden" if it does.
                    ?: "Bitwarden"

                cipherName to it
            }
        sendAction(
            Fido2Action.Internal.GetCredentialsResultReceive.Success(
                request = fido2GetCredentialsRequest,
                credentials = credentials,
            ),
        )
    }

    private fun startFido2CredentialAssertion(
        fido2AssertCredentialRequest: Fido2CredentialAssertionRequest,
        data: VaultData,
    ) {
        val selectedCipher = data
            .cipherViewList
            .filter { it.isActiveWithFido2Credentials }
            .find { it.id == fido2AssertCredentialRequest.cipherId }
            ?: run {
                trySendAction(
                    Fido2Action.Internal.AuthenticateCredentialResultReceive.Error(
                        R.string.passkey_operation_failed_because_passkey_does_not_exist.asText(),
                    ),
                )
                return
            }
        val assertionOptions = fido2CredentialManager
            .getPasskeyAssertionOptionsOrNull(fido2AssertCredentialRequest.requestJson)
            ?: run {
                trySendAction(
                    Fido2Action.Internal.AuthenticateCredentialResultReceive.Error(
                        R.string.passkey_operation_failed_because_app_could_not_be_verified
                            .asText(),
                    ),
                )
                return
            }

        when {
            fido2CredentialManager.isUserVerified ||
                assertionOptions.userVerification == UserVerificationRequirement.DISCOURAGED -> {
                trySendAction(Fido2Action.DeviceUserVerificationSuccess(selectedCipher))
            }

            assertionOptions.userVerification == UserVerificationRequirement.PREFERRED -> {
                sendUserVerificationEvent(
                    isRequired = false,
                    selectedCipher = selectedCipher,
                )
            }

            assertionOptions.userVerification == UserVerificationRequirement.REQUIRED -> {
                sendUserVerificationEvent(
                    isRequired = true,
                    selectedCipher = selectedCipher,
                )
            }
        }
    }

    private fun sendUserVerificationEvent(isRequired: Boolean, selectedCipher: CipherView) {
        sendEvent(Fido2Event.Fido2UserVerification(isRequired, selectedCipher))
    }

    private fun handleGetCredentialsResultReceive(
        action: Fido2Action.Internal.GetCredentialsResultReceive,
    ) {
        when (action) {
            Fido2Action.Internal.GetCredentialsResultReceive.Error -> {
                showFido2ErrorDialog(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_app_could_not_be_verified
                        .asText(),
                )
            }

            is Fido2Action.Internal.GetCredentialsResultReceive.Success -> {
                sendEvent(
                    Fido2Event.CompleteFido2GetCredentialsRequest(
                        Fido2GetCredentialsResult.Success(
                            userId = action.request.userId,
                            options = action.request.option,
                            credentials = action.credentials,
                            alternateAccounts = emptyList(),
                        ),
                    ),
                )
            }
        }
    }

    private fun handleAuthenticateCredentialResultReceive(
        action: Fido2Action.Internal.AuthenticateCredentialResultReceive,
    ) {
        when (action) {
            is Fido2Action.Internal.AuthenticateCredentialResultReceive.Error -> {
                showFido2ErrorDialog(
                    title = R.string.an_error_has_occurred.asText(),
                    message = action.message,
                )
            }

            is Fido2Action.Internal.AuthenticateCredentialResultReceive.Success -> {
                fido2CredentialManager.isUserVerified = false
                clearDialogState()
                sendEvent(Fido2Event.CompleteFido2Assertion(action.result))
            }
        }
    }

    private fun handleVaultDataLoading() {
        mutableStateFlow.update { it.copy(dialog = Fido2State.DialogState.Loading) }
    }

    private fun handleNoNetwork() {
        mutableStateFlow.update {
            it.copy(
                dialog = Fido2State.DialogState.Error(
                    R.string.internet_connection_required_title.asText(),
                    R.string.internet_connection_required_message.asText(),
                ),
            )
        }
    }

    //region Device based user verification handlers

    private fun handleUserVerificationSuccess(
        action: Fido2Action.DeviceUserVerificationSuccess,
    ) {
        viewModelScope.launch {
            fido2CredentialManager.isUserVerified = true
            continueFido2Assertion(action.selectedCipherView)
        }
    }

    private fun handleDeviceUserVerificationFail() {
        showUserVerificationErrorDialog()
    }

    private fun handleUserVerificationCancelled() {
        fido2CredentialManager.isUserVerified = false
        clearDialogState()
        sendEvent(
            Fido2Event.CompleteFido2GetCredentialsRequest(
                result = Fido2GetCredentialsResult.Cancelled,
            ),
        )
    }

    private fun handleDeviceUserVerificationNotSupported(
        action: Fido2Action.DeviceUserVerificationNotSupported,
    ) {
        fido2CredentialManager.isUserVerified = false

        val selectedCipherId = action
            .selectedCipherId
            ?: run {
                showFido2ErrorDialog(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_passkey_does_not_exist
                        .asText(),
                )
                return
            }

        val activeAccount = authRepository
            .userStateFlow
            .value
            ?.activeAccount
            ?: run {
                showUserVerificationErrorDialog()
                return
            }

        if (settingsRepository.isUnlockWithPinEnabled) {
            mutableStateFlow.update {
                it.copy(
                    dialog = Fido2State.DialogState.Fido2PinPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
                )
            }
        } else if (activeAccount.hasMasterPassword) {
            mutableStateFlow.update {
                it.copy(
                    dialog = Fido2State.DialogState.Fido2MasterPasswordPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
                )
            }
        } else {
            // Prompt the user to set up a PIN for their account.
            mutableStateFlow.update {
                it.copy(
                    dialog = Fido2State.DialogState.Fido2PinSetUpPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
                )
            }
        }
    }
    //endregion Device based user verification handlers

    //region Bitwarden based user verification handlers

    private fun handleMasterPasswordSubmit(
        action: Fido2Action.MasterPasswordFido2VerificationSubmit,
    ) {
        viewModelScope.launch {
            clearDialogState()
            val result = authRepository.validatePassword(action.password)
            when (result) {
                ValidatePasswordResult.Error -> showUserVerificationErrorDialog()

                is ValidatePasswordResult.Success -> {
                    if (result.isValid) {
                        handleValidBitwardenAuthentication(action.selectedCipherId)
                    } else {
                        handleInvalidAuthentication(
                            errorDialogState = Fido2State
                                .DialogState
                                .Fido2MasterPasswordError(
                                    title = null,
                                    message = R.string.invalid_master_password.asText(),
                                    selectedCipherId = action.selectedCipherId,
                                ),
                        )
                    }
                }
            }
        }
    }

    private fun handlePinSetUpSubmit(
        action: Fido2Action.PinFido2SetUpSubmit,
    ) {
        if (action.pin.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = Fido2State.DialogState.Fido2PinSetUpError(
                        title = null,
                        message = R.string.validation_field_required.asText(R.string.pin.asText()),
                        selectedCipherId = action.selectedCipherId,
                    ),
                )
            }
            return
        }

        // There's no need to ask the user whether or not they want to use their master password
        // on login, and shouldRequireMasterPasswordOnRestart is hardcoded to false, because the
        // user can only reach this part of the flow if they have no master password.
        settingsRepository.storeUnlockPin(
            pin = action.pin,
            shouldRequireMasterPasswordOnRestart = false,
        )

        // After storing the PIN, the user can proceed with their original FIDO 2 request.
        viewModelScope.launch {
            handleValidBitwardenAuthentication(
                selectedCipherId = action.selectedCipherId,
            )
        }
    }

    private fun handlePinSubmit(
        action: Fido2Action.PinFido2VerificationSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePin(action.pin)
            sendAction(
                Fido2Action.Internal.ValidateFido2PinResultReceive(
                    result = result,
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleValidateFido2PinResultReceive(
        action: Fido2Action.Internal.ValidateFido2PinResultReceive,
    ) {
        clearDialogState()

        when (action.result) {
            ValidatePinResult.Error -> {
                showUserVerificationErrorDialog()
            }

            is ValidatePinResult.Success -> {
                viewModelScope.launch {
                    if (action.result.isValid) {
                        handleValidBitwardenAuthentication(action.selectedCipherId)
                    } else {
                        handleInvalidAuthentication(
                            errorDialogState = Fido2State
                                .DialogState
                                .Fido2PinError(
                                    title = null,
                                    message = R.string.invalid_pin.asText(),
                                    selectedCipherId = action.selectedCipherId,
                                ),
                        )
                    }
                }
            }
        }
    }

    private fun handleRetryFido2PasswordVerificationClick(
        action: Fido2Action.RetryFido2PasswordVerificationClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = Fido2State.DialogState.Fido2MasterPasswordPrompt(
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleRetryFido2PinSetUpClick(
        action: Fido2Action.RetryFido2PinSetUpClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = Fido2State.DialogState.Fido2PinSetUpPrompt(
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleRetryFido2PinVerificationClick(
        action: Fido2Action.RetryFido2PinVerificationClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = Fido2State.DialogState.Fido2PinPrompt(
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private suspend fun handleValidBitwardenAuthentication(selectedCipherId: String) {
        fido2CredentialManager.isUserVerified = true
        fido2CredentialManager.authenticationAttempts = 0

        val cipherView = getCipherViewOrNull(cipherId = selectedCipherId)
            ?: run {
                showFido2ErrorDialog(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_passkey_does_not_exist
                        .asText(),
                )
                return
            }

        continueFido2Assertion(cipherView)
    }

    private fun handleInvalidAuthentication(
        errorDialogState: Fido2State.DialogState,
    ) {
        fido2CredentialManager.authenticationAttempts += 1
        if (fido2CredentialManager.hasAuthenticationAttemptsRemaining()) {
            mutableStateFlow.update {
                it.copy(dialog = errorDialogState)
            }
        } else {
            showUserVerificationErrorDialog()
        }
    }

    //endregion Bitwarden based user verification handlers

    private suspend fun continueFido2Assertion(cipherView: CipherView) {
        state.fido2AssertCredentialRequest
            ?.let { request ->
                authenticateFido2Credential(
                    request = request,
                    cipherView = cipherView,
                )
            }
            ?: sendAction(
                Fido2Action.Internal.AuthenticateCredentialResultReceive.Error(
                    R.string.generic_error_message.asText(),
                ),
            )
    }

    private suspend fun authenticateFido2Credential(
        request: Fido2CredentialAssertionRequest,
        cipherView: CipherView,
    ) {
        val result = fido2CredentialManager
            .authenticateFido2Credential(
                userId = request.userId,
                selectedCipherView = cipherView,
                request = request,
            )
        sendAction(
            Fido2Action.Internal.AuthenticateCredentialResultReceive.Success(
                result = result,
            ),
        )
    }

    private fun showUserVerificationErrorDialog() {
        fido2CredentialManager.isUserVerified = false
        fido2CredentialManager.authenticationAttempts = 0
        showFido2ErrorDialog(
            title = R.string.an_error_has_occurred.asText(),
            message = R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
        )
    }

    private fun showFido2ErrorDialog(title: Text, message: Text) {
        mutableStateFlow.update {
            it.copy(
                dialog = Fido2State.DialogState.Error(title, message),
            )
        }
    }

    private fun clearDialogState() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun getCipherViewOrNull(cipherId: String): CipherView? =
        vaultRepository
            .vaultDataStateFlow
            .value
            .data
            ?.cipherViewList
            ?.firstOrNull { it.id == cipherId }

    /**
     * Decrypt and filter the FIDO 2 autofill credentials.
     */
    @Suppress("MaxLineLength")
    private suspend fun VaultData.toFido2CredentialAutofillViews(): List<Fido2CredentialAutofillView> {
        val decryptFido2CredentialAutofillViewsResult = vaultRepository
            .getDecryptedFido2CredentialAutofillViews(
                cipherViewList = this
                    .cipherViewList
                    .filter { it.isActiveWithFido2Credentials },
            )

        return when (decryptFido2CredentialAutofillViewsResult) {
            DecryptFido2CredentialAutofillViewResult.Error -> emptyList()

            is DecryptFido2CredentialAutofillViewResult.Success -> {
                decryptFido2CredentialAutofillViewsResult.fido2CredentialAutofillViews
            }
        }
    }
}

/**
 * Represents the UI state for [Fido2Screen].
 * @property requestUserId User ID contained within the FIDO 2 request.
 * @property fido2GetCredentialsRequest Data required to discover FIDO 2 credential.
 * @property fido2AssertCredentialRequest Data required to authenticate a FIDO 2 credential.
 */
data class Fido2State(
    val requestUserId: String,
    val fido2GetCredentialsRequest: Fido2GetCredentialsRequest?,
    val fido2AssertCredentialRequest: Fido2CredentialAssertionRequest?,
    val dialog: DialogState?,
) {

    /**
     * Represents the dialog UI state for [Fido2Screen].
     */
    sealed class DialogState {
        /**
         * Displays a loading dialog.
         */
        data object Loading : DialogState()

        /**
         * Displays a generic error dialog with a [title] and [message].
         */
        data class Error(val title: Text, val message: Text) : DialogState()

        /**
         * Displays a PIN entry dialog to verify the user.
         */
        data class Fido2PinPrompt(val selectedCipherId: String) : DialogState()

        /**
         * Displays a master password entry dialog to verify the user.
         */
        data class Fido2MasterPasswordPrompt(val selectedCipherId: String) : DialogState()

        /**
         * Displays a PIN creation dialog for user verification.
         */
        data class Fido2PinSetUpPrompt(val selectedCipherId: String) : DialogState()

        /**
         * Displays a master password validation error dialog.
         */
        data class Fido2MasterPasswordError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Displays a PIN set up error dialog.
         */
        data class Fido2PinSetUpError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Displays a PIN validation error dialog.
         */
        data class Fido2PinError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()
    }
}

/**
 * Models events for [Fido2Screen].
 */
sealed class Fido2Event {

    /**
     * Completes FIDO 2 credential discovery with the given [result].
     */
    data class CompleteFido2GetCredentialsRequest(
        val result: Fido2GetCredentialsResult,
    ) : BackgroundEvent, Fido2Event()

    /**
     * Performs device based user verification.
     */
    data class Fido2UserVerification(
        val required: Boolean,
        val selectedCipher: CipherView,
    ) : BackgroundEvent, Fido2Event()

    /**
     * Completes FIDO 2 credential authentication with the given [result].
     */
    data class CompleteFido2Assertion(
        val result: Fido2CredentialAssertionResult,
    ) : BackgroundEvent, Fido2Event()
}

/**
 * Models actions for [Fido2Screen].
 */
sealed class Fido2Action {

    /**
     * Indicates the user dismissed the error dialog.
     */
    data object DismissErrorDialogClick : Fido2Action()

    /**
     * Indicates the user has cancelled user verification.
     */
    data object DeviceUserVerificationCancelled : Fido2Action()

    /**
     * Indicates the user has locked out device based user verification.
     */
    data object DeviceUserVerificationLockOut : Fido2Action()

    /**
     * Indicates the user has failed device based user verification.
     */
    data object DeviceUserVerificationFail : Fido2Action()

    /**
     * Indicates the user has dismissed Bitwarden user verification.
     */
    data object DismissBitwardenUserVerification : Fido2Action()

    /**
     * Indicates the user's device does not support user verification.
     */
    data class DeviceUserVerificationNotSupported(
        val selectedCipherId: String?,
    ) : Fido2Action()

    /**
     * Indicates device based user verification was successful.
     */
    data class DeviceUserVerificationSuccess(
        val selectedCipherView: CipherView,
    ) : Fido2Action()

    /**
     * Indicates the user has submitted [password] for user verification.
     */
    data class MasterPasswordFido2VerificationSubmit(
        val password: String,
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Indicates the user has submitted a new [pin] to enable PIN verification and unlock.
     */
    data class PinFido2SetUpSubmit(
        val pin: String,
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Indicates the user has submitted [pin] for user verification.
     */
    data class PinFido2VerificationSubmit(
        val pin: String,
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Indicates the user has clicked retry master password verification.
     */
    data class RetryFido2PasswordVerificationClick(
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Indicates the user has clicked retry PIN verification.
     */
    data class RetryFido2PinVerificationClick(
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Indicates the user has clicked retry PIN creation.
     */
    data class RetryFido2PinSetUpClick(
        val selectedCipherId: String,
    ) : Fido2Action()

    /**
     * Models actions [Fido2ViewModel] may itself send.
     */
    sealed class Internal : Fido2Action() {

        /**
         * Indicates the [userState] has changed.
         */
        data class UserStateChangeReceive(
            val userState: UserState?,
        ) : Internal()

        /**
         * Indicates the [vaultData] has changed.
         */
        data class VaultDataStateChangeReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()

        /**
         * Indicates the result of a [Fido2GetCredentialsRequest] has been received.
         */
        sealed class GetCredentialsResultReceive : Internal() {
            /**
             * Indicates the [Fido2GetCredentialsRequest] was processed without error.
             * @property request in the starting intent.
             * @property credentials matching the relying party request.
             */
            data class Success(
                val request: Fido2GetCredentialsRequest,
                val credentials: Map<String, Fido2CredentialAutofillView>,
            ) : GetCredentialsResultReceive()

            /**
             * Indicates there was an error while retrieving the credentials.
             */
            data object Error : GetCredentialsResultReceive()
        }

        /**
         * Indicates the result of a [Fido2CredentialAssertionRequest] has been received.
         */
        sealed class AuthenticateCredentialResultReceive : Internal() {

            /**
             * Indicates the [Fido2CredentialAssertionRequest] was processed without error.
             * @property result of the credential authentication.
             */
            data class Success(
                val result: Fido2CredentialAssertionResult,
            ) : AuthenticateCredentialResultReceive()

            /**
             * Indicates credential authentication failed.
             */
            data class Error(
                val message: Text,
            ) : AuthenticateCredentialResultReceive()
        }

        /**
         * Indicates the result of PIN verification has been received.
         */
        data class ValidateFido2PinResultReceive(
            val result: ValidatePinResult,
            val selectedCipherId: String,
        ) : Internal()
    }
}
