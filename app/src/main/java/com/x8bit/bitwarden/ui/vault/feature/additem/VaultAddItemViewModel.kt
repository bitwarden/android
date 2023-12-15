package com.x8bit.bitwarden.ui.vault.feature.additem

import android.os.Parcelable
import androidx.annotation.StringRes
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
                    VaultAddEditType.AddItem -> VaultAddItemState.ViewState.Content.Login()
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
            is VaultAddItemAction.SaveClick -> {
                handleSaveClick()
            }

            is VaultAddItemAction.CloseClick -> {
                handleCloseClick()
            }

            is VaultAddItemAction.DismissDialog -> {
                handleDismissDialog()
            }

            is VaultAddItemAction.TypeOptionSelect -> {
                handleTypeOptionSelect(action)
            }

            is VaultAddItemAction.ItemType.LoginType -> {
                handleAddLoginTypeAction(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType -> {
                handleAddSecureNoteTypeAction(action)
            }

            is VaultAddItemAction.Internal -> handleInternalActions(action)
        }
    }

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

    //endregion Initialization and Overrides

    //region Top Level Handlers

    private fun handleSaveClick() = onContent { content ->
        if (content.name.isBlank()) {
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
        updateContent { VaultAddItemState.ViewState.Content.Login() }
    }

    private fun handleSwitchToAddSecureNotesItem() {
        updateContent { VaultAddItemState.ViewState.Content.SecureNotes() }
    }

    private fun handleSwitchToAddCardItem() {
        updateContent { VaultAddItemState.ViewState.Content.Card() }
    }

    private fun handleSwitchToAddIdentityItem() {
        updateContent { VaultAddItemState.ViewState.Content.Identity() }
    }

    //endregion Type Option Handlers

    //region Add Login Item Type Handlers

    @Suppress("LongMethod")
    private fun handleAddLoginTypeAction(
        action: VaultAddItemAction.ItemType.LoginType,
    ) {
        when (action) {
            is VaultAddItemAction.ItemType.LoginType.NameTextChange -> {
                handleLoginNameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.UsernameTextChange -> {
                handleLoginUsernameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.PasswordTextChange -> {
                handleLoginPasswordTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.UriTextChange -> {
                handleLoginUriTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.FolderChange -> {
                handleLoginFolderTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.ToggleFavorite -> {
                handleLoginToggleFavorite(action)
            }

            is VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt -> {
                handleLoginToggleMasterPasswordReprompt(action)
            }

            is VaultAddItemAction.ItemType.LoginType.NotesTextChange -> {
                handleLoginNotesTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.LoginType.OwnershipChange -> {
                handleLoginOwnershipTextInputChange(action)
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

            is VaultAddItemAction.ItemType.LoginType.TooltipClick -> {
                handleLoginTooltipClick()
            }

            is VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick -> {
                handleLoginAddNewCustomFieldClick(action)
            }

            is VaultAddItemAction.ItemType.LoginType.CustomFieldValueChange -> {
                handleLoginCustomFieldValueChange(action)
            }
        }
    }

    private fun handleLoginNameTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.NameTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(name = action.name)
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

    private fun handleLoginFolderTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.FolderChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(folderName = action.folder)
        }
    }

    private fun handleLoginToggleFavorite(
        action: VaultAddItemAction.ItemType.LoginType.ToggleFavorite,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(favorite = action.isFavorite)
        }
    }

    private fun handleLoginToggleMasterPasswordReprompt(
        action: VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(masterPasswordReprompt = action.isMasterPasswordReprompt)
        }
    }

    private fun handleLoginNotesTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.NotesTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(notes = action.notes)
        }
    }

    private fun handleLoginOwnershipTextInputChange(
        action: VaultAddItemAction.ItemType.LoginType.OwnershipChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(ownership = action.ownership)
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

    private fun handleLoginTooltipClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = "Tooltip",
                ),
            )
        }
    }

    private fun handleLoginAddNewCustomFieldClick(
        action: VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick,
    ) {
        val newCustomData: VaultAddItemState.Custom =
            action.customFieldType.toCustomField(action.name)

        updateLoginContent { loginType ->
            loginType.copy(customFieldData = loginType.customFieldData + newCustomData)
        }
    }

    private fun handleLoginCustomFieldValueChange(
        action: VaultAddItemAction.ItemType.LoginType.CustomFieldValueChange,
    ) {
        updateLoginContent { login ->
            login.copy(
                customFieldData = login.customFieldData.map { customField ->
                    if (customField.itemId == action.customField.itemId) {
                        action.customField
                    } else {
                        customField
                    }
                },
            )
        }
    }

    //endregion Add Login Item Type Handlers

    //region Secure Note Item Type Handlers

    private fun handleAddSecureNoteTypeAction(
        action: VaultAddItemAction.ItemType.SecureNotesType,
    ) {
        when (action) {
            is VaultAddItemAction.ItemType.SecureNotesType.NameTextChange -> {
                handleSecureNoteNameTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.FolderChange -> {
                handleSecureNoteFolderTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite -> {
                handleSecureNoteToggleFavorite(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt -> {
                handleSecureNoteToggleMasterPasswordReprompt(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange -> {
                handleSecureNoteNotesTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange -> {
                handleSecureNoteOwnershipTextInputChange(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.TooltipClick -> {
                handleSecureNoteTooltipClick()
            }

            is VaultAddItemAction.ItemType.SecureNotesType.AddNewCustomFieldClick -> {
                handleSecureNoteAddNewCustomFieldClick(action)
            }

            is VaultAddItemAction.ItemType.SecureNotesType.CustomFieldValueChange -> {
                handleSecureNoteCustomFieldValueChange(action)
            }
        }
    }

    private fun handleSecureNoteNameTextInputChange(
        action: VaultAddItemAction.ItemType.SecureNotesType.NameTextChange,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(name = action.name)
        }
    }

    private fun handleSecureNoteFolderTextInputChange(
        action: VaultAddItemAction.ItemType.SecureNotesType.FolderChange,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(folderName = action.folderName)
        }
    }

    private fun handleSecureNoteToggleFavorite(
        action: VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(favorite = action.isFavorite)
        }
    }

    private fun handleSecureNoteToggleMasterPasswordReprompt(
        action: VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(masterPasswordReprompt = action.isMasterPasswordReprompt)
        }
    }

    private fun handleSecureNoteNotesTextInputChange(
        action: VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(notes = action.note)
        }
    }

    private fun handleSecureNoteOwnershipTextInputChange(
        action: VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange,
    ) {
        updateSecureNoteContent { secureNoteType ->
            secureNoteType.copy(ownership = action.ownership)
        }
    }

    private fun handleSecureNoteTooltipClick() {
        // TODO Add the text for the prompt (BIT-1079)
        sendEvent(
            event = VaultAddItemEvent.ShowToast(
                message = "Not yet implemented",
            ),
        )
    }

    private fun handleSecureNoteAddNewCustomFieldClick(
        action: VaultAddItemAction.ItemType.SecureNotesType.AddNewCustomFieldClick,
    ) {
        val newCustomData: VaultAddItemState.Custom =
            action.customFieldType.toCustomField(action.name)

        updateSecureNoteContent { secureNotesType ->
            secureNotesType.copy(customFieldData = secureNotesType.customFieldData + newCustomData)
        }
    }

    private fun handleSecureNoteCustomFieldValueChange(
        action: VaultAddItemAction.ItemType.SecureNotesType.CustomFieldValueChange,
    ) {
        updateSecureNoteContent { secureNote ->
            secureNote.copy(
                customFieldData = secureNote.customFieldData.map { customField ->
                    if (customField.itemId == action.customField.itemId) {
                        action.customField
                    } else {
                        customField
                    }
                },
            )
        }
    }

    //endregion Secure Notes Item Type Handlers

    //region Internal Type Handlers

    @Suppress("MaxLineLength")
    private fun handleCreateCipherResultReceive(action: VaultAddItemAction.Internal.CreateCipherResultReceive) {
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

    private inline fun updateLoginContent(
        crossinline block: (
            VaultAddItemState.ViewState.Content.Login,
        ) -> VaultAddItemState.ViewState.Content.Login,
    ) {
        updateContent { (it as? VaultAddItemState.ViewState.Content.Login)?.let(block) }
    }

    private inline fun updateSecureNoteContent(
        crossinline block: (
            VaultAddItemState.ViewState.Content.SecureNotes,
        ) -> VaultAddItemState.ViewState.Content.SecureNotes,
    ) {
        updateContent { (it as? VaultAddItemState.ViewState.Content.SecureNotes)?.let(block) }
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
        sealed class Content : ViewState() {
            /**
             * The original cipher from the vault that the user is editing.
             *
             * This is only present when editing a pre-existing cipher.
             */
            @IgnoredOnParcel
            abstract val originalCipher: CipherView?

            /**
             * Represents the resource ID for the display string. This is an abstract property
             * that must be overridden by each subclass to provide the appropriate string resource
             * ID for display purposes.
             */
            @get:StringRes
            abstract val displayStringResId: Int

            /**
             * Represents the name for the item type. This is an abstract property that must be
             * overridden to save the item
             */
            abstract val name: String

            /**
             * Indicates if a master password reprompt is required.
             */
            abstract val masterPasswordReprompt: Boolean

            /**
             * Indicates whether this item is marked as a favorite.
             */
            abstract val favorite: Boolean

            /**
             * Additional custom fields associated with the item.
             */
            abstract val customFieldData: List<Custom>

            /**
             * Any additional notes or comments associated with the item.
             */
            abstract val notes: String

            /**
             * The folder that this item belongs too.
             */
            abstract val folderName: Text

            /**
             * The list of folders that this item could be added too.
             */
            abstract val availableFolders: List<Text>

            /**
             * The ownership email associated with the item.
             */
            abstract val ownership: String

            /**
             * A list of available owners.
             */
            abstract val availableOwners: List<String>

            /**
             * Represents the login item information.
             *
             * @property username The username required for the login item.
             * @property password The password required for the login item.
             * @property uri The URI associated with the login item.
             */
            @Parcelize
            data class Login(
                @IgnoredOnParcel
                override val originalCipher: CipherView? = null,
                override val name: String = "",
                override val folderName: Text = DEFAULT_FOLDER,
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val availableFolders: List<Text> = listOf(
                    "Folder 1".asText(),
                    "Folder 2".asText(),
                    "Folder 3".asText(),
                ),
                override val favorite: Boolean = false,
                override val masterPasswordReprompt: Boolean = false,
                override val customFieldData: List<Custom> = emptyList(),
                override val notes: String = "",
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val ownership: String = DEFAULT_OWNERSHIP,
                override val availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
                val username: String = "",
                val password: String = "",
                val uri: String = "",
            ) : Content() {
                override val displayStringResId: Int get() = ItemTypeOption.LOGIN.labelRes
            }

            /**
             * Represents the `Card` item type.
             */
            @Parcelize
            data class Card(
                @IgnoredOnParcel
                override val originalCipher: CipherView? = null,
                override val name: String = "",
                override val folderName: Text = DEFAULT_FOLDER,
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val availableFolders: List<Text> = listOf(
                    "Folder 1".asText(),
                    "Folder 2".asText(),
                    "Folder 3".asText(),
                ),
                override val favorite: Boolean = false,
                override val masterPasswordReprompt: Boolean = false,
                override val customFieldData: List<Custom> = emptyList(),
                override val notes: String = "",
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val ownership: String = DEFAULT_OWNERSHIP,
                override val availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
            ) : Content() {
                override val displayStringResId: Int get() = ItemTypeOption.CARD.labelRes
            }

            /**
             * Represents the `Identity` item type.
             */
            @Parcelize
            data class Identity(
                @IgnoredOnParcel
                override val originalCipher: CipherView? = null,
                override val name: String = "",
                override val folderName: Text = DEFAULT_FOLDER,
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val availableFolders: List<Text> = listOf(
                    "Folder 1".asText(),
                    "Folder 2".asText(),
                    "Folder 3".asText(),
                ),
                override val favorite: Boolean = false,
                override val masterPasswordReprompt: Boolean = false,
                override val customFieldData: List<Custom> = emptyList(),
                override val notes: String = "",
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val ownership: String = DEFAULT_OWNERSHIP,
                override val availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
            ) : Content() {
                override val displayStringResId: Int get() = ItemTypeOption.IDENTITY.labelRes
            }

            /**
             * Represents the `SecureNotes` item type.
             *
             * @property folderName The folder used for the SecureNotes item
             * @property availableFolders A list  of available folders.
             */
            @Parcelize
            data class SecureNotes(
                @IgnoredOnParcel
                override val originalCipher: CipherView? = null,
                override val name: String = "",
                override val folderName: Text = DEFAULT_FOLDER,
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val availableFolders: List<Text> = listOf(
                    "Folder 1".asText(),
                    "Folder 2".asText(),
                    "Folder 3".asText(),
                ),
                override val favorite: Boolean = false,
                override val masterPasswordReprompt: Boolean = false,
                override val customFieldData: List<Custom> = emptyList(),
                override val notes: String = "",
                // TODO: Update this property to get available owners from the data layer (BIT-501)
                override val ownership: String = DEFAULT_OWNERSHIP,
                override val availableOwners: List<String> = listOf("a@b.com", "c@d.com"),
            ) : Content() {
                override val displayStringResId: Int get() = ItemTypeOption.SECURE_NOTES.labelRes
            }

            companion object {
                private val DEFAULT_FOLDER: Text = R.string.folder_none.asText()
                private const val DEFAULT_OWNERSHIP: String = "placeholder@email.com"
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
     * Represents the action when the save button is clicked.
     */
    data object SaveClick : VaultAddItemAction()

    /**
     * User clicked close.
     */
    data object CloseClick : VaultAddItemAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialog : VaultAddItemAction()

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
            data class FolderChange(val folder: Text) : LoginType()

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
             * Represents the action to add a new custom field.
             */
            data class AddNewCustomFieldClick(
                val customFieldType: CustomFieldType,
                val name: String,
            ) : LoginType()

            /**
             *  Fired when the custom field data is changed.
             */
            data class CustomFieldValueChange(
                val customField: VaultAddItemState.Custom,
            ) : LoginType()

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
        }

        /**
         * Represents actions specific to the SecureNotes type.
         */
        sealed class SecureNotesType : ItemType() {
            /**
             * Fired when the name text input is changed.
             *
             * @property name The new name text.
             */
            data class NameTextChange(val name: String) : SecureNotesType()

            /**
             * Fired when the folder text input is changed.
             *
             * @property folderName The new folder text.
             */
            data class FolderChange(val folderName: Text) : SecureNotesType()

            /**
             * Fired when the Favorite toggle is changed.
             *
             * @property isFavorite The new state of the Favorite toggle.
             */
            data class ToggleFavorite(val isFavorite: Boolean) : SecureNotesType()

            /**
             * Fired when the Master Password Reprompt toggle is changed.
             *
             * @property isMasterPasswordReprompt The new state of the Master
             * Password Re-prompt toggle.
             */
            data class ToggleMasterPasswordReprompt(
                val isMasterPasswordReprompt: Boolean,
            ) : SecureNotesType()

            /**
             * Fired when the note text input is changed.
             *
             * @property note The new note text.
             */
            data class NotesTextChange(val note: String) : SecureNotesType()

            /**
             * Fired when the ownership text input is changed.
             *
             * @property ownership The new ownership text.
             */
            data class OwnershipChange(val ownership: String) : SecureNotesType()

            /**
             * Represents the action to open tooltip
             */
            data object TooltipClick : SecureNotesType()

            /**
             * Represents the action to add a new custom field.
             */
            data class AddNewCustomFieldClick(
                val customFieldType: CustomFieldType,
                val name: String,
            ) : SecureNotesType()

            /**
             *  Fired when the custom field data is changed.
             */
            data class CustomFieldValueChange(
                val customField: VaultAddItemState.Custom,
            ) : SecureNotesType()
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
