package com.x8bit.bitwarden.ui.vault.feature.vault

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toActiveAccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages [VaultState], handles [VaultAction], and launches [VaultEvent] for the [VaultScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    vaultRepository: VaultRepository,
) : BaseViewModel<VaultState, VaultEvent, VaultAction>(
    initialState = run {
        val userState = requireNotNull(authRepository.userStateFlow.value)
        val accountSummaries = userState.toAccountSummaries()
        val activeAccountSummary = userState.toActiveAccountSummary()
        VaultState(
            initials = activeAccountSummary.initials,
            avatarColorString = activeAccountSummary.avatarColorHex,
            accountSummaries = accountSummaries,
            viewState = VaultState.ViewState.Loading,
        )
    },
) {

    init {
        vaultRepository
            .vaultDataStateFlow
            .onEach { sendAction(VaultAction.Internal.VaultDataReceive(vaultData = it)) }
            .launchIn(viewModelScope)
        // TODO remove this block once vault unlocked is implemented in BIT-1082
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(5000)
            if (vaultRepository.vaultDataStateFlow.value == DataState.Loading) {
                sendAction(
                    VaultAction.Internal.VaultDataReceive(
                        DataState.Error(error = IllegalStateException()),
                    ),
                )
            }
        }

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
            is VaultAction.IdentityGroupClick -> handleIdentityClick()
            is VaultAction.LoginGroupClick -> handleLoginClick()
            is VaultAction.SearchIconClick -> handleSearchIconClick()
            is VaultAction.AccountSwitchClick -> handleAccountSwitchClick(action)
            is VaultAction.AddAccountClick -> handleAddAccountClick()
            is VaultAction.SecureNoteGroupClick -> handleSecureNoteClick()
            is VaultAction.TrashClick -> handleTrashClick()
            is VaultAction.VaultItemClick -> handleVaultItemClick(action)
            is VaultAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
            is VaultAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
        }
    }

    //region VaultAction Handlers
    private fun handleAddItemClick() {
        sendEvent(VaultEvent.NavigateToAddItemScreen)
    }

    private fun handleCardClick() {
        sendEvent(VaultEvent.NavigateToCardGroup)
    }

    private fun handleFolderItemClick(action: VaultAction.FolderClick) {
        sendEvent(VaultEvent.NavigateToFolder(action.folderItem.id))
    }

    private fun handleIdentityClick() {
        sendEvent(VaultEvent.NavigateToIdentityGroup)
    }

    private fun handleLoginClick() {
        sendEvent(VaultEvent.NavigateToLoginGroup)
    }

    private fun handleSearchIconClick() {
        sendEvent(VaultEvent.NavigateToVaultSearchScreen)
    }

    private fun handleAccountSwitchClick(action: VaultAction.AccountSwitchClick) {
        when (action.accountSummary.status) {
            AccountSummary.Status.ACTIVE -> {
                // Nothing to do for the active account
            }

            AccountSummary.Status.LOCKED -> {
                // TODO: Handle switching accounts (BIT-853)
                sendEvent(VaultEvent.NavigateToVaultUnlockScreen)
            }

            AccountSummary.Status.UNLOCKED -> {
                // TODO: Handle switching accounts (BIT-853)
                sendEvent(VaultEvent.ShowToast(message = "Not yet implemented."))
            }
        }
    }

    private fun handleAddAccountClick() {
        authRepository.specialCircumstance = UserState.SpecialCircumstance.PendingAccountAddition
    }

    private fun handleTrashClick() {
        sendEvent(VaultEvent.NavigateToTrash)
    }

    private fun handleSecureNoteClick() {
        sendEvent(VaultEvent.NavigateToSecureNotesGroup)
    }

    private fun handleVaultItemClick(action: VaultAction.VaultItemClick) {
        sendEvent(VaultEvent.NavigateToVaultItem(action.vaultItem.id))
    }

    private fun handleUserStateUpdateReceive(action: VaultAction.Internal.UserStateUpdateReceive) {
        // Leave the current data alone if there is no UserState; we are in the process of logging
        // out.
        val userState = action.userState ?: return

        mutableStateFlow.update {
            val accountSummaries = userState.toAccountSummaries()
            val activeAccountSummary = userState.toActiveAccountSummary()
            it.copy(
                initials = activeAccountSummary.initials,
                avatarColorString = activeAccountSummary.avatarColorHex,
                accountSummaries = accountSummaries,
            )
        }
    }

    private fun handleVaultDataReceive(action: VaultAction.Internal.VaultDataReceive) {
        when (val vaultData = action.vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        // TODO update state to error state BIT-1157
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        sendEvent(VaultEvent.ShowToast(message = "Vault error state not yet implemented"))
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        mutableStateFlow.update { it.copy(viewState = vaultData.data.toViewState()) }
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        // TODO update state to no network state BIT-1158
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        sendEvent(VaultEvent.ShowToast(message = "Vault no network state not yet implemented"))
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        // TODO update state to refresh state BIT-505
        mutableStateFlow.update { it.copy(viewState = vaultData.data.toViewState()) }
        sendEvent(VaultEvent.ShowToast(message = "Refreshing"))
    }
    //endregion VaultAction Handlers
}

/**
 * Represents the overall state for the [VaultScreen].
 *
 * @property avatarColorString The color of the avatar in HEX format.
 * @property initials The initials to be displayed on the avatar.
 * @property viewState The specific view state representing loading, no items, or content state.
 */
@Parcelize
data class VaultState(
    private val avatarColorString: String,
    val initials: String,
    val accountSummaries: List<AccountSummary>,
    val viewState: ViewState,
) : Parcelable {

    /**
     * The [Color] of the avatar.
     */
    val avatarColor: Color get() = avatarColorString.hexToColor()

    /**
     * Represents the specific view states for the [VaultScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Loading state for the [VaultScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a state where the [VaultScreen] has no items to display.
         */
        @Parcelize
        data object NoItems : ViewState()

        /**
         * Content state for the [VaultScreen] showing the actual content or items.
         *
         * @property loginItemsCount The count of Login type items.
         * @property cardItemsCount The count of Card type items.
         * @property identityItemsCount The count of Identity type items.
         * @property secureNoteItemsCount The count of Secure Notes type items.
         * @property favoriteItems The list of favorites to be displayed.
         * @property folderItems The list of folders to be displayed.
         * @property noFolderItems The list of non-folders to be displayed.
         * @property trashItemsCount The number of items present in the trash.
         */
        @Parcelize
        data class Content(
            val loginItemsCount: Int,
            val cardItemsCount: Int,
            val identityItemsCount: Int,
            val secureNoteItemsCount: Int,
            val favoriteItems: List<VaultItem>,
            val folderItems: List<FolderItem>,
            val noFolderItems: List<VaultItem>,
            val trashItemsCount: Int,
        ) : ViewState()

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
     * Navigate to the card group screen.
     */
    data object NavigateToCardGroup : VaultEvent()

    /**
     * Navigate to the folder screen.
     */
    data class NavigateToFolder(
        val folderId: String?,
    ) : VaultEvent()

    /**
     * Navigate to the identity group screen.
     */
    data object NavigateToIdentityGroup : VaultEvent()

    /**
     * Navigate to the login group screen.
     */
    data object NavigateToLoginGroup : VaultEvent()

    /**
     * Navigate to the trash screen.
     */
    data object NavigateToTrash : VaultEvent()

    /**
     * Navigate to the secure notes group screen.
     */
    data object NavigateToSecureNotesGroup : VaultEvent()

    /**
     * Navigate to the vault unlock screen.
     */
    data object NavigateToVaultUnlockScreen : VaultEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: String) : VaultEvent()
}

/**
 * Models actions for the [VaultScreen].
 */
sealed class VaultAction {
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
     * User clicked an account in the account switcher.
     */
    data class AccountSwitchClick(
        val accountSummary: AccountSummary,
    ) : VaultAction()

    /**
     * User clicked on Add Account in the account switcher.
     */
    data object AddAccountClick : VaultAction()

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
     * Models actions that the [VaultViewModel] itself might send.
     */
    sealed class Internal : VaultAction() {

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
