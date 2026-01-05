package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.map
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.manager.OriginManager
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.ProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult
import com.x8bit.bitwarden.data.credentials.parser.RelyingPartyParser
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.credentials.util.getCreatePasskeyCredentialRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.util.toAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toCreateCredentialRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toFido2AssertionRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toGetCredentialsRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toPasswordGetRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toTotpDataOrNull
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.util.getSignatureFingerprintAsHexString
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult
import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.search.util.filterAndOrganize
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendItemType
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.components.util.toVaultItemCipherTypeOrNull
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.determineListingPredicate
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.messageResourceId
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toItemListingType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toSearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toSendItemType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toVaultItemCipherType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.updateWithAdditionalDataIfNecessary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.util.toVaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

/**
 * Manages [VaultItemListingState], handles [VaultItemListingsAction],
 * and launches [VaultItemListingEvent] for the [VaultItemListingScreen].
 */
@HiltViewModel
@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList", "LargeClass")
class VaultItemListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val privilegedAppRepository: PrivilegedAppRepository,
    private val accessibilitySelectionManager: AccessibilitySelectionManager,
    private val autofillSelectionManager: AutofillSelectionManager,
    private val cipherMatchingManager: CipherMatchingManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val policyManager: PolicyManager,
    private val originManager: OriginManager,
    private val bitwardenCredentialManager: BitwardenCredentialManager,
    private val organizationEventManager: OrganizationEventManager,
    private val networkConnectionManager: NetworkConnectionManager,
    private val relyingPartyParser: RelyingPartyParser,
    private val toastManager: ToastManager,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val activeAccountSummary = userState.toActiveAccountSummary()
        val accountSummaries = userState.toAccountSummaries()
        val specialCircumstance = specialCircumstanceManager.specialCircumstance
        val providerCreateCredentialRequest = specialCircumstance?.toCreateCredentialRequestOrNull()
        val providerGetCredentialsRequest = specialCircumstance?.toGetCredentialsRequestOrNull()
        val fido2AssertCredentialRequest = specialCircumstance?.toFido2AssertionRequestOrNull()
        val passwordGetCredentialRequest = specialCircumstance?.toPasswordGetRequestOrNull()
        VaultItemListingState(
            itemListingType = savedStateHandle
                .toVaultItemListingArgs()
                .vaultItemListingType
                .toItemListingType(),
            activeAccountSummary = activeAccountSummary,
            accountSummaries = accountSummaries,
            viewState = VaultItemListingState.ViewState.Loading,
            vaultFilterType = vaultRepository.vaultFilterType,
            baseWebSendUrl = environmentRepository.environment.environmentUrlData.baseWebSendUrl,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
            dialogState = providerCreateCredentialRequest
                ?.createPublicKeyCredentialRequest
                ?.let {
                    VaultItemListingState.DialogState.Loading(BitwardenString.loading.asText())
                },
            policyDisablesSend = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
                .any(),
            restrictItemTypesPolicyOrgIds = persistentListOf(),
            autofillSelectionData = specialCircumstance?.toAutofillSelectionDataOrNull(),
            hasMasterPassword = userState.activeAccount.hasMasterPassword,
            totpData = specialCircumstance?.toTotpDataOrNull(),
            createCredentialRequest = providerCreateCredentialRequest,
            fido2CredentialAssertionRequest = fido2AssertCredentialRequest,
            providerGetPasswordCredentialRequest = passwordGetCredentialRequest,
            getCredentialsRequest = providerGetCredentialsRequest,
            isPremium = userState.activeAccount.isPremium,
            isRefreshing = false,
        )
    },
) {

    init {
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { VaultItemListingsAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isIconLoadingDisabledFlow
            .onEach { sendAction(VaultItemListingsAction.Internal.IconLoadingSettingReceive(it)) }
            .launchIn(viewModelScope)

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND)
            .map { VaultItemListingsAction.Internal.PolicyUpdateReceive(it.any()) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
            .map { policies -> policies.map { it.organizationId } }
            .map { VaultItemListingsAction.Internal.RestrictItemTypesPolicyUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(
                SnackbarRelay.CIPHER_CREATED,
                SnackbarRelay.CIPHER_DELETED,
                SnackbarRelay.CIPHER_DELETED_SOFT,
                SnackbarRelay.CIPHER_RESTORED,
                SnackbarRelay.CIPHER_UPDATED,
                SnackbarRelay.SEND_DELETED,
                SnackbarRelay.SEND_UPDATED,
            )
            .map { VaultItemListingsAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        specialCircumstanceManager.specialCircumstance
            ?.toCreateCredentialRequestOrNull()
            ?.let { request ->
                trySendAction(
                    VaultItemListingsAction.Internal.CreateCredentialRequestReceive(
                        request = request,
                    ),
                )
            }
            ?: observeVaultData()
    }

    private fun observeVaultData() {
        vaultRepository
            .vaultDataStateFlow
            .map {
                VaultItemListingsAction.Internal.VaultDataReceive(
                    it
                        .filterForAutofillIfNecessary()
                        .filterForCredentialCreationIfNecessary()
                        .filterForTotpIfNecessary(),
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    @Suppress("LongMethod")
    override fun handleAction(action: VaultItemListingsAction) {
        when (action) {
            is VaultItemListingsAction.LockAccountClick -> handleLockAccountClick(action)
            is VaultItemListingsAction.LogoutAccountClick -> handleLogoutAccountClick(action)
            is VaultItemListingsAction.SwitchAccountClick -> handleSwitchAccountClick(action)
            is VaultItemListingsAction.DismissDialogClick -> handleDismissDialogClick()
            is VaultItemListingsAction.DismissCredentialManagerErrorDialogClick -> {
                handleDismissCredentialManagerErrorDialogClick(action)
            }

            is VaultItemListingsAction.MasterPasswordUserVerificationSubmit -> {
                handleMasterPasswordUserVerificationSubmit(action)
            }

            is VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick -> {
                handleRetryUserVerificationPasswordVerificationClick(action)
            }

            is VaultItemListingsAction.PinUserVerificationSubmit -> {
                handlePinUserVerificationSubmit(action)
            }

            is VaultItemListingsAction.RetryUserVerificationPinVerificationClick -> {
                handleRetryUserPinVerificationClick(action)
            }

            is VaultItemListingsAction.UserVerificationPinSetUpSubmit -> {
                handleUserVerificationPinSetUpSubmit(action)
            }

            is VaultItemListingsAction.UserVerificationPinSetUpRetryClick -> {
                handleUserVerificationPinSetUpRetryClick(action)
            }

            VaultItemListingsAction.DismissUserVerificationDialogClick -> {
                handleDismissUserVerificationDialogClick()
            }

            is VaultItemListingsAction.BackClick -> handleBackClick()
            is VaultItemListingsAction.FolderClick -> handleFolderClick(action)
            is VaultItemListingsAction.CollectionClick -> handleCollectionClick(action)
            is VaultItemListingsAction.LockClick -> handleLockClick()
            is VaultItemListingsAction.SyncClick -> handleSyncClick()
            is VaultItemListingsAction.SearchIconClick -> handleSearchIconClick()
            is VaultItemListingsAction.OverflowOptionClick -> handleOverflowOptionClick(action)
            is VaultItemListingsAction.ItemClick -> handleItemClick(action)
            is VaultItemListingsAction.MasterPasswordRepromptSubmit -> {
                handleMasterPasswordRepromptSubmit(action)
            }

            is VaultItemListingsAction.AddVaultItemClick -> handleAddVaultItemClick()
            is VaultItemListingsAction.RefreshClick -> handleRefreshClick()
            is VaultItemListingsAction.RefreshPull -> handleRefreshPull()
            is VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick -> {
                handleConfirmOverwriteExistingPasskeyClick(action)
            }

            VaultItemListingsAction.UserVerificationLockOut -> {
                handleUserVerificationLockOut()
            }

            VaultItemListingsAction.UserVerificationCancelled -> {
                handleUserVerificationCancelled()
            }

            VaultItemListingsAction.UserVerificationFail -> {
                handleUserVerificationFail()
            }

            is VaultItemListingsAction.UserVerificationSuccess -> {
                handleUserVerificationSuccess(action)
            }

            is VaultItemListingsAction.UserVerificationNotSupported -> {
                handleUserVerificationNotSupported(action)
            }

            is VaultItemListingsAction.ItemTypeToAddSelected -> {
                handleItemTypeToAddSelected(action)
            }

            is VaultItemListingsAction.TrustPrivilegedAppClick -> {
                handleTrustPrivilegedAppClick(action)
            }

            is VaultItemListingsAction.Internal -> handleInternalAction(action)

            is VaultItemListingsAction.ShareCipherDecryptionErrorClick -> {
                handleShareCipherDecryptionErrorClick(action)
            }
        }
    }

    //region VaultItemListing Handlers
    private fun handleLockAccountClick(action: VaultItemListingsAction.LockAccountClick) {
        vaultRepository.lockVault(userId = action.accountSummary.userId, isUserInitiated = true)
    }

    private fun handleLogoutAccountClick(action: VaultItemListingsAction.LogoutAccountClick) {
        authRepository.logout(
            userId = action.accountSummary.userId,
            reason = LogoutReason.Click(source = "VaultItemListingViewModel"),
        )
    }

    private fun handleSwitchAccountClick(action: VaultItemListingsAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleCollectionClick(action: VaultItemListingsAction.CollectionClick) {
        sendEvent(VaultItemListingEvent.NavigateToCollectionItem(action.id))
    }

    private fun handleFolderClick(action: VaultItemListingsAction.FolderClick) {
        sendEvent(VaultItemListingEvent.NavigateToFolderItem(action.id))
    }

    private fun handleRefreshClick() {
        vaultRepository.sync(forced = true)
    }

    @Suppress("MagicNumber")
    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(250)
            if (networkConnectionManager.isNetworkConnected) {
                vaultRepository.sync(forced = false)
            } else {
                sendAction(VaultItemListingsAction.Internal.InternetConnectionErrorReceived)
            }
        }
    }

    private fun handleConfirmOverwriteExistingPasskeyClick(
        action: VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick,
    ) {
        clearDialogState()
        viewModelScope.launch {
            getCipherViewForCredentialOrNull(action.cipherViewId)
                ?.let { registerFido2Credential(it) }
        }
    }

    private fun handleUserVerificationLockOut() {
        bitwardenCredentialManager.isUserVerified = false
        showCredentialManagerErrorDialog(
            BitwardenString.credential_operation_failed_because_user_is_locked_out.asText(),
        )
    }

    private fun handleUserVerificationSuccess(
        action: VaultItemListingsAction.UserVerificationSuccess,
    ) {
        bitwardenCredentialManager.isUserVerified = true
        continueCredentialManagerOperation(action.selectedCipherView)
    }

    private fun handleUserVerificationFail() {
        bitwardenCredentialManager.isUserVerified = false
        showCredentialManagerErrorDialog(
            BitwardenString.credential_operation_failed_because_user_could_not_be_verified.asText(),
        )
    }

    private fun handleUserVerificationCancelled() {
        bitwardenCredentialManager.isUserVerified = false
        clearDialogState()
        state.createCredentialRequest
            ?.let {
                sendEvent(
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        result = CreateCredentialResult.Cancelled,
                    ),
                )
            }
            ?: state.fido2CredentialAssertionRequest
                ?.let {
                    sendEvent(
                        VaultItemListingEvent.CompleteFido2Assertion(
                            result = AssertFido2CredentialResult.Cancelled,
                        ),
                    )
                }
            ?: state.providerGetPasswordCredentialRequest
                ?.let {
                    sendEvent(
                        VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                            result = GetPasswordCredentialResult.Cancelled,
                        ),
                    )
                }
    }

    private fun handleUserVerificationNotSupported(
        action: VaultItemListingsAction.UserVerificationNotSupported,
    ) {
        bitwardenCredentialManager.isUserVerified = false

        val selectedCipherId = action
            .selectedCipherId
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.credential_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
                return
            }

        val activeAccount = authRepository
            .userStateFlow
            .value
            ?.activeAccount
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.credential_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
                return
            }

        if (settingsRepository.isUnlockWithPinEnabled) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState.DialogState.UserVerificationPinPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
                )
            }
        } else if (activeAccount.hasMasterPassword) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState
                        .DialogState
                        .UserVerificationMasterPasswordPrompt(
                            selectedCipherId = selectedCipherId,
                        ),
                )
            }
        } else {
            // Prompt the user to set up a PIN for their account.
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
                )
            }
        }
    }

    private fun handleMasterPasswordUserVerificationSubmit(
        action: VaultItemListingsAction.MasterPasswordUserVerificationSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.password)
            sendAction(
                VaultItemListingsAction.Internal.ValidateUserVerificationPasswordResultReceive(
                    result = result,
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleRetryUserVerificationPasswordVerificationClick(
        action: VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState
                    .DialogState
                    .UserVerificationMasterPasswordPrompt(
                        selectedCipherId = action.selectedCipherId,
                    ),
            )
        }
    }

    private fun handlePinUserVerificationSubmit(
        action: VaultItemListingsAction.PinUserVerificationSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePinUserKey(action.pin)
            sendAction(
                VaultItemListingsAction.Internal.ValidateUserVerificationPinResultReceive(
                    result = result,
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleRetryUserPinVerificationClick(
        action: VaultItemListingsAction.RetryUserVerificationPinVerificationClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinPrompt(
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleUserVerificationPinSetUpSubmit(
        action: VaultItemListingsAction.UserVerificationPinSetUpSubmit,
    ) {
        if (action.pin.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState.DialogState.UserVerificationPinSetUpError(
                        title = null,
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.pin.asText()),
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

        // After storing the PIN, the user can proceed with their original CredentialManager
        // request.
        handleValidAuthentication(selectedCipherId = action.selectedCipherId)
    }

    private fun handleUserVerificationPinSetUpRetryClick(
        action: VaultItemListingsAction.UserVerificationPinSetUpRetryClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt(
                    selectedCipherId = action.selectedCipherId,
                ),
            )
        }
    }

    private fun handleDismissUserVerificationDialogClick() {
        showCredentialManagerErrorDialog(
            BitwardenString.credential_operation_failed_because_user_verification_was_cancelled
                .asText(),
        )
    }

    private fun handleCopySendUrlClick(action: ListingItemOverflowAction.SendAction.CopyUrlClick) {
        clipboardManager.setText(
            text = action.sendUrl,
            toastDescriptorOverride = BitwardenString.send_link.asText(),
        )
    }

    private fun handleDeleteSendClick(action: ListingItemOverflowAction.SendAction.DeleteClick) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = BitwardenString.deleting.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.deleteSend(action.sendId)
            sendAction(VaultItemListingsAction.Internal.DeleteSendResultReceive(result))
        }
    }

    private fun handleShareSendUrlClick(
        action: ListingItemOverflowAction.SendAction.ShareUrlClick,
    ) {
        sendEvent(VaultItemListingEvent.ShowShareSheet(action.sendUrl))
    }

    private fun handleShareCipherDecryptionErrorClick(
        action: VaultItemListingsAction.ShareCipherDecryptionErrorClick,
    ) {
        sendEvent(
            event = VaultItemListingEvent.ShowShareSheet(
                content = action.selectedCipherId,
            ),
        )
    }

    private fun handleRemoveSendPasswordClick(
        action: ListingItemOverflowAction.SendAction.RemovePasswordClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = BitwardenString.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.removePasswordSend(action.sendId)
            sendAction(VaultItemListingsAction.Internal.RemovePasswordSendResultReceive(result))
        }
    }

    private fun handleItemTypeToAddSelected(
        action: VaultItemListingsAction.ItemTypeToAddSelected,
    ) {
        val listingType = state.itemListingType
        val collectionId = (listingType as? VaultItemListingState.ItemListingType.Vault.Collection)
            ?.collectionId
        val folderId = (listingType as? VaultItemListingState.ItemListingType.Vault.Folder)
            ?.folderId
        when (val vaultItemType = action.itemType) {
            CreateVaultItemType.LOGIN,
            CreateVaultItemType.CARD,
            CreateVaultItemType.IDENTITY,
            CreateVaultItemType.SECURE_NOTE,
            CreateVaultItemType.SSH_KEY,
                -> {
                vaultItemType
                    .toVaultItemCipherTypeOrNull()
                    ?.let {
                        sendEvent(
                            VaultItemListingEvent.NavigateToAddVaultItem(
                                vaultItemCipherType = it,
                                selectedCollectionId = collectionId,
                                selectedFolderId = folderId,
                            ),
                        )
                    }
            }

            CreateVaultItemType.FOLDER -> {
                if (listingType is VaultItemListingState.ItemListingType.Vault.Folder) {
                    sendEvent(
                        VaultItemListingEvent.NavigateToAddFolder(
                            parentFolderName = listingType.fullyQualifiedName,
                        ),
                    )
                } else {
                    throw IllegalArgumentException("$listingType does not support adding a folder")
                }
            }
        }
    }

    private fun handleTrustPrivilegedAppClick(
        action: VaultItemListingsAction.TrustPrivilegedAppClick,
    ) {
        clearDialogState()
        state.createCredentialRequest
            ?.let { trustPrivilegedAppAndWaitForCreationResult(request = it) }
            ?: state.getCredentialsRequest
                ?.let { trustPrivilegedAppAndGetCredentials(request = it) }
            ?: state.fido2CredentialAssertionRequest
                ?.let {
                    trustPrivilegedAppAndAuthenticateCredential(
                        request = it,
                        selectedCipherId = action.selectedCipherId,
                    )
                }
    }

    private fun trustPrivilegedAppAndWaitForCreationResult(request: CreateCredentialRequest) {
        val signature = request.callingAppInfo.getSignatureFingerprintAsHexString()
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
                return
            }
        viewModelScope.launch {
            trustPrivilegedApp(
                packageName = request.callingAppInfo.packageName,
                signature = signature,
            )
            // Wait for the user to complete the credential creation flow.
        }
    }

    private fun trustPrivilegedAppAndGetCredentials(request: GetCredentialsRequest) {
        val callingAppInfo = request.callingAppInfo
        val signature = callingAppInfo?.getSignatureFingerprintAsHexString()
        if (callingAppInfo == null || signature.isNullOrEmpty()) {
            showCredentialManagerErrorDialog(
                BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                    .asText(),
            )
            return
        }
        viewModelScope.launch {
            trustPrivilegedApp(
                packageName = callingAppInfo.packageName,
                signature = signature,
            )
            sendAction(
                VaultItemListingsAction.Internal.GetCredentialEntriesResultReceive(
                    userId = request.userId,
                    result = bitwardenCredentialManager.getCredentialEntries(
                        getCredentialsRequest = request,
                    ),
                ),
            )
        }
    }

    private fun trustPrivilegedAppAndAuthenticateCredential(
        request: Fido2CredentialAssertionRequest,
        selectedCipherId: String?,
    ) {
        val signature = request
            .callingAppInfo
            .getSignatureFingerprintAsHexString()
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
                return
            }
        selectedCipherId
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_no_item_was_selected.asText(),
                )
                return
            }
        viewModelScope.launch {
            getCipherViewForCredentialOrNull(selectedCipherId)
                ?.let { cipherView ->
                    trustPrivilegedApp(
                        packageName = request.callingAppInfo.packageName,
                        signature = signature,
                    )
                    authenticateFido2Credential(
                        request = request.providerRequest,
                        cipherView = cipherView,
                    )
                }
        }
    }

    private suspend fun trustPrivilegedApp(
        packageName: String,
        signature: String,
    ) {
        privilegedAppRepository.addTrustedPrivilegedApp(
            packageName = packageName,
            signature = signature,
        )
    }

    private fun createVaultItemTypeSelectionExcludedOptions(): ImmutableList<CreateVaultItemType> {
        // If policy is enable for any organization, exclude the card option
        return if (state.restrictItemTypesPolicyOrgIds.isNotEmpty()) {
            persistentListOf(
                CreateVaultItemType.CARD,
                CreateVaultItemType.FOLDER,
                CreateVaultItemType.SSH_KEY,
            )
        } else {
            persistentListOf(
                CreateVaultItemType.SSH_KEY,
                CreateVaultItemType.FOLDER,
            )
        }
    }

    private fun handleAddVaultItemClick() {
        when (val itemListingType = state.itemListingType) {
            is VaultItemListingState.ItemListingType.Vault.Collection -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                            excludedOptions = createVaultItemTypeSelectionExcludedOptions(),
                        ),
                    )
                }
            }

            is VaultItemListingState.ItemListingType.Vault.Folder -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                            excludedOptions = createVaultItemTypeSelectionExcludedOptions(),
                        ),
                    )
                }
            }

            is VaultItemListingState.ItemListingType.Vault -> {
                sendEvent(
                    VaultItemListingEvent.NavigateToAddVaultItem(
                        vaultItemCipherType = itemListingType.toVaultItemCipherType(),
                    ),
                )
            }

            is VaultItemListingState.ItemListingType.Send -> {
                when (val sendType = itemListingType.toSendItemType()) {
                    SendItemType.FILE -> {
                        if (state.isPremium) {
                            sendEvent(VaultItemListingEvent.NavigateToAddSendItem(sendType))
                        } else {
                            mutableStateFlow.update {
                                it.copy(
                                    dialogState = VaultItemListingState.DialogState.Error(
                                        title = BitwardenString.send.asText(),
                                        message = BitwardenString
                                            .send_file_premium_required
                                            .asText(),
                                    ),
                                )
                            }
                        }
                    }

                    SendItemType.TEXT -> {
                        sendEvent(VaultItemListingEvent.NavigateToAddSendItem(sendType))
                    }
                }
            }
        }
    }

    private fun handleViewSendClick(action: ListingItemOverflowAction.SendAction.ViewClick) {
        sendEvent(
            VaultItemListingEvent.NavigateToViewSendItem(
                id = action.sendId,
                sendType = action.sendType.toSendItemType(),
            ),
        )
    }

    private fun handleEditSendClick(action: ListingItemOverflowAction.SendAction.EditClick) {
        sendEvent(
            event = VaultItemListingEvent.NavigateToEditSendItem(
                id = action.sendId,
                sendType = action.sendType.toSendItemType(),
            ),
        )
    }

    private fun handleItemClick(action: VaultItemListingsAction.ItemClick) {
        state.autofillSelectionData?.let { autofillSelectionData ->
            completeAutofillSelection(
                itemId = action.id,
                autofillSelectionData = autofillSelectionData,
            )
            return
        }
        state.totpData?.let {
            sendEvent(
                event = VaultItemListingEvent.NavigateToEditCipher(
                    cipherId = action.id,
                    cipherType = VaultItemCipherType.LOGIN,
                ),
            )
            return
        }

        state.createCredentialRequest
            ?.let {
                handleItemClickForProviderCreateCredentialRequest(action, it)
                return
            }

        val event = when (val itemType = action.type) {
            is VaultItemListingState.DisplayItem.ItemType.Vault -> {
                VaultItemListingEvent.NavigateToVaultItem(
                    id = action.id,
                    type = itemType.type.toVaultItemCipherType(),
                )
            }

            is VaultItemListingState.DisplayItem.ItemType.Sends -> {
                VaultItemListingEvent.NavigateToViewSendItem(
                    id = action.id,
                    sendType = itemType.type.toSendItemType(),
                )
            }

            VaultItemListingState.DisplayItem.ItemType.DecryptionError -> {
                showCipherDecryptionErrorItemClick(itemId = action.id)
                return
            }
        }

        sendEvent(event)
    }

    private fun showCipherDecryptionErrorItemClick(itemId: String) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CipherDecryptionError(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString
                        .bitwarden_could_not_decrypt_this_vault_item_description_long
                        .asText(),
                    selectedCipherId = itemId,
                ),
            )
        }
    }

    private fun handleItemClickForProviderCreateCredentialRequest(
        action: VaultItemListingsAction.ItemClick,
        createCredentialRequest: CreateCredentialRequest,
    ) {
        viewModelScope.launch {
            getCipherViewForCredentialOrNull(action.id)?.let { cipherView ->
                createCredentialRequest
                    .providerRequest
                    .getCreatePasskeyCredentialRequestOrNull()
                    ?.let {
                        handleItemClickForCreatePublicKeyCredentialRequest(
                            cipherId = action.id,
                            cipherView = cipherView,
                        )
                    }
                    ?: run {
                        sendAction(
                            VaultItemListingsAction.Internal.CredentialOperationFailureReceive(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString
                                    .credential_operation_failed_because_the_request_is_unsupported
                                    .asText(),
                                error = null,
                            ),
                        )
                    }
            }
        }
    }

    private fun handleItemClickForCreatePublicKeyCredentialRequest(
        cipherId: String,
        cipherView: CipherView,
    ) {
        if (cipherView.isActiveWithFido2Credentials) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState
                        .DialogState
                        .OverwritePasskeyConfirmationPrompt(
                            cipherViewId = cipherId,
                        ),
                )
            }
        } else {
            registerFido2Credential(cipherView)
        }
    }

    private fun registerFido2Credential(cipherView: CipherView) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = BitwardenString.saving.asText(),
                ),
            )
        }

        val providerRequest = state
            .createCredentialRequest
            ?.providerRequest
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
                return
            }

        if (bitwardenCredentialManager.isUserVerified) {
            // The user has performed verification implicitly so we continue FIDO 2 registration
            // without checking the request's user verification settings.
            registerFido2CredentialToCipher(cipherView, providerRequest)
        } else {
            performUserVerificationIfRequired(cipherView, providerRequest)
        }
    }

    private fun performUserVerificationIfRequired(
        cipherView: CipherView,
        providerRequest: ProviderCreateCredentialRequest,
    ) {
        val userVerificationRequirement = providerRequest
            .getCreatePasskeyCredentialRequestOrNull()
            ?.let { bitwardenCredentialManager.getUserVerificationRequirement(it) }
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_unsupported
                        .asText(),
                )
                return
            }

        when (userVerificationRequirement) {
            UserVerificationRequirement.DISCOURAGED -> {
                registerFido2CredentialToCipher(
                    cipherView = cipherView,
                    providerRequest = providerRequest,
                )
            }

            UserVerificationRequirement.PREFERRED -> {
                sendEvent(
                    VaultItemListingEvent.CredentialManagerUserVerification(
                        isRequired = false,
                        selectedCipherView = cipherView,
                    ),
                )
            }

            UserVerificationRequirement.REQUIRED -> {
                sendEvent(
                    VaultItemListingEvent.CredentialManagerUserVerification(
                        isRequired = true,
                        selectedCipherView = cipherView,
                    ),
                )
            }
        }
    }

    private fun registerCredentialToCipher(
        cipherView: CipherView,
        providerRequest: ProviderCreateCredentialRequest,
    ) {
        when (providerRequest.callingRequest) {
            is CreatePublicKeyCredentialRequest -> {
                registerFido2CredentialToCipher(
                    cipherView = cipherView,
                    providerRequest = providerRequest,
                )
            }

            else -> {
                showCredentialManagerErrorDialog(
                    BitwardenString.credential_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
            }
        }
    }

    private fun registerFido2CredentialToCipher(
        cipherView: CipherView,
        providerRequest: ProviderCreateCredentialRequest,
    ) {
        val activeUserId = authRepository.activeUserId
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
                return
            }

        val createRequest = providerRequest
            .getCreatePasskeyCredentialRequestOrNull()
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
                return
            }

        viewModelScope.launch {
            val result: Fido2RegisterCredentialResult =
                bitwardenCredentialManager.registerFido2Credential(
                    userId = activeUserId,
                    callingAppInfo = providerRequest.callingAppInfo,
                    createPublicKeyCredentialRequest = createRequest,
                    selectedCipherView = cipherView,
                )
            sendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(result),
            )
        }
    }

    @Suppress("LongMethod")
    private fun authenticateFido2Credential(
        request: ProviderGetCredentialRequest,
        cipherView: CipherView,
    ) {
        val activeUserId = authRepository.activeUserId
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
                return
            }
        val option = request.credentialOptions
            .filterIsInstance<GetPublicKeyCredentialOption>()
            .firstOrNull()
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
                return
            }
        val relyingPartyId = relyingPartyParser.parse(option)
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString
                        .passkey_operation_failed_because_relying_party_cannot_be_identified
                        .asText(),
                )
                return
            }
        viewModelScope.launch {
            val validateOriginResult = originManager
                .validateOrigin(
                    relyingPartyId = relyingPartyId,
                    callingAppInfo = request.callingAppInfo,
                )

            when (validateOriginResult) {
                is ValidateOriginResult.Error -> {
                    handleOriginValidationFail(
                        error = validateOriginResult,
                        callingAppInfo = request.callingAppInfo,
                        selectedCipherId = cipherView.id,
                    )
                }

                is ValidateOriginResult.Success -> {
                    sendAction(
                        VaultItemListingsAction.Internal.Fido2AssertionResultReceive(
                            result = bitwardenCredentialManager.authenticateFido2Credential(
                                userId = activeUserId,
                                selectedCipherView = cipherView,
                                request = option,
                                callingAppInfo = request.callingAppInfo,
                                origin = validateOriginResult.origin,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun handleMasterPasswordRepromptSubmit(
        action: VaultItemListingsAction.MasterPasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.password)
            sendAction(
                VaultItemListingsAction.Internal.ValidatePasswordResultReceive(
                    masterPasswordRepromptData = action.masterPasswordRepromptData,
                    result = result,
                ),
            )
        }
    }

    private fun handleCopyNoteClick(action: ListingItemOverflowAction.VaultAction.CopyNoteClick) {
        viewModelScope.launch {
            getCipherViewOrNull(action.cipherId)?.let {
                clipboardManager.setText(
                    text = it.notes.orEmpty(),
                    toastDescriptorOverride = BitwardenString.notes.asText(),
                )
            }
        }
    }

    private fun handleCopyNumberClick(
        action: ListingItemOverflowAction.VaultAction.CopyNumberClick,
    ) {
        viewModelScope.launch {
            getCipherViewOrNull(action.cipherId)?.let {
                clipboardManager.setText(
                    text = it.card?.number.orEmpty(),
                    toastDescriptorOverride = BitwardenString.number.asText(),
                )
            }
        }
    }

    private fun handleCopyPasswordClick(
        action: ListingItemOverflowAction.VaultAction.CopyPasswordClick,
    ) {
        viewModelScope.launch {
            getCipherViewOrNull(action.cipherId)?.let {
                clipboardManager.setText(
                    text = it.login?.password.orEmpty(),
                    toastDescriptorOverride = BitwardenString.password.asText(),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedPassword(
                        cipherId = action.cipherId,
                    ),
                )
            }
        }
    }

    private fun handleCopySecurityCodeClick(
        action: ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
    ) {
        viewModelScope.launch {
            getCipherViewOrNull(action.cipherId)?.let {
                clipboardManager.setText(
                    text = it.card?.code.orEmpty(),
                    toastDescriptorOverride = BitwardenString.security_code.asText(),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientCopiedCardCode(
                        cipherId = action.cipherId,
                    ),
                )
            }
        }
    }

    private fun handleCopyTotpClick(
        action: ListingItemOverflowAction.VaultAction.CopyTotpClick,
    ) {
        viewModelScope.launch {
            val result = vaultRepository.generateTotp(action.cipherId, clock.instant())
            sendAction(VaultItemListingsAction.Internal.GenerateTotpResultReceive(result))
        }
    }

    private fun handleCopyUsernameClick(
        action: ListingItemOverflowAction.VaultAction.CopyUsernameClick,
    ) {
        clipboardManager.setText(
            text = action.username,
            toastDescriptorOverride = BitwardenString.username.asText(),
        )
    }

    private fun handleEditCipherClick(action: ListingItemOverflowAction.VaultAction.EditClick) {
        sendEvent(
            event = VaultItemListingEvent.NavigateToEditCipher(
                cipherId = action.cipherId,
                cipherType = when (action.cipherType) {
                    CipherType.LOGIN -> VaultItemCipherType.LOGIN
                    CipherType.SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
                    CipherType.CARD -> VaultItemCipherType.CARD
                    CipherType.IDENTITY -> VaultItemCipherType.IDENTITY
                    CipherType.SSH_KEY -> VaultItemCipherType.SSH_KEY
                },
            ),
        )
    }

    private fun handleLaunchCipherUrlClick(
        action: ListingItemOverflowAction.VaultAction.LaunchClick,
    ) {
        sendEvent(VaultItemListingEvent.NavigateToUrl(action.url))
    }

    private fun handleViewCipherClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(
            event = VaultItemListingEvent.NavigateToVaultItem(
                id = action.cipherId,
                type = when (action.cipherType) {
                    CipherType.LOGIN -> VaultItemCipherType.LOGIN
                    CipherType.SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
                    CipherType.CARD -> VaultItemCipherType.CARD
                    CipherType.IDENTITY -> VaultItemCipherType.IDENTITY
                    CipherType.SSH_KEY -> VaultItemCipherType.SSH_KEY
                },
            ),
        )
    }

    private fun handleDismissDialogClick() {
        clearDialogState()
    }

    private fun handleDismissCredentialManagerErrorDialogClick(
        action: VaultItemListingsAction.DismissCredentialManagerErrorDialogClick,
    ) {
        clearDialogState()
        when {
            state.createCredentialRequest != null -> {
                sendEvent(
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        result = CreateCredentialResult.Error(action.message),
                    ),
                )
            }

            state.fido2CredentialAssertionRequest != null -> {
                sendEvent(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        result = AssertFido2CredentialResult.Error(
                            message = action.message,
                        ),
                    ),
                )
            }

            state.providerGetPasswordCredentialRequest != null -> {
                sendEvent(
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        result = GetPasswordCredentialResult.Error(
                            message = action.message,
                        ),
                    ),
                )
            }

            state.getCredentialsRequest != null -> {
                sendEvent(
                    VaultItemListingEvent.CompleteProviderGetCredentialsRequest(
                        result = GetCredentialsResult.Error(
                            message = action.message,
                        ),
                    ),
                )
            }

            else -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = action.message,
                        ),
                    )
                }
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(
            event = if (state.isTotp || state.isAutofill) {
                VaultItemListingEvent.ExitApp
            } else {
                VaultItemListingEvent.NavigateBack
            },
        )
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser(isUserInitiated = true)
    }

    private fun handleSyncClick() {
        if (networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState.DialogState.Loading(
                        message = BitwardenString.syncing.asText(),
                    ),
                )
            }
            vaultRepository.sync(forced = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialogState = VaultItemListingState.DialogState.Error(
                        BitwardenString.internet_connection_required_title.asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
                )
            }
        }
    }

    private fun handleSearchIconClick() {
        val searchType = if (state.autofillSelectionData != null) {
            SearchType.Vault.All
        } else {
            state.itemListingType.toSearchType()
        }

        sendEvent(
            event = VaultItemListingEvent.NavigateToSearchScreen(
                searchType = searchType,
            ),
        )
    }

    private fun handleOverflowOptionClick(action: VaultItemListingsAction.OverflowOptionClick) {
        when (val overflowAction = action.action) {
            is ListingItemOverflowAction.SendAction.CopyUrlClick -> {
                handleCopySendUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.DeleteClick -> {
                handleDeleteSendClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ViewClick -> {
                handleViewSendClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.EditClick -> {
                handleEditSendClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.RemovePasswordClick -> {
                handleRemoveSendPasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ShareUrlClick -> {
                handleShareSendUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyNoteClick -> {
                handleCopyNoteClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyNumberClick -> {
                handleCopyNumberClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyPasswordClick -> {
                handleCopyPasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopySecurityCodeClick -> {
                handleCopySecurityCodeClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyTotpClick -> {
                handleCopyTotpClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyUsernameClick -> {
                handleCopyUsernameClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.EditClick -> {
                handleEditCipherClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.LaunchClick -> {
                handleLaunchCipherUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.ViewClick -> {
                handleViewCipherClick(overflowAction)
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleInternalAction(action: VaultItemListingsAction.Internal) {
        when (action) {
            is VaultItemListingsAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is VaultItemListingsAction.Internal.DeleteSendResultReceive -> {
                handleDeleteSendResultReceive(action)
            }

            is VaultItemListingsAction.Internal.RemovePasswordSendResultReceive -> {
                handleRemovePasswordSendResultReceive(action)
            }

            is VaultItemListingsAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemListingsAction.Internal.IconLoadingSettingReceive -> {
                handleIconsSettingReceived(action)
            }

            is VaultItemListingsAction.Internal.GenerateTotpResultReceive -> {
                handleGenerateTotpResultReceive(action)
            }

            is VaultItemListingsAction.Internal.ValidatePasswordResultReceive -> {
                handleMasterPasswordRepromptResultReceive(action)
            }

            is VaultItemListingsAction.Internal.ValidateUserVerificationPasswordResultReceive -> {
                handleValidateUserVerificationPasswordResultReceive(action)
            }

            is VaultItemListingsAction.Internal.ValidateUserVerificationPinResultReceive -> {
                handleValidateUserVerificationPinResultReceive(action)
            }

            is VaultItemListingsAction.Internal.PolicyUpdateReceive -> {
                handlePolicyUpdateReceive(action)
            }

            is VaultItemListingsAction.Internal.CreateCredentialRequestReceive -> {
                handleRegisterCredentialRequestReceive(action)
            }

            is VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive -> {
                handleFido2RegisterCredentialResultReceive(action)
            }

            is VaultItemListingsAction.Internal.Fido2AssertionDataReceive -> {
                handleFido2AssertionDataReceive(action)
            }

            is VaultItemListingsAction.Internal.Fido2AssertionResultReceive -> {
                handleFido2AssertionResultReceive(action)
            }

            is VaultItemListingsAction.Internal.ProviderGetPasswordCredentialRequestReceive -> {
                handleProviderGetPasswordCredentialRequestReceive(action)
            }

            VaultItemListingsAction.Internal.InternetConnectionErrorReceived -> {
                handleInternetConnectionErrorReceived()
            }

            is VaultItemListingsAction.Internal.GetCredentialEntriesResultReceive -> {
                handleGetCredentialEntriesResultReceive(action)
            }

            is VaultItemListingsAction.Internal.RestrictItemTypesPolicyUpdateReceive -> {
                handleRestrictItemTypesPolicyUpdateReceive(action)
            }

            is VaultItemListingsAction.Internal.SnackbarDataReceived -> {
                handleSnackbarDataReceived(action)
            }

            is VaultItemListingsAction.Internal.DecryptCipherErrorReceive -> {
                handleDecryptCipherErrorReceive(action)
            }

            is VaultItemListingsAction.Internal.CredentialOperationFailureReceive -> {
                handleCredentialOperationFailureReceive(action)
            }
        }
    }

    private fun handleCredentialOperationFailureReceive(
        action: VaultItemListingsAction.Internal.CredentialOperationFailureReceive,
    ) {
        showCredentialManagerErrorDialog(
            title = action.title,
            message = action.message,
            error = action.error,
        )
    }

    private fun handleDecryptCipherErrorReceive(
        action: VaultItemListingsAction.Internal.DecryptCipherErrorReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Error(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                    throwable = action.error,
                ),
            )
        }
    }

    private fun handleRestrictItemTypesPolicyUpdateReceive(
        action: VaultItemListingsAction.Internal.RestrictItemTypesPolicyUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                restrictItemTypesPolicyOrgIds = action
                    .restrictItemTypesPolicyOrdIds
                    .toImmutableList(),
            )
        }

        vaultRepository.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData, clearDialogState = false)
        }
    }

    private fun handleInternetConnectionErrorReceived() {
        mutableStateFlow.update {
            it.copy(
                isRefreshing = false,
                dialogState = VaultItemListingState.DialogState.Error(
                    BitwardenString.internet_connection_required_title.asText(),
                    BitwardenString.internet_connection_required_message.asText(),
                ),
            )
        }
    }

    private fun handlePullToRefreshEnableReceive(
        action: VaultItemListingsAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleDeleteSendResultReceive(
        action: VaultItemListingsAction.Internal.DeleteSendResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                clearDialogState()
                sendEvent(VaultItemListingEvent.ShowSnackbar(BitwardenString.send_deleted.asText()))
            }
        }
    }

    private fun handleRemovePasswordSendResultReceive(
        action: VaultItemListingsAction.Internal.RemovePasswordSendResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                clearDialogState()
                sendEvent(
                    VaultItemListingEvent.ShowSnackbar(
                        BitwardenString.password_removed.asText(),
                    ),
                )
            }
        }
    }

    private fun handleGenerateTotpResultReceive(
        action: VaultItemListingsAction.Internal.GenerateTotpResultReceive,
    ) {
        when (val result = action.result) {
            is GenerateTotpResult.Error -> Unit
            is GenerateTotpResult.Success -> {
                clipboardManager.setText(
                    text = result.code,
                    toastDescriptorOverride = BitwardenString.totp.asText(),
                )
            }
        }
    }

    private fun handleVaultDataReceive(
        action: VaultItemListingsAction.Internal.VaultDataReceive,
    ) {
        if (state.activeAccountSummary.userId != authRepository.userStateFlow.value?.activeUserId) {
            // We are in the process of switching accounts, so we should ignore any updates here
            // to avoid any unnecessary visual changes.
            return
        }

        when (val vaultData = action.vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun handleIconsSettingReceived(
        action: VaultItemListingsAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled)
        }

        vaultRepository.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData, clearDialogState = false)
        }
    }

    private fun handleMasterPasswordRepromptResultReceive(
        action: VaultItemListingsAction.Internal.ValidatePasswordResultReceive,
    ) {
        clearDialogState()

        when (val result = action.result) {
            is ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = null,
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                if (!result.isValid) {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = VaultItemListingState.DialogState.Error(
                                title = null,
                                message = BitwardenString.invalid_master_password.asText(),
                            ),
                        )
                    }
                    return
                }
                handleMasterPasswordRepromptData(
                    data = action.masterPasswordRepromptData,
                )
            }
        }
    }

    private fun handleMasterPasswordRepromptData(
        data: MasterPasswordRepromptData,
    ) {
        when (data) {
            is MasterPasswordRepromptData.Autofill -> {
                // Complete the autofill selection flow
                completeAutofillSelection(
                    itemId = data.cipherId,
                    autofillSelectionData = state.autofillSelectionData ?: return,
                )
            }

            is MasterPasswordRepromptData.OverflowItem -> {
                handleOverflowOptionClick(
                    VaultItemListingsAction.OverflowOptionClick(data.action),
                )
            }

            is MasterPasswordRepromptData.ViewItem -> {
                trySendAction(VaultItemListingsAction.ItemClick(id = data.id, type = data.itemType))
            }
        }
    }

    private fun handleValidateUserVerificationPasswordResultReceive(
        action: VaultItemListingsAction.Internal.ValidateUserVerificationPasswordResultReceive,
    ) {
        clearDialogState()

        when (action.result) {
            is ValidatePasswordResult.Error -> {
                showUserVerificationErrorDialog()
            }

            is ValidatePasswordResult.Success -> {
                if (action.result.isValid) {
                    handleValidAuthentication(action.selectedCipherId)
                } else {
                    handleInvalidAuthentication(
                        errorDialogState = VaultItemListingState
                            .DialogState
                            .UserVerificationMasterPasswordError(
                                title = null,
                                message = BitwardenString.invalid_master_password.asText(),
                                selectedCipherId = action.selectedCipherId,
                            ),
                    )
                }
            }
        }
    }

    private fun handleValidateUserVerificationPinResultReceive(
        action: VaultItemListingsAction.Internal.ValidateUserVerificationPinResultReceive,
    ) {
        clearDialogState()

        when (action.result) {
            is ValidatePinResult.Error -> {
                showUserVerificationErrorDialog()
            }

            is ValidatePinResult.Success -> {
                if (action.result.isValid) {
                    handleValidAuthentication(action.selectedCipherId)
                } else {
                    handleInvalidAuthentication(
                        errorDialogState = VaultItemListingState
                            .DialogState
                            .UserVerificationPinError(
                                title = null,
                                message = BitwardenString.invalid_pin.asText(),
                                selectedCipherId = action.selectedCipherId,
                            ),
                    )
                }
            }
        }
    }

    private fun handleInvalidAuthentication(
        errorDialogState: VaultItemListingState.DialogState,
    ) {
        bitwardenCredentialManager.authenticationAttempts += 1
        if (bitwardenCredentialManager.hasAuthenticationAttemptsRemaining()) {
            mutableStateFlow.update {
                it.copy(dialogState = errorDialogState)
            }
        } else {
            showCredentialManagerErrorDialog(
                BitwardenString
                    .credential_operation_failed_because_user_verification_attempts_exceeded
                    .asText(),
            )
        }
    }

    private fun handleValidAuthentication(selectedCipherId: String) {
        bitwardenCredentialManager.isUserVerified = true
        bitwardenCredentialManager.authenticationAttempts = 0

        viewModelScope.launch {
            getCipherViewForCredentialOrNull(selectedCipherId)
                ?.let { cipherView -> continueCredentialManagerOperation(cipherView) }
        }
    }

    private fun completeAutofillSelection(
        itemId: String,
        autofillSelectionData: AutofillSelectionData,
    ) {
        viewModelScope.launch {
            val cipherView = getCipherViewOrNull(cipherId = itemId) ?: return@launch
            when (autofillSelectionData.framework) {
                AutofillSelectionData.Framework.ACCESSIBILITY -> {
                    accessibilitySelectionManager.emitAccessibilitySelection(
                        cipherView = cipherView,
                    )
                }

                AutofillSelectionData.Framework.AUTOFILL -> {
                    autofillSelectionManager.emitAutofillSelection(cipherView = cipherView)
                }
            }
        }
    }

    private fun continueCredentialManagerOperation(cipherView: CipherView) {
        state.createCredentialRequest
            ?.providerRequest
            ?.let { request ->
                registerCredentialToCipher(
                    cipherView = cipherView,
                    providerRequest = request,
                )
            }
            ?: state.fido2CredentialAssertionRequest
                ?.providerRequest
                ?.let { request ->
                    authenticateFido2Credential(
                        request = request,
                        cipherView = cipherView,
                    )
                }
            ?: state.providerGetPasswordCredentialRequest
                ?.providerRequest
                ?.let {
                    handlePasswordCredentialResult(
                        selectedCipher = cipherView,
                    )
                }
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_the_request_is_invalid
                        .asText(),
                )
            }
    }
    //endregion VaultItemListing Handlers

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        val data = vaultData.data
        if (data != null) {
            updateStateWithVaultData(vaultData = data, clearDialogState = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                    dialogState = null,
                )
            }
        }
        mutableStateFlow.update { it.copy(isRefreshing = false) }
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)

        state.getCredentialsRequest
            ?.let { handleProviderGetCredentialsRequest(it) }
            ?: state.fido2CredentialAssertionRequest
                ?.let { request ->
                    trySendAction(
                        VaultItemListingsAction.Internal.Fido2AssertionDataReceive(
                            data = request,
                        ),
                    )
                }
            ?: state.providerGetPasswordCredentialRequest
                ?.let { request ->
                    trySendAction(
                        VaultItemListingsAction
                            .Internal
                            .ProviderGetPasswordCredentialRequestReceive(
                                data = request,
                            ),
                    )
                }
            ?: mutableStateFlow.update { it.copy(isRefreshing = false) }
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        val data = vaultData.data
        if (data != null) {
            updateStateWithVaultData(vaultData = data, clearDialogState = true)
        } else {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                BitwardenString.internet_connection_required_message.asText(),
                            ),
                    ),
                    dialogState = null,
                )
            }
        }
        mutableStateFlow.update { it.copy(isRefreshing = false) }
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = false)
    }

    private fun handlePolicyUpdateReceive(
        action: VaultItemListingsAction.Internal.PolicyUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                policyDisablesSend = action.policyDisablesSend,
            )
        }
    }

    private fun handleRegisterCredentialRequestReceive(
        action: VaultItemListingsAction.Internal.CreateCredentialRequestReceive,
    ) {
        when (action.request.providerRequest.callingRequest) {
            is CreatePublicKeyCredentialRequest -> {
                handleRegisterFido2CredentialRequestReceive(action)
            }

            is CreatePasswordRequest -> {
                observeVaultData()
            }

            else -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState =
                            VaultItemListingState.DialogState.CredentialManagerOperationFail(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleRegisterFido2CredentialRequestReceive(
        action: VaultItemListingsAction.Internal.CreateCredentialRequestReceive,
    ) {
        val relyingPartyId = action.request.providerRequest
            .getCreatePasskeyCredentialRequestOrNull()
            ?.let { relyingPartyParser.parse(it) }
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString
                        .passkey_operation_failed_because_relying_party_cannot_be_identified
                        .asText(),
                )
                return
            }
        viewModelScope.launch {
            val validateOriginResult = originManager
                .validateOrigin(
                    relyingPartyId = relyingPartyId,
                    callingAppInfo = action.request.callingAppInfo,
                )
            when (validateOriginResult) {
                is ValidateOriginResult.Error -> {
                    handleOriginValidationFail(
                        error = validateOriginResult,
                        callingAppInfo = action.request.callingAppInfo,
                        selectedCipherId = null,
                    )
                }

                is ValidateOriginResult.Success -> {
                    observeVaultData()
                }
            }
        }
    }

    private fun handleFido2RegisterCredentialResultReceive(
        action: VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive,
    ) {
        clearDialogState()
        when (action.result) {
            is Fido2RegisterCredentialResult.Error -> {
                handleRegisterFido2CredentialResultErrorReceive(action.result)
            }

            is Fido2RegisterCredentialResult.Success -> {
                // This must be a toast because we are finishing the activity and we want the
                // user to have time to see the message.
                toastManager.show(messageId = BitwardenString.item_updated)
                sendEvent(
                    VaultItemListingEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Success.Fido2CredentialRegistered(
                            responseJson = action.result.responseJson,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleRegisterFido2CredentialResultErrorReceive(
        error: Fido2RegisterCredentialResult.Error,
    ) {
        // This must be a toast because we are finishing the activity and we want the
        // user to have time to see the message.
        toastManager.show(messageId = BitwardenString.an_error_has_occurred)
        sendEvent(
            VaultItemListingEvent.CompleteCredentialRegistration(
                CreateCredentialResult.Error(
                    message = error.messageResourceId.asText(),
                ),
            ),
        )
    }

    private fun handleProviderGetCredentialsRequest(
        request: GetCredentialsRequest,
    ) {
        if (request.beginGetPublicKeyCredentialOptions.isNotEmpty()) {
            handleProviderGetPublicKeyCredentialsRequestOriginValidation(request)
        } else {
            viewModelScope.launch {
                sendAction(
                    VaultItemListingsAction.Internal.GetCredentialEntriesResultReceive(
                        userId = request.userId,
                        result = bitwardenCredentialManager.getCredentialEntries(
                            getCredentialsRequest = request,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleProviderGetPublicKeyCredentialsRequestOriginValidation(
        request: GetCredentialsRequest,
    ) {
        val callingAppInfo = request.callingAppInfo
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString.passkey_operation_failed_because_app_could_not_be_verified
                        .asText(),
                )
                return
            }

        val relyingPartyId = request
            .beginGetPublicKeyCredentialOptions
            .mapNotNull { relyingPartyParser.parse(it) }
            .distinct()
            .firstOrNull()
            ?: run {
                showCredentialManagerErrorDialog(
                    BitwardenString
                        .passkey_operation_failed_because_relying_party_cannot_be_identified
                        .asText(),
                )
                return
            }

        viewModelScope.launch {
            val validateOriginResult = originManager.validateOrigin(
                relyingPartyId = relyingPartyId,
                callingAppInfo = callingAppInfo,
            )
            when (validateOriginResult) {
                is ValidateOriginResult.Success -> {
                    sendAction(
                        VaultItemListingsAction.Internal.GetCredentialEntriesResultReceive(
                            userId = request.userId,
                            result = bitwardenCredentialManager.getCredentialEntries(
                                getCredentialsRequest = request,
                            ),
                        ),
                    )
                }

                is ValidateOriginResult.Error -> {
                    handleOriginValidationFail(
                        error = validateOriginResult,
                        callingAppInfo = callingAppInfo,
                        selectedCipherId = null,
                    )
                    return@launch
                }
            }
        }
    }

    private fun handleOriginValidationFail(
        error: ValidateOriginResult.Error,
        callingAppInfo: CallingAppInfo,
        selectedCipherId: String?,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = when {
                    shouldShowTrustPrompt(error) -> {
                        VaultItemListingState.DialogState.TrustPrivilegedAddPrompt(
                            message = BitwardenString
                                .passkey_operation_failed_because_browser_x_is_not_trusted
                                .asText(callingAppInfo.packageName),
                            selectedCipherId = selectedCipherId,
                        )
                    }

                    else -> {
                        VaultItemListingState.DialogState.CredentialManagerOperationFail(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = error.messageResourceId.asText(),
                        )
                    }
                },
            )
        }
    }

    private fun shouldShowTrustPrompt(error: ValidateOriginResult.Error): Boolean =
        error is ValidateOriginResult.Error.PrivilegedAppNotAllowed

    private fun handleFido2AssertionDataReceive(
        action: VaultItemListingsAction.Internal.Fido2AssertionDataReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val request = action.data
            getCipherViewForCredentialOrNull(request.cipherId)
                ?.let { cipherView ->
                    if (state.hasMasterPassword &&
                        cipherView.reprompt == CipherRepromptType.PASSWORD
                    ) {
                        repromptMasterPasswordForUserVerification(request.cipherId)
                    } else {
                        verifyUserAndAuthenticateCredential(
                            request = request.providerRequest,
                            selectedCipher = cipherView,
                        )
                    }
                }
        }
    }

    private fun handleProviderGetPasswordCredentialRequestReceive(
        action: VaultItemListingsAction.Internal.ProviderGetPasswordCredentialRequestReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
        }

        val cipherListView = vaultRepository.vaultDataStateFlow.value.data
            ?.decryptCipherListResult
            ?.successes
            ?.find { it.id == action.data.cipherId }
            ?: run {
                sendCredentialItemNotFoundError()
                return
            }

        if (
            state.hasMasterPassword &&
            cipherListView.reprompt == CipherRepromptType.PASSWORD
        ) {
            repromptMasterPasswordForUserVerification(action.data.cipherId)
            return
        }

        viewModelScope.launch {
            val request = action.data
            getCipherViewForCredentialOrNull(request.cipherId)
                ?.let { cipherView ->
                    handlePasswordCredentialResult(
                        selectedCipher = cipherView,
                    )
                }
        }
    }

    private suspend fun sendCredentialDecryptionError(throwable: Throwable?) {
        sendAction(
            VaultItemListingsAction.Internal.CredentialOperationFailureReceive(
                title = BitwardenString.decryption_error.asText(),
                message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                error = throwable,
            ),
        )
    }

    private fun sendCredentialItemNotFoundError() {
        trySendAction(
            VaultItemListingsAction.Internal.CredentialOperationFailureReceive(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = BitwardenString
                    .credential_operation_failed_because_the_selected_item_does_not_exist
                    .asText(),
                error = null,
            ),
        )
    }

    private fun repromptMasterPasswordForUserVerification(cipherId: String) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState
                    .DialogState
                    .UserVerificationMasterPasswordPrompt(
                        selectedCipherId = cipherId,
                    ),
            )
        }
    }

    private fun verifyUserAndAuthenticateCredential(
        request: ProviderGetCredentialRequest,
        selectedCipher: CipherView,
    ) {

        if (bitwardenCredentialManager.isUserVerified) {
            authenticateFido2Credential(
                request = request,
                cipherView = selectedCipher,
            )
            return
        }

        val userVerificationRequirement =
            bitwardenCredentialManager.getUserVerificationRequirement(request)
        when (userVerificationRequirement) {
            UserVerificationRequirement.DISCOURAGED -> {
                authenticateFido2Credential(
                    request = request,
                    cipherView = selectedCipher,
                )
            }

            UserVerificationRequirement.PREFERRED -> {
                sendUserVerificationEvent(isRequired = false, selectedCipher = selectedCipher)
            }

            UserVerificationRequirement.REQUIRED -> {
                sendUserVerificationEvent(isRequired = true, selectedCipher = selectedCipher)
            }
        }
    }

    private fun handlePasswordCredentialResult(
        selectedCipher: CipherView,
    ) {
        viewModelScope.launch {
            bitwardenCredentialManager.isUserVerified = false
            clearDialogState()

            val event = selectedCipher.login
                ?.let { credential ->
                    VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                        GetPasswordCredentialResult.Success(credential = credential),
                    )
                }
                ?: VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(
                    GetPasswordCredentialResult.Error(
                        message = BitwardenString
                            .password_operation_failed_because_the_selected_item_does_not_exist
                            .asText(),
                    ),
                )

            sendEvent(event)
        }
    }

    private fun handleFido2AssertionResultReceive(
        action: VaultItemListingsAction.Internal.Fido2AssertionResultReceive,
    ) {
        bitwardenCredentialManager.isUserVerified = false
        clearDialogState()
        when (action.result) {
            is Fido2CredentialAssertionResult.Error -> {
                sendEvent(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        AssertFido2CredentialResult.Error(
                            message = action.result.messageResourceId.asText(),
                        ),
                    ),
                )
            }

            is Fido2CredentialAssertionResult.Success -> {
                sendEvent(
                    VaultItemListingEvent.CompleteFido2Assertion(
                        AssertFido2CredentialResult.Success(
                            responseJson = action.result.responseJson,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleGetCredentialEntriesResultReceive(
        action: VaultItemListingsAction.Internal.GetCredentialEntriesResultReceive,
    ) {
        action.result
            .onFailure {
                showCredentialManagerErrorDialog(
                    message = BitwardenString.generic_error_message.asText(),
                )
            }
            .onSuccess { credentialEntries ->
                sendEvent(
                    VaultItemListingEvent.CompleteProviderGetCredentialsRequest(
                        GetCredentialsResult.Success(
                            credentialEntries = credentialEntries,
                            userId = action.userId,
                        ),
                    ),
                )
            }
    }

    private fun handleSnackbarDataReceived(
        action: VaultItemListingsAction.Internal.SnackbarDataReceived,
    ) {
        sendEvent(VaultItemListingEvent.ShowSnackbar(action.data))
    }

    private fun updateStateWithVaultData(vaultData: VaultData, clearDialogState: Boolean) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                itemListingType = currentState
                    .itemListingType
                    .updateWithAdditionalDataIfNecessary(
                        folderList = vaultData.folderViewList,
                        collectionList = vaultData
                            .collectionViewList
                            .toFilteredList(state.vaultFilterType),
                    ),
                viewState = when (val listingType = currentState.itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault -> {
                        vaultData.toViewState(
                            itemListingType = listingType,
                            vaultFilterType = state.vaultFilterType,
                            hasMasterPassword = state.hasMasterPassword,
                            baseIconUrl = state.baseIconUrl,
                            isIconLoadingDisabled = state.isIconLoadingDisabled,
                            autofillSelectionData = state.autofillSelectionData,
                            createCredentialRequestData = state.createCredentialRequest,
                            totpData = state.totpData,
                            isPremiumUser = state.isPremium,
                            restrictItemTypesPolicyOrgIds = state.restrictItemTypesPolicyOrgIds,
                        )
                    }

                    is VaultItemListingState.ItemListingType.Send -> {
                        vaultData
                            .sendViewList
                            .filter { sendView ->
                                sendView.determineListingPredicate(listingType)
                            }
                            .toViewState(
                                itemListingType = listingType,
                                baseWebSendUrl = state.baseWebSendUrl,
                                clock = clock,
                            )
                    }
                },
                dialogState = currentState.dialogState.takeUnless { clearDialogState },
            )
        }
    }

    /**
     * Attempts to decrypt a cipher with the given [cipherId], or null.
     */
    private suspend fun getCipherViewOrNull(
        cipherId: String,
    ) = when (val result = vaultRepository.getCipher(cipherId)) {
        is GetCipherResult.Success -> result.cipherView
        is GetCipherResult.Failure -> {
            Timber.e(result.error, "Failed to decrypt cipher.")
            sendAction(
                VaultItemListingsAction.Internal.DecryptCipherErrorReceive(result.error),
            )
            null
        }

        is GetCipherResult.CipherNotFound -> {
            Timber.e("Cipher not found.")
            sendAction(
                VaultItemListingsAction.Internal.DecryptCipherErrorReceive(error = null),
            )
            null
        }
    }

    private suspend fun getCipherViewForCredentialOrNull(cipherId: String): CipherView? =
        when (val result = vaultRepository.getCipher(cipherId)) {
            GetCipherResult.CipherNotFound -> {
                sendCredentialItemNotFoundError()
                null
            }

            is GetCipherResult.Failure -> {
                sendCredentialDecryptionError(result.error)
                null
            }

            is GetCipherResult.Success -> result.cipherView
        }

    private fun sendUserVerificationEvent(isRequired: Boolean, selectedCipher: CipherView) {
        sendEvent(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = isRequired,
                selectedCipherView = selectedCipher,
            ),
        )
    }

    /**
     * Takes the given vault data and filters it for autofill if necessary.
     */
    private suspend fun DataState<VaultData>.filterForAutofillIfNecessary(): DataState<VaultData> {
        val autofillSelectionData = state.autofillSelectionData ?: return this
        return when (autofillSelectionData.type) {
            AutofillSelectionData.Type.CARD -> {
                this.map { vaultData ->
                    vaultData.copy(
                        decryptCipherListResult = vaultData.decryptCipherListResult.copy(
                            successes = vaultData.decryptCipherListResult.successes
                                .filter { it.type is CipherListViewType.Card },
                            failures = emptyList(),
                        ),
                    )
                }
            }

            AutofillSelectionData.Type.LOGIN -> {
                val matchUri = state
                    .autofillSelectionData
                    ?.uri
                    ?: return this
                this.map { vaultData ->
                    vaultData.copy(
                        decryptCipherListResult = vaultData.decryptCipherListResult.copy(
                            successes = cipherMatchingManager.filterCiphersForMatches(
                                cipherListViews = vaultData.decryptCipherListResult.successes,
                                matchUri = matchUri,
                            ),
                            failures = emptyList(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Takes the given vault data and filters it for credential creation if necessary.
     */
    @Suppress("MaxLineLength")
    private suspend fun DataState<VaultData>.filterForCredentialCreationIfNecessary(): DataState<VaultData> {
        val request = state.createCredentialRequest ?: return this
        return this.map { vaultData ->
            val matchUri = request.providerRequest.callingRequest.origin
                ?: request.callingAppInfo.packageName.toAndroidAppUriString()

            vaultData.copy(
                decryptCipherListResult = vaultData.decryptCipherListResult.copy(
                    successes = cipherMatchingManager.filterCiphersForMatches(
                        cipherListViews = vaultData.decryptCipherListResult.successes,
                        matchUri = matchUri,
                    ),
                    failures = emptyList(),
                ),
            )
        }
    }

    /**
     * Takes the given vault data and filters it for totp data.
     */
    private fun DataState<VaultData>.filterForTotpIfNecessary(): DataState<VaultData> {
        val totpData = state.totpData ?: return this
        val query = totpData.issuer ?: totpData.accountName ?: return this
        return this.map { vaultData ->
            vaultData.copy(
                decryptCipherListResult = vaultData.decryptCipherListResult.copy(
                    successes = vaultData
                        .decryptCipherListResult
                        .successes
                        .filterAndOrganize(
                            searchTypeData = SearchTypeData.Vault.Logins,
                            searchTerm = query,
                        ),
                    failures = emptyList(),
                ),
            )
        }
    }

    private fun showUserVerificationErrorDialog() {
        showCredentialManagerErrorDialog(
            message = BitwardenString
                .credential_operation_failed_because_user_could_not_be_verified
                .asText(),
        )
    }

    private fun showCredentialManagerErrorDialog(
        message: Text,
        title: Text = BitwardenString.an_error_has_occurred.asText(),
        error: Throwable? = null,
    ) {
        bitwardenCredentialManager.authenticationAttempts = 0
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = title,
                    message = message,
                    throwable = error,
                ),
            )
        }
    }

    private fun clearDialogState() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }
}

/**
 * Models state for the [VaultItemListingScreen].
 */
data class VaultItemListingState(
    val itemListingType: ItemListingType,
    val activeAccountSummary: AccountSummary,
    val accountSummaries: List<AccountSummary>,
    val viewState: ViewState,
    val vaultFilterType: VaultFilterType,
    val baseWebSendUrl: String,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    val dialogState: DialogState?,
    val policyDisablesSend: Boolean,
    val restrictItemTypesPolicyOrgIds: ImmutableList<String>,
    // Internal
    private val isPullToRefreshSettingEnabled: Boolean,
    val totpData: TotpData? = null,
    val autofillSelectionData: AutofillSelectionData? = null,
    val createCredentialRequest: CreateCredentialRequest? = null,
    val fido2CredentialAssertionRequest: Fido2CredentialAssertionRequest? = null,
    val providerGetPasswordCredentialRequest: ProviderGetPasswordCredentialRequest? = null,
    val getCredentialsRequest: GetCredentialsRequest? = null,
    val hasMasterPassword: Boolean,
    val isPremium: Boolean,
    val isRefreshing: Boolean,
) {
    /**
     * Whether or not the add FAB should be shown.
     */
    val hasAddItemFabButton: Boolean
        get() = if (restrictItemTypesPolicyOrgIds.isNotEmpty() &&
            itemListingType == ItemListingType.Vault.Card
        ) {
            false
        } else {
            itemListingType.hasFab ||
                (viewState as? ViewState.NoItems)?.shouldShowAddButton == true
        }

    /**
     * Whether or not this represents a listing screen for autofill.
     */
    val isAutofill: Boolean
        get() = autofillSelectionData != null

    /**
     * Whether or not this represents a listing screen for CredentialManager creation requests.
     */
    val isCredentialManagerCreation: Boolean
        get() = createCredentialRequest != null

    /**
     * Whether or not this represents a listing screen for totp.
     */
    val isTotp: Boolean get() = totpData != null

    /**
     * A displayable title for the AppBar.
     */
    val appBarTitle: Text
        get() = autofillSelectionData
            ?.let { data ->
                data.uri
                    ?.toHostOrPathOrNull()
                    ?.let {
                        when (data.type) {
                            AutofillSelectionData.Type.CARD -> {
                                BitwardenString.select_a_card_for_x.asText(it)
                            }

                            AutofillSelectionData.Type.LOGIN -> {
                                BitwardenString.items_for_uri.asText(it)
                            }
                        }
                    }
            }
            ?: createCredentialRequest
                ?.relyingPartyIdOrNull
                ?.let { BitwardenString.items_for_uri.asText(it) }
            ?: totpData?.let {
                BitwardenString.items_for_uri.asText(
                    it.issuer ?: it.accountName ?: "--",
                )
            }
            ?: itemListingType.titleText

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Whether or not the account switcher should be shown.
     */
    val shouldShowAccountSwitcher: Boolean
        get() = isAutofill || isCredentialManagerCreation || isTotp

    /**
     * Whether or not the navigation icon should be shown.
     */
    val shouldShowNavigationIcon: Boolean
        get() = !isAutofill && !isCredentialManagerCreation && !isTotp

    /**
     * Whether or not the overflow menu should be shown.
     */
    val shouldShowOverflowMenu: Boolean
        get() = !isAutofill && !isCredentialManagerCreation && !isTotp

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a dialog indicating that a cipher decryption error occurred.
         */
        @Parcelize
        data class CipherDecryptionError(
            val title: Text,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog indicating that a CredentialManager operation encountered an error.
         */
        @Parcelize
        data class CredentialManagerOperationFail(
            val title: Text,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the overwrite passkey confirmation prompt to the user.
         */
        @Parcelize
        data class OverwritePasskeyConfirmationPrompt(val cipherViewId: String) : DialogState()

        /**
         * Represents a dialog to prompt the user for their master password as part of the
         * CredentialManager user verification flow.
         */
        @Parcelize
        data class UserVerificationMasterPasswordPrompt(
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog to alert the user that their password for the CredentialManager user
         * verification flow was incorrect and to retry.
         */
        @Parcelize
        data class UserVerificationMasterPasswordError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog to prompt the user for their PIN as part of the CredentialManager
         * user verification flow.
         */
        @Parcelize
        data class UserVerificationPinPrompt(
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog to alert the user that their PIN for the CredentialManager user
         * verification flow was incorrect and to retry.
         */
        @Parcelize
        data class UserVerificationPinError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog to prompt the user to set up a PIN for the CredentialManager user
         * verification flow.
         */
        @Parcelize
        data class UserVerificationPinSetUpPrompt(
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a dialog to alert the user that the PIN is a required field.
         */
        @Parcelize
        data class UserVerificationPinSetUpError(
            val title: Text?,
            val message: Text,
            val selectedCipherId: String,
        ) : DialogState()

        /**
         * Represents a selection dialog to choose a vault item type to add to folder.
         */
        @Parcelize
        data class VaultItemTypeSelection(
            val excludedOptions: ImmutableList<CreateVaultItemType>,
        ) : DialogState()

        /**
         * Represents a dialog to prompting the user to trust a privileged app for Credential
         * Manager operations.
         */
        @Parcelize
        data class TrustPrivilegedAddPrompt(
            val message: Text,
            val selectedCipherId: String?,
        ) : DialogState()
    }

    /**
     * Represents the specific view states for the [VaultItemListingScreen].
     */
    sealed class ViewState {
        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Loading state for the [VaultItemListingScreen],
         * signifying that the content is being processed.
         */
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
        }

        /**
         * Represents a state where the [VaultItemListingScreen] has no items to display.
         */
        data class NoItems(
            val message: Text,
            val buttonText: Text,
            val header: Text? = null,
            @field:DrawableRes val vectorRes: Int? = null,
            val shouldShowAddButton: Boolean,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Content state for the [VaultItemListingScreen] showing the actual content or items.
         *
         * @property displayItemList List of items to display.
         * @property displayFolderList list of folders to display.
         * @property displayCollectionList list of collections to display.
         */
        data class Content(
            val displayItemList: List<DisplayItem>,
            val displayFolderList: List<FolderDisplayItem>,
            val displayCollectionList: List<CollectionDisplayItem>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Represents an error state for the [VaultItemListingScreen].
         *
         * @property message Error message to display.
         */
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }
    }

    /**
     * An item to be displayed.
     *
     * @property id the id of the item.
     * @property title title of the item.
     * @property titleTestTag The test tag associated with the [title].
     * @property secondSubtitle The second subtitle of the item (nullable).
     * @property secondSubtitleTestTag The test tag associated with the [secondSubtitle].
     * @property subtitle subtitle of the item (nullable).
     * @property subtitleTestTag The test tag associated with the [subtitle].
     * @property iconData data for the icon to be displayed (nullable).
     * @property iconTestTag The test tag for the icon (nullable).
     * @property overflowOptions list of options for the item's overflow menu.
     * @property optionsTestTag The test tag associated with the [overflowOptions].
     * @property isAutofill whether or not this screen is part of an autofill flow.
     * @property isCredentialCreation whether or not this screen is part of CredentialManager
     * creation flow.
     * @property shouldShowMasterPasswordReprompt whether or not a master password reprompt is
     * required for various secure actions.
     * @property itemType Indicates the type of item this is.
     */
    data class DisplayItem(
        val id: String,
        val title: Text,
        val titleTestTag: String,
        val secondSubtitle: String?,
        val secondSubtitleTestTag: String?,
        val subtitle: String?,
        val subtitleTestTag: String,
        val iconData: IconData,
        val iconTestTag: String?,
        val extraIconList: ImmutableList<IconData>,
        val overflowOptions: List<ListingItemOverflowAction>,
        val optionsTestTag: String,
        val isAutofill: Boolean,
        val isCredentialCreation: Boolean,
        val shouldShowMasterPasswordReprompt: Boolean,
        val itemType: ItemType,
    ) {
        /**
         * Indicates the item type as a send or vault item.
         */
        sealed class ItemType {
            /**
             * Indicates the item type is a send.
             */
            data class Sends(val type: SendType) : ItemType()

            /**
             * Indicates the item type is a vault item.
             */
            data class Vault(val type: CipherType) : ItemType()

            /**
             * Indicates the item type is a decryption error.
             */
            object DecryptionError : ItemType()
        }
    }

    /**
     * The folder that is displayed to the user on the ItemListingScreen.
     *
     * @property id the id of the folder.
     * @property name the name of the folder.
     * @property count the amount of ciphers in the folder.
     */
    data class FolderDisplayItem(
        val id: String,
        val name: String,
        val count: Int,
    )

    /**
     * The collection that is displayed to the user on the ItemListingScreen.
     *
     * @property id the id of the collection.
     * @property name the name of the collection.
     * @property count the amount of ciphers in the collection.
     */
    data class CollectionDisplayItem(
        val id: String,
        val name: String,
        val count: Int,
    )

    /**
     * Represents different types of item listing.
     */
    sealed class ItemListingType {

        /**
         * The title to display at the top of the screen.
         */
        abstract val titleText: Text

        /**
         * Whether or not the screen has a floating action button (FAB).
         */
        abstract val hasFab: Boolean

        /**
         * Represents different types of vault item listings.
         */
        sealed class Vault : ItemListingType() {

            /**
             * A Login item listing.
             */
            data object Login : Vault() {
                override val titleText: Text get() = BitwardenString.logins.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Card item listing.
             */
            data object Card : Vault() {
                override val titleText: Text get() = BitwardenString.cards.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * An Identity item listing.
             */
            data object Identity : Vault() {
                override val titleText: Text get() = BitwardenString.identities.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Secure Note item listing.
             */
            data object SecureNote : Vault() {
                override val titleText: Text get() = BitwardenString.secure_notes.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A SSH key item listing.
             */
            data object SshKey : Vault() {
                override val titleText: Text get() = BitwardenString.ssh_keys.asText()
                override val hasFab: Boolean get() = false
            }

            /**
             * A Secure Trash item listing.
             */
            data object Trash : Vault() {
                override val titleText: Text get() = BitwardenString.trash.asText()
                override val hasFab: Boolean get() = false
            }

            /**
             * A Folder item listing.
             *
             * @property folderId the id of the folder.
             * @property folderName the name of the folder.
             */
            data class Folder(
                val folderId: String?,
                // The folderName will always initially be an empty string
                val folderName: String = "",
                val fullyQualifiedName: String = "",
            ) : Vault() {
                override val titleText: Text
                    get() = folderId
                        ?.let { folderName.asText() }
                        ?: BitwardenString.folder_none.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Collection item listing.
             *
             * @property collectionId the ID of the collection.
             * @property collectionName the name of the collection.
             */
            data class Collection(
                val collectionId: String,
                // The collectionName will always initially be an empty string
                val collectionName: String = "",
            ) : Vault() {
                override val titleText: Text get() = collectionName.asText()
                override val hasFab: Boolean get() = true
            }
        }

        /**
         * Represents different types of vault item listings.
         */
        sealed class Send : ItemListingType() {
            /**
             * A Send File item listing.
             */
            data object SendFile : Send() {
                override val titleText: Text get() = BitwardenString.file.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Send Text item listing.
             */
            data object SendText : Send() {
                override val titleText: Text get() = BitwardenString.text.asText()
                override val hasFab: Boolean get() = true
            }
        }
    }
}

/**
 * Models events for the [VaultItemListingScreen].
 */
sealed class VaultItemListingEvent {
    /**
     * Closes the app.
     */
    data object ExitApp : VaultItemListingEvent()

    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateBack : VaultItemListingEvent()

    /**
     * Navigates to the VaultAddItemScreen.
     */
    data class NavigateToAddVaultItem(
        val vaultItemCipherType: VaultItemCipherType,
        val selectedFolderId: String? = null,
        val selectedCollectionId: String? = null,
    ) : VaultItemListingEvent()

    /**
     * Navigates to add the folder item.
     */
    data class NavigateToAddFolder(
        val parentFolderName: String,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the collection.
     */
    data class NavigateToCollectionItem(val collectionId: String) : VaultItemListingEvent()

    /**
     * Navigates to the folder.
     */
    data class NavigateToFolderItem(val folderId: String) : VaultItemListingEvent()

    /**
     * Navigates to the AddSendItemScreen.
     */
    data class NavigateToAddSendItem(
        val sendType: SendItemType,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the AddSendScreen.
     *
     * @property id the id of the send to navigate to.
     */
    data class NavigateToEditSendItem(
        val sendType: SendItemType,
        val id: String,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the ViewSendScreen.
     */
    data class NavigateToViewSendItem(
        val id: String,
        val sendType: SendItemType,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the VaultItemScreen.
     *
     * @property id the id of the item to navigate to.
     */
    data class NavigateToVaultItem(
        val id: String,
        val type: VaultItemCipherType,
    ) : VaultItemListingEvent()

    /**
     * Navigates to view a cipher.
     */
    data class NavigateToEditCipher(
        val cipherId: String,
        val cipherType: VaultItemCipherType,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the given [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the SearchScreen with the given type filter.
     */
    data class NavigateToSearchScreen(
        val searchType: SearchType,
    ) : VaultItemListingEvent()

    /**
     * Show a share sheet with the given content.
     */
    data class ShowShareSheet(val content: String) : VaultItemListingEvent()

    /**
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : VaultItemListingEvent(), BackgroundEvent {
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

    /**
     * Complete the current credential registration process.
     *
     * @property result The result of the credential registration.
     */
    data class CompleteCredentialRegistration(
        val result: CreateCredentialResult,
    ) : BackgroundEvent, VaultItemListingEvent()

    /**
     * Perform user verification for a CredentialManager operation.
     */
    data class CredentialManagerUserVerification(
        val isRequired: Boolean,
        val selectedCipherView: CipherView,
    ) : BackgroundEvent, VaultItemListingEvent()

    /**
     * FIDO 2 credential assertion result has been received and the process is ready to be
     * completed.
     *
     * @property result The result of the FIDO 2 credential assertion.
     */
    data class CompleteFido2Assertion(
        val result: AssertFido2CredentialResult,
    ) : BackgroundEvent, VaultItemListingEvent()

    /**
     * Password credential assertion result has been received and the process is ready to be
     * completed.
     *
     * @property result The result of the Password credential assertion.
     */
    data class CompleteProviderGetPasswordCredentialRequest(
        val result: GetPasswordCredentialResult,
    ) : BackgroundEvent, VaultItemListingEvent()

    /**
     * Credential lookup result has been received and the process is ready to be completed.
     *
     * @property result The result of querying for matching credentials.
     */
    data class CompleteProviderGetCredentialsRequest(
        val result: GetCredentialsResult,
    ) : BackgroundEvent, VaultItemListingEvent()
}

/**
 * Models actions for the [VaultItemListingScreen].
 */
sealed class VaultItemListingsAction {
    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to lock
     * the associated account's vault.
     */
    data class LockAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultItemListingsAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to log out
     * of that account.
     */
    data class LogoutAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultItemListingsAction()

    /**
     * The user has clicked the an account to switch too.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultItemListingsAction()

    /**
     * Click to dismiss the dialog.
     */
    data object DismissDialogClick : VaultItemListingsAction()

    /**
     * Click to dismiss the CredentialManager error dialog.
     */
    data class DismissCredentialManagerErrorDialogClick(
        val message: Text,
    ) : VaultItemListingsAction()

    /**
     * Click to submit the master password for user verification.
     */
    data class MasterPasswordUserVerificationSubmit(
        val password: String,
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to retry the password based user verification.
     */
    data class RetryUserVerificationPasswordVerificationClick(
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to submit the PIN for user verification.
     */
    data class PinUserVerificationSubmit(
        val pin: String,
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to retry PIN based user verification.
     */
    data class RetryUserVerificationPinVerificationClick(
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to submit to set up a PIN for the user verification flow.
     */
    data class UserVerificationPinSetUpSubmit(
        val pin: String,
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to retry setting up a PIN for the user verification flow.
     */
    data class UserVerificationPinSetUpRetryClick(
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click to dismiss the password or PIN based user verification dialog.
     */
    data object DismissUserVerificationDialogClick : VaultItemListingsAction()

    /**
     * Click the refresh button.
     */
    data object RefreshClick : VaultItemListingsAction()

    /**
     * Click the lock button.
     */
    data object LockClick : VaultItemListingsAction()

    /**
     * Click to share cipher decryption error details.
     */
    data class ShareCipherDecryptionErrorClick(
        val selectedCipherId: String,
    ) : VaultItemListingsAction()

    /**
     * Click the refresh button.
     */
    data object SyncClick : VaultItemListingsAction()

    /**
     * Click the back button.
     */
    data object BackClick : VaultItemListingsAction()

    /**
     * Click the search icon.
     */
    data object SearchIconClick : VaultItemListingsAction()

    /**
     * Click the add item button.
     */
    data object AddVaultItemClick : VaultItemListingsAction()

    /**
     * Click on overflow option.
     */
    data class OverflowOptionClick(
        val action: ListingItemOverflowAction,
    ) : VaultItemListingsAction()

    /**
     * Click on an item.
     *
     * @property id the id of the item that has been clicked.
     */
    data class ItemClick(
        val id: String,
        val type: VaultItemListingState.DisplayItem.ItemType,
    ) : VaultItemListingsAction()

    /**
     * Click on the collection.
     *
     * @property id the id of the collection that has been clicked
     */
    data class CollectionClick(val id: String) : VaultItemListingsAction()

    /**
     * Click on the folder.
     *
     * @property id the id of the folder that has been clicked
     */
    data class FolderClick(val id: String) : VaultItemListingsAction()

    /**
     * A master password prompt was encountered when trying to perform a sensitive action described
     * by the given [masterPasswordRepromptData] and the given [password] was submitted.
     */
    data class MasterPasswordRepromptSubmit(
        val password: String,
        val masterPasswordRepromptData: MasterPasswordRepromptData,
    ) : VaultItemListingsAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : VaultItemListingsAction()

    /**
     * The user has too many failed verification attempts for CredentialManager operations and can
     * no longer use biometric verification for some time.
     */
    data object UserVerificationLockOut : VaultItemListingsAction()

    /**
     * The user has failed biometric verification for CredentialManager operations.
     */
    data object UserVerificationFail : VaultItemListingsAction()

    /**
     * The user has successfully verified themself using biometrics.
     */
    data class UserVerificationSuccess(
        val selectedCipherView: CipherView,
    ) : VaultItemListingsAction()

    /**
     * The user has cancelled biometric user verification.
     */
    data object UserVerificationCancelled : VaultItemListingsAction()

    /**
     * The user cannot perform verification because it is not supported by the device.
     */
    data class UserVerificationNotSupported(
        val selectedCipherId: String?,
    ) : VaultItemListingsAction()

    /**
     * The user has confirmed overwriting the existing cipher's passkey.
     */
    data class ConfirmOverwriteExistingPasskeyClick(
        val cipherViewId: String,
    ) : VaultItemListingsAction()

    /**
     * Indicated a selection was made to add a new item to the vault.
     */
    data class ItemTypeToAddSelected(
        val itemType: CreateVaultItemType,
    ) : VaultItemListingsAction()

    /**
     * The user has chosen to trust the calling application for performing Credential Manager
     * operations.
     */
    data class TrustPrivilegedAppClick(
        val selectedCipherId: String?,
    ) : VaultItemListingsAction()

    /**
     * Models actions that the [VaultItemListingViewModel] itself might send.
     */
    sealed class Internal : VaultItemListingsAction() {
        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(val isPullToRefreshEnabled: Boolean) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(val result: DeleteSendResult) : Internal()

        /**
         * Indicates a result for generating a verification code has been received.
         */
        data class GenerateTotpResultReceive(
            val result: GenerateTotpResult,
        ) : Internal()

        /**
         * Indicates a result for removing the password protection from a send has been received.
         */
        data class RemovePasswordSendResultReceive(
            val result: RemovePasswordSendResult,
        ) : Internal()

        /**
         * Indicates the icon setting was received.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's master password has been received.
         */
        data class ValidatePasswordResultReceive(
            val masterPasswordRepromptData: MasterPasswordRepromptData,
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's master password as part of the user
         * verification flow has been received.
         */
        data class ValidateUserVerificationPasswordResultReceive(
            val result: ValidatePasswordResult,
            val selectedCipherId: String,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's PIN as part of the user verification
         * flow has been received.
         */
        data class ValidateUserVerificationPinResultReceive(
            val result: ValidatePinResult,
            val selectedCipherId: String,
        ) : Internal()

        /**
         * Indicates that a policy update has been received.
         */
        data class PolicyUpdateReceive(
            val policyDisablesSend: Boolean,
        ) : Internal()

        /**
         * Indicates that a restrict item types policy update has been received.
         */
        data class RestrictItemTypesPolicyUpdateReceive(
            val restrictItemTypesPolicyOrdIds: List<String>,
        ) : Internal()

        /**
         * Indicates that a credential creation request has been received from the
         * CredentialManager.
         */
        data class CreateCredentialRequestReceive(
            val request: CreateCredentialRequest,
        ) : Internal()

        /**
         * Indicates that a result for FIDO 2 credential registration has been received.
         */
        data class Fido2RegisterCredentialResultReceive(
            val result: Fido2RegisterCredentialResult,
        ) : Internal()

        /**
         * Indicates that FIDO 2 assertion request data has been received.
         */
        data class Fido2AssertionDataReceive(
            val data: Fido2CredentialAssertionRequest,
        ) : Internal()

        /**
         * Indicates that a result of a FIDO 2 credential assertion has been received.
         */
        data class Fido2AssertionResultReceive(
            val result: Fido2CredentialAssertionResult,
        ) : Internal()

        /**
         * Indicates that Password get request data has been received.
         */
        data class ProviderGetPasswordCredentialRequestReceive(
            val data: ProviderGetPasswordCredentialRequest,
        ) : Internal()

        /**
         * Indicates that the there is not internet connection.
         */
        data object InternetConnectionErrorReceived : Internal()

        /**
         * Indicates that a result for building credential entries has been received.
         */
        data class GetCredentialEntriesResultReceive(
            val userId: String,
            val result: Result<List<CredentialEntry>>,
        ) : Internal()

        /**
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal(), BackgroundEvent

        /**
         * Indicates that an error occurred while decrypting a cipher.
         */
        data class DecryptCipherErrorReceive(
            val error: Throwable?,
        ) : Internal()

        /**
         * Indicates that a credential operation failure was received.
         */
        data class CredentialOperationFailureReceive(
            val title: Text,
            val message: Text,
            val error: Throwable?,
        ) : Internal()
    }
}

/**
 * Data tracking the type of request that triggered a master password reprompt.
 */
sealed class MasterPasswordRepromptData {
    /**
     * Autofill was selected.
     */
    data class Autofill(
        val cipherId: String,
    ) : MasterPasswordRepromptData()

    /**
     * A cipher overflow menu item action was selected.
     */
    data class OverflowItem(
        val action: ListingItemOverflowAction.VaultAction,
    ) : MasterPasswordRepromptData()

    /**
     * An item was selected to be viewed.
     */
    data class ViewItem(
        val id: String,
        val itemType: VaultItemListingState.DisplayItem.ItemType,
    ) : MasterPasswordRepromptData()
}
