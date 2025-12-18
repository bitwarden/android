package com.x8bit.bitwarden.ui.vault.feature.vault

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.hexToColor
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.account.util.initials
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserAutofillDialogManager
import com.x8bit.bitwarden.data.platform.manager.CredentialExchangeRegistryManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.components.util.toVaultItemCipherTypeOrNull
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAppBarTitle
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toSnackbarData
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toVaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.vault.util.vaultFilterDataIfRequired
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import com.x8bit.bitwarden.ui.vault.util.shortName
import com.x8bit.bitwarden.ui.vault.util.toVaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

private const val VAULT_DATA_RECEIVED_DELAY: Long = 550L
private const val LOGIN_SUCCESS_SNACKBAR_DELAY: Long = 550L
private const val BROWSER_AUTOFILL_DIALOG_DELAY: Long = 550L

/**
 * Manages [VaultState], handles [VaultAction], and launches [VaultEvent] for the [VaultScreen].
 */
@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val clipboardManager: BitwardenClipboardManager,
    private val organizationEventManager: OrganizationEventManager,
    private val clock: Clock,
    private val policyManager: PolicyManager,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
    private val reviewPromptManager: ReviewPromptManager,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val networkConnectionManager: NetworkConnectionManager,
    private val browserAutofillDialogManager: BrowserAutofillDialogManager,
    private val credentialExchangeRegistryManager: CredentialExchangeRegistryManager,
    featureFlagManager: FeatureFlagManager,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<VaultState, VaultEvent, VaultAction>(
    initialState = run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val accountSummaries = userState.toAccountSummaries()
        val activeAccountSummary = userState.toActiveAccountSummary()
        val vaultFilterData = userState.activeAccount.toVaultFilterData(
            isIndividualVaultDisabled = policyManager
                .getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
                .any(),
        )
        VaultState(
            appBarTitle = vaultFilterData.toAppBarTitle(),
            initials = activeAccountSummary.initials,
            avatarColorString = activeAccountSummary.avatarColorHex,
            accountSummaries = accountSummaries,
            vaultFilterData = vaultFilterData,
            viewState = VaultState.ViewState.Loading,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPremium = userState.activeAccount.isPremium,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
            baseIconUrl = userState.activeAccount.environment.environmentUrlData.baseIconUrl,
            hasMasterPassword = userState.activeAccount.hasMasterPassword,
            isRefreshing = false,
            showImportActionCard = false,
            flightRecorderSnackBar = settingsRepository
                .flightRecorderData
                .toSnackbarData(clock = clock),
            restrictItemTypesPolicyOrgIds = emptyList(),
            cipherDecryptionFailureIds = persistentListOf(),
            hasShownDecryptionFailureAlert = false,
        )
    },
) {
    /**
     * Helper for retrieving the selected vault filter type from the state (or a default).
     */
    private val vaultFilterTypeOrDefault: VaultFilterType
        get() = state.vaultFilterData?.selectedVaultFilterType ?: VaultFilterType.AllVaults

    init {
        // Attempt a sync each time we are on a fresh Vault Screen.
        vaultRepository.syncIfNecessary()

        // Reset the current vault filter type for the current user
        vaultRepository.vaultFilterType = vaultFilterTypeOrDefault
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { VaultAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .isIconLoadingDisabledFlow
            .map { VaultAction.Internal.IconLoadingSettingReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepository
            .vaultDataStateFlow
            .map { VaultAction.Internal.VaultDataReceive(it) }
            .onEach {
                // When the vault data is received, the current activity is about to
                // be recreated. Adding this delay prevents the dialogs from disappearing.
                delay(VAULT_DATA_RECEIVED_DELAY)
                trySendAction(it)
            }
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .map { VaultAction.Internal.UserStateUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        merge(
            snackbarRelayManager
                .getSnackbarDataFlow(SnackbarRelay.LOGIN_SUCCESS)
                .onEach {
                    // When the login success relay is triggered, the current activity is about to
                    // be recreated. Adding this delay prevents the Snackbar from disappearing.
                    delay(timeMillis = LOGIN_SUCCESS_SNACKBAR_DELAY)
                },
            snackbarRelayManager.getSnackbarDataFlow(
                SnackbarRelay.CIPHER_CREATED,
                SnackbarRelay.CIPHER_DELETED,
                SnackbarRelay.CIPHER_DELETED_SOFT,
                SnackbarRelay.CIPHER_RESTORED,
                SnackbarRelay.CIPHER_UPDATED,
                SnackbarRelay.FOLDER_CREATED,
                SnackbarRelay.LOGINS_IMPORTED,
            ),
        )
            .map { VaultAction.Internal.SnackbarDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepository
            .flightRecorderDataFlow
            .map { VaultAction.Internal.FlightRecorderDataReceive(data = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.RESTRICT_ITEM_TYPES)
            .map { policies -> policies.map { it.organizationId } }
            .map { VaultAction.Internal.PolicyUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        featureFlagManager.getFeatureFlagFlow(FlagKey.CredentialExchangeProtocolExport)
            .map { VaultAction.Internal.CredentialExchangeProtocolExportFlagUpdateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            delay(timeMillis = BROWSER_AUTOFILL_DIALOG_DELAY)
            mutableStateFlow.update { vaultState ->
                vaultState.copy(
                    dialog = VaultState.DialogState
                        .ThirdPartyBrowserAutofill(browserAutofillDialogManager.browserCount)
                        .takeIf {
                            vaultState.dialog == null &&
                                browserAutofillDialogManager.shouldShowDialog
                        }
                        ?: vaultState.dialog,
                )
            }
        }
    }

    override fun handleAction(action: VaultAction) {
        when (action) {
            is VaultAction.AddItemClick -> handleAddItemClick(action)
            is VaultAction.CardGroupClick -> handleCardClick()
            is VaultAction.FolderClick -> handleFolderItemClick(action)
            is VaultAction.CollectionClick -> handleCollectionItemClick(action)
            is VaultAction.IdentityGroupClick -> handleIdentityClick()
            is VaultAction.VerificationCodesClick -> handleVerificationCodeClick()
            is VaultAction.LoginGroupClick -> handleLoginClick()
            is VaultAction.SearchIconClick -> handleSearchIconClick()
            is VaultAction.LockAccountClick -> handleLockAccountClick(action)
            is VaultAction.LogoutAccountClick -> handleLogoutAccountClick(action)
            is VaultAction.SwitchAccountClick -> handleSwitchAccountClick(action)
            is VaultAction.AddAccountClick -> handleAddAccountClick()
            is VaultAction.SyncClick -> handleSyncClick()
            is VaultAction.LockClick -> handleLockClick()
            is VaultAction.VaultFilterTypeSelect -> handleVaultFilterTypeSelect(action)
            is VaultAction.SecureNoteGroupClick -> handleSecureNoteClick()
            is VaultAction.SshKeyGroupClick -> handleSshKeyClick()
            is VaultAction.TrashClick -> handleTrashClick()
            is VaultAction.VaultItemClick -> handleVaultItemClick(action)
            is VaultAction.TryAgainClick -> handleTryAgainClick()
            is VaultAction.DialogDismiss -> handleDialogDismiss()
            is VaultAction.RefreshPull -> handleRefreshPull()
            is VaultAction.OverflowOptionClick -> handleOverflowOptionClick(action)
            is VaultAction.OverflowMasterPasswordRepromptSubmit -> {
                handleOverflowMasterPasswordRepromptSubmit(action)
            }

            is VaultAction.MasterPasswordRepromptSubmit -> {
                handleMasterPasswordRepromptSubmit(action)
            }

            is VaultAction.Internal -> handleInternalAction(action)
            VaultAction.DismissImportActionCard -> handleDismissImportActionCard()
            VaultAction.ImportActionCardClick -> handleImportActionCardClick()
            VaultAction.LifecycleResumed -> handleLifecycleResumed()
            VaultAction.SelectAddItemType -> handleSelectAddItemType()
            VaultAction.DismissFlightRecorderSnackbar -> handleDismissFlightRecorderSnackbar()
            VaultAction.FlightRecorderGoToSettingsClick -> handleFlightRecorderGoToSettingsClick()
            is VaultAction.ShareCipherDecryptionErrorClick -> {
                handleShareCipherDecryptionErrorClick(action)
            }

            VaultAction.ShareAllCipherDecryptionErrorsClick -> {
                handleShareAllCipherDecryptionErrorsClick()
            }

            is VaultAction.KdfUpdatePasswordRepromptSubmit -> {
                handleKdfUpdatePasswordRepromptSubmit(action)
            }

            VaultAction.EnableThirdPartyAutofillClick -> handleEnableThirdPartyAutofillClick()
            VaultAction.DismissThirdPartyAutofillDialogClick -> {
                handleDismissThirdPartyAutofillDialogClick()
            }
        }
    }

    private fun handleDismissFlightRecorderSnackbar() {
        settingsRepository.dismissFlightRecorderBanner()
    }

    private fun handleFlightRecorderGoToSettingsClick() {
        sendEvent(VaultEvent.NavigateToAbout)
    }

    private fun handleShareCipherDecryptionErrorClick(
        action: VaultAction.ShareCipherDecryptionErrorClick,
    ) {
        sendEvent(
            event = VaultEvent.ShowShareSheet(
                content = action.selectedCipherId,
            ),
        )
    }

    private fun handleShareAllCipherDecryptionErrorsClick() {
        sendEvent(
            event = VaultEvent.ShowShareSheet(
                content = state
                    .cipherDecryptionFailureIds
                    .joinToString(separator = "\n"),
            ),
        )
    }

    private fun handleEnableThirdPartyAutofillClick() {
        browserAutofillDialogManager.delayDialog()
        mutableStateFlow.update { it.copy(dialog = null) }
        sendEvent(VaultEvent.NavigateToAutofillSettings)
    }

    private fun handleDismissThirdPartyAutofillDialogClick() {
        browserAutofillDialogManager.delayDialog()
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleSelectAddItemType() {
        // If policy is enable for any organization, exclude the card option
        val excludedOptions = persistentListOfNotNull(
            CreateVaultItemType.SSH_KEY,
            CreateVaultItemType.CARD.takeUnless {
                state.restrictItemTypesPolicyOrgIds.isEmpty()
            },
        )

        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.SelectVaultAddItemType(excludedOptions),
            )
        }
    }

    private fun handleLifecycleResumed() {
        when (specialCircumstanceManager.specialCircumstance) {
            is SpecialCircumstance.SearchShortcut -> {
                sendEvent(VaultEvent.NavigateToVaultSearchScreen)
                // not clearing SpecialCircumstance as it contains necessary data
                return
            }

            is SpecialCircumstance.VerificationCodeShortcut -> {
                sendEvent(VaultEvent.NavigateToVerificationCodeScreen)
                specialCircumstanceManager.specialCircumstance = null
                return
            }

            else -> Unit
        }

        if (reviewPromptManager.shouldPromptForAppReview()) {
            sendEvent(VaultEvent.PromptForAppReview)
        }
    }

    private fun handleImportActionCardClick() {
        sendEvent(VaultEvent.NavigateToImportLogins)
    }

    private fun handleDismissImportActionCard() {
        firstTimeActionManager.storeShowImportLoginsSettingsBadge(true)
        if (!state.showImportActionCard) return
        firstTimeActionManager.storeShowImportLogins(false)
    }

    private fun handleIconLoadingSettingReceive(
        action: VaultAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled)
        }

        updateViewState(
            vaultData = vaultRepository.vaultDataStateFlow.value,
        )
    }

    private fun handleAddItemClick(action: VaultAction.AddItemClick) {
        when (val vaultItemType = action.type) {
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
                            VaultEvent.NavigateToAddItemScreen(
                                type = it,
                            ),
                        )
                    }
            }

            CreateVaultItemType.FOLDER -> {
                sendEvent(
                    VaultEvent.NavigateToAddFolder,
                )
            }
        }
    }

    private fun handleCardClick() {
        sendEvent(
            VaultEvent.NavigateToItemListing(VaultItemListingType.Card),
        )
    }

    private fun handleFolderItemClick(action: VaultAction.FolderClick) {
        sendEvent(
            VaultEvent.NavigateToItemListing(
                VaultItemListingType.Folder(action.folderItem.id),
            ),
        )
    }

    private fun handleCollectionItemClick(action: VaultAction.CollectionClick) {
        sendEvent(
            VaultEvent.NavigateToItemListing(
                VaultItemListingType.Collection(action.collectionItem.id),
            ),
        )
    }

    private fun handleVerificationCodeClick() {
        sendEvent(VaultEvent.NavigateToVerificationCodeScreen)
    }

    private fun handleIdentityClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.Identity))
    }

    private fun handleLoginClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.Login))
    }

    private fun handleSearchIconClick() {
        sendEvent(VaultEvent.NavigateToVaultSearchScreen)
    }

    private fun handleLockAccountClick(action: VaultAction.LockAccountClick) {
        vaultRepository.lockVault(userId = action.accountSummary.userId, isUserInitiated = true)
    }

    private fun handleLogoutAccountClick(action: VaultAction.LogoutAccountClick) {
        authRepository.logout(
            userId = action.accountSummary.userId,
            reason = LogoutReason.Click(source = "VaultViewModel"),
        )
        mutableStateFlow.update {
            it.copy(isSwitchingAccounts = action.accountSummary.isActive)
        }
    }

    private fun handleSwitchAccountClick(action: VaultAction.SwitchAccountClick) {
        val isSwitchingAccounts =
            when (authRepository.switchAccount(userId = action.accountSummary.userId)) {
                SwitchAccountResult.AccountSwitched -> true
                SwitchAccountResult.NoChange -> false
            }
        mutableStateFlow.update {
            it.copy(isSwitchingAccounts = isSwitchingAccounts)
        }
    }

    private fun handleAddAccountClick() {
        authRepository.hasPendingAccountAddition = true
    }

    private fun handleSyncClick() {
        if (networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(dialog = VaultState.DialogState.Syncing)
            }
            vaultRepository.sync(forced = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialog = VaultState.DialogState.Error(
                        BitwardenString.internet_connection_required_title.asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
                )
            }
        }
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser(isUserInitiated = true)
    }

    private fun handleVaultFilterTypeSelect(action: VaultAction.VaultFilterTypeSelect) {
        // Update the current filter
        vaultRepository.vaultFilterType = action.vaultFilterType

        mutableStateFlow.update {
            it.copy(
                vaultFilterData = it.vaultFilterData?.copy(
                    selectedVaultFilterType = action.vaultFilterType,
                ),
            )
        }

        // Re-process the current vault data with the new filter
        updateViewState(
            vaultData = vaultRepository.vaultDataStateFlow.value,
        )
    }

    private fun handleTrashClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.Trash))
    }

    private fun handleSecureNoteClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.SecureNote))
    }

    private fun handleSshKeyClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.SshKey))
    }

    private fun handleVaultItemClick(action: VaultAction.VaultItemClick) {
        if (action.vaultItem.hasDecryptionError) {
            showCipherDecryptionErrorItemClick(itemId = action.vaultItem.id)
            return
        }

        sendEvent(
            event = VaultEvent.NavigateToVaultItem(
                itemId = action.vaultItem.id,
                type = action.vaultItem.type,
            ),
        )
    }

    private fun handleTryAgainClick() {
        vaultRepository.sync(forced = true)
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    @Suppress("MagicNumber")
    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(250)
            if (networkConnectionManager.isNetworkConnected) {
                vaultRepository.sync(forced = false)
            } else {
                sendAction(VaultAction.Internal.InternetConnectionErrorReceived)
            }
        }
    }

    private fun handleOverflowOptionClick(action: VaultAction.OverflowOptionClick) {
        when (val overflowAction = action.overflowAction) {
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
                handleEditClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.LaunchClick -> {
                handleLaunchClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.ViewClick -> {
                handleViewClick(overflowAction)
            }
        }
    }

    private fun handleOverflowMasterPasswordRepromptSubmit(
        action: VaultAction.OverflowMasterPasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.password)
            sendAction(
                VaultAction.Internal.OverflowValidatePasswordResultReceive(
                    overflowAction = action.overflowAction,
                    result = result,
                ),
            )
        }
    }

    private fun handleMasterPasswordRepromptSubmit(
        action: VaultAction.MasterPasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(password = action.password)
            sendAction(
                VaultAction.Internal.ValidatePasswordResultReceive(
                    item = action.item,
                    result = result,
                ),
            )
        }
    }

    private fun handleCopyNoteClick(action: ListingItemOverflowAction.VaultAction.CopyNoteClick) {
        viewModelScope.launch {
            getCipherForCopyOrNull(action.cipherId)?.let {
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
            getCipherForCopyOrNull(cipherId = action.cipherId)?.let {
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
            getCipherForCopyOrNull(cipherId = action.cipherId)?.let {
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
            getCipherForCopyOrNull(cipherId = action.cipherId)?.let {
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
            sendAction(VaultAction.Internal.GenerateTotpResultReceive(result))
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

    private fun handleEditClick(action: ListingItemOverflowAction.VaultAction.EditClick) {
        sendEvent(
            event = VaultEvent.NavigateToEditVaultItem(
                itemId = action.cipherId,
                type = action.cipherType.toVaultItemCipherType(),
            ),
        )
    }

    private fun handleLaunchClick(action: ListingItemOverflowAction.VaultAction.LaunchClick) {
        sendEvent(VaultEvent.NavigateToUrl(action.url))
    }

    private fun handleViewClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(
            event = VaultEvent.NavigateToVaultItem(
                itemId = action.cipherId,
                type = action.cipherType.toVaultItemCipherType(),
            ),
        )
    }

    private fun showCipherDecryptionErrorItemClick(itemId: String) {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.CipherDecryptionError(
                    title = BitwardenString.decryption_error.asText(),
                    message = BitwardenString
                        .bitwarden_could_not_decrypt_this_vault_item_description_long.asText(),
                    selectedCipherId = itemId,
                ),
            )
        }
    }

    private fun handleInternalAction(action: VaultAction.Internal) {
        when (action) {
            is VaultAction.Internal.GenerateTotpResultReceive -> {
                handleGenerateTotpResultReceive(action)
            }

            is VaultAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is VaultAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
            is VaultAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultAction.Internal.IconLoadingSettingReceive -> handleIconLoadingSettingReceive(
                action,
            )

            is VaultAction.Internal.OverflowValidatePasswordResultReceive -> {
                handleOverflowValidatePasswordResultReceive(action)
            }

            is VaultAction.Internal.ValidatePasswordResultReceive -> {
                handleValidatePasswordResultReceive(action)
            }

            is VaultAction.Internal.SnackbarDataReceive -> handleSnackbarDataReceive(action)

            VaultAction.Internal.InternetConnectionErrorReceived -> {
                handleInternetConnectionErrorReceived()
            }

            is VaultAction.Internal.FlightRecorderDataReceive -> {
                handleFlightRecorderDataReceive(action)
            }

            is VaultAction.Internal.PolicyUpdateReceive -> {
                handlePolicyUpdateReceive(action)
            }

            is VaultAction.Internal.DecryptionErrorReceive -> {
                handleDecryptionErrorReceive(action)
            }

            is VaultAction.Internal.UpdatedKdfToMinimumsReceived -> {
                handleUpdatedKdfToMinimumsReceived(action)
            }

            is VaultAction.Internal.CredentialExchangeProtocolExportFlagUpdateReceive -> {
                handleCredentialExchangeProtocolExportFlagUpdateReceive(action)
            }
        }
    }

    private fun handleUpdatedKdfToMinimumsReceived(
        action: VaultAction.Internal.UpdatedKdfToMinimumsReceived,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        when (val result = action.result) {
            UpdateKdfMinimumsResult.ActiveAccountNotFound -> {
                showGenericError(
                    message = BitwardenString.kdf_update_failed_active_account_not_found.asText(),
                )
                Timber.e(message = "Failed to update kdf to minimums: Active account not found")
            }

            is UpdateKdfMinimumsResult.Error -> {
                showGenericError(
                    message = BitwardenString
                        .an_error_occurred_while_trying_to_update_your_kdf_settings
                        .asText(),
                    error = result.error,
                )
                Timber.e(result.error, message = "Failed to update kdf to minimums.")
            }

            UpdateKdfMinimumsResult.Success -> {
                sendEvent(
                    event = VaultEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.encryption_settings_updated.asText(),
                        ),
                    ),
                )
            }
        }
    }

    private fun handleCredentialExchangeProtocolExportFlagUpdateReceive(
        action: VaultAction.Internal.CredentialExchangeProtocolExportFlagUpdateReceive,
    ) {
        viewModelScope.launch {
            if (action.isCredentialExchangeProtocolExportEnabled) {
                credentialExchangeRegistryManager.register()
            } else {
                credentialExchangeRegistryManager.unregister()
            }
        }
    }

    private fun handleDecryptionErrorReceive(action: VaultAction.Internal.DecryptionErrorReceive) {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.Error(
                    title = action.title,
                    message = action.message,
                    error = action.error,
                ),
            )
        }
    }

    private fun handlePolicyUpdateReceive(action: VaultAction.Internal.PolicyUpdateReceive) {
        mutableStateFlow.update {
            it.copy(restrictItemTypesPolicyOrgIds = action.restrictItemTypesPolicyOrdIds)
        }

        vaultRepository.vaultDataStateFlow.value.data?.let { vaultData ->
            updateVaultState(
                vaultData = vaultData,
                dialog = state.dialog,
            )
        }
    }

    private fun handleInternetConnectionErrorReceived() {
        mutableStateFlow.update {
            it.copy(
                isRefreshing = false,
                dialog = VaultState.DialogState.Error(
                    BitwardenString.internet_connection_required_title.asText(),
                    BitwardenString.internet_connection_required_message.asText(),
                ),
            )
        }
    }

    private fun handleFlightRecorderDataReceive(
        action: VaultAction.Internal.FlightRecorderDataReceive,
    ) {
        mutableStateFlow.update {
            it.copy(flightRecorderSnackBar = action.data.toSnackbarData(clock = clock))
        }
    }

    private fun handleSnackbarDataReceive(action: VaultAction.Internal.SnackbarDataReceive) {
        sendEvent(VaultEvent.ShowSnackbar(action.data))
    }

    private fun handleGenerateTotpResultReceive(
        action: VaultAction.Internal.GenerateTotpResultReceive,
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

    private fun handlePullToRefreshEnableReceive(
        action: VaultAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleUserStateUpdateReceive(action: VaultAction.Internal.UserStateUpdateReceive) {
        // Leave the current data alone if there is no UserState; we are in the process of logging
        // out.
        val userState = action.userState ?: return
        val firstTimeState = userState.activeUserFirstTimeState

        // Avoid updating the UI if we are actively switching users to avoid changes while
        // navigating.
        if (state.isSwitchingAccounts) return

        val vaultFilterData = userState.activeAccount.toVaultFilterData(
            isIndividualVaultDisabled = policyManager
                .getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
                .any(),
        )
        val appBarTitle = vaultFilterData.toAppBarTitle()

        mutableStateFlow.update {
            val accountSummaries = userState.toAccountSummaries()
            val activeAccountSummary = userState.toActiveAccountSummary()
            it.copy(
                appBarTitle = appBarTitle,
                initials = activeAccountSummary.initials,
                avatarColorString = activeAccountSummary.avatarColorHex,
                accountSummaries = accountSummaries,
                vaultFilterData = vaultFilterData,
                isPremium = userState.activeAccount.isPremium,
                showImportActionCard = firstTimeState.showImportLoginsCard,
            )
        }
    }

    private fun handleVaultDataReceive(action: VaultAction.Internal.VaultDataReceive) {
        // Avoid updating the UI if we are actively switching users to avoid changes while
        // navigating.
        if (state.isSwitchingAccounts) return

        updateViewState(
            vaultData = action.vaultData,
        )
    }

    private fun updateViewState(vaultData: DataState<VaultData>) {
        when (vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(
                vaultData = vaultData,
            )

            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(
                vaultData = vaultData,
            )

            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        mutableStateFlow.updateToErrorStateOrDialog(
            baseIconUrl = state.baseIconUrl,
            vaultData = vaultData.data,
            vaultFilterType = vaultFilterTypeOrDefault,
            isIconLoadingDisabled = state.isIconLoadingDisabled,
            isPremium = state.isPremium,
            hasMasterPassword = state.hasMasterPassword,
            errorTitle = BitwardenString.an_error_has_occurred.asText(),
            errorMessage = BitwardenString.generic_error_message.asText(),
            isRefreshing = false,
            restrictItemTypesPolicyOrgIds = state.restrictItemTypesPolicyOrgIds,
        )
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        if (state.dialog == VaultState.DialogState.Syncing) {
            sendEvent(VaultEvent.ShowSnackbar(message = BitwardenString.syncing_complete.asText()))
        }

        val shouldShowDecryptionAlert = !state.hasShownDecryptionFailureAlert &&
            vaultData.data.decryptCipherListResult.failures.isNotEmpty() &&
            state.dialog == null

        updateVaultState(
            vaultData = vaultData.data,
            dialog = getDialogVaultLoaded(
                shouldShowDecryptionAlert = shouldShowDecryptionAlert,
                vaultData = vaultData,
            ),
            hasShownDecryptionFailureAlert = if (shouldShowDecryptionAlert) {
                true
            } else {
                state.hasShownDecryptionFailureAlert
            },
        )
    }

    private fun getDialogVaultLoaded(
        shouldShowDecryptionAlert: Boolean,
        vaultData: DataState.Loaded<VaultData>,
    ): VaultState.DialogState? = if (authRepository.needsKdfUpdateToMinimums()) {
        VaultState.DialogState.VaultLoadKdfUpdateRequired(
            title = BitwardenString.update_your_encryption_settings.asText(),
            message = BitwardenString
                .the_new_recommended_encryption_settings_will_improve_your_account_desc_long
                .asText(),
        )
    } else if (shouldShowDecryptionAlert ||
        state.dialog is VaultState.DialogState.VaultLoadCipherDecryptionError
    ) {
        VaultState.DialogState.VaultLoadCipherDecryptionError(
            title = BitwardenString.decryption_error.asText(),
            cipherCount = vaultData.data.decryptCipherListResult.failures.size,
        )
    } else if (state.dialog is VaultState.DialogState.ThirdPartyBrowserAutofill) {
        state.dialog
    } else {
        null
    }

    private fun updateVaultState(
        vaultData: VaultData,
        dialog: VaultState.DialogState? = null,
        hasShownDecryptionFailureAlert: Boolean = state.hasShownDecryptionFailureAlert,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.toViewState(
                    baseIconUrl = state.baseIconUrl,
                    isIconLoadingDisabled = state.isIconLoadingDisabled,
                    isPremium = state.isPremium,
                    hasMasterPassword = state.hasMasterPassword,
                    vaultFilterType = vaultFilterTypeOrDefault,
                    restrictItemTypesPolicyOrgIds = state.restrictItemTypesPolicyOrgIds,
                ),
                dialog = dialog,
                isRefreshing = false,
                cipherDecryptionFailureIds = vaultData
                    .decryptCipherListResult
                    .failures
                    .mapNotNull { cipher -> cipher.id }
                    .toImmutableList(),
                hasShownDecryptionFailureAlert = hasShownDecryptionFailureAlert,
            )
        }
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(
        vaultData: DataState.NoNetwork<VaultData>,
    ) {
        val data = vaultData.data ?: VaultData(
            decryptCipherListResult = DecryptCipherListResult(
                successes = emptyList(),
                failures = emptyList(),
            ),
            collectionViewList = emptyList(),
            folderViewList = emptyList(),
            sendViewList = emptyList(),
        )
        updateVaultState(
            vaultData = data,
        )
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.data.toViewState(
                    baseIconUrl = state.baseIconUrl,
                    isIconLoadingDisabled = state.isIconLoadingDisabled,
                    isPremium = state.isPremium,
                    hasMasterPassword = state.hasMasterPassword,
                    vaultFilterType = vaultFilterTypeOrDefault,
                    restrictItemTypesPolicyOrgIds = state.restrictItemTypesPolicyOrgIds,
                ),
            )
        }
    }

    private fun handleOverflowValidatePasswordResultReceive(
        action: VaultAction.Internal.OverflowValidatePasswordResultReceive,
    ) {
        when (val result = action.result) {
            is ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                if (!result.isValid) {
                    mutableStateFlow.update {
                        it.copy(
                            dialog = VaultState.DialogState.Error(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.invalid_master_password.asText(),
                            ),
                        )
                    }
                    return
                }
                // Complete the overflow action.
                trySendAction(VaultAction.OverflowOptionClick(action.overflowAction))
            }
        }
    }

    private fun handleValidatePasswordResultReceive(
        action: VaultAction.Internal.ValidatePasswordResultReceive,
    ) {
        when (val result = action.result) {
            is ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                if (result.isValid) {
                    trySendAction(VaultAction.VaultItemClick(vaultItem = action.item))
                } else {
                    mutableStateFlow.update {
                        it.copy(
                            dialog = VaultState.DialogState.Error(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.invalid_master_password.asText(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private suspend fun getCipherForCopyOrNull(cipherId: String): CipherView? =
        when (val result = vaultRepository.getCipher(cipherId)) {
            GetCipherResult.CipherNotFound -> {
                Timber.e("Cipher not found while copying number")
                sendAction(
                    VaultAction.Internal.DecryptionErrorReceive(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.generic_error_message.asText(),
                        error = null,
                    ),
                )
                null
            }

            is GetCipherResult.Failure -> {
                Timber.e(result.error, "Failed to decrypt cipher while copying number.")
                sendAction(
                    VaultAction.Internal.DecryptionErrorReceive(
                        title = BitwardenString.decryption_error.asText(),
                        message = BitwardenString.failed_to_decrypt_cipher_contact_support.asText(),
                        error = result.error,
                    ),
                )
                null
            }

            is GetCipherResult.Success -> result.cipherView
        }

    private fun handleKdfUpdatePasswordRepromptSubmit(
        action: VaultAction.KdfUpdatePasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.updateKdfToMinimumsIfNeeded(password = action.password)
            sendAction(action = VaultAction.Internal.UpdatedKdfToMinimumsReceived(result))
        }
    }

    private fun showGenericError(
        message: Text = BitwardenString.generic_error_message.asText(),
        error: Throwable? = null,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = message,
                    error = error,
                ),
            )
        }
    }
}

/**
 * Represents the overall state for the [VaultScreen].
 *
 * @property avatarColorString The color of the avatar in HEX format.
 * @property initials The initials to be displayed on the avatar.
 * @property accountSummaries List of all the current accounts.
 * @property viewState The specific view state representing loading, no items, or content state.
 * @property dialog Information about any dialogs that may need to be displayed.
 * @property isSwitchingAccounts Whether or not we are actively switching accounts.
 * @property isPremium Whether the user is a premium user.
 */
@Parcelize
data class VaultState(
    val appBarTitle: Text,
    private val avatarColorString: String,
    val initials: String,
    val accountSummaries: List<AccountSummary>,
    val vaultFilterData: VaultFilterData? = null,
    val viewState: ViewState,
    val dialog: DialogState? = null,
    val isRefreshing: Boolean,
    val showImportActionCard: Boolean,
    val flightRecorderSnackBar: BitwardenSnackbarData?,
    // Internal-use properties
    val isSwitchingAccounts: Boolean = false,
    val isPremium: Boolean,
    val hasMasterPassword: Boolean,
    private val isPullToRefreshSettingEnabled: Boolean,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    val cipherDecryptionFailureIds: ImmutableList<String>,
    val hasShownDecryptionFailureAlert: Boolean,
    val restrictItemTypesPolicyOrgIds: List<String>,
) : Parcelable {

    /**
     * The [Color] of the avatar.
     */
    val avatarColor: Color get() = avatarColorString.hexToColor()

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * VaultFilterData that the user has access to.
     */
    val vaultFilterDataWithFilter: VaultFilterData?
        get() = viewState.vaultFilterDataIfRequired(vaultFilterData = vaultFilterData)

    /**
     * Represents the specific view states for the [VaultScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Determines whether or not the Floating Action Button (FAB) should be shown for the
         * given state.
         */
        abstract val hasFab: Boolean

        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Loading state for the [VaultScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val hasFab: Boolean get() = false
            override val isPullToRefreshEnabled: Boolean get() = false
        }

        /**
         * Represents a state where the [VaultScreen] has no items to display.
         */
        @Parcelize
        data object NoItems : ViewState() {
            override val hasFab: Boolean get() = true
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Represents a state where the [VaultScreen] is unable to display data due to an error
         * retrieving it. The given [message] should be displayed.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val hasFab: Boolean get() = false
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Content state for the [VaultScreen] showing the actual content or items.
         *
         * @property totpItemsCount The count of totp code items.
         * @property loginItemsCount The count of Login type items.
         * @property cardItemsCount The count of Card type items.
         * @property identityItemsCount The count of Identity type items.
         * @property secureNoteItemsCount The count of Secure Notes type items.
         * @property favoriteItems The list of favorites to be displayed.
         * @property folderItems The list of folders to be displayed.
         * @property noFolderItems The list of non-folders to be displayed.
         * @property collectionItems The list of collections to be displayed.
         * @property trashItemsCount The number of items present in the trash.
         */
        @Parcelize
        data class Content(
            val itemTypesCount: Int,
            val totpItemsCount: Int,
            val loginItemsCount: Int,
            val cardItemsCount: Int,
            val identityItemsCount: Int,
            val secureNoteItemsCount: Int,
            val sshKeyItemsCount: Int,
            val favoriteItems: List<VaultItem>,
            val folderItems: List<FolderItem>,
            val noFolderItems: List<VaultItem>,
            val collectionItems: List<CollectionItem>,
            val trashItemsCount: Int,
            val showCardGroup: Boolean,
        ) : ViewState() {
            override val hasFab: Boolean get() = true
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Represents a folder item with a name and item count.
         *
         * @property id The unique identifier for this folder or null to indicate that it is
         * "no folder".
         * @property name The display name of this folder.
         * @property itemCount The number of items this folder contains.
         */
        @Parcelize
        data class FolderItem(
            val id: String?,
            val name: Text,
            val itemCount: Int,
        ) : Parcelable

        /**
         * Represents a collection.
         *
         * @property id The unique identifier for this collection.
         * @property name The display name of the collection.
         * @property itemCount The number of items this collection contains.
         */
        @Parcelize
        data class CollectionItem(
            val id: String,
            val name: String,
            val itemCount: Int,
        ) : Parcelable

        /**
         * A sealed class hierarchy representing different types of items in the vault.
         */
        @Parcelize
        sealed class VaultItem : Parcelable {

            /**
             * The unique identifier for this item.
             */
            abstract val id: String

            /**
             * The display name of the vault item.
             */
            abstract val name: Text

            /**
             * The icon at the start of the item.
             */
            abstract val startIcon: IconData

            /**
             * The test tag for the icon at the start of the item.
             */
            abstract val startIconTestTag: String

            /**
             * The icons shown after the item name.
             */
            abstract val extraIconList: ImmutableList<IconData>

            /**
             * An optional supporting label for the vault item that provides additional information.
             * This property is open to be overridden by subclasses that can provide their own
             * supporting label relevant to the item's type.
             */
            abstract val supportingLabel: Text?

            /**
             * The overflow options to be displayed for the vault item.
             */
            abstract val overflowOptions: List<ListingItemOverflowAction.VaultAction>

            /**
             * Whether to prompt the user for their password when they select an overflow option.
             */
            abstract val shouldShowMasterPasswordReprompt: Boolean

            /**
             * The [VaultItemCipherType] this item represents.
             */
            abstract val type: VaultItemCipherType

            /**
             * Indicates whether this item has a decryption error.
             */
            abstract val hasDecryptionError: Boolean

            /**
             * Represents a login item within the vault.
             *
             * @property username The username associated with this login item.
             */
            @Parcelize
            data class Login(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(BitwardenDrawable.ic_globe),
                override val startIconTestTag: String = "LoginCipherIcon",
                override val extraIconList: ImmutableList<IconData> = persistentListOf(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                override val hasDecryptionError: Boolean,
                val username: Text?,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = username
                override val type: VaultItemCipherType get() = VaultItemCipherType.LOGIN
            }

            /**
             * Represents a card item within the vault, storing details about a user's payment card.
             *
             * @property brand The brand of the card, e.g., Visa, MasterCard.
             * @property lastFourDigits The last four digits of the card number.
             */
            @Parcelize
            data class Card(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(
                    BitwardenDrawable.ic_payment_card,
                ),
                override val startIconTestTag: String = "CardCipherIcon",
                override val extraIconList: ImmutableList<IconData> = persistentListOf(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                override val hasDecryptionError: Boolean,
                private val brand: VaultCardBrand? = null,
                val lastFourDigits: Text? = null,
            ) : VaultItem() {
                override val supportingLabel: Text?
                    get() = when {
                        brand != null && lastFourDigits != null ->
                            brand.shortName.concat(", *".asText(), lastFourDigits)

                        brand != null -> brand.shortName
                        lastFourDigits != null -> "*".asText().concat(lastFourDigits)
                        else -> null
                    }

                override val type: VaultItemCipherType get() = VaultItemCipherType.CARD
            }

            /**
             * Represents an identity item within the vault, containing personal identification
             * information.
             *
             * @property fullName The first and last name of the individual associated with
             * this identity item.
             */
            @Parcelize
            data class Identity(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(BitwardenDrawable.ic_id_card),
                override val startIconTestTag: String = "IdentityCipherIcon",
                override val extraIconList: ImmutableList<IconData> = persistentListOf(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val hasDecryptionError: Boolean,
                override val shouldShowMasterPasswordReprompt: Boolean,
                val fullName: Text?,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = fullName
                override val type: VaultItemCipherType get() = VaultItemCipherType.IDENTITY
            }

            /**
             * Represents a secure note item within the vault, designed to store secure,
             * non-categorized textual information.
             */
            @Parcelize
            data class SecureNote(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(BitwardenDrawable.ic_note),
                override val startIconTestTag: String = "SecureNoteCipherIcon",
                override val extraIconList: ImmutableList<IconData> = persistentListOf(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val hasDecryptionError: Boolean,
                override val shouldShowMasterPasswordReprompt: Boolean,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = null
                override val type: VaultItemCipherType get() = VaultItemCipherType.SECURE_NOTE
            }

            /**
             * Represents a SSH key item within the vault, designed to store SSH keys.
             */
            @Parcelize
            data class SshKey(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(BitwardenDrawable.ic_ssh_key),
                override val startIconTestTag: String = "SshKeyCipherIcon",
                override val extraIconList: ImmutableList<IconData> = persistentListOf(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                override val hasDecryptionError: Boolean,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = null
                override val type: VaultItemCipherType get() = VaultItemCipherType.SSH_KEY
            }
        }
    }

    /**
     * Information about a dialog to display.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dialog indication and ongoing manual sync.
         */
        @Parcelize
        data object Syncing : DialogState()

        /**
         * Represents a dialog for selecting a vault item type to add.
         */
        @Parcelize
        data class SelectVaultAddItemType(
            val excludedOptions: ImmutableList<CreateVaultItemType>,
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
         * Represents a dialog indicating that a 3rd party browser required Autofill configuration.
         */
        @Parcelize
        data class ThirdPartyBrowserAutofill(
            val browserCount: Int,
        ) : DialogState()

        /**
         * Represents a dialog indicating that there was a decryption error loading ciphers.
         */
        @Parcelize
        data class VaultLoadCipherDecryptionError(
            val title: Text,
            val cipherCount: Int,
        ) : DialogState()

        /**
         * Represents a dialog indicating that the user needs to update their kdf settings.
         */
        @Parcelize
        data class VaultLoadKdfUpdateRequired(
            val title: Text,
            val message: Text,
        ) : DialogState()

        /**
         * Represents an error dialog with the given [title] and [message].
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()
    }
}

/**
 * Models effects for the [VaultScreen].
 */
sealed class VaultEvent {
    /**
     * Navigate to the Vault Search screen.
     */
    data object NavigateToVaultSearchScreen : VaultEvent()

    /**
     * Navigate to the Add Item screen.
     */
    data class NavigateToAddItemScreen(
        val type: VaultItemCipherType,
    ) : VaultEvent()

    /**
     * Navigate to the item details screen.
     */
    data class NavigateToVaultItem(
        val itemId: String,
        val type: VaultItemCipherType,
    ) : VaultEvent()

    /**
     * Navigate to the item edit screen.
     */
    data class NavigateToEditVaultItem(
        val itemId: String,
        val type: VaultItemCipherType,
    ) : VaultEvent()

    /**
     * Navigate to the item listing screen.
     */
    data class NavigateToItemListing(
        val itemListingType: VaultItemListingType,
    ) : VaultEvent()

    /**
     * Navigates to the given [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : VaultEvent()

    /**
     * Navigate to the verification code screen.
     */
    data object NavigateToVerificationCodeScreen : VaultEvent()

    /**
     * Navigate to the import logins screen.
     */
    data object NavigateToImportLogins : VaultEvent()

    /**
     * Indicates that we should prompt the user for app review.
     */
    data object PromptForAppReview : VaultEvent()

    /**
     * Show a snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : VaultEvent(), BackgroundEvent {
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
     * Show a share sheet with the given content.
     */
    data class ShowShareSheet(val content: String) : VaultEvent()

    /**
     * Navigate to the add folder screen
     */
    data object NavigateToAddFolder : VaultEvent()

    /**
     * Navigate to settings.
     */
    data object NavigateToAbout : VaultEvent()

    /**
     * Navigate to Autofill settings screen.
     */
    data object NavigateToAutofillSettings : VaultEvent()
}

/**
 * Models actions for the [VaultScreen].
 */
sealed class VaultAction {
    /**
     * User has clicked the go to settings button.
     */
    data object FlightRecorderGoToSettingsClick : VaultAction()

    /**
     * User has dismissed the flight recorder.
     */
    data object DismissFlightRecorderSnackbar : VaultAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : VaultAction()

    /**
     * Click the add an item button.
     * This can either be the floating action button or actual add an item button.
     */
    data class AddItemClick(val type: CreateVaultItemType) : VaultAction()

    /**
     * Click the search icon.
     */
    data object SearchIconClick : VaultAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to lock
     * the associated account's vault.
     */
    data class LockAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to log out
     * of that account.
     */
    data class LogoutAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultAction()

    /**
     * User clicked an account in the account switcher.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : VaultAction()

    /**
     * User clicked on Add Account in the account switcher.
     */
    data object AddAccountClick : VaultAction()

    /**
     * User clicked the Sync option in the overflow menu.
     */
    data object SyncClick : VaultAction()

    /**
     * User clicked the Lock option in the overflow menu.
     */
    data object LockClick : VaultAction()

    /**
     * User selected a [VaultFilterType] from the Vault Filter menu.
     */
    data class VaultFilterTypeSelect(
        val vaultFilterType: VaultFilterType,
    ) : VaultAction()

    /**
     * Action to trigger when a specific vault item is clicked.
     */
    data class VaultItemClick(
        val vaultItem: VaultState.ViewState.VaultItem,
    ) : VaultAction()

    /**
     * Action to trigger when a specific folder item is clicked.
     */
    data class FolderClick(
        val folderItem: VaultState.ViewState.FolderItem,
    ) : VaultAction()

    /**
     * Action to trigger when a specific collection item is clicked.
     */
    data class CollectionClick(
        val collectionItem: VaultState.ViewState.CollectionItem,
    ) : VaultAction()

    /**
     * User clicked on the verification codes button.
     */
    data object VerificationCodesClick : VaultAction()

    /**
     * User clicked the login types button.
     */
    data object LoginGroupClick : VaultAction()

    /**
     * User clicked the card types button.
     */
    data object CardGroupClick : VaultAction()

    /**
     * User clicked the identity types button.
     */
    data object IdentityGroupClick : VaultAction()

    /**
     * User clicked the secure notes types button.
     */
    data object SecureNoteGroupClick : VaultAction()

    /**
     * Click to enabled 3rd party autofill for a browser.
     */
    data object EnableThirdPartyAutofillClick : VaultAction()

    /**
     * Click to dismiss 3rd party autofill dialog.
     */
    data object DismissThirdPartyAutofillDialogClick : VaultAction()

    /**
     * Click to share cipher decryption error details.
     */
    data class ShareCipherDecryptionErrorClick(
        val selectedCipherId: String,
    ) : VaultAction()

    /**
     * Click to submit the update kdf password reprompt form.
     */
    data class KdfUpdatePasswordRepromptSubmit(
        val password: String,
    ) : VaultAction()

    /**
     * Click to share all cipher decryption error details.
     */
    data object ShareAllCipherDecryptionErrorsClick : VaultAction()

    /**
     * User clicked the SSH key types button.
     */
    data object SshKeyGroupClick : VaultAction()

    /**
     * User clicked the trash button.
     */
    data object TrashClick : VaultAction()

    /**
     * The user has requested that any visible dialogs are dismissed.
     */
    data object DialogDismiss : VaultAction()

    /**
     * User clicked the Try Again button when there is an error displayed.
     */
    data object TryAgainClick : VaultAction()

    /**
     * The user has dismissed the import action card.
     */
    data object DismissImportActionCard : VaultAction()

    /**
     * The user has clicked the import action card.
     */
    data object ImportActionCardClick : VaultAction()

    /**
     * User clicked an overflow action.
     */
    data class OverflowOptionClick(
        val overflowAction: ListingItemOverflowAction.VaultAction,
    ) : VaultAction()

    /**
     * User submitted their master password to authenticate before continuing with
     * the selected overflow action.
     */
    data class OverflowMasterPasswordRepromptSubmit(
        val overflowAction: ListingItemOverflowAction.VaultAction,
        val password: String,
    ) : VaultAction()

    /**
     * User submitted their master password to authenticate before continuing with the primary
     * action.
     */
    data class MasterPasswordRepromptSubmit(
        val item: VaultState.ViewState.VaultItem,
        val password: String,
    ) : VaultAction()

    /**
     * The lifecycle of the VaultScreen has entered a resumed state.
     */
    data object LifecycleResumed : VaultAction()

    /**
     * User has clicked button to bring up the add item selection dialog.
     */
    data object SelectAddItemType : VaultAction()

    /**
     * Models actions that the [VaultViewModel] itself might send.
     */
    sealed class Internal : VaultAction() {

        /**
         * Indicates that the icon loading setting has been changed.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates that the there is not internet connection.
         */
        data object InternetConnectionErrorReceived : Internal()

        /**
         * Indicates a result for generating a verification code has been received.
         */
        data class GenerateTotpResultReceive(
            val result: GenerateTotpResult,
        ) : Internal()

        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(val isPullToRefreshEnabled: Boolean) : Internal()

        /**
         * Indicates a change in user state has been received.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
        ) : Internal()

        /**
         * Indicates a vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's master password has been received.
         */
        data class OverflowValidatePasswordResultReceive(
            val overflowAction: ListingItemOverflowAction.VaultAction,
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's master password has been received.
         */
        data class ValidatePasswordResultReceive(
            val item: VaultState.ViewState.VaultItem,
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a snackbar data was received.
         */
        data class SnackbarDataReceive(
            val data: BitwardenSnackbarData,
        ) : Internal(), BackgroundEvent

        /**
         * Indicates that the flight recorder data was received.
         */
        data class FlightRecorderDataReceive(
            val data: FlightRecorderDataSet,
        ) : Internal()

        /**
         * Indicates that a policy update has been received.
         */
        data class PolicyUpdateReceive(
            val restrictItemTypesPolicyOrdIds: List<String>,
        ) : Internal()

        /**
         * Indicates that a decryption error has occurred.
         */
        data class DecryptionErrorReceive(
            val title: Text,
            val message: Text,
            val error: Throwable?,
        ) : Internal()

        /**
         * Indicates that a result for updating the kdf has been received.
         */
        data class UpdatedKdfToMinimumsReceived(
            val result: UpdateKdfMinimumsResult,
        ) : Internal()

        /**
         * Indicates that the Credential Exchange Protocol export flag has been updated.
         */
        data class CredentialExchangeProtocolExportFlagUpdateReceive(
            val isCredentialExchangeProtocolExportEnabled: Boolean,
        ) : Internal()
    }
}

@Suppress("LongParameterList")
private fun MutableStateFlow<VaultState>.updateToErrorStateOrDialog(
    baseIconUrl: String,
    vaultData: VaultData?,
    vaultFilterType: VaultFilterType,
    isIconLoadingDisabled: Boolean,
    isPremium: Boolean,
    hasMasterPassword: Boolean,
    errorTitle: Text,
    errorMessage: Text,
    isRefreshing: Boolean,
    restrictItemTypesPolicyOrgIds: List<String>,
) {
    this.update {
        if (vaultData != null) {
            it.copy(
                viewState = vaultData.toViewState(
                    baseIconUrl = baseIconUrl,
                    isPremium = isPremium,
                    hasMasterPassword = hasMasterPassword,
                    vaultFilterType = vaultFilterType,
                    isIconLoadingDisabled = isIconLoadingDisabled,
                    restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
                ),
                dialog = VaultState.DialogState.Error(
                    title = errorTitle,
                    message = errorMessage,
                ),
                isRefreshing = isRefreshing,
            )
        } else {
            it.copy(
                viewState = VaultState.ViewState.Error(
                    message = errorMessage,
                ),
                dialog = null,
                isRefreshing = isRefreshing,
            )
        }
    }
}
