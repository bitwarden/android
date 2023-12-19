package com.x8bit.bitwarden.ui.vault.feature.additem

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.takeUntilLoaded
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.additem.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.additem.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.additem.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toCipherView
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the vault add item screen
 *
 * This ViewModel processes UI actions, manages the state of the generator screen,
 * and provides data for the UI to render. It extends a `BaseViewModel` and works
 * with a `SavedStateHandle` for retrieving navigation arguments.
 *
 * @param savedStateHandle Handles the navigation arguments of this ViewModel.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class VaultAddItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<VaultAddItemState, VaultAddItemEvent, VaultAddItemAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val vaultAddEditType = VaultAddEditItemArgs(savedStateHandle).vaultAddEditType
            VaultAddItemState(
                vaultAddEditType = vaultAddEditType,
                viewState = when (vaultAddEditType) {
                    VaultAddEditType.AddItem -> VaultAddItemState.ViewState.Content(
                        common = VaultAddItemState.ViewState.Content.Common(),
                        type = VaultAddItemState.ViewState.Content.ItemType.Login(),
                    )

                    is VaultAddEditType.EditItem -> VaultAddItemState.ViewState.Loading
                },
                dialog = null,
            )
        },
) {

    //region Initialization and Overrides

    init {
        when (val vaultAddEditType = state.vaultAddEditType) {
            VaultAddEditType.AddItem -> Unit
            is VaultAddEditType.EditItem -> {
                vaultRepository
                    .getVaultItemStateFlow(vaultAddEditType.vaultItemId)
                    // We'll stop getting updates as soon as we get some loaded data.
                    .takeUntilLoaded()
                    .map { VaultAddItemAction.Internal.VaultDataReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun handleAction(action: VaultAddItemAction) {
        when (action) {
            is VaultAddItemAction.Common -> handleCommonActions(action)
            is VaultAddItemAction.ItemType.LoginType -> handleAddLoginTypeAction(action)
            is VaultAddItemAction.Internal -> handleInternalActions(action)
        }
    }

    //endregion Initialization and Overrides

    //region Common Handlers

    private fun handleCommonActions(action: VaultAddItemAction.Common) {
        when (action) {
            is VaultAddItemAction.Common.CustomFieldValueChange -> handleCustomFieldValueChange(
                action,
            )

            is VaultAddItemAction.Common.FolderChange -> handleFolderTextInputChange(action)
            is VaultAddItemAction.Common.NameTextChange -> handleNameTextInputChange(action)
            is VaultAddItemAction.Common.NotesTextChange -> handleNotesTextInputChange(action)
            is VaultAddItemAction.Common.OwnershipChange -> handleOwnershipTextInputChange(action)
            is VaultAddItemAction.Common.ToggleFavorite -> handleToggleFavorite(action)
            is VaultAddItemAction.Common.ToggleMasterPasswordReprompt -> {
                handleToggleMasterPasswordReprompt(action)
            }

            is VaultAddItemAction.Common.CloseClick -> handleCloseClick()
            is VaultAddItemAction.Common.DismissDialog -> handleDismissDialog()
            is VaultAddItemAction.Common.SaveClick -> handleSaveClick()
            is VaultAddItemAction.Common.TypeOptionSelect -> handleTypeOptionSelect(action)
            is VaultAddItemAction.Common.AddNewCustomFieldClick -> handleAddNewCustomFieldClick(
                action,
            )

            is VaultAddItemAction.Common.TooltipClick -> handleTooltipClick()
        }
    }

    private fun handleTypeOptionSelect(action: VaultAddItemAction.Common.TypeOptionSelect) {
        when (action.typeOption) {
            VaultAddItemState.ItemTypeOption.LOGIN -> handleSwitchToAddLoginItem()
            VaultAddItemState.ItemTypeOption.CARD -> handleSwitchToAddCardItem()
            VaultAddItemState.ItemTypeOption.IDENTITY -> handleSwitchToAddIdentityItem()
            VaultAddItemState.ItemTypeOption.SECURE_NOTES -> handleSwitchToAddSecureNotesItem()
        }
    }

    private fun handleSwitchToAddLoginItem() {
        updateContent { currentContent ->
            currentContent.copy(
                type = VaultAddItemState.ViewState.Content.ItemType.Login(),
            )
        }
    }

    private fun handleSwitchToAddSecureNotesItem() {
        updateContent { currentContent ->
            currentContent.copy(
                type = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
            )
        }
    }

    private fun handleSwitchToAddCardItem() {
        updateContent { currentContent ->
            currentContent.copy(
                type = VaultAddItemState.ViewState.Content.ItemType.Card,
            )
        }
    }

    private fun handleSwitchToAddIdentityItem() {
        updateContent { currentContent ->
            currentContent.copy(
                type = VaultAddItemState.ViewState.Content.ItemType.Identity,
            )
        }
    }

    private fun handleSaveClick() = onContent { content ->
        if (content.common.name.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = VaultAddItemState.DialogState.Error(
                        R.string.validation_field_required
                            .asText(R.string.name.asText()),
                    ),
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddItemState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )
        }

        viewModelScope.launch {
            when (val vaultAddEditType = state.vaultAddEditType) {
                VaultAddEditType.AddItem -> {
                    val result = vaultRepository.createCipher(cipherView = content.toCipherView())
                    sendAction(VaultAddItemAction.Internal.CreateCipherResultReceive(result))
                }

                is VaultAddEditType.EditItem -> {
                    val result = vaultRepository.updateCipher(
                        cipherId = vaultAddEditType.vaultItemId,
                        cipherView = content.toCipherView(),
                    )
                    sendAction(VaultAddItemAction.Internal.UpdateCipherResultReceive(result))
                }
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = VaultAddItemEvent.NavigateBack,
        )
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleAddNewCustomFieldClick(
        action: VaultAddItemAction.Common.AddNewCustomFieldClick,
    ) {
        val newCustomData: VaultAddItemState.Custom =
            action.customFieldType.toCustomField(action.name)

        updateCommonContent { loginType ->
            loginType.copy(customFieldData = loginType.customFieldData + newCustomData)
        }
    }

    private fun handleCustomFieldValueChange(
        action: VaultAddItemAction.Common.CustomFieldValueChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(
                customFieldData = commonContent.customFieldData.map { customField ->
                    if (customField.itemId == action.customField.itemId) {
                        action.customField
                    } else {
                        customField
                    }
                },
            )
        }
    }

    private fun handleFolderTextInputChange(
        action: VaultAddItemAction.Common.FolderChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(folderName = action.folder)
        }
    }

    private fun handleToggleFavorite(
        action: VaultAddItemAction.Common.ToggleFavorite,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(favorite = action.isFavorite)
        }
    }

    private fun handleToggleMasterPasswordReprompt(
        action: VaultAddItemAction.Common.ToggleMasterPasswordReprompt,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(masterPasswordReprompt = action.isMasterPasswordReprompt)
        }
    }

    private fun handleNotesTextInputChange(
        action: VaultAddItemAction.Common.NotesTextChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(notes = action.notes)
        }
    }

    private fun handleOwnershipTextInputChange(
        action: VaultAddItemAction.Common.OwnershipChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(ownership = action.ownership)
        }
    }

    private fun handleNameTextInputChange(
        action: VaultAddItemAction.Common.NameTextChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(name = action.name)
        }
    }

    private fun handleTooltipClick() {
        // TODO Add the text for the prompt (BIT-1079)
        sendEvent(
            event = VaultAddItemEvent.ShowToast(
                message = "Not yet implemented",
            ),
        )
    }

    //endregion Common Handlers

    //region Add Login Item Type Handlers

    @Suppress("LongMethod")
    private fun handleAddLoginTypeAction(
        action: VaultAddItemAction.ItemType.LoginType,
    ) {
        when (action) {
            is VaultAddItemAction.ItemType.LoginType.UsernameTextChange -> {
                handleLoginUsernameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.PasswordTextChange -> {
                handleLoginPasswordTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.UriTextChange -> {
                handleLoginUriTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick -> {
                handleLoginOpenUsernameGeneratorClick()
            }

            is VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick -> {
                handleLoginPasswordCheckerClick()
            }

            is VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick -> {
                handleLoginOpenPasswordGeneratorClick()
            }

            is VaultAddItemAction.ItemType.LoginType.SetupTotpClick -> {
                handleLoginSetupTotpClick()
            }

            is VaultAddItemAction.ItemType.LoginType.UriSettingsClick -> {
                handleLoginUriSettingsClick()
            }

            is VaultAddItemAction.ItemType.LoginType.AddNewUriClick -> {
                handleLoginAddNewUriClick()
            }
        }
    }

    private fun handleLoginUsernameTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.UsernameTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(username = action.username)
        }
    }

    private fun handleLoginPasswordTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.PasswordTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(password = action.password)
        }
    }

    private fun handleLoginUriTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.UriTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(uri = action.uri)
        }
    }

    private fun handleLoginOpenUsernameGeneratorClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Open Username Generator",
                ),
            )
        }
    }

    private fun handleLoginPasswordCheckerClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Password Checker",
                ),
            )
        }
    }

    private fun handleLoginOpenPasswordGeneratorClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Open Password Generator",
                ),
            )
        }
    }

    private fun handleLoginSetupTotpClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Setup TOTP",
                ),
            )
        }
    }

    private fun handleLoginUriSettingsClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "URI Settings",
                ),
            )
        }
    }

    private fun handleLoginAddNewUriClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Add New URI",
                ),
            )
        }
    }

    //endregion Add Login Item Type Handlers

    //region Internal Type Handlers

    private fun handleInternalActions(action: VaultAddItemAction.Internal) {
        when (action) {
            is VaultAddItemAction.Internal.CreateCipherResultReceive -> {
                handleCreateCipherResultReceive(action)
            }

            is VaultAddItemAction.Internal.UpdateCipherResultReceive -> {
                handleUpdateCipherResultReceive(action)
            }

            is VaultAddItemAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
        }
    }

    private fun handleCreateCipherResultReceive(
        action: VaultAddItemAction.Internal.CreateCipherResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        when (action.createCipherResult) {
            is CreateCipherResult.Error -> {
                // TODO Display error dialog BIT-501
                sendEvent(
                    event = VaultAddItemEvent.ShowToast(
                        message = "Save Item Failure",
                    ),
                )
            }

            is CreateCipherResult.Success -> {
                sendEvent(
                    event = VaultAddItemEvent.NavigateBack,
                )
            }
        }
    }

    private fun handleUpdateCipherResultReceive(
        action: VaultAddItemAction.Internal.UpdateCipherResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (action.updateCipherResult) {
            is UpdateCipherResult.Error -> {
                // TODO Display error dialog BIT-501
                sendEvent(VaultAddItemEvent.ShowToast(message = "Save Item Failure"))
            }

            is UpdateCipherResult.Success -> {
                sendEvent(VaultAddItemEvent.NavigateBack)
            }
        }
    }

    private fun handleVaultDataReceive(action: VaultAddItemAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultAddItemState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState
                            .data
                            ?.toViewState()
                            ?: VaultAddItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = VaultAddItemState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultAddItemState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState
                            .data
                            ?.toViewState()
                            ?: VaultAddItemState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    //endregion Internal Type Handlers

    //region Utility Functions

    private inline fun onContent(
        crossinline block: (VaultAddItemState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? VaultAddItemState.ViewState.Content)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            VaultAddItemState.ViewState.Content,
        ) -> VaultAddItemState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? VaultAddItemState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateCommonContent(
        crossinline block: (VaultAddItemState.ViewState.Content.Common) ->
        VaultAddItemState.ViewState.Content.Common,
    ) {
        updateContent { currentContent ->
            currentContent.copy(common = block(currentContent.common))
        }
    }

    private inline fun updateLoginContent(
        crossinline block: (VaultAddItemState.ViewState.Content.ItemType.Login) ->
        VaultAddItemState.ViewState.Content.ItemType.Login,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddItemState.ViewState.Content.ItemType.Login)
                ?.let { currentContent.copy(type = block(it)) }
        }
    }

    //endregion Utility Functions
}

/**
 * Represents the state for adding an item to the vault.
 *
 * @property vaultAddEditType Indicates whether the VM is in add or edit mode.
 * @property viewState indicates what view state the screen is in.
 * @property dialog the state for the dialogs that can be displayed
 */
@Parcelize
data class VaultAddItemState(
    val vaultAddEditType: VaultAddEditType,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (vaultAddEditType) {
            VaultAddEditType.AddItem -> R.string.add_item.asText()
            is VaultAddEditType.EditItem -> R.string.edit_item.asText()
        }

    /**
     * Helper to determine if the UI should display the content in add item mode.
     */
    val isAddItemMode: Boolean get() = vaultAddEditType == VaultAddEditType.AddItem

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
     * Represents the specific view states for the [VaultAddItemScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultAddItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Loading state for the [VaultAddItemScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultAddItemScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val type: ItemType,
        ) : ViewState() {

            /**
             * Content data that is common for all item types.
             *
             * @property originalCipher The original cipher from the vault that the user is editing.
             * This is only present when editing a pre-existing cipher.
             * @property name Represents the name for the item type. This is an abstract property
             * that must be overridden to save the item.
             * @property masterPasswordReprompt Indicates if a master password reprompt is required.
             * @property favorite Indicates whether this item is marked as a favorite.
             * @property customFieldData Additional custom fields associated with the item.
             * @property notes Any additional notes or comments associated with the item.
             * @property folderName The folder that this item belongs to.
             * @property availableFolders The list of folders that this item could be added too.
             * @property ownership The ownership email associated with the item.
             * @property availableOwners A list of available owners.
             */
            @Parcelize
            data class Common(
                @IgnoredOnParcel
                val originalCipher: CipherView? = null,
                val name: String = "",
                val masterPasswordReprompt: Boolean = false,
                val favorite: Boolean = false,
                val customFieldData: List<Custom> = emptyList(),
                val notes: String = "",
                val folderName: Text = DEFAULT_FOLDER,
                val availableFolders: List<Text> = listOf(
                    "Folder 1".asText(),
                    "Folder 2".asText(),
                    "Folder 3".asText(),
                ),
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                val ownership: String = DEFAULT_OWNERSHIP,
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                val availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
            ) : Parcelable {
                companion object {
                    private val DEFAULT_FOLDER: Text = R.string.folder_none.asText()
                    private const val DEFAULT_OWNERSHIP: String = "placeholder@email.com"
                }
            }

            /**
             * Content data specific to an item type.
             */
            @Parcelize
            sealed class ItemType : Parcelable {

                /**
                 * Represents the resource ID for the display string. This is an abstract property
                 * that must be overridden by each subclass to provide the appropriate string
                 * resource for display purposes.
                 */
                abstract val displayStringResId: Int

                /**
                 * Represents the login item information.
                 *
                 * @property username The username required for the login item.
                 * @property password The password required for the login item.
                 * @property uri The URI associated with the login item.
                 */
                @Parcelize
                data class Login(
                    val username: String = "",
                    val password: String = "",
                    val uri: String = "",
                ) : ItemType() {
                    override val displayStringResId: Int get() = ItemTypeOption.LOGIN.labelRes
                }

                /**
                 * Represents the `Card` item type.
                 */
                @Parcelize
                data object Card : ItemType() {
                    override val displayStringResId: Int get() = ItemTypeOption.CARD.labelRes
                }

                /**
                 * Represents the `Identity` item type.
                 */
                @Parcelize
                data object Identity : ItemType() {
                    override val displayStringResId: Int get() = ItemTypeOption.IDENTITY.labelRes
                }

                /**
                 * Represents the `SecureNotes` item type.
                 */
                @Parcelize
                data object SecureNotes : ItemType() {
                    override val displayStringResId: Int
                        get() = ItemTypeOption.SECURE_NOTES.labelRes
                }
            }
        }
    }

    /**
     * This Models the Custom field type chosen by the user.
     */
    @Parcelize
    sealed class Custom : Parcelable {

        /**
         * The itemId that is used to identify the Custom item on updates.
         */
        abstract val itemId: String

        /**
         * Represents the data for displaying a custom text field.
         */
        @Parcelize
        data class TextField(
            override val itemId: String,
            val name: String,
            val value: String,
        ) : Custom()

        /**
         * Represents the data for displaying a custom hidden text field.
         */
        @Parcelize
        data class HiddenField(
            override val itemId: String,
            val name: String,
            val value: String,
        ) : Custom()

        /**
         * Represents the data for displaying a custom boolean property field.
         */
        @Parcelize
        data class BooleanField(
            override val itemId: String,
            val name: String,
            val value: Boolean,
        ) : Custom()

        /**
         * Represents the data for displaying a custom linked field.
         */
        @Parcelize
        data class LinkedField(
            override val itemId: String,
            val name: String,
            val vaultLinkedFieldType: VaultLinkedFieldType,
        ) : Custom()
    }

    /**
     * Displays a dialog.
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a loading dialog to the user.
         */
        @Parcelize
        data class Loading(val label: Text) : DialogState()

        /**
         * Displays an error dialog to the user.
         */
        @Parcelize
        data class Error(val message: Text) : DialogState()
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
     * Represents actions common across all item types.
     */
    sealed class Common : VaultAddItemAction() {

        /**
         * Represents the action when the save button is clicked.
         */
        data object SaveClick : Common()

        /**
         * User clicked close.
         */
        data object CloseClick : Common()

        /**
         * The user has clicked to dismiss the dialog.
         */
        data object DismissDialog : Common()

        /**
         * Represents the action when a type option is selected.
         *
         * @property typeOption The selected type option.
         */
        data class TypeOptionSelect(
            val typeOption: VaultAddItemState.ItemTypeOption,
        ) : Common()

        /**
         * Fired when the name text input is changed.
         *
         * @property name The new name text.
         */
        data class NameTextChange(val name: String) : Common()

        /**
         * Fired when the folder text input is changed.
         *
         * @property folder The new folder text.
         */
        data class FolderChange(val folder: Text) : Common()

        /**
         * Fired when the Favorite toggle is changed.
         *
         * @property isFavorite The new state of the Favorite toggle.
         */
        data class ToggleFavorite(val isFavorite: Boolean) : Common()

        /**
         * Fired when the Master Password Reprompt toggle is changed.
         *
         * @property isMasterPasswordReprompt The new state of the Master
         * Password Re-prompt toggle.
         */
        data class ToggleMasterPasswordReprompt(val isMasterPasswordReprompt: Boolean) : Common()

        /**
         * Fired when the notes text input is changed.
         *
         * @property notes The new notes text.
         */
        data class NotesTextChange(val notes: String) : Common()

        /**
         * Fired when the ownership text input is changed.
         *
         * @property ownership The new ownership text.
         */
        data class OwnershipChange(val ownership: String) : Common()

        /**
         * Represents the action to add a new custom field.
         */
        data class AddNewCustomFieldClick(
            val customFieldType: CustomFieldType,
            val name: String,
        ) : Common()

        /**
         *  Fired when the custom field data is changed.
         */
        data class CustomFieldValueChange(val customField: VaultAddItemState.Custom) : Common()

        /**
         * Represents the action to open tooltip
         */
        data object TooltipClick : Common()
    }

    /**
     * Represents actions specific to an item type.
     */
    sealed class ItemType : VaultAddItemAction() {

        /**
         * Represents actions specific to the Login type.
         */
        sealed class LoginType : ItemType() {

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
        }
    }

    /**
     * Models actions that the [VaultAddItemViewModel] itself might send.
     */
    sealed class Internal : VaultAddItemAction() {
        /**
         * Indicates that the vault item data has been received.
         */
        data class VaultDataReceive(
            val vaultDataState: DataState<CipherView?>,
        ) : Internal()

        /**
         * Indicates a result for creating a cipher has been received.
         */
        data class CreateCipherResultReceive(
            val createCipherResult: CreateCipherResult,
        ) : Internal()

        /**
         * Indicates a result for updating a cipher has been received.
         */
        data class UpdateCipherResultReceive(
            val updateCipherResult: UpdateCipherResult,
        ) : Internal()
    }
}
