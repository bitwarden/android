package com.x8bit.bitwarden.ui.vault.feature.additem

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState.ItemType.Card.displayStringResId
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState.ItemType.Identity.displayStringResId
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState.ItemType.SecureNotes.displayStringResId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the vault add item screen
 *
 * This ViewModel processes UI actions, manages the state of the generator screen,
 * and provides data for the UI to render. It extends a `BaseViewModel` and works
 * with a `SavedStateHandle` for state restoration.
 *
 * @property savedStateHandle Handles the saved state of this ViewModel.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class VaultAddItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultAddItemState, VaultAddItemEvent, VaultAddItemAction>(
    initialState = savedStateHandle[KEY_STATE] ?: INITIAL_STATE,
) {

    //region Initialization and Overrides

    init {
        stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultAddItemAction) {
        when (action) {
            is VaultAddItemAction.SaveClick -> {
                handleSaveClick()
            }

            is VaultAddItemAction.CloseClick -> {
                handleCloseClick()
            }

            is VaultAddItemAction.TypeOptionSelect -> {
                handleTypeOptionSelect(action)
            }

            is VaultAddItemAction.ItemType.LoginType -> {
                handleAddLoginTypeAction(action)
            }
        }
    }

    //endregion Initialization and Overrides

    //region Top Level Handlers

    private fun handleSaveClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Save Item",
                ),
            )
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = VaultAddItemEvent.NavigateBack,
        )
    }

    //endregion Top Level Handlers

    //region Type Option Handlers

    private fun handleTypeOptionSelect(action: VaultAddItemAction.TypeOptionSelect) {
        when (action.typeOption) {
            VaultAddItemState.ItemTypeOption.LOGIN -> handleSwitchToAddLoginItem()
            VaultAddItemState.ItemTypeOption.CARD -> handleSwitchToAddCardItem()
            VaultAddItemState.ItemTypeOption.IDENTITY -> handleSwitchToAddIdentityItem()
            VaultAddItemState.ItemTypeOption.SECURE_NOTES -> handleSwitchToAddSecureNotesItem()
        }
    }

    private fun handleSwitchToAddLoginItem() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = VaultAddItemState.ItemType.Login(),
            )
        }
    }

    //endregion Type Option Handlers

    //region Add Login Item Type Handlers

    private fun handleAddLoginTypeAction(
        action: VaultAddItemAction.ItemType.LoginType,
    ) {
        when (action) {
            is VaultAddItemAction.ItemType.LoginType.NameTextChange -> {
                handleNameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.UsernameTextChange -> {
                handleUsernameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.PasswordTextChange -> {
                handlePasswordTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.UriTextChange -> {
                handleURITextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.FolderChange -> {
                handleFolderTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.ToggleFavorite -> {
                handleToggleFavorite(action)
            }

            is VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt -> {
                handleToggleMasterPasswordReprompt(action)
            }

            is VaultAddItemAction.ItemType.LoginType.NotesTextChange -> {
                handleNotesTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.OwnershipChange -> {
                handleOwnershipTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick -> {
                handleOpenUsernameGeneratorClick()
            }

            is VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick -> {
                handlePasswordCheckerClick()
            }

            is VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick -> {
                handleOpenPasswordGeneratorClick()
            }

            is VaultAddItemAction.ItemType.LoginType.SetupTotpClick -> {
                handleSetupTotpClick()
            }

            is VaultAddItemAction.ItemType.LoginType.UriSettingsClick -> {
                handleUriSettingsClick()
            }

            is VaultAddItemAction.ItemType.LoginType.AddNewUriClick -> {
                handleAddNewUriClick()
            }

            is VaultAddItemAction.ItemType.LoginType.TooltipClick -> {
                handleTooltipClick()
            }

            is VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick -> {
                handleAddNewCustomFieldClick()
            }
        }
    }

    private fun handleSwitchToAddCardItem() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = VaultAddItemState.ItemType.Card,
            )
        }
    }

    private fun handleSwitchToAddIdentityItem() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = VaultAddItemState.ItemType.Identity,
            )
        }
    }

    private fun handleSwitchToAddSecureNotesItem() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                selectedType = VaultAddItemState.ItemType.SecureNotes,
            )
        }
    }

    private fun handleNameTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.NameTextChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(name = action.name)
        }
    }

    private fun handleUsernameTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.UsernameTextChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(username = action.username)
        }
    }

    private fun handlePasswordTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.PasswordTextChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(password = action.password)
        }
    }

    private fun handleURITextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.UriTextChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(uri = action.uri)
        }
    }

    private fun handleFolderTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.FolderChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(folder = action.folder)
        }
    }

    private fun handleToggleFavorite(
        action: VaultAddItemAction.ItemType.LoginType.ToggleFavorite,
    ) {
        updateLoginType { loginType ->
            loginType.copy(favorite = action.isFavorite)
        }
    }

    private fun handleToggleMasterPasswordReprompt(
        action: VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt,
    ) {
        updateLoginType { loginType ->
            loginType.copy(masterPasswordReprompt = action.isMasterPasswordReprompt)
        }
    }

    private fun handleNotesTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.NotesTextChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(notes = action.notes)
        }
    }

    private fun handleOwnershipTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.OwnershipChange,
    ) {
        updateLoginType { loginType ->
            loginType.copy(ownership = action.ownership)
        }
    }

    private fun handleOpenUsernameGeneratorClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Open Username Generator",
                ),
            )
        }
    }

    private fun handlePasswordCheckerClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Password Checker",
                ),
            )
        }
    }

    private fun handleOpenPasswordGeneratorClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Open Password Generator",
                ),
            )
        }
    }

    private fun handleSetupTotpClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Setup TOTP",
                ),
            )
        }
    }

    private fun handleUriSettingsClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "URI Settings",
                ),
            )
        }
    }

    private fun handleAddNewUriClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Add New URI",
                ),
            )
        }
    }

    private fun handleTooltipClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Tooltip",
                ),
            )
        }
    }

    private fun handleAddNewCustomFieldClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Add New Custom Field",
                ),
            )
        }
    }

    //endregion Add Login Item Type Handlers

    //region Utility Functions

    private inline fun updateLoginType(
        crossinline block: (VaultAddItemState.ItemType.Login) -> VaultAddItemState.ItemType.Login,
    ) {
        mutableStateFlow.update { currentState ->
            val currentSelectedType = currentState.selectedType
            if (currentSelectedType !is VaultAddItemState.ItemType.Login) return@update currentState

            val updatedLogin = block(currentSelectedType)

            currentState.copy(selectedType = updatedLogin)
        }
    }

    //endregion Utility Functions

    companion object {
        val INITIAL_STATE: VaultAddItemState = VaultAddItemState(
            selectedType = VaultAddItemState.ItemType.Login(),
        )
    }
}

/**
 * Represents the state for adding an item to the vault.
 *
 * @property selectedType The type of the item (e.g., Card, Identity, SecureNotes)
 * that has been selected to be added to the vault.
 */
@Parcelize
data class VaultAddItemState(
    val selectedType: ItemType,
) : Parcelable {

    /**
     * Provides a list of available item types for the vault.
     */
    val typeOptions: List<ItemTypeOption>
        get() = ItemTypeOption.values().toList()

    /**
     * Enum representing the main type options for the vault, such as LOGIN, CARD, etc.
     *
     * @property labelRes The resource ID of the string that represents the label of each type.
     */
    enum class ItemTypeOption(val labelRes: Int) {
        LOGIN(R.string.type_login),
        CARD(R.string.type_card),
        IDENTITY(R.string.type_identity),
        SECURE_NOTES(R.string.type_secure_note),
    }

    /**
     * A sealed class representing the item types that can be selected in the vault,
     * encapsulating the different configurations and properties each item type has.
     */
    @Parcelize
    sealed class ItemType : Parcelable {

        /**
         * Represents the resource ID for the display string. This is an abstract property
         * that must be overridden by each subclass to provide the appropriate string resource ID
         * for display purposes.
         */
        abstract val displayStringResId: Int

        /**
         * Represents the login item information.
         *
         * @property name The name associated with the login item.
         * @property username The username required for the login item.
         * @property password The password required for the login item.
         * @property uri The URI associated with the login item.
         * @property folder The folder used for the login item
         * @property favorite Indicates whether this login item is marked as a favorite.
         * @property masterPasswordReprompt Indicates if a master password reprompt is required.
         * @property notes Any additional notes or comments associated with the login item.
         * @property ownership The ownership email associated with the login item.
         */
        @Parcelize
        data class Login(
            val name: String = "",
            val username: String = "",
            val password: String = "",
            val uri: String = "",
            val folder: String = DEFAULT_FOLDER,
            val favorite: Boolean = false,
            val masterPasswordReprompt: Boolean = false,
            val notes: String = "",
            val ownership: String = DEFAULT_OWNERSHIP,
        ) : ItemType() {
            override val displayStringResId: Int
                get() = ItemTypeOption.LOGIN.labelRes

            /**
             * Retrieves a list of available folders.
             *
             * TODO(BIT-501): Update this property to pull available folders from the data layer.
             * Currently, it returns a hardcoded list of folders.
             */
            val availableFolders: List<String>
                get() = listOf("Folder 1", "Folder 2", "Folder 3")

            /**
             * Retrieves a list of available owners.
             *
             * TODO(BIT-501): Update this property to pull available owners from the data layer.
             * Currently, it returns a hardcoded list of email addresses.
             */
            val availableOwners: List<String>
                get() = listOf("a@b.com", "c@d.com")

            companion object {
                private const val DEFAULT_FOLDER: String = "No Folder"
                private const val DEFAULT_OWNERSHIP: String = "placeholder@email.com"
            }
        }

        /**
         * Represents the `Card` item type.
         *
         * @property displayStringResId Resource ID for the display string of the card type.
         */
        @Parcelize
        data object Card : ItemType() {
            override val displayStringResId: Int
                get() = ItemTypeOption.CARD.labelRes
        }

        /**
         * Represents the `Identity` item type.
         *
         * @property displayStringResId Resource ID for the display string of the identity type.
         */
        @Parcelize
        data object Identity : ItemType() {
            override val displayStringResId: Int
                get() = ItemTypeOption.IDENTITY.labelRes
        }

        /**
         * Represents the `SecureNotes` item type.
         *
         * @property displayStringResId Resource ID for the display string of the secure notes type.
         */
        @Parcelize
        data object SecureNotes : ItemType() {
            override val displayStringResId: Int
                get() = ItemTypeOption.SECURE_NOTES.labelRes
        }
    }
}

/**
 * Represents a set of events that can be emitted during the process of adding an item to the vault.
 * Each subclass of this sealed class denotes a distinct event that can occur.
 */
sealed class VaultAddItemEvent {
    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: String) : VaultAddItemEvent()

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : VaultAddItemEvent()
}

/**
 * Represents a set of actions related to the process of adding an item to the vault.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class VaultAddItemAction {

    /**
     * Represents the action when the save button is clicked.
     */
    data object SaveClick : VaultAddItemAction()

    /**
     * User clicked close.
     */
    data object CloseClick : VaultAddItemAction()

    /**
     * Represents the action when a type option is selected.
     *
     * @property typeOption The selected type option.
     */
    data class TypeOptionSelect(
        val typeOption: VaultAddItemState.ItemTypeOption,
    ) : VaultAddItemAction()

    /**
     * Represents actions specific to the item types.
     */
    sealed class ItemType : VaultAddItemAction() {

        /**
         * Represents actions specific to the Login type.
         */
        sealed class LoginType : ItemType() {
            /**
             * Fired when the name text input is changed.
             *
             * @property name The new name text.
             */
            data class NameTextChange(val name: String) : LoginType()

            /**
             * Fired when the username text input is changed.
             *
             * @property username The new username text.
             */
            data class UsernameTextChange(val username: String) : LoginType()

            /**
             * Fired when the password text input is changed.
             *
             * @property password The new password text.
             */
            data class PasswordTextChange(val password: String) : LoginType()

            /**
             * Fired when the URI text input is changed.
             *
             * @property uri The new URI text.
             */
            data class UriTextChange(val uri: String) : LoginType()

            /**
             * Fired when the folder text input is changed.
             *
             * @property folder The new folder text.
             */
            data class FolderChange(val folder: String) : LoginType()

            /**
             * Fired when the Favorite toggle is changed.
             *
             * @property isFavorite The new state of the Favorite toggle.
             */
            data class ToggleFavorite(val isFavorite: Boolean) : LoginType()

            /**
             * Fired when the Master Password Reprompt toggle is changed.
             *
             * @property isMasterPasswordReprompt The new state of the Master
             * Password Re-prompt toggle.
             */
            data class ToggleMasterPasswordReprompt(
                val isMasterPasswordReprompt: Boolean,
            ) : LoginType()

            /**
             * Fired when the notes text input is changed.
             *
             * @property notes The new notes text.
             */
            data class NotesTextChange(val notes: String) : LoginType()

            /**
             * Fired when the ownership text input is changed.
             *
             * @property ownership The new ownership text.
             */
            data class OwnershipChange(val ownership: String) : LoginType()

            /**
             * Represents the action to open the username generator.
             */
            data object OpenUsernameGeneratorClick : LoginType()

            /**
             * Represents the action to check the password's strength or integrity.
             */
            data object PasswordCheckerClick : LoginType()

            /**
             * Represents the action to open the password generator.
             */
            data object OpenPasswordGeneratorClick : LoginType()

            /**
             * Represents the action to set up TOTP.
             */
            data object SetupTotpClick : LoginType()

            /**
             * Represents the action of clicking TOTP settings
             */
            data object UriSettingsClick : LoginType()

            /**
             * Represents the action to add a new URI field.
             */
            data object AddNewUriClick : LoginType()

            /**
             * Represents the action to open tooltip
             */
            data object TooltipClick : LoginType()

            /**
             * Represents the action to add a new custom field.
             */
            data object AddNewCustomFieldClick : LoginType()
        }
    }
}
