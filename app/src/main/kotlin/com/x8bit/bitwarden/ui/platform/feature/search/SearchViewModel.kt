package com.x8bit.bitwarden.ui.platform.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilitySelectionManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillSelectionManager
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.util.toAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toTotpDataOrNull
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.search.util.filterAndOrganize
import com.x8bit.bitwarden.ui.platform.feature.search.util.toSearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.util.toViewState
import com.x8bit.bitwarden.ui.platform.feature.search.util.updateWithAdditionalDataIfNecessary
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendItemType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.applyRestrictItemTypesPolicy
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toVaultFilterData
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.util.toVaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the search screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val policyManager: PolicyManager,
    private val accessibilitySelectionManager: AccessibilitySelectionManager,
    private val autofillSelectionManager: AutofillSelectionManager,
    private val organizationEventManager: OrganizationEventManager,
    private val vaultRepo: VaultRepository,
    private val authRepo: AuthRepository,
    environmentRepo: EnvironmentRepository,
    settingsRepo: SettingsRepository,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<SearchState, SearchEvent, SearchAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val searchType = savedStateHandle.toSearchArgs().type
            val userState = requireNotNull(authRepo.userStateFlow.value)
            val specialCircumstance = specialCircumstanceManager.specialCircumstance
            val searchTerm = (specialCircumstance as? SpecialCircumstance.SearchShortcut)
                ?.searchTerm
                ?.also {
                    specialCircumstanceManager.specialCircumstance = null
                }
                .orEmpty()

            SearchState(
                searchTerm = searchTerm,
                searchType = searchType.toSearchTypeData(),
                viewState = SearchState.ViewState.Loading,
                dialogState = null,
                vaultFilterData = when (searchType) {
                    is SearchType.Sends -> null
                    is SearchType.Vault -> userState.activeAccount.toVaultFilterData(
                        isIndividualVaultDisabled = policyManager
                            .getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
                            .any(),
                    )
                },
                baseWebSendUrl = environmentRepo.environment.environmentUrlData.baseWebSendUrl,
                baseIconUrl = environmentRepo.environment.environmentUrlData.baseIconUrl,
                isIconLoadingDisabled = settingsRepo.isIconLoadingDisabled,
                autofillSelectionData = specialCircumstance?.toAutofillSelectionDataOrNull(),
                totpData = specialCircumstance?.toTotpDataOrNull(),
                hasMasterPassword = userState.activeAccount.hasMasterPassword,
                isPremium = userState.activeAccount.isPremium,
                restrictItemTypesPolicyOrgIds = persistentListOf(),
            )
        },
) {
    init {
        settingsRepo
            .isIconLoadingDisabledFlow
            .map { SearchAction.Internal.IconLoadingSettingReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepo
            .vaultDataStateFlow
            .map { SearchAction.Internal.VaultDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
            .map { policies -> policies.map { it.organizationId } }
            .map { SearchAction.Internal.RestrictItemTypesPolicyUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(
                SnackbarRelay.CIPHER_DELETED,
                SnackbarRelay.CIPHER_DELETED_SOFT,
                SnackbarRelay.CIPHER_RESTORED,
                SnackbarRelay.CIPHER_UPDATED,
                SnackbarRelay.SEND_DELETED,
                SnackbarRelay.SEND_UPDATED,
            )
            .map { SearchAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SearchAction) {
        when (action) {
            SearchAction.BackClick -> handleBackClick()
            SearchAction.DismissDialogClick -> handleDismissClick()
            is SearchAction.ItemClick -> handleItemClick(action)
            is SearchAction.AutofillItemClick -> handleAutofillItemClick(action)
            is SearchAction.AutofillAndSaveItemClick -> handleAutofillAndSaveItemClick(action)
            is SearchAction.MasterPasswordRepromptSubmit -> {
                handleMasterPasswordRepromptSubmit(action)
            }

            is SearchAction.SearchTermChange -> handleSearchTermChange(action)
            is SearchAction.VaultFilterSelect -> handleVaultFilterSelect(action)
            is SearchAction.OverflowOptionClick -> handleOverflowItemClick(action)
            is SearchAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(SearchEvent.NavigateBack)
    }

    private fun handleDismissClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleItemClick(action: SearchAction.ItemClick) {
        val event = when (val itemType = action.itemType) {
            is SearchState.DisplayItem.ItemType.Vault -> {
                if (state.isTotp) {
                    SearchEvent.NavigateToEditCipher(
                        cipherId = action.itemId,
                        cipherType = itemType.type.toVaultItemCipherType(),
                    )
                } else {
                    SearchEvent.NavigateToViewCipher(
                        cipherId = action.itemId,
                        cipherType = itemType.type.toVaultItemCipherType(),
                    )
                }
            }

            is SearchState.DisplayItem.ItemType.Sends -> {
                SearchEvent.NavigateToViewSend(
                    sendId = action.itemId,
                    sendType = itemType.type.toSendItemType(),
                )
            }
        }
        sendEvent(event)
    }

    private fun handleAutofillItemClick(action: SearchAction.AutofillItemClick) {
        useCipherForAutofill(cipherId = action.itemId)
    }

    private fun handleAutofillAndSaveItemClick(action: SearchAction.AutofillAndSaveItemClick) {
        val cipherListView = getCipherListViewOrNull(cipherId = action.itemId) ?: return
        val uris = cipherListView.login?.uris.orEmpty()

        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Loading(
                    message = BitwardenString.loading.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result = decryptCipherViewOrNull(cipherId = action.itemId)
                ?.let { cipherView ->
                    vaultRepo.updateCipher(
                        cipherId = action.itemId,
                        cipherView = cipherView.copy(
                            login = cipherView
                                .login
                                ?.copy(
                                    uris = uris + LoginUriView(
                                        uri = state.autofillSelectionData?.uri,
                                        match = null,
                                        uriChecksum = null,
                                    ),
                                ),
                        ),
                    )
                }
                ?: UpdateCipherResult.Error(error = null, errorMessage = null)

            sendAction(
                SearchAction.Internal.UpdateCipherResultReceive(
                    cipherId = action.itemId,
                    result = result,
                ),
            )
        }
    }

    private fun handleMasterPasswordRepromptSubmit(
        action: SearchAction.MasterPasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepo.validatePassword(password = action.password)
            sendAction(
                SearchAction.Internal.ValidatePasswordResultReceive(
                    masterPasswordRepromptData = action.masterPasswordRepromptData,
                    result = result,
                ),
            )
        }
    }

    private fun handleSearchTermChange(action: SearchAction.SearchTermChange) {
        mutableStateFlow.update { it.copy(searchTerm = action.searchTerm) }
        recalculateViewState()
    }

    private fun handleVaultFilterSelect(action: SearchAction.VaultFilterSelect) {
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = it.vaultFilterData?.copy(
                    selectedVaultFilterType = action.vaultFilterType,
                ),
            )
        }
        recalculateViewState()
    }

    private fun handleOverflowItemClick(action: SearchAction.OverflowOptionClick) {
        when (val overflowAction = action.overflowAction) {
            is ListingItemOverflowAction.SendAction.CopyUrlClick -> {
                handleCopyUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.DeleteClick -> {
                handleDeleteClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ViewClick -> handleViewClick(overflowAction)
            is ListingItemOverflowAction.SendAction.EditClick -> handleEditClick(overflowAction)
            is ListingItemOverflowAction.SendAction.RemovePasswordClick -> {
                handleRemovePasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ShareUrlClick -> {
                handleShareUrlClick(overflowAction)
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

            is ListingItemOverflowAction.VaultAction.CopyTotpClick -> {
                handleCopyTotpClick(overflowAction)
            }
        }
    }

    private fun handleCopyUrlClick(action: ListingItemOverflowAction.SendAction.CopyUrlClick) {
        clipboardManager.setText(
            text = action.sendUrl,
            toastDescriptorOverride = BitwardenString.link.asText(),
        )
    }

    private fun handleDeleteClick(action: ListingItemOverflowAction.SendAction.DeleteClick) {
        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Loading(BitwardenString.deleting.asText()),
            )
        }
        viewModelScope.launch {
            val result = vaultRepo.deleteSend(action.sendId)
            sendAction(SearchAction.Internal.DeleteSendResultReceive(result))
        }
    }

    private fun handleViewClick(action: ListingItemOverflowAction.SendAction.ViewClick) {
        sendEvent(
            SearchEvent.NavigateToViewSend(
                sendId = action.sendId,
                sendType = action.sendType.toSendItemType(),
            ),
        )
    }

    private fun handleEditClick(action: ListingItemOverflowAction.SendAction.EditClick) {
        sendEvent(
            event = SearchEvent.NavigateToEditSend(
                sendId = action.sendId,
                sendType = action.sendType.toSendItemType(),
            ),
        )
    }

    private fun handleCopyTotpClick(
        action: ListingItemOverflowAction.VaultAction.CopyTotpClick,
    ) {
        viewModelScope.launch {
            val result = vaultRepo.generateTotp(action.cipherId, clock.instant())
            sendAction(SearchAction.Internal.GenerateTotpResultReceive(result))
        }
    }

    private fun handleRemovePasswordClick(
        action: ListingItemOverflowAction.SendAction.RemovePasswordClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Loading(
                    message = BitwardenString.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepo.removePasswordSend(action.sendId)
            sendAction(SearchAction.Internal.RemovePasswordSendResultReceive(result))
        }
    }

    private fun handleShareUrlClick(action: ListingItemOverflowAction.SendAction.ShareUrlClick) {
        sendEvent(SearchEvent.ShowShareSheet(action.sendUrl))
    }

    private fun handleCopyNoteClick(action: ListingItemOverflowAction.VaultAction.CopyNoteClick) {
        viewModelScope.launch {
            decryptCipherViewOrNull(action.cipherId)
                ?.let {
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
            decryptCipherViewOrNull(action.cipherId)
                ?.let {
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
            decryptCipherViewOrNull(action.cipherId)
                ?.let { cipherView ->
                    clipboardManager.setText(
                        text = cipherView.login?.password.orEmpty(),
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
            decryptCipherViewOrNull(action.cipherId)
                ?.let { cipherView ->
                    clipboardManager.setText(
                        text = cipherView.card?.code.orEmpty(),
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
            event = SearchEvent.NavigateToEditCipher(
                cipherId = action.cipherId,
                cipherType = action.cipherType.toVaultItemCipherType(),
            ),
        )
    }

    private fun handleLaunchCipherUrlClick(
        action: ListingItemOverflowAction.VaultAction.LaunchClick,
    ) {
        sendEvent(SearchEvent.NavigateToUrl(action.url))
    }

    private fun handleViewCipherClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(
            event = SearchEvent.NavigateToViewCipher(
                cipherId = action.cipherId,
                cipherType = action.cipherType.toVaultItemCipherType(),
            ),
        )
    }

    private fun handleInternalAction(action: SearchAction.Internal) {
        when (action) {
            is SearchAction.Internal.IconLoadingSettingReceive -> {
                handleIconLoadingSettingReceive(action)
            }

            is SearchAction.Internal.DeleteSendResultReceive -> {
                handleDeleteSendResultReceive(action)
            }

            is SearchAction.Internal.GenerateTotpResultReceive -> {
                handleGenerateTotpResultReceive(action)
            }

            is SearchAction.Internal.RemovePasswordSendResultReceive -> {
                handleRemovePasswordSendResultReceive(action)
            }

            is SearchAction.Internal.SnackbarDataReceived -> handleSnackbarDataReceived(action)

            is SearchAction.Internal.UpdateCipherResultReceive -> {
                handleUpdateCipherResultReceive(action)
            }

            is SearchAction.Internal.ValidatePasswordResultReceive -> {
                handleValidatePasswordResultReceive(action)
            }

            is SearchAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)

            is SearchAction.Internal.RestrictItemTypesPolicyUpdateReceive -> {
                handleRestrictItemTypesPolicyUpdateReceive(action)
            }

            is SearchAction.Internal.DecryptCipherErrorReceive -> {
                handleDecryptCipherErrorReceive(action)
            }
        }
    }

    private fun handleDecryptCipherErrorReceive(
        action: SearchAction.Internal.DecryptCipherErrorReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Error(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                    throwable = action.error,
                ),
            )
        }
    }

    private fun handleIconLoadingSettingReceive(
        action: SearchAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update { it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled) }
        recalculateViewState()
    }

    private fun handleDeleteSendResultReceive(
        action: SearchAction.Internal.DeleteSendResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SearchEvent.ShowSnackbar(BitwardenString.send_deleted.asText()))
            }
        }
    }

    private fun handleGenerateTotpResultReceive(
        action: SearchAction.Internal.GenerateTotpResultReceive,
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

    private fun handleRemovePasswordSendResultReceive(
        action: SearchAction.Internal.RemovePasswordSendResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SearchEvent.ShowSnackbar(BitwardenString.password_removed.asText()))
            }
        }
    }

    private fun handleSnackbarDataReceived(action: SearchAction.Internal.SnackbarDataReceived) {
        sendEvent(SearchEvent.ShowSnackbar(action.data))
    }

    private fun handleUpdateCipherResultReceive(
        action: SearchAction.Internal.UpdateCipherResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (val result = action.result) {
            is UpdateCipherResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
                            title = null,
                            message = result.errorMessage?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            UpdateCipherResult.Success -> {
                // Complete the autofill selection flow
                useCipherForAutofill(cipherId = action.cipherId)
            }
        }
    }

    private fun handleValidatePasswordResultReceive(
        action: SearchAction.Internal.ValidatePasswordResultReceive,
    ) {
        when (val result = action.result) {
            is ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
                            title = null,
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                if (!action.result.isValid) {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = SearchState.DialogState.Error(
                                title = null,
                                message = BitwardenString.invalid_master_password.asText(),
                            ),
                        )
                    }
                    return
                }
                handleMasterPasswordRepromptData(data = action.masterPasswordRepromptData)
            }
        }
    }

    private fun handleMasterPasswordRepromptData(
        data: MasterPasswordRepromptData,
    ) {
        // Complete the deferred actions
        when (data) {
            is MasterPasswordRepromptData.Autofill -> {
                trySendAction(
                    SearchAction.AutofillItemClick(
                        itemId = data.cipherId,
                    ),
                )
            }

            is MasterPasswordRepromptData.AutofillAndSave -> {
                trySendAction(
                    SearchAction.AutofillAndSaveItemClick(
                        itemId = data.cipherId,
                    ),
                )
            }

            is MasterPasswordRepromptData.OverflowItem -> {
                trySendAction(
                    SearchAction.OverflowOptionClick(
                        overflowAction = data.action,
                    ),
                )
            }

            is MasterPasswordRepromptData.ViewItem -> {
                trySendAction(
                    action = SearchAction.ItemClick(
                        itemId = data.cipherId,
                        itemType = data.itemType,
                    ),
                )
            }
        }
    }

    private fun handleVaultDataReceive(
        action: SearchAction.Internal.VaultDataReceive,
    ) {
        when (val vaultData = action.vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun handleRestrictItemTypesPolicyUpdateReceive(
        action: SearchAction.Internal.RestrictItemTypesPolicyUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                restrictItemTypesPolicyOrgIds = action
                    .restrictItemTypesPolicyOrdIds
                    .toImmutableList(),
            )
        }

        vaultRepo.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData = vaultData, clearDialogState = false)
        }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        vaultData
            .data
            ?.let { updateStateWithVaultData(vaultData = it, clearDialogState = true) }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SearchState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                        dialogState = null,
                    )
                }
            }
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = SearchState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        vaultData
            .data
            ?.let { updateStateWithVaultData(vaultData = it, clearDialogState = true) }
            ?: run {
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = SearchState.ViewState.Error(
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
    }

    private fun useCipherForAutofill(cipherId: String) {
        viewModelScope.launch {
            decryptCipherViewOrNull(cipherId)?.let {
                when (state.autofillSelectionData?.framework) {
                    AutofillSelectionData.Framework.ACCESSIBILITY -> {
                        accessibilitySelectionManager.emitAccessibilitySelection(cipherView = it)
                    }

                    AutofillSelectionData.Framework.AUTOFILL -> {
                        autofillSelectionManager.emitAutofillSelection(cipherView = it)
                    }

                    null -> Unit
                }
            }
        }
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = false)
    }

    private fun recalculateViewState() {
        vaultRepo.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData = vaultData, clearDialogState = false)
        }
    }

    private fun updateStateWithVaultData(vaultData: VaultData, clearDialogState: Boolean) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                searchType = currentState
                    .searchType
                    .updateWithAdditionalDataIfNecessary(
                        folderList = vaultData.folderViewList,
                        collectionList = vaultData.collectionViewList,
                    ),
                viewState = when (val searchType = currentState.searchType) {
                    is SearchTypeData.Vault -> {
                        vaultData
                            .decryptCipherListResult
                            .successes
                            .filterAndOrganize(searchType, state.searchTerm)
                            .applyRestrictItemTypesPolicy(
                                restrictItemTypesPolicyOrgIds = state.restrictItemTypesPolicyOrgIds,
                            )
                            .toFilteredList(
                                vaultFilterType = state
                                    .vaultFilterData
                                    ?.selectedVaultFilterType
                                    ?: VaultFilterType.AllVaults,
                            )
                            .toViewState(
                                searchTerm = state.searchTerm,
                                hasMasterPassword = state.hasMasterPassword,
                                baseIconUrl = state.baseIconUrl,
                                isIconLoadingDisabled = state.isIconLoadingDisabled,
                                isAutofill = state.isAutofill,
                                isPremiumUser = state.isPremium,
                            )
                    }

                    is SearchTypeData.Sends -> {
                        vaultData
                            .sendViewList
                            .filterAndOrganize(searchType, state.searchTerm)
                            .toViewState(
                                searchTerm = state.searchTerm,
                                baseWebSendUrl = state.baseWebSendUrl,
                                clock = clock,
                            )
                    }
                },
                dialogState = currentState.dialogState.takeUnless { clearDialogState },
            )
        }
    }

    private fun getCipherListViewOrNull(cipherId: String) =
        vaultRepo
            .vaultDataStateFlow
            .value
            .data
            ?.decryptCipherListResult
            ?.successes
            .orEmpty()
            .firstOrNull { it.id == cipherId }

    private suspend fun decryptCipherViewOrNull(cipherId: String): CipherView? =
        when (val result = vaultRepo.getCipher(cipherId = cipherId)) {
            GetCipherResult.CipherNotFound -> {
                Timber.e("Cipher not found.")
                sendAction(SearchAction.Internal.DecryptCipherErrorReceive(error = null))
                null
            }

            is GetCipherResult.Failure -> {
                Timber.e(result.error, "Failed to decrypt cipher.")
                sendAction(SearchAction.Internal.DecryptCipherErrorReceive(error = result.error))
                null
            }

            is GetCipherResult.Success -> result.cipherView
        }
}

/**
 * Represents the overall state for the [SearchScreen].
 */
@Parcelize
data class SearchState(
    val searchTerm: String,
    val searchType: SearchTypeData,
    val viewState: ViewState,
    val dialogState: DialogState?,
    val vaultFilterData: VaultFilterData?,
    val baseWebSendUrl: String,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    // Internal
    val autofillSelectionData: AutofillSelectionData?,
    val totpData: TotpData?,
    val hasMasterPassword: Boolean,
    val isPremium: Boolean,
    val restrictItemTypesPolicyOrgIds: ImmutableList<String>,
) : Parcelable {

    /**
     * Whether or not this represents an autofill selection flow.
     */
    val isAutofill: Boolean
        get() = autofillSelectionData != null

    /**
     * Whether or not this represents a listing screen for totp.
     */
    val isTotp: Boolean get() = totpData != null

    /**
     * Represents the specific view states for the search screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Determines whether or not the the Vault Filter may be shown (when applicable).
         */
        abstract val hasVaultFilter: Boolean

        /**
         * Show the populated state.
         */
        @Parcelize
        data class Content(
            val displayItems: List<DisplayItem>,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = true
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data class Empty(
            val message: Text?,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = true
        }

        /**
         * Show the error state.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = false
        }

        /**
         * Show the loading state.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val hasVaultFilter: Boolean get() = false
        }
    }

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
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

    /**
     * An item to be displayed.
     */
    @Parcelize
    data class DisplayItem(
        val id: String,
        val title: String,
        val titleTestTag: String,
        val subtitle: String?,
        val subtitleTestTag: String,
        val totpCode: String?,
        val iconData: IconData,
        val extraIconList: ImmutableList<IconData>,
        val overflowOptions: List<ListingItemOverflowAction>,
        val overflowTestTag: String?,
        val autofillSelectionOptions: List<AutofillSelectionOption>,
        val shouldDisplayMasterPasswordReprompt: Boolean,
        val itemType: ItemType,
    ) : Parcelable {
        /**
         * Indicates the item type as a send or vault item.
         */
        sealed class ItemType : Parcelable {
            /**
             * Indicates the item type is a send.
             */
            @Parcelize
            data class Sends(val type: SendType) : ItemType()

            /**
             * Indicates the item type is a vault item.
             */
            @Parcelize
            data class Vault(val type: CipherType) : ItemType()
        }
    }
}

/**
 * Represents the difference between searching sends and searching vault items.
 */
sealed class SearchTypeData : Parcelable {
    /**
     * The hint to display in the search toolbar at the top of the screen.
     */
    abstract val title: Text

    /**
     * Indicates that we should be searching sends.
     */
    @Parcelize
    sealed class Sends : SearchTypeData() {
        /**
         * Indicates that we should be searching all sends.
         */
        data object All : Sends() {
            override val title: Text get() = BitwardenString.search_sends.asText()
        }

        /**
         * Indicates that we should be searching only text sends.
         */
        data object Texts : Sends() {
            override val title: Text get() = BitwardenString.search_text_sends.asText()
        }

        /**
         * Indicates that we should be searching only file sends.
         */
        data object Files : Sends() {
            override val title: Text get() = BitwardenString.search_file_sends.asText()
        }
    }

    /**
     * Indicates that we should be searching vault items.
     */
    @Parcelize
    @OmitFromCoverage
    sealed class Vault : SearchTypeData() {
        /**
         * Indicates that we should be searching all vault items.
         */
        data object All : Vault() {
            override val title: Text get() = BitwardenString.search_vault.asText()
        }

        /**
         * Indicates that we should be searching only login ciphers.
         */
        data object Logins : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.logins.asText())
        }

        /**
         * Indicates that we should be searching only card ciphers.
         */
        data object Cards : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.cards.asText())
        }

        /**
         * Indicates that we should be searching only identity ciphers.
         */
        data object Identities : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.identities.asText())
        }

        /**
         * Indicates that we should be searching only secure note ciphers.
         */
        data object SecureNotes : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.secure_notes.asText())
        }

        /**
         * Indicates that we should be searching only ssh key ciphers.
         */
        data object SshKeys : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.ssh_keys.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the given collection.
         */
        data class Collection(
            val collectionId: String,
            val collectionName: String = "",
        ) : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(collectionName.asText())
        }

        /**
         * Indicates that we should be searching only ciphers not in a folder.
         */
        data object NoFolder : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.folder_none.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the given folder.
         */
        data class Folder(
            val folderId: String,
            val folderName: String = "",
        ) : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(folderName.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the trash.
         */
        data object Trash : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.trash.asText())
        }

        /**
         * Indicates that we should be searching only for verification code items.
         */
        data object VerificationCodes : Vault() {
            override val title: Text
                get() = BitwardenString.search.asText()
                    .concat(" ".asText())
                    .concat(BitwardenString.verification_codes.asText())
        }
    }
}

/**
 * Models actions for the [SearchScreen].
 */
sealed class SearchAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : SearchAction()

    /**
     * User clicked to dismiss the dialog.
     */
    data object DismissDialogClick : SearchAction()

    /**
     * User clicked a row item.
     */
    data class ItemClick(
        val itemId: String,
        val itemType: SearchState.DisplayItem.ItemType,
    ) : SearchAction()

    /**
     * User clicked a row item as an autofill selection.
     */
    data class AutofillItemClick(
        val itemId: String,
    ) : SearchAction()

    /**
     * User clicked a row item as an autofill-and-save selection.
     */
    data class AutofillAndSaveItemClick(
        val itemId: String,
    ) : SearchAction()

    /**
     * User clicked a row item for autofill but must satisfy the master password reprompt.
     */
    data class MasterPasswordRepromptSubmit(
        val password: String,
        val masterPasswordRepromptData: MasterPasswordRepromptData,
    ) : SearchAction()

    /**
     * User updated the search term.
     */
    data class SearchTermChange(
        val searchTerm: String,
    ) : SearchAction()

    /**
     * User selected a new vault filter type.
     */
    data class VaultFilterSelect(
        val vaultFilterType: VaultFilterType,
    ) : SearchAction()

    /**
     * User clicked on an overflow action.
     */
    data class OverflowOptionClick(
        val overflowAction: ListingItemOverflowAction,
    ) : SearchAction()

    /**
     * Models actions that the [SearchViewModel] itself might send.
     */
    sealed class Internal : SearchAction() {
        /**
         * Indicates the icon setting was received.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(
            val result: DeleteSendResult,
        ) : Internal()

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
         * Indicates that a restrict item types policy update has been received.
         */
        data class RestrictItemTypesPolicyUpdateReceive(
            val restrictItemTypesPolicyOrdIds: List<String>,
        ) : Internal()

        /**
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal(), BackgroundEvent

        /**
         * Indicates a result for updating a cipher during the autofill-and-save process.
         */
        data class UpdateCipherResultReceive(
            val cipherId: String,
            val result: UpdateCipherResult,
        ) : Internal()

        /**
         * Indicates a result for validating the user's master password during an autofill selection
         * process.
         */
        data class ValidatePasswordResultReceive(
            val masterPasswordRepromptData: MasterPasswordRepromptData,
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()

        /**
         * Indicates an error occurred while decrypting a cipher.
         */
        data class DecryptCipherErrorReceive(
            val error: Throwable?,
        ) : Internal()
    }
}

/**
 * Models events for the [SearchScreen].
 */
sealed class SearchEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : SearchEvent()

    /**
     * Navigates to edit a send.
     */
    data class NavigateToEditSend(
        val sendId: String,
        val sendType: SendItemType,
    ) : SearchEvent()

    /**
     * Navigates to view a send.
     */
    data class NavigateToViewSend(
        val sendId: String,
        val sendType: SendItemType,
    ) : SearchEvent()

    /**
     * Navigates to view a cipher.
     */
    data class NavigateToEditCipher(
        val cipherId: String,
        val cipherType: VaultItemCipherType,
    ) : SearchEvent()

    /**
     * Navigates to edit a cipher.
     */
    data class NavigateToViewCipher(
        val cipherId: String,
        val cipherType: VaultItemCipherType,
    ) : SearchEvent()

    /**
     * Navigates to the given [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : SearchEvent()

    /**
     * Shares the [content] with share sheet.
     */
    data class ShowShareSheet(
        val content: String,
    ) : SearchEvent()

    /**
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : SearchEvent(), BackgroundEvent {
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
     * Autofill-and-save was selected.
     */
    @Parcelize
    data class AutofillAndSave(
        val cipherId: String,
    ) : MasterPasswordRepromptData()

    /**
     * A cipher overflow menu item action was selected.
     */
    @Parcelize
    data class OverflowItem(
        val action: ListingItemOverflowAction.VaultAction,
    ) : MasterPasswordRepromptData()

    /**
     * Item was selected to be viewed.
     */
    @Parcelize
    data class ViewItem(
        val cipherId: String,
        val itemType: SearchState.DisplayItem.ItemType,
    ) : MasterPasswordRepromptData()
}
