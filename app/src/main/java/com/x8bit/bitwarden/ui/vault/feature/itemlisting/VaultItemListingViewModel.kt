package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.PublicKeyCredentialCreationOptions.AuthenticatorSelectionCriteria.UserVerificationRequirement
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.util.toFido2RequestOrNull
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.platform.repository.util.map
import com.x8bit.bitwarden.data.platform.util.getFido2RpIdOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DecryptFido2CredentialAutofillViewResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.base.util.toAndroidAppUriString
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.determineListingPredicate
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toItemListingType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toSearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toVaultItemCipherType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.updateWithAdditionalDataIfNecessary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
    private val autofillSelectionManager: AutofillSelectionManager,
    private val cipherMatchingManager: CipherMatchingManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val policyManager: PolicyManager,
    private val fido2CredentialManager: Fido2CredentialManager,
    private val organizationEventManager: OrganizationEventManager,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val activeAccountSummary = userState.toActiveAccountSummary()
        val accountSummaries = userState.toAccountSummaries()
        val specialCircumstance = specialCircumstanceManager.specialCircumstance
        val autofillSelectionData = specialCircumstance as? SpecialCircumstance.AutofillSelection
        val fido2CreationData = specialCircumstance as? SpecialCircumstance.Fido2Save
        val shouldFinishOnComplete = autofillSelectionData
            ?.shouldFinishWhenComplete
            ?: (fido2CreationData != null)
        val dialogState = fido2CreationData
            ?.let { VaultItemListingState.DialogState.Loading(R.string.loading.asText()) }
        VaultItemListingState(
            itemListingType = VaultItemListingArgs(savedStateHandle = savedStateHandle)
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
            dialogState = dialogState,
            policyDisablesSend = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
                .any(),
            autofillSelectionData = autofillSelectionData?.autofillSelectionData,
            shouldFinishOnComplete = shouldFinishOnComplete,
            hasMasterPassword = userState.activeAccount.hasMasterPassword,
            fido2CredentialRequest = fido2CreationData?.fido2CredentialRequest,
            isPremium = userState.activeAccount.isPremium,
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

        viewModelScope.launch {
            state
                .fido2CredentialRequest
                ?.let { request ->
                    sendAction(
                        VaultItemListingsAction.Internal.ValidateFido2OriginResultReceive(
                            result = fido2CredentialManager.validateOrigin(request),
                        ),
                    )
                }
                ?: observeVaultData()
        }

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND)
            .map { VaultItemListingsAction.Internal.PolicyUpdateReceive(it.any()) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    private fun observeVaultData() {
        vaultRepository
            .vaultDataStateFlow
            .map {
                VaultItemListingsAction.Internal.VaultDataReceive(
                    it
                        .filterForAutofillIfNecessary()
                        .filterForFido2CreationIfNecessary(),
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultItemListingsAction) {
        when (action) {
            is VaultItemListingsAction.LockAccountClick -> handleLockAccountClick(action)
            is VaultItemListingsAction.LogoutAccountClick -> handleLogoutAccountClick(action)
            is VaultItemListingsAction.SwitchAccountClick -> handleSwitchAccountClick(action)
            is VaultItemListingsAction.DismissDialogClick -> handleDismissDialogClick()
            is VaultItemListingsAction.DismissFido2CreationErrorDialogClick -> {
                handleDismissFido2ErrorDialogClick()
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

            VaultItemListingsAction.UserVerificationNotSupported -> {
                handleUserVerificationNotSupported()
            }

            is VaultItemListingsAction.Internal -> handleInternalAction(action)
        }
    }

    //region VaultItemListing Handlers
    private fun handleLockAccountClick(action: VaultItemListingsAction.LockAccountClick) {
        vaultRepository.lockVault(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClick(action: VaultItemListingsAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
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
        vaultRepository.sync()
    }

    private fun handleRefreshPull() {
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepository.sync()
    }

    private fun handleUserVerificationLockOut() {
        fido2CredentialManager.isUserVerified = false
        showFido2ErrorDialog()
    }

    private fun handleUserVerificationSuccess(
        action: VaultItemListingsAction.UserVerificationSuccess,
    ) {
        fido2CredentialManager.isUserVerified = true
        specialCircumstanceManager
            .specialCircumstance
            ?.toFido2RequestOrNull()
            ?.let { request ->
                registerFido2CredentialToCipher(
                    request = request,
                    cipherView = action.selectedCipherView,
                )
            }
            ?: showFido2ErrorDialog()
    }

    private fun handleUserVerificationFail() {
        fido2CredentialManager.isUserVerified = false
        showFido2ErrorDialog()
    }

    private fun handleUserVerificationCancelled() {
        fido2CredentialManager.isUserVerified = false
        clearDialogState()
        sendEvent(
            VaultItemListingEvent.CompleteFido2Registration(
                result = Fido2RegisterCredentialResult.Cancelled,
            ),
        )
    }

    private fun handleUserVerificationNotSupported() {
        fido2CredentialManager.isUserVerified = false
        showFido2ErrorDialog()
    }

    private fun handleCopySendUrlClick(action: ListingItemOverflowAction.SendAction.CopyUrlClick) {
        clipboardManager.setText(text = action.sendUrl)
    }

    private fun handleDeleteSendClick(action: ListingItemOverflowAction.SendAction.DeleteClick) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.deleting.asText(),
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

    private fun handleRemoveSendPasswordClick(
        action: ListingItemOverflowAction.SendAction.RemovePasswordClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.removePasswordSend(action.sendId)
            sendAction(VaultItemListingsAction.Internal.RemovePasswordSendResultReceive(result))
        }
    }

    private fun handleAddVaultItemClick() {
        val event = when (val itemListingType = state.itemListingType) {
            is VaultItemListingState.ItemListingType.Vault -> {
                VaultItemListingEvent.NavigateToAddVaultItem(
                    vaultItemCipherType = itemListingType.toVaultItemCipherType(),
                )
            }

            is VaultItemListingState.ItemListingType.Send -> {
                VaultItemListingEvent.NavigateToAddSendItem
            }
        }
        sendEvent(event)
    }

    private fun handleEditSendClick(action: ListingItemOverflowAction.SendAction.EditClick) {
        sendEvent(VaultItemListingEvent.NavigateToSendItem(id = action.sendId))
    }

    private fun handleItemClick(action: VaultItemListingsAction.ItemClick) {
        if (state.isAutofill) {
            val cipherView = getCipherViewOrNull(action.id) ?: return
            autofillSelectionManager.emitAutofillSelection(cipherView = cipherView)
            return
        }

        if (state.isFido2Creation) {
            handleFido2RegistrationRequestReceive(action)
            return
        }

        val event = when (state.itemListingType) {
            is VaultItemListingState.ItemListingType.Vault -> {
                VaultItemListingEvent.NavigateToVaultItem(id = action.id)
            }

            is VaultItemListingState.ItemListingType.Send -> {
                VaultItemListingEvent.NavigateToSendItem(id = action.id)
            }
        }
        sendEvent(event)
    }

    private fun handleFido2RegistrationRequestReceive(action: VaultItemListingsAction.ItemClick) {
        val cipherView = getCipherViewOrNull(action.id)
            ?: run {
                showFido2ErrorDialog()
                return
            }
        val credentialRequest = state
            .fido2CredentialRequest
            ?: run {
                // This scenario should not occur because `isFido2Creation` is false when
                // `fido2CredentialRequest` is null. We show the FIDO 2 error dialog to inform
                // the user and terminate the flow just in case it does occur.
                showFido2ErrorDialog()
                return
            }
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.saving.asText(),
                ),
            )
        }
        if (fido2CredentialManager.isUserVerified) {
            // The user has performed verification implicitly so we continue FIDO 2 registration
            // without checking the request's user verification settings.
            registerFido2CredentialToCipher(
                request = credentialRequest,
                cipherView = cipherView,
            )
        } else {
            performUserVerificationIfRequired(credentialRequest, cipherView)
        }
    }

    private fun performUserVerificationIfRequired(
        credentialRequest: Fido2CredentialRequest,
        cipherView: CipherView,
    ) {
        val createOptions = fido2CredentialManager
            .getPasskeyCreateOptionsOrNull(credentialRequest.requestJson)
            ?: run {
                showFido2ErrorDialog()
                return
            }
        when (createOptions.authenticatorSelection.userVerification) {
            UserVerificationRequirement.DISCOURAGED -> {
                registerFido2CredentialToCipher(
                    request = credentialRequest,
                    cipherView = cipherView,
                )
            }

            UserVerificationRequirement.PREFERRED -> {
                sendEvent(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = false,
                        selectedCipherView = cipherView,
                    ),
                )
            }

            UserVerificationRequirement.REQUIRED -> {
                sendEvent(
                    VaultItemListingEvent.Fido2UserVerification(
                        isRequired = true,
                        selectedCipherView = cipherView,
                    ),
                )
            }

            null -> {
                // Per WebAuthn spec members should be ignored when invalid. Since the request
                // violates spec we display an error and terminate the operation.
                showFido2ErrorDialog()
            }
        }
    }

    private fun registerFido2CredentialToCipher(
        request: Fido2CredentialRequest,
        cipherView: CipherView,
    ) {
        val activeUserId = authRepository.activeUserId
            ?: run {
                showFido2ErrorDialog()
                return
            }
        viewModelScope.launch {
            val result: Fido2RegisterCredentialResult =
                fido2CredentialManager.registerFido2Credential(
                    userId = activeUserId,
                    fido2CredentialRequest = request,
                    selectedCipherView = cipherView,
                )
            sendAction(
                VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive(result),
            )
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
        clipboardManager.setText(action.notes)
    }

    private fun handleCopyNumberClick(
        action: ListingItemOverflowAction.VaultAction.CopyNumberClick,
    ) {
        clipboardManager.setText(action.number)
    }

    private fun handleCopyPasswordClick(
        action: ListingItemOverflowAction.VaultAction.CopyPasswordClick,
    ) {
        clipboardManager.setText(action.password)
        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientCopiedPassword(cipherId = action.cipherId),
        )
    }

    private fun handleCopySecurityCodeClick(
        action: ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
    ) {
        clipboardManager.setText(action.securityCode)
        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientCopiedCardCode(cipherId = action.cipherId),
        )
    }

    private fun handleCopyTotpClick(
        action: ListingItemOverflowAction.VaultAction.CopyTotpClick,
    ) {
        viewModelScope.launch {
            val result = vaultRepository.generateTotp(action.totpCode, clock.instant())
            sendAction(VaultItemListingsAction.Internal.GenerateTotpResultReceive(result))
        }
    }

    private fun handleCopyUsernameClick(
        action: ListingItemOverflowAction.VaultAction.CopyUsernameClick,
    ) {
        clipboardManager.setText(action.username)
    }

    private fun handleEditCipherClick(action: ListingItemOverflowAction.VaultAction.EditClick) {
        sendEvent(VaultItemListingEvent.NavigateToEditCipher(action.cipherId))
    }

    private fun handleLaunchCipherUrlClick(
        action: ListingItemOverflowAction.VaultAction.LaunchClick,
    ) {
        sendEvent(VaultItemListingEvent.NavigateToUrl(action.url))
    }

    private fun handleViewCipherClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(VaultItemListingEvent.NavigateToVaultItem(action.cipherId))
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleDismissFido2ErrorDialogClick() {
        clearDialogState()
        sendEvent(
            VaultItemListingEvent.CompleteFido2Registration(
                result = Fido2RegisterCredentialResult.Error,
            ),
        )
    }

    private fun handleBackClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateBack,
        )
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            )
        }
        vaultRepository.sync()
    }

    private fun handleSearchIconClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateToSearchScreen(
                searchType = state.itemListingType.toSearchType(),
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

            is VaultItemListingsAction.Internal.PolicyUpdateReceive -> {
                handlePolicyUpdateReceive(action)
            }

            is VaultItemListingsAction.Internal.ValidateFido2OriginResultReceive -> {
                handleValidateFido2OriginResultReceive(action)
            }

            is VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive -> {
                handleFido2RegisterCredentialResultReceive(action)
            }
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
        when (action.result) {
            DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(VaultItemListingEvent.ShowToast(R.string.send_deleted.asText()))
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
                            title = R.string.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    VaultItemListingEvent.ShowToast(
                        text = R.string.send_password_removed.asText(),
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
                clipboardManager.setText(result.code)
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
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (val result = action.result) {
            ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = null,
                            message = R.string.generic_error_message.asText(),
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
                                message = R.string.invalid_master_password.asText(),
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
                val cipherView = getCipherViewOrNull(cipherId = data.cipherId) ?: return
                autofillSelectionManager.emitAutofillSelection(cipherView = cipherView)
            }

            is MasterPasswordRepromptData.OverflowItem -> {
                handleOverflowOptionClick(
                    VaultItemListingsAction.OverflowOptionClick(data.action),
                )
            }
        }
    }
    //endregion VaultItemListing Handlers

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                    dialogState = null,
                )
            }
        }
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        } else {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(
                                " ".asText(),
                                R.string.internet_connection_required_message.asText(),
                            ),
                    ),
                    dialogState = null,
                )
            }
        }
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
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

    private fun handleFido2RegisterCredentialResultReceive(
        action: VaultItemListingsAction.Internal.Fido2RegisterCredentialResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialogState = null) }
        when (action.result) {
            is Fido2RegisterCredentialResult.Error -> {
                sendEvent(VaultItemListingEvent.ShowToast(R.string.an_error_has_occurred.asText()))
            }

            is Fido2RegisterCredentialResult.Success -> {
                sendEvent(VaultItemListingEvent.ShowToast(R.string.item_updated.asText()))
            }

            Fido2RegisterCredentialResult.Cancelled -> {
                // no-op: The OS will handle re-displaying the system prompt.
            }
        }
        sendEvent(VaultItemListingEvent.CompleteFido2Registration(action.result))
    }

    private fun handleValidateFido2OriginResultReceive(
        action: VaultItemListingsAction.Internal.ValidateFido2OriginResultReceive,
    ) {
        when (val result = action.result) {
            is Fido2ValidateOriginResult.Error -> {
                handleFido2OriginValidationFail(result)
            }

            Fido2ValidateOriginResult.Success -> {
                handleFido2OriginValidationSuccess()
            }
        }
    }

    private fun handleFido2OriginValidationFail(error: Fido2ValidateOriginResult.Error) {
        val messageResId = when (error) {
            Fido2ValidateOriginResult.Error.ApplicationNotFound -> {
                R.string.passkey_operation_failed_because_app_not_found_in_asset_links
            }

            Fido2ValidateOriginResult.Error.ApplicationNotVerified -> {
                R.string.passkey_operation_failed_because_app_could_not_be_verified
            }

            Fido2ValidateOriginResult.Error.AssetLinkNotFound -> {
                R.string.passkey_operation_failed_because_of_missing_asset_links
            }

            Fido2ValidateOriginResult.Error.PrivilegedAppNotAllowed -> {
                R.string.passkey_operation_failed_because_browser_is_not_privileged
            }

            Fido2ValidateOriginResult.Error.PasskeyNotSupportedForApp -> {
                R.string.passkeys_not_supported_for_this_app
            }

            Fido2ValidateOriginResult.Error.PrivilegedAppSignatureNotFound -> {
                R.string.passkey_operation_failed_because_browser_signature_does_not_match
            }

            Fido2ValidateOriginResult.Error.Unknown -> {
                R.string.generic_error_message
            }
        }
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Fido2CreationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = messageResId.asText(),
                ),
            )
        }
    }

    private fun handleFido2OriginValidationSuccess() {
        observeVaultData()
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
                            vaultFilterType = state.vaultFilterType,
                            itemListingType = listingType,
                            hasMasterPassword = state.hasMasterPassword,
                            baseIconUrl = state.baseIconUrl,
                            isIconLoadingDisabled = state.isIconLoadingDisabled,
                            autofillSelectionData = state.autofillSelectionData,
                            fido2CreationData = state.fido2CredentialRequest,
                            fido2CredentialAutofillViews = vaultData
                                .fido2CredentialAutofillViewList,
                            isPremiumUser = state.isPremium,
                        )
                    }

                    is VaultItemListingState.ItemListingType.Send -> {
                        vaultData
                            .sendViewList
                            .filter { sendView ->
                                sendView.determineListingPredicate(listingType)
                            }
                            .toViewState(
                                baseWebSendUrl = state.baseWebSendUrl,
                                clock = clock,
                            )
                    }
                },
                dialogState = currentState.dialogState.takeUnless { clearDialogState },
            )
        }
    }

    private fun getCipherViewOrNull(cipherId: String) =
        vaultRepository
            .vaultDataStateFlow
            .value
            .data
            ?.cipherViewList
            ?.firstOrNull { it.id == cipherId }

    /**
     * Takes the given vault data and filters it for autofill if necessary.
     */
    private suspend fun DataState<VaultData>.filterForAutofillIfNecessary(): DataState<VaultData> {
        val matchUri = state
            .autofillSelectionData
            ?.uri
            ?: return this
        return this.map { vaultData ->
            vaultData.copy(
                cipherViewList = cipherMatchingManager.filterCiphersForMatches(
                    ciphers = vaultData.cipherViewList,
                    matchUri = matchUri,
                ),
                fido2CredentialAutofillViewList = vaultData.toFido2CredentialAutofillViews(),
            )
        }
    }

    /**
     * Takes the given vault data and filters it for fido2 credential creation if necessary.
     */
    @Suppress("MaxLineLength")
    private suspend fun DataState<VaultData>.filterForFido2CreationIfNecessary(): DataState<VaultData> {
        val request = state.fido2CredentialRequest ?: return this
        return this.map { vaultData ->
            val matchUri = request.origin
                ?: request.packageName
                    .toAndroidAppUriString()

            vaultData.copy(
                cipherViewList = cipherMatchingManager.filterCiphersForMatches(
                    ciphers = vaultData.cipherViewList,
                    matchUri = matchUri,
                ),
                fido2CredentialAutofillViewList = vaultData.toFido2CredentialAutofillViews(),
            )
        }
    }

    /**
     * Decrypt and filter the fido 2 autofill credentials.
     */
    @Suppress("MaxLineLength")
    private suspend fun VaultData.toFido2CredentialAutofillViews(): List<Fido2CredentialAutofillView>? =
        (vaultRepository
            .getDecryptedFido2CredentialAutofillViews(
                cipherViewList = this
                    .cipherViewList
                    .filter { it.isActiveWithFido2Credentials },
            )
            as? DecryptFido2CredentialAutofillViewResult.Success)
            ?.fido2CredentialAutofillViews

    private fun showFido2ErrorDialog() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Fido2CreationFail(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
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
    // Internal
    private val isPullToRefreshSettingEnabled: Boolean,
    val autofillSelectionData: AutofillSelectionData? = null,
    val fido2CredentialRequest: Fido2CredentialRequest? = null,
    val shouldFinishOnComplete: Boolean = false,
    val hasMasterPassword: Boolean,
    val isPremium: Boolean,
) {
    /**
     * Whether or not this represents a listing screen for autofill.
     */
    val isAutofill: Boolean
        get() = autofillSelectionData != null

    /**
     * Whether or not this represents a listing screen for FIDO2 creation.
     */
    val isFido2Creation: Boolean
        get() = fido2CredentialRequest != null

    /**
     * A displayable title for the AppBar.
     */
    val appBarTitle: Text
        get() = autofillSelectionData
            ?.uri
            ?.toHostOrPathOrNull()
            ?.let { R.string.items_for_uri.asText(it) }
            ?: fido2CredentialRequest
                ?.callingAppInfo
                ?.getFido2RpIdOrNull()
                ?.let { R.string.items_for_uri.asText(it) }
            ?: itemListingType.titleText

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Whether or not the account switcher should be shown.
     */
    val shouldShowAccountSwitcher: Boolean get() = isAutofill || isFido2Creation

    /**
     * Whether or not the navigation icon should be shown.
     */
    val shouldShowNavigationIcon: Boolean get() = !isAutofill && !isFido2Creation

    /**
     * Whether or not the overflow menu should be shown.
     */
    val shouldShowOverflowMenu: Boolean get() = !isAutofill && !isFido2Creation

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
        ) : DialogState()

        /**
         * Represents a dialog indicating that the FIDO 2 credential creation flow was not
         * successful.
         */
        @Parcelize
        data class Fido2CreationFail(
            val title: Text,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
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
            val shouldShowAddButton: Boolean,
            val buttonText: Text,
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
            val shouldShowDivider: Boolean
                get() = displayItemList.isNotEmpty() &&
                    (displayFolderList.isNotEmpty() || displayCollectionList.isNotEmpty())
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
     * @property isFido2Creation whether or not this screen is part of fido2 creation flow.
     * @property shouldShowMasterPasswordReprompt whether or not a master password reprompt is
     * required for various secure actions.
     */
    data class DisplayItem(
        val id: String,
        val title: String,
        val titleTestTag: String,
        val secondSubtitle: String?,
        val secondSubtitleTestTag: String?,
        val subtitle: String?,
        val subtitleTestTag: String,
        val iconData: IconData,
        val iconTestTag: String?,
        val extraIconList: List<IconRes>,
        val overflowOptions: List<ListingItemOverflowAction>,
        val optionsTestTag: String,
        val isAutofill: Boolean,
        val isFido2Creation: Boolean,
        val shouldShowMasterPasswordReprompt: Boolean,
    )

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
                override val titleText: Text get() = R.string.logins.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Card item listing.
             */
            data object Card : Vault() {
                override val titleText: Text get() = R.string.cards.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * An Identity item listing.
             */
            data object Identity : Vault() {
                override val titleText: Text get() = R.string.identities.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Secure Note item listing.
             */
            data object SecureNote : Vault() {
                override val titleText: Text get() = R.string.secure_notes.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Secure Trash item listing.
             */
            data object Trash : Vault() {
                override val titleText: Text get() = R.string.trash.asText()
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
            ) : Vault() {
                override val titleText: Text
                    get() = folderId
                        ?.let { folderName.asText() }
                        ?: R.string.folder_none.asText()
                override val hasFab: Boolean get() = false
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
                override val hasFab: Boolean get() = false
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
                override val titleText: Text get() = R.string.file.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Send Text item listing.
             */
            data object SendText : Send() {
                override val titleText: Text get() = R.string.text.asText()
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
     * Dismisses the pull-to-refresh indicator.
     */
    data object DismissPullToRefresh : VaultItemListingEvent()

    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateBack : VaultItemListingEvent()

    /**
     * Navigates to the VaultAddItemScreen.
     */
    data class NavigateToAddVaultItem(
        val vaultItemCipherType: VaultItemCipherType,
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
    data object NavigateToAddSendItem : VaultItemListingEvent()

    /**
     * Navigates to the AddSendScreen.
     *
     * @property id the id of the send to navigate to.
     */
    data class NavigateToSendItem(val id: String) : VaultItemListingEvent()

    /**
     * Navigates to the VaultItemScreen.
     *
     * @property id the id of the item to navigate to.
     */
    data class NavigateToVaultItem(val id: String) : VaultItemListingEvent()

    /**
     * Navigates to view a cipher.
     */
    data class NavigateToEditCipher(
        val cipherId: String,
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
     * Show a toast with the given message.
     *
     * @property text the text to display.
     */
    data class ShowToast(val text: Text) : VaultItemListingEvent()

    /**
     * Complete the current FIDO 2 credential registration process.
     *
     * @property result the result of FIDO 2 credential registration.
     */
    data class CompleteFido2Registration(
        val result: Fido2RegisterCredentialResult,
    ) : VaultItemListingEvent()

    /**
     * Perform user verification for a FIDO 2 credential operation.
     */
    data class Fido2UserVerification(
        val isRequired: Boolean,
        val selectedCipherView: CipherView,
    ) : VaultItemListingEvent()
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
     * Click to dismiss the FIDO 2 creation error dialog.
     */
    data object DismissFido2CreationErrorDialogClick : VaultItemListingsAction()

    /**
     * Click the refresh button.
     */
    data object RefreshClick : VaultItemListingsAction()

    /**
     * Click the lock button.
     */
    data object LockClick : VaultItemListingsAction()

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
    data class ItemClick(val id: String) : VaultItemListingsAction()

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
     * The user has too many failed verification attempts for FIDO operations and can no longer
     * use biometric verification for some time.
     */
    data object UserVerificationLockOut : VaultItemListingsAction()

    /**
     * The user has failed biometric verification for FIDO 2 operations.
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
    data object UserVerificationNotSupported : VaultItemListingsAction()

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
         * Indicates that a policy update has been received.
         */
        data class PolicyUpdateReceive(
            val policyDisablesSend: Boolean,
        ) : Internal()

        /**
         * Indicates that a result for validating the relying party's origin during a FIDO 2
         * request.
         */
        data class ValidateFido2OriginResultReceive(
            val result: Fido2ValidateOriginResult,
        ) : Internal()

        /**
         * Indicates that a result for FIDO 2 credential registration has been received.
         */
        data class Fido2RegisterCredentialResultReceive(
            val result: Fido2RegisterCredentialResult,
        ) : Internal()
    }
}

/**
 * Data tracking the type of request that triggered a master password reprompt.
 */
sealed class MasterPasswordRepromptData : Parcelable {

    /**
     * Autofill was selected.
     */
    @Parcelize
    data class Autofill(
        val cipherId: String,
    ) : MasterPasswordRepromptData()

    /**
     * A cipher overflow menu item action was selected.
     */
    @Parcelize
    data class OverflowItem(
        val action: ListingItemOverflowAction.VaultAction,
    ) : MasterPasswordRepromptData()
}
