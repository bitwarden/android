package com.x8bit.bitwarden.ui.vault.feature.vault

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAppBarTitle
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toVaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages [VaultState], handles [VaultAction], and launches [VaultEvent] for the [VaultScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<VaultState, VaultEvent, VaultAction>(
    initialState = run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val accountSummaries = userState.toAccountSummaries()
        val activeAccountSummary = userState.toActiveAccountSummary()
        val vaultFilterData = userState.activeAccount.toVaultFilterData()
        val appBarTitle = vaultFilterData.toAppBarTitle()
        VaultState(
            appBarTitle = appBarTitle,
            initials = activeAccountSummary.initials,
            avatarColorString = activeAccountSummary.avatarColorHex,
            accountSummaries = accountSummaries,
            vaultFilterData = vaultFilterData,
            viewState = VaultState.ViewState.Loading,
            isPremium = userState.activeAccount.isPremium,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
        )
    },
) {
    /**
     * Helper for retrieving the selected vault filter type from the state (or a default).
     */
    private val vaultFilterTypeOrDefault: VaultFilterType
        get() = state.vaultFilterData?.selectedVaultFilterType ?: VaultFilterType.AllVaults

    init {
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { VaultAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepository
            .vaultDataStateFlow
            .onEach { sendAction(VaultAction.Internal.VaultDataReceive(vaultData = it)) }
            .launchIn(viewModelScope)

        authRepository
            .userStateFlow
            .onEach {
                sendAction(VaultAction.Internal.UserStateUpdateReceive(userState = it))
            }
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
            is VaultAction.TrashClick -> handleTrashClick()
            is VaultAction.VaultItemClick -> handleVaultItemClick(action)
            is VaultAction.TryAgainClick -> handleTryAgainClick()
            is VaultAction.DialogDismiss -> handleDialogDismiss()
            is VaultAction.RefreshPull -> handleRefreshPull()
            is VaultAction.Internal -> handleInternalAction(action)
        }
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
        mutableStateFlow.update {
            it.copy(isSwitchingAccounts = isSwitchingAccounts)
        }
    }

    private fun handleAddAccountClick() {
        authRepository.specialCircumstance = UserState.SpecialCircumstance.PendingAccountAddition
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.Syncing)
        }
        vaultRepository.sync()
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleExitConfirmationClick() {
        sendEvent(VaultEvent.NavigateOutOfApp)
    }

    private fun handleVaultFilterTypeSelect(action: VaultAction.VaultFilterTypeSelect) {
        // Update the current filter
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = it.vaultFilterData?.copy(
                    selectedVaultFilterType = action.vaultFilterType,
                ),
            )
        }

        // Re-process the current vault data with the new filter
        updateViewState(vaultData = vaultRepository.vaultDataStateFlow.value)
    }

    private fun handleTrashClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.Trash))
    }

    private fun handleSecureNoteClick() {
        sendEvent(VaultEvent.NavigateToItemListing(VaultItemListingType.SecureNote))
    }

    private fun handleVaultItemClick(action: VaultAction.VaultItemClick) {
        sendEvent(VaultEvent.NavigateToVaultItem(action.vaultItem.id))
    }

    private fun handleTryAgainClick() {
        vaultRepository.sync()
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleRefreshPull() {
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepository.sync()
    }

    private fun handleInternalAction(action: VaultAction.Internal) {
        when (action) {
            is VaultAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is VaultAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
            is VaultAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
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

        // Avoid updating the UI if we are actively switching users to avoid changes while
        // navigating.
        if (state.isSwitchingAccounts) return

        val vaultFilterData = userState.activeAccount.toVaultFilterData()
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
            )
        }
    }

    private fun handleVaultDataReceive(action: VaultAction.Internal.VaultDataReceive) {
        // Avoid updating the UI if we are actively switching users to avoid changes while
        // navigating.
        if (state.isSwitchingAccounts) return

        updateViewState(vaultData = action.vaultData)
    }

    private fun updateViewState(vaultData: DataState<VaultData>) {
        when (vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        mutableStateFlow.updateToErrorStateOrDialog(
            vaultData = vaultData.data,
            vaultFilterType = vaultFilterTypeOrDefault,
            isPremium = state.isPremium,
            errorTitle = R.string.an_error_has_occurred.asText(),
            errorMessage = R.string.generic_error_message.asText(),
        )
        sendEvent(VaultEvent.DismissPullToRefresh)
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        if (state.dialog == VaultState.DialogState.Syncing) {
            sendEvent(
                VaultEvent.ShowToast(
                    message = R.string.syncing_complete.asText(),
                ),
            )
        }
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.data.toViewState(
                    isPremium = state.isPremium,
                    vaultFilterType = vaultFilterTypeOrDefault,
                ),
                dialog = null,
            )
        }
        sendEvent(VaultEvent.DismissPullToRefresh)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        mutableStateFlow.updateToErrorStateOrDialog(
            vaultData = vaultData.data,
            vaultFilterType = vaultFilterTypeOrDefault,
            isPremium = state.isPremium,
            errorTitle = R.string.internet_connection_required_title.asText(),
            errorMessage = R.string.internet_connection_required_message.asText(),
        )
        sendEvent(VaultEvent.DismissPullToRefresh)
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        // TODO update state to refresh state BIT-505
        mutableStateFlow.update {
            it.copy(
                viewState = vaultData.data.toViewState(
                    isPremium = state.isPremium,
                    vaultFilterType = vaultFilterTypeOrDefault,
                ),
            )
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
    private val isPullToRefreshSettingEnabled: Boolean,
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
         * Determines whether or not the the Vault Filter may be shown (when applicable).
         */
        abstract val hasVaultFilter: Boolean

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
            override val hasVaultFilter: Boolean get() = false
            override val isPullToRefreshEnabled: Boolean get() = false
        }

        /**
         * Represents a state where the [VaultScreen] has no items to display.
         */
        @Parcelize
        data object NoItems : ViewState() {
            override val hasFab: Boolean get() = true
            override val hasVaultFilter: Boolean get() = true
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
            override val hasVaultFilter: Boolean get() = false
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
            val totpItemsCount: Int,
            val loginItemsCount: Int,
            val cardItemsCount: Int,
            val identityItemsCount: Int,
            val secureNoteItemsCount: Int,
            val favoriteItems: List<VaultItem>,
            val folderItems: List<FolderItem>,
            val noFolderItems: List<VaultItem>,
            val collectionItems: List<CollectionItem>,
            val trashItemsCount: Int,
        ) : ViewState() {
            override val hasFab: Boolean get() = true
            override val hasVaultFilter: Boolean get() = true
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

            @get:DrawableRes
            abstract val startIcon: Int

            /**
             * An optional supporting label for the vault item that provides additional information.
             * This property is open to be overridden by subclasses that can provide their own
             * supporting label relevant to the item's type.
             */
            abstract val supportingLabel: Text?

            /**
             * Represents a login item within the vault.
             *
             * @property username The username associated with this login item.
             */
            @Parcelize
            data class Login(
                override val id: String,
                override val name: Text,
                val username: Text?,
            ) : VaultItem() {
                override val startIcon: Int get() = R.drawable.ic_login_item
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
                val brand: Text? = null,
                val lastFourDigits: Text? = null,
            ) : VaultItem() {
                override val startIcon: Int get() = R.drawable.ic_card_item
                override val supportingLabel: Text?
                    get() = when {
                        brand != null && lastFourDigits != null -> brand
                            .concat(", *".asText(), lastFourDigits)

                        brand != null -> brand
                        lastFourDigits != null -> "*".asText().concat(lastFourDigits)
                        else -> null
                    }
            }

            /**
             * Represents an identity item within the vault, containing personal identification
             * information.
             *
             * @property firstName The first name of the individual associated with this
             * identity item.
             */
            @Parcelize
            data class Identity(
                override val id: String,
                override val name: Text,
                val firstName: Text?,
            ) : VaultItem() {
                override val startIcon: Int get() = R.drawable.ic_identity_item
                override val supportingLabel: Text? get() = firstName
            }

            /**
             * Represents a secure note item within the vault, designed to store secure,
             * non-categorized textual information.
             */
            @Parcelize
            data class SecureNote(
                override val id: String,
                override val name: Text,
            ) : VaultItem() {
                override val startIcon: Int get() = R.drawable.ic_secure_note_item
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

    companion object {
        /**
         * The maximum number of no folder items that can be displayed before the UI creates a
         * no folder "folder".
         */
        private const val NO_FOLDER_ITEM_THRESHOLD: Int = 100
    }
}

/**
 * Models effects for the [VaultScreen].
 */
sealed class VaultEvent {
    /**
     * Dismisses the pull-to-refresh indicator.
     */
    data object DismissPullToRefresh : VaultEvent()

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
     * Navigate to the verification code screen.
     */
    data object NavigateToVerificationCodeScreen : VaultEvent()

    /**
     * Navigate out of the app.
     */
    data object NavigateOutOfApp : VaultEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : VaultEvent()
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
     * Models actions that the [VaultViewModel] itself might send.
     */
    sealed class Internal : VaultAction() {

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
    }
}

private fun MutableStateFlow<VaultState>.updateToErrorStateOrDialog(
    vaultData: VaultData?,
    vaultFilterType: VaultFilterType,
    isPremium: Boolean,
    errorTitle: Text,
    errorMessage: Text,
) {
    this.update {
        if (vaultData != null) {
            it.copy(
                viewState = vaultData.toViewState(
                    isPremium = isPremium,
                    vaultFilterType = vaultFilterType,
                ),
                dialog = VaultState.DialogState.Error(
                    title = errorTitle,
                    message = errorMessage,
                ),
            )
        } else {
            it.copy(
                viewState = VaultState.ViewState.Error(
                    message = errorMessage,
                ),
                dialog = null,
            )
        }
    }
}
