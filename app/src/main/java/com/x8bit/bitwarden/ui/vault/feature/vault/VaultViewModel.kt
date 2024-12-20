package com.x8bit.bitwarden.ui.vault.feature.vault

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.BackgroundEvent
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.getOrganizationPremiumStatusMap
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAppBarTitle
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toVaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.vault.util.vaultFilterDataIfRequired
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import com.x8bit.bitwarden.ui.vault.util.shortName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

/**
 * Manages [VaultState], handles [VaultAction], and launches [VaultEvent] for the [VaultScreen].
 */
@Suppress("TooManyFunctions", "LongParameterList")
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
    private val snackbarRelayManager: SnackbarRelayManager,
    private val reviewPromptManager: ReviewPromptManager,
    private val featureFlagManager: FeatureFlagManager,
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
        val appBarTitle = vaultFilterData.toAppBarTitle()
        val showSshKeys = featureFlagManager.getFeatureFlag(FlagKey.SshKeyCipherItems)
        VaultState(
            appBarTitle = appBarTitle,
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
            showSshKeys = showSshKeys,
            organizationPremiumStatusMap = userState
                .activeAccount
                .getOrganizationPremiumStatusMap(),
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

        combine(
            vaultRepository.vaultDataStateFlow,
            featureFlagManager.getFeatureFlagFlow(FlagKey.SshKeyCipherItems),
        ) { vaultData, sshKeyCipherItemsEnabled ->
            VaultAction.Internal.VaultDataReceive(
                vaultData = vaultData,
                showSshKeys = sshKeyCipherItemsEnabled,
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .combine(
                featureFlagManager.getFeatureFlagFlow(FlagKey.ImportLoginsFlow),
            ) { userState, importLoginsEnabled ->
                VaultAction.Internal.UserStateUpdateReceive(
                    userState = userState,
                    importLoginsFlowEnabled = importLoginsEnabled,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.MY_VAULT_RELAY)
            .map {
                VaultAction.Internal.SnackbarDataReceive(it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultAction) {
        when (action) {
            is VaultAction.AddItemClick -> handleAddItemClick()
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
            is VaultAction.ExitConfirmationClick -> handleExitConfirmationClick()
            is VaultAction.VaultFilterTypeSelect -> handleVaultFilterTypeSelect(action)
            is VaultAction.SecureNoteGroupClick -> handleSecureNoteClick()
            is VaultAction.SshKeyGroupClick -> handleSshKeyClick()
            is VaultAction.TrashClick -> handleTrashClick()
            is VaultAction.VaultItemClick -> handleVaultItemClick(action)
            is VaultAction.TryAgainClick -> handleTryAgainClick()
            is VaultAction.DialogDismiss -> handleDialogDismiss()
            is VaultAction.RefreshPull -> handleRefreshPull()
            is VaultAction.OverflowOptionClick -> handleOverflowOptionClick(action)

            is VaultAction.MasterPasswordRepromptSubmit -> {
                handleMasterPasswordRepromptSubmit(action)
            }

            is VaultAction.Internal -> handleInternalAction(action)
            VaultAction.DismissImportActionCard -> handleDismissImportActionCard()
            VaultAction.ImportActionCardClick -> handleImportActionCardClick()
            VaultAction.LifecycleResumed -> handleLifecycleResumed()
        }
    }

    private fun handleLifecycleResumed() {
        val shouldShowPrompt = reviewPromptManager.shouldPromptForAppReview() &&
            featureFlagManager.getFeatureFlag(FlagKey.AppReviewPrompt)
        if (shouldShowPrompt) {
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
            showSshKeys = state.showSshKeys,
        )
    }

    //region VaultAction Handlers
    private fun handleAddItemClick() {
        sendEvent(VaultEvent.NavigateToAddItemScreen)
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
        vaultRepository.lockVault(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClick(action: VaultAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
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
        if (isSwitchingAccounts) {
            snackbarRelayManager.clearRelayBuffer(SnackbarRelay.MY_VAULT_RELAY)
        }
        mutableStateFlow.update {
            it.copy(isSwitchingAccounts = isSwitchingAccounts)
        }
    }

    private fun handleAddAccountClick() {
        authRepository.hasPendingAccountAddition = true
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.Syncing)
        }
        vaultRepository.sync(forced = true)
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleExitConfirmationClick() {
        sendEvent(VaultEvent.NavigateOutOfApp)
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
            showSshKeys = state.showSshKeys,
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
        sendEvent(VaultEvent.NavigateToVaultItem(action.vaultItem.id))
    }

    private fun handleTryAgainClick() {
        vaultRepository.sync(forced = true)
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepository.sync(forced = false)
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

    private fun handleMasterPasswordRepromptSubmit(
        action: VaultAction.MasterPasswordRepromptSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.password)
            sendAction(
                VaultAction.Internal.ValidatePasswordResultReceive(
                    overflowAction = action.overflowAction,
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
            sendAction(VaultAction.Internal.GenerateTotpResultReceive(result))
        }
    }

    private fun handleCopyUsernameClick(
        action: ListingItemOverflowAction.VaultAction.CopyUsernameClick,
    ) {
        clipboardManager.setText(action.username)
    }

    private fun handleEditClick(action: ListingItemOverflowAction.VaultAction.EditClick) {
        sendEvent(VaultEvent.NavigateToEditVaultItem(action.cipherId))
    }

    private fun handleLaunchClick(action: ListingItemOverflowAction.VaultAction.LaunchClick) {
        sendEvent(VaultEvent.NavigateToUrl(action.url))
    }

    private fun handleViewClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(VaultEvent.NavigateToVaultItem(action.cipherId))
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

            is VaultAction.Internal.ValidatePasswordResultReceive -> {
                handleValidatePasswordResultReceive(action)
            }

            is VaultAction.Internal.SnackbarDataReceive -> handleSnackbarDataReceive(action)
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
                clipboardManager.setText(result.code)
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
        val shouldShowImportActionCard = action.importLoginsFlowEnabled &&
            firstTimeState.showImportLoginsCard

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
                showImportActionCard = shouldShowImportActionCard,
            )
        }
    }

    private fun handleVaultDataReceive(action: VaultAction.Internal.VaultDataReceive) {
        // Avoid updating the UI if we are actively switching users to avoid changes while
        // navigating.
        if (state.isSwitchingAccounts) return

        updateViewState(
            vaultData = action.vaultData,
            showSshKeys = action.showSshKeys,
        )
    }

    private fun updateViewState(vaultData: DataState<VaultData>, showSshKeys: Boolean) {
        when (vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(
                vaultData = vaultData,
                showSshKeys = showSshKeys,
            )

            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(
                vaultData = vaultData,
                showSshKeys = showSshKeys,
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
            errorTitle = R.string.an_error_has_occurred.asText(),
            errorMessage = R.string.generic_error_message.asText(),
            isRefreshing = false,
        )
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>, showSshKeys: Boolean) {
        if (state.dialog == VaultState.DialogState.Syncing) {
            sendEvent(
                VaultEvent.ShowToast(
                    message = R.string.syncing_complete.asText(),
                ),
            )
        }
        updateVaultState(vaultData.data, showSshKeys)
    }

    private fun updateVaultState(
        vaultData: VaultData,
        showSshKeys: Boolean,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.toViewState(
                    baseIconUrl = state.baseIconUrl,
                    isIconLoadingDisabled = state.isIconLoadingDisabled,
                    isPremium = state.isPremium,
                    hasMasterPassword = state.hasMasterPassword,
                    vaultFilterType = vaultFilterTypeOrDefault,
                    showSshKeys = showSshKeys,
                    organizationPremiumStatusMap = state.organizationPremiumStatusMap,
                ),
                dialog = null,
                isRefreshing = false,
                showSshKeys = showSshKeys,
            )
        }
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(
        vaultData: DataState.NoNetwork<VaultData>,
        showSshKeys: Boolean,
    ) {
        val data = vaultData.data ?: VaultData(
            cipherViewList = emptyList(),
            collectionViewList = emptyList(),
            folderViewList = emptyList(),
            sendViewList = emptyList(),
        )
        updateVaultState(
            vaultData = data,
            showSshKeys = showSshKeys,
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
                    showSshKeys = state.showSshKeys,
                    organizationPremiumStatusMap = state.organizationPremiumStatusMap,
                ),
            )
        }
    }

    private fun handleValidatePasswordResultReceive(
        action: VaultAction.Internal.ValidatePasswordResultReceive,
    ) {
        when (val result = action.result) {
            ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                if (!result.isValid) {
                    mutableStateFlow.update {
                        it.copy(
                            dialog = VaultState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.invalid_master_password.asText(),
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

    //endregion VaultAction Handlers
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
    // Internal-use properties
    val isSwitchingAccounts: Boolean = false,
    val isPremium: Boolean,
    val hasMasterPassword: Boolean,
    private val isPullToRefreshSettingEnabled: Boolean,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    val isRefreshing: Boolean,
    val showImportActionCard: Boolean,
    val showSshKeys: Boolean,
    val organizationPremiumStatusMap: Map<String, Boolean>,
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
            abstract val extraIconList: List<IconRes>

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
             * Represents a login item within the vault.
             *
             * @property username The username associated with this login item.
             */
            @Parcelize
            data class Login(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(R.drawable.ic_globe),
                override val startIconTestTag: String = "LoginCipherIcon",
                override val extraIconList: List<IconRes> = emptyList(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                val username: Text?,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = username
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
                override val startIcon: IconData = IconData.Local(R.drawable.ic_payment_card),
                override val startIconTestTag: String = "CardCipherIcon",
                override val extraIconList: List<IconRes> = emptyList(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                private val brand: VaultCardBrand? = null,
                val lastFourDigits: Text? = null,
            ) : VaultItem() {
                override val supportingLabel: Text?
                    get() = when {
                        brand != null && lastFourDigits != null -> brand.shortName
                            .concat(", *".asText(), lastFourDigits)

                        brand != null -> brand.shortName
                        lastFourDigits != null -> "*".asText().concat(lastFourDigits)
                        else -> null
                    }
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
                override val startIcon: IconData = IconData.Local(R.drawable.ic_id_card),
                override val startIconTestTag: String = "IdentityCipherIcon",
                override val extraIconList: List<IconRes> = emptyList(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                val fullName: Text?,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = fullName
            }

            /**
             * Represents a secure note item within the vault, designed to store secure,
             * non-categorized textual information.
             */
            @Parcelize
            data class SecureNote(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(R.drawable.ic_note),
                override val startIconTestTag: String = "SecureNoteCipherIcon",
                override val extraIconList: List<IconRes> = emptyList(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = null
            }

            /**
             * Represents a SSH key item within the vault, designed to store SSH keys.
             *
             * @property publicKey The public key associated with this SSH key item.
             * @property privateKey The private key associated with this SSH key item.
             * @property fingerprint The fingerprint associated with this SSH key item.
             */
            @Parcelize
            data class SshKey(
                override val id: String,
                override val name: Text,
                override val startIcon: IconData = IconData.Local(R.drawable.ic_ssh_key),
                override val startIconTestTag: String = "SshKeyCipherIcon",
                override val extraIconList: List<IconRes> = emptyList(),
                override val overflowOptions: List<ListingItemOverflowAction.VaultAction>,
                override val shouldShowMasterPasswordReprompt: Boolean,
                val publicKey: Text,
                val privateKey: Text,
                val fingerprint: Text,
            ) : VaultItem() {
                override val supportingLabel: Text? get() = null
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
         * Represents an error dialog with the given [title] and [message].
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
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
    data object NavigateToAddItemScreen : VaultEvent()

    /**
     * Navigate to the item details screen.
     */
    data class NavigateToVaultItem(
        val itemId: String,
    ) : VaultEvent()

    /**
     * Navigate to the item edit screen.
     */
    data class NavigateToEditVaultItem(
        val itemId: String,
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
     * Navigate out of the app.
     */
    data object NavigateOutOfApp : VaultEvent()

    /**
     * Navigate to the import logins screen.
     */
    data object NavigateToImportLogins : VaultEvent()

    /**
     * Indicates that we should prompt the user for app review.
     */
    data object PromptForAppReview : VaultEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : VaultEvent()

    /**
     * Show a snackbar with the given [data].
     */
    data class ShowSnackbar(val data: BitwardenSnackbarData) : VaultEvent(), BackgroundEvent
}

/**
 * Models actions for the [VaultScreen].
 */
sealed class VaultAction {
    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : VaultAction()

    /**
     * Click the add an item button.
     * This can either be the floating action button or actual add an item button.
     */
    data object AddItemClick : VaultAction()

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
     * User confirmed that they want to exit the app after clicking the Sync option in the overflow
     * menu.
     */
    data object ExitConfirmationClick : VaultAction()

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
    data class MasterPasswordRepromptSubmit(
        val overflowAction: ListingItemOverflowAction.VaultAction,
        val password: String,
    ) : VaultAction()

    /**
     * The lifecycle of the VaultScreen has entered a resumed state.
     */
    data object LifecycleResumed : VaultAction()

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
            val importLoginsFlowEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates a vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
            val showSshKeys: Boolean,
        ) : Internal()

        /**
         * Indicates that a result for verifying the user's master password has been received.
         */
        data class ValidatePasswordResultReceive(
            val overflowAction: ListingItemOverflowAction.VaultAction,
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a snackbar data was received.
         */
        data class SnackbarDataReceive(
            val data: BitwardenSnackbarData,
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
                    showSshKeys = it.showSshKeys,
                    organizationPremiumStatusMap = it.organizationPremiumStatusMap,
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
