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
import com.x8bit.bitwarden.ui.vault.feature.additem.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.additem.util.toViewState
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
@Suppress("TooManyFunctions", "LargeClass")
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
            is VaultAddItemAction.ItemType.IdentityType -> handleIdentityTypeActions(action)
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
                type = VaultAddItemState.ViewState.Content.ItemType.Identity(),
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
                handleLoginSetupTotpClick(action)
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

    private fun handleLoginSetupTotpClick(
        action: VaultAddItemAction.ItemType.LoginType.SetupTotpClick,
    ) {
        viewModelScope.launch {
            val message = if (action.isGranted) {
                "Permission Granted, QR Code Scanner Not Implemented"
            } else {
                "Permission Not Granted, Manual QR Code Entry Not Implemented"
            }
            sendEvent(
                event = VaultAddItemEvent.ShowToast(
                    message = message,
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

    //region Identity Type Handlers
    private fun handleIdentityTypeActions(action: VaultAddItemAction.ItemType.IdentityType) {
        when (action) {
            is VaultAddItemAction.ItemType.IdentityType.FirstNameTextChange -> {
                handleIdentityFirstNameTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.Address1TextChange -> {
                handleIdentityAddress1TextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.Address2TextChange -> {
                handleIdentityAddress2TextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.Address3TextChange -> {
                handleIdentityAddress3TextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.CityTextChange -> {
                handleIdentityCityTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.StateTextChange -> {
                handleIdentityStateTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.CompanyTextChange -> {
                handleIdentityCompanyTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.CountryTextChange -> {
                handleIdentityCountryTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.EmailTextChange -> {
                handleIdentityEmailTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.LastNameTextChange -> {
                handleIdentityLastNameTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.LicenseNumberTextChange -> {
                handleIdentityLicenseNumberTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.MiddleNameTextChange -> {
                handleIdentityMiddleNameTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.PassportNumberTextChange -> {
                handleIdentityPassportNumberTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.PhoneTextChange -> {
                handleIdentityPhoneTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.ZipTextChange -> {
                handleIdentityZipTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.SsnTextChange -> {
                handleIdentitySsnTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.UsernameTextChange -> {
                handleIdentityUsernameTextChange(action)
            }

            is VaultAddItemAction.ItemType.IdentityType.TitleSelected -> {
                handleIdentityTitleSelected(action)
            }
        }
    }

    private fun handleIdentityAddress1TextChange(
        action: VaultAddItemAction.ItemType.IdentityType.Address1TextChange,
    ) {
        updateIdentityContent { it.copy(address1 = action.address1) }
    }

    private fun handleIdentityAddress2TextChange(
        action: VaultAddItemAction.ItemType.IdentityType.Address2TextChange,
    ) {
        updateIdentityContent { it.copy(address2 = action.address2) }
    }

    private fun handleIdentityAddress3TextChange(
        action: VaultAddItemAction.ItemType.IdentityType.Address3TextChange,
    ) {
        updateIdentityContent { it.copy(address3 = action.address3) }
    }

    private fun handleIdentityCityTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.CityTextChange,
    ) {
        updateIdentityContent { it.copy(city = action.city) }
    }

    private fun handleIdentityStateTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.StateTextChange,
    ) {
        updateIdentityContent { it.copy(state = action.state) }
    }

    private fun handleIdentityCompanyTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.CompanyTextChange,
    ) {
        updateIdentityContent { it.copy(company = action.company) }
    }

    private fun handleIdentityCountryTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.CountryTextChange,
    ) {
        updateIdentityContent { it.copy(country = action.country) }
    }

    private fun handleIdentityEmailTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.EmailTextChange,
    ) {
        updateIdentityContent { it.copy(email = action.email) }
    }

    private fun handleIdentityLastNameTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.LastNameTextChange,
    ) {
        updateIdentityContent { it.copy(lastName = action.lastName) }
    }

    private fun handleIdentityLicenseNumberTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.LicenseNumberTextChange,
    ) {
        updateIdentityContent { it.copy(licenseNumber = action.licenseNumber) }
    }

    private fun handleIdentityMiddleNameTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.MiddleNameTextChange,
    ) {
        updateIdentityContent { it.copy(middleName = action.middleName) }
    }

    private fun handleIdentityPassportNumberTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.PassportNumberTextChange,
    ) {
        updateIdentityContent { it.copy(passportNumber = action.passportNumber) }
    }

    private fun handleIdentityPhoneTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.PhoneTextChange,
    ) {
        updateIdentityContent { it.copy(phone = action.phone) }
    }

    private fun handleIdentityZipTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.ZipTextChange,
    ) {
        updateIdentityContent { it.copy(zip = action.zip) }
    }

    private fun handleIdentitySsnTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.SsnTextChange,
    ) {
        updateIdentityContent { it.copy(ssn = action.ssn) }
    }

    private fun handleIdentityUsernameTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.UsernameTextChange,
    ) {
        updateIdentityContent { it.copy(username = action.username) }
    }

    private fun handleIdentityFirstNameTextChange(
        action: VaultAddItemAction.ItemType.IdentityType.FirstNameTextChange,
    ) {
        updateIdentityContent { it.copy(firstName = action.firstName) }
    }

    private fun handleIdentityTitleSelected(
        action: VaultAddItemAction.ItemType.IdentityType.TitleSelected,
    ) {
        updateIdentityContent { it.copy(selectedTitle = action.title) }
    }
    //endregion Identity Type Handlers

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

    private inline fun updateIdentityContent(
        crossinline block: (VaultAddItemState.ViewState.Content.ItemType.Identity) ->
        VaultAddItemState.ViewState.Content.ItemType.Identity,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddItemState.ViewState.Content.ItemType.Identity)
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
                 *
                 * @property selectedTitle The selected title for the identity item.
                 * @property firstName The first name for the identity item.
                 * @property middleName The middle name for the identity item.
                 * @property lastName The last name for the identity item.
                 * @property username The username for the identity item.
                 * @property company The company for the identity item.
                 * @property ssn The SSN for the identity item.
                 * @property passportNumber The passport number for the identity item.
                 * @property licenseNumber The license number for the identity item.
                 * @property email The email for the identity item.
                 * @property phone The phone for the identity item.
                 * @property address1 The address1 for the identity item.
                 * @property address2 The address2 for the identity item.
                 * @property address3 The address3 for the identity item.
                 * @property city The city for the identity item.
                 * @property state the state for the identity item.
                 * @property zip The zip for the identity item.
                 * @property country The country for the identity item.
                 */
                @Parcelize
                data class Identity(
                    val selectedTitle: Title = Title.MR,
                    val firstName: String = "",
                    val middleName: String = "",
                    val lastName: String = "",
                    val username: String = "",
                    val company: String = "",
                    val ssn: String = "",
                    val passportNumber: String = "",
                    val licenseNumber: String = "",
                    val email: String = "",
                    val phone: String = "",
                    val address1: String = "",
                    val address2: String = "",
                    val address3: String = "",
                    val city: String = "",
                    val state: String = "",
                    val zip: String = "",
                    val country: String = "",
                ) : ItemType() {

                    /**
                     * Defines all available title options for identities.
                     */
                    enum class Title(val value: Text) {
                        MR(value = R.string.mr.asText()),
                        MRS(value = R.string.mrs.asText()),
                        MS(value = R.string.ms.asText()),
                        MX(value = R.string.mx.asText()),
                        DR(value = R.string.dr.asText()),
                    }

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
             * Represents the action to set up TOTP.
             *
             * @property isGranted the status of the camera permission
             */
            data class SetupTotpClick(val isGranted: Boolean) : LoginType()

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
             * Represents the action of clicking TOTP settings
             */
            data object UriSettingsClick : LoginType()

            /**
             * Represents the action to add a new URI field.
             */
            data object AddNewUriClick : LoginType()
        }

        /**
         * Represents actions specific to the Identity type.
         */
        sealed class IdentityType : ItemType() {

            /**
             * Fired when the first name text input is changed.
             *
             * @property firstName The new first name text.
             */
            data class FirstNameTextChange(val firstName: String) : IdentityType()

            /**
             * Fired when the middle name text input is changed.
             *
             * @property middleName The new middle name text.
             */
            data class MiddleNameTextChange(val middleName: String) : IdentityType()

            /**
             * Fired when the last name text input is changed.
             *
             * @property lastName The new last name text.
             */
            data class LastNameTextChange(val lastName: String) : IdentityType()

            /**
             * Fired when the username text input is changed.
             *
             * @property username The new username text.
             */
            data class UsernameTextChange(val username: String) : IdentityType()

            /**
             * Fired when the company text input is changed.
             *
             * @property company The new company text.
             */
            data class CompanyTextChange(val company: String) : IdentityType()

            /**
             * Fired when the SSN text input is changed.
             *
             * @property ssn The new SSN text.
             */
            data class SsnTextChange(val ssn: String) : IdentityType()

            /**
             * Fired when the passport number text input is changed.
             *
             * @property passportNumber The new passport number text.
             */
            data class PassportNumberTextChange(val passportNumber: String) : IdentityType()

            /**
             * Fired when the license number text input is changed.
             *
             * @property licenseNumber The new license number text.
             */
            data class LicenseNumberTextChange(val licenseNumber: String) : IdentityType()

            /**
             * Fired when the email text input is changed.
             *
             * @property email The new email text.
             */
            data class EmailTextChange(val email: String) : IdentityType()

            /**
             * Fired when the phone text input is changed.
             *
             * @property phone The new phone text.
             */
            data class PhoneTextChange(val phone: String) : IdentityType()

            /**
             * Fired when the address1 text input is changed.
             *
             * @property address1 The new address1 text.
             */
            data class Address1TextChange(val address1: String) : IdentityType()

            /**
             * Fired when the address2 text input is changed.
             *
             * @property address2 The new address2 text.
             */
            data class Address2TextChange(val address2: String) : IdentityType()

            /**
             * Fired when the address3 text input is changed.
             *
             * @property address3 The new address3 text.
             */
            data class Address3TextChange(val address3: String) : IdentityType()

            /**
             * Fired when the city text input is changed.
             *
             * @property city The new city text.
             */
            data class CityTextChange(val city: String) : IdentityType()

            /**
             * Fired when the state text input is changed.
             *
             * @property state The new state text.
             */
            data class StateTextChange(val state: String) : IdentityType()

            /**
             * Fired when the zip text input is changed.
             *
             * @property zip The new postal text.
             */
            data class ZipTextChange(val zip: String) : IdentityType()

            /**
             * Fired when the country text input is changed.
             *
             * @property country The new country text.
             */
            data class CountryTextChange(val country: String) : IdentityType()

            /**
             * Fired when the title input is selected.
             *
             * @property title The selected title.
             */
            data class TitleSelected(
                val title: VaultAddItemState.ViewState.Content.ItemType.Identity.Title,
            ) : IdentityType()
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
