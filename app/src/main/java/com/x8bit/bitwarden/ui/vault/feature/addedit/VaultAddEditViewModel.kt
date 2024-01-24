package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.takeUntilLoaded
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toCipherView
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Collections
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
class VaultAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val vaultRepository: VaultRepository,
    private val generatorRepository: GeneratorRepository,
    private val resourceManager: ResourceManager,
) : BaseViewModel<VaultAddEditState, VaultAddEditEvent, VaultAddEditAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val vaultAddEditType = VaultAddEditArgs(savedStateHandle).vaultAddEditType
            VaultAddEditState(
                vaultAddEditType = vaultAddEditType,
                viewState = when (vaultAddEditType) {
                    VaultAddEditType.AddItem -> VaultAddEditState.ViewState.Content(
                        common = VaultAddEditState.ViewState.Content.Common(),
                        type = VaultAddEditState.ViewState.Content.ItemType.Login(),
                    )

                    is VaultAddEditType.EditItem -> VaultAddEditState.ViewState.Loading
                    is VaultAddEditType.CloneItem -> VaultAddEditState.ViewState.Loading
                },
                dialog = null,
            )
        },
) {

    //region Initialization and Overrides

    init {
        state
            .vaultAddEditType
            .vaultItemId
            ?.let { itemId ->
                vaultRepository
                    .getVaultItemStateFlow(itemId)
                    // We'll stop getting updates as soon as we get some loaded data.
                    .takeUntilLoaded()
                    .map { VaultAddEditAction.Internal.VaultDataReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }

        vaultRepository
            .totpCodeFlow
            .map { VaultAddEditAction.Internal.TotpCodeReceive(totpResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        generatorRepository
            .generatorResultFlow
            .map {
                VaultAddEditAction.Internal.GeneratorResultReceive(generatorResult = it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultAddEditAction) {
        when (action) {
            is VaultAddEditAction.Common -> handleCommonActions(action)
            is VaultAddEditAction.ItemType.LoginType -> handleAddLoginTypeAction(action)
            is VaultAddEditAction.ItemType.IdentityType -> handleIdentityTypeActions(action)
            is VaultAddEditAction.ItemType.CardType -> handleCardTypeActions(action)
            is VaultAddEditAction.Internal -> handleInternalActions(action)
        }
    }

    //endregion Initialization and Overrides

    //region Common Handlers

    private fun handleCommonActions(action: VaultAddEditAction.Common) {
        when (action) {
            is VaultAddEditAction.Common.CustomFieldValueChange -> handleCustomFieldValueChange(
                action,
            )

            is VaultAddEditAction.Common.FolderChange -> handleFolderTextInputChange(action)
            is VaultAddEditAction.Common.NameTextChange -> handleNameTextInputChange(action)
            is VaultAddEditAction.Common.NotesTextChange -> handleNotesTextInputChange(action)
            is VaultAddEditAction.Common.OwnershipChange -> handleOwnershipTextInputChange(action)
            is VaultAddEditAction.Common.ToggleFavorite -> handleToggleFavorite(action)
            is VaultAddEditAction.Common.ToggleMasterPasswordReprompt -> {
                handleToggleMasterPasswordReprompt(action)
            }

            is VaultAddEditAction.Common.AttachmentsClick -> handleAttachmentsClick()
            is VaultAddEditAction.Common.MoveToOrganizationClick -> handleMoveToOrganizationClick()
            is VaultAddEditAction.Common.CollectionsClick -> handleCollectionsClick()
            is VaultAddEditAction.Common.CloseClick -> handleCloseClick()
            is VaultAddEditAction.Common.DismissDialog -> handleDismissDialog()
            is VaultAddEditAction.Common.SaveClick -> handleSaveClick()
            is VaultAddEditAction.Common.TypeOptionSelect -> handleTypeOptionSelect(action)
            is VaultAddEditAction.Common.AddNewCustomFieldClick -> handleAddNewCustomFieldClick(
                action,
            )

            is VaultAddEditAction.Common.TooltipClick -> handleTooltipClick()
            is VaultAddEditAction.Common.CustomFieldActionSelect -> handleCustomFieldActionSelected(
                action,
            )
        }
    }

    private fun handleTypeOptionSelect(action: VaultAddEditAction.Common.TypeOptionSelect) {
        when (action.typeOption) {
            VaultAddEditState.ItemTypeOption.LOGIN -> handleSwitchToAddLoginItem()
            VaultAddEditState.ItemTypeOption.CARD -> handleSwitchToAddCardItem()
            VaultAddEditState.ItemTypeOption.IDENTITY -> handleSwitchToAddIdentityItem()
            VaultAddEditState.ItemTypeOption.SECURE_NOTES -> handleSwitchToAddSecureNotesItem()
        }
    }

    private fun handleSwitchToAddLoginItem() {
        updateContent { currentContent ->
            currentContent.copy(
                common = currentContent.clearNonSharedData(),
                type = currentContent.previousItemTypeOrDefault(
                    itemType = VaultAddEditState.ItemTypeOption.LOGIN,
                ),
                previousItemTypes = currentContent.toUpdatedPreviousItemTypes(),
            )
        }
    }

    private fun handleSwitchToAddSecureNotesItem() {
        updateContent { currentContent ->
            currentContent.copy(
                common = currentContent.clearNonSharedData(),
                type = currentContent.previousItemTypeOrDefault(
                    itemType = VaultAddEditState.ItemTypeOption.SECURE_NOTES,
                ),
                previousItemTypes = currentContent.toUpdatedPreviousItemTypes(),
            )
        }
    }

    private fun handleSwitchToAddCardItem() {
        updateContent { currentContent ->
            currentContent.copy(
                common = currentContent.clearNonSharedData(),
                type = currentContent.previousItemTypeOrDefault(
                    itemType = VaultAddEditState.ItemTypeOption.CARD,
                ),
                previousItemTypes = currentContent.toUpdatedPreviousItemTypes(),
            )
        }
    }

    private fun handleSwitchToAddIdentityItem() {
        updateContent { currentContent ->
            currentContent.copy(
                common = currentContent.clearNonSharedData(),
                type = currentContent.previousItemTypeOrDefault(
                    itemType = VaultAddEditState.ItemTypeOption.IDENTITY,
                ),
                previousItemTypes = currentContent.toUpdatedPreviousItemTypes(),
            )
        }
    }

    private fun handleSaveClick() = onContent { content ->
        if (content.common.name.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = VaultAddEditState.DialogState.Error(
                        R.string.validation_field_required
                            .asText(R.string.name.asText()),
                    ),
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )
        }

        viewModelScope.launch {
            when (val vaultAddEditType = state.vaultAddEditType) {
                VaultAddEditType.AddItem -> {
                    val result = vaultRepository.createCipher(cipherView = content.toCipherView())
                    sendAction(VaultAddEditAction.Internal.CreateCipherResultReceive(result))
                }

                is VaultAddEditType.EditItem -> {
                    val result = vaultRepository.updateCipher(
                        cipherId = vaultAddEditType.vaultItemId,
                        cipherView = content.toCipherView(),
                    )
                    sendAction(VaultAddEditAction.Internal.UpdateCipherResultReceive(result))
                }

                is VaultAddEditType.CloneItem -> {
                    val result = vaultRepository.createCipher(cipherView = content.toCipherView())
                    sendAction(VaultAddEditAction.Internal.CreateCipherResultReceive(result))
                }
            }
        }
    }

    private fun handleAttachmentsClick() {
        onEdit { sendEvent(VaultAddEditEvent.NavigateToAttachments(it.vaultItemId)) }
    }

    private fun handleMoveToOrganizationClick() {
        onEdit { sendEvent(VaultAddEditEvent.NavigateToMoveToOrganization(it.vaultItemId)) }
    }

    private fun handleCollectionsClick() {
        onEdit { sendEvent(VaultAddEditEvent.NavigateToCollections(it.vaultItemId)) }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = VaultAddEditEvent.NavigateBack,
        )
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleAddNewCustomFieldClick(
        action: VaultAddEditAction.Common.AddNewCustomFieldClick,
    ) {
        updateContent {
            val newCustomData: VaultAddEditState.Custom =
                action.customFieldType.toCustomField(
                    name = action.name,
                    itemType = it.type,
                )
            it.copy(
                common = it
                    .common
                    .copy(customFieldData = it.common.customFieldData + newCustomData),
            )
        }
    }

    private fun handleCustomFieldValueChange(
        action: VaultAddEditAction.Common.CustomFieldValueChange,
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

    private fun handleCustomFieldActionSelected(
        action: VaultAddEditAction.Common.CustomFieldActionSelect,
    ) {
        when (action.customFieldAction) {
            CustomFieldAction.MOVE_UP -> {
                val items =
                    (mutableStateFlow.value.viewState as VaultAddEditState.ViewState.Content)
                        .common
                        .customFieldData
                        .toMutableList()

                val index = items.lastIndexOf(action.customField)
                if (index == 0) {
                    return
                }

                Collections.swap(items, index, index - 1)

                updateCommonContent { commonContent ->
                    commonContent.copy(
                        customFieldData = items,
                    )
                }
            }

            CustomFieldAction.MOVE_DOWN -> {
                val items =
                    (mutableStateFlow.value.viewState as VaultAddEditState.ViewState.Content)
                        .common
                        .customFieldData
                        .toMutableList()

                val index = items.indexOf(action.customField)
                if (index == items.lastIndex) {
                    return
                }

                Collections.swap(items, index, index + 1)

                updateCommonContent { commonContent ->
                    commonContent.copy(
                        customFieldData = items,
                    )
                }
            }

            CustomFieldAction.DELETE -> {
                updateCommonContent { commonContent ->
                    commonContent.copy(
                        customFieldData = commonContent.customFieldData.filter {
                            it != action.customField
                        },
                    )
                }
            }

            // Nothing is done here since we handle this with a CustomFieldValueChange action
            CustomFieldAction.EDIT -> Unit
        }
    }

    private fun handleFolderTextInputChange(
        action: VaultAddEditAction.Common.FolderChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(folderName = action.folder)
        }
    }

    private fun handleToggleFavorite(
        action: VaultAddEditAction.Common.ToggleFavorite,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(favorite = action.isFavorite)
        }
    }

    private fun handleToggleMasterPasswordReprompt(
        action: VaultAddEditAction.Common.ToggleMasterPasswordReprompt,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(masterPasswordReprompt = action.isMasterPasswordReprompt)
        }
    }

    private fun handleNotesTextInputChange(
        action: VaultAddEditAction.Common.NotesTextChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(notes = action.notes)
        }
    }

    private fun handleOwnershipTextInputChange(
        action: VaultAddEditAction.Common.OwnershipChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(ownership = action.ownership)
        }
    }

    private fun handleNameTextInputChange(
        action: VaultAddEditAction.Common.NameTextChange,
    ) {
        updateCommonContent { commonContent ->
            commonContent.copy(name = action.name)
        }
    }

    private fun handleTooltipClick() {
        // TODO Add the text for the prompt (BIT-1079)
        sendEvent(
            event = VaultAddEditEvent.ShowToast(
                message = "Not yet implemented".asText(),
            ),
        )
    }

    //endregion Common Handlers

    //region Add Login Item Type Handlers

    @Suppress("LongMethod")
    private fun handleAddLoginTypeAction(
        action: VaultAddEditAction.ItemType.LoginType,
    ) {
        when (action) {
            is VaultAddEditAction.ItemType.LoginType.UsernameTextChange -> {
                handleLoginUsernameTextInputChange(action)
            }

            is VaultAddEditAction.ItemType.LoginType.PasswordTextChange -> {
                handleLoginPasswordTextInputChange(action)
            }

            is VaultAddEditAction.ItemType.LoginType.UriTextChange -> {
                handleLoginUriTextInputChange(action)
            }

            is VaultAddEditAction.ItemType.LoginType.OpenUsernameGeneratorClick -> {
                handleLoginOpenUsernameGeneratorClick()
            }

            is VaultAddEditAction.ItemType.LoginType.PasswordCheckerClick -> {
                handleLoginPasswordCheckerClick()
            }

            is VaultAddEditAction.ItemType.LoginType.OpenPasswordGeneratorClick -> {
                handleLoginOpenPasswordGeneratorClick()
            }

            is VaultAddEditAction.ItemType.LoginType.SetupTotpClick -> {
                handleLoginSetupTotpClick(action)
            }

            is VaultAddEditAction.ItemType.LoginType.UriSettingsClick -> {
                handleLoginUriSettingsClick()
            }

            is VaultAddEditAction.ItemType.LoginType.AddNewUriClick -> {
                handleLoginAddNewUriClick()
            }

            is VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick -> {
                handleLoginCopyTotpKeyText(action)
            }

            is VaultAddEditAction.ItemType.LoginType.ClearTotpKeyClick -> {
                handleLoginClearTotpKey()
            }
        }
    }

    private fun handleLoginUsernameTextInputChange(
        action: VaultAddEditAction.ItemType.LoginType.UsernameTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(username = action.username)
        }
    }

    private fun handleLoginPasswordTextInputChange(
        action: VaultAddEditAction.ItemType.LoginType.PasswordTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(password = action.password)
        }
    }

    private fun handleLoginUriTextInputChange(
        action: VaultAddEditAction.ItemType.LoginType.UriTextChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(uri = action.uri)
        }
    }

    private fun handleLoginOpenUsernameGeneratorClick() {
        sendEvent(event = VaultAddEditEvent.NavigateToGeneratorModal(GeneratorMode.Modal.Username))
    }

    private fun handleLoginPasswordCheckerClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddEditEvent.ShowToast(
                    message = "Password Checker".asText(),
                ),
            )
        }
    }

    private fun handleLoginOpenPasswordGeneratorClick() {
        sendEvent(event = VaultAddEditEvent.NavigateToGeneratorModal(GeneratorMode.Modal.Password))
    }

    private fun handleLoginSetupTotpClick(
        action: VaultAddEditAction.ItemType.LoginType.SetupTotpClick,
    ) {
        if (action.isGranted) {
            sendEvent(event = VaultAddEditEvent.NavigateToQrCodeScan)
        } else {
            sendEvent(event = VaultAddEditEvent.NavigateToManualCodeEntry)
        }
    }

    private fun handleLoginCopyTotpKeyText(
        action: VaultAddEditAction.ItemType.LoginType.CopyTotpKeyClick,
    ) {
        clipboardManager.setText(text = action.totpKey)
    }

    private fun handleLoginClearTotpKey() {
        updateLoginContent { loginType ->
            loginType.copy(totp = null)
        }
    }

    private fun handleLoginUriSettingsClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddEditEvent.ShowToast(
                    message = "URI Settings".asText(),
                ),
            )
        }
    }

    private fun handleLoginAddNewUriClick() {
        viewModelScope.launch {
            sendEvent(
                event = VaultAddEditEvent.ShowToast(
                    message = "Add New URI".asText(),
                ),
            )
        }
    }

    //endregion Add Login Item Type Handlers

    //region Identity Type Handlers
    @Suppress("LongMethod")
    private fun handleIdentityTypeActions(action: VaultAddEditAction.ItemType.IdentityType) {
        when (action) {
            is VaultAddEditAction.ItemType.IdentityType.FirstNameTextChange -> {
                handleIdentityFirstNameTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.Address1TextChange -> {
                handleIdentityAddress1TextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.Address2TextChange -> {
                handleIdentityAddress2TextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.Address3TextChange -> {
                handleIdentityAddress3TextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.CityTextChange -> {
                handleIdentityCityTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.StateTextChange -> {
                handleIdentityStateTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.CompanyTextChange -> {
                handleIdentityCompanyTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.CountryTextChange -> {
                handleIdentityCountryTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.EmailTextChange -> {
                handleIdentityEmailTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.LastNameTextChange -> {
                handleIdentityLastNameTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.LicenseNumberTextChange -> {
                handleIdentityLicenseNumberTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.MiddleNameTextChange -> {
                handleIdentityMiddleNameTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.PassportNumberTextChange -> {
                handleIdentityPassportNumberTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.PhoneTextChange -> {
                handleIdentityPhoneTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.ZipTextChange -> {
                handleIdentityZipTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.SsnTextChange -> {
                handleIdentitySsnTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.UsernameTextChange -> {
                handleIdentityUsernameTextChange(action)
            }

            is VaultAddEditAction.ItemType.IdentityType.TitleSelect -> {
                handleIdentityTitleSelected(action)
            }
        }
    }

    private fun handleIdentityAddress1TextChange(
        action: VaultAddEditAction.ItemType.IdentityType.Address1TextChange,
    ) {
        updateIdentityContent { it.copy(address1 = action.address1) }
    }

    private fun handleIdentityAddress2TextChange(
        action: VaultAddEditAction.ItemType.IdentityType.Address2TextChange,
    ) {
        updateIdentityContent { it.copy(address2 = action.address2) }
    }

    private fun handleIdentityAddress3TextChange(
        action: VaultAddEditAction.ItemType.IdentityType.Address3TextChange,
    ) {
        updateIdentityContent { it.copy(address3 = action.address3) }
    }

    private fun handleIdentityCityTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.CityTextChange,
    ) {
        updateIdentityContent { it.copy(city = action.city) }
    }

    private fun handleIdentityStateTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.StateTextChange,
    ) {
        updateIdentityContent { it.copy(state = action.state) }
    }

    private fun handleIdentityCompanyTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.CompanyTextChange,
    ) {
        updateIdentityContent { it.copy(company = action.company) }
    }

    private fun handleIdentityCountryTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.CountryTextChange,
    ) {
        updateIdentityContent { it.copy(country = action.country) }
    }

    private fun handleIdentityEmailTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.EmailTextChange,
    ) {
        updateIdentityContent { it.copy(email = action.email) }
    }

    private fun handleIdentityLastNameTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.LastNameTextChange,
    ) {
        updateIdentityContent { it.copy(lastName = action.lastName) }
    }

    private fun handleIdentityLicenseNumberTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.LicenseNumberTextChange,
    ) {
        updateIdentityContent { it.copy(licenseNumber = action.licenseNumber) }
    }

    private fun handleIdentityMiddleNameTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.MiddleNameTextChange,
    ) {
        updateIdentityContent { it.copy(middleName = action.middleName) }
    }

    private fun handleIdentityPassportNumberTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.PassportNumberTextChange,
    ) {
        updateIdentityContent { it.copy(passportNumber = action.passportNumber) }
    }

    private fun handleIdentityPhoneTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.PhoneTextChange,
    ) {
        updateIdentityContent { it.copy(phone = action.phone) }
    }

    private fun handleIdentityZipTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.ZipTextChange,
    ) {
        updateIdentityContent { it.copy(zip = action.zip) }
    }

    private fun handleIdentitySsnTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.SsnTextChange,
    ) {
        updateIdentityContent { it.copy(ssn = action.ssn) }
    }

    private fun handleIdentityUsernameTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.UsernameTextChange,
    ) {
        updateIdentityContent { it.copy(username = action.username) }
    }

    private fun handleIdentityFirstNameTextChange(
        action: VaultAddEditAction.ItemType.IdentityType.FirstNameTextChange,
    ) {
        updateIdentityContent { it.copy(firstName = action.firstName) }
    }

    private fun handleIdentityTitleSelected(
        action: VaultAddEditAction.ItemType.IdentityType.TitleSelect,
    ) {
        updateIdentityContent { it.copy(selectedTitle = action.title) }
    }
    //endregion Identity Type Handlers

    //region Card Type Handlers
    private fun handleCardTypeActions(action: VaultAddEditAction.ItemType.CardType) {
        when (action) {
            is VaultAddEditAction.ItemType.CardType.BrandSelect -> {
                handleCardBrandSelected(action)
            }

            is VaultAddEditAction.ItemType.CardType.CardHolderNameTextChange -> {
                handleCardCardHolderNameTextChange(action)
            }

            is VaultAddEditAction.ItemType.CardType.ExpirationMonthSelect -> {
                handleCardExpirationMonthSelected(action)
            }

            is VaultAddEditAction.ItemType.CardType.ExpirationYearTextChange -> {
                handleCardExpirationYearTextChange(action)
            }

            is VaultAddEditAction.ItemType.CardType.NumberTextChange -> {
                handleCardNumberTextChange(action)
            }

            is VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange -> {
                handleCardSecurityCodeTextChange(action)
            }
        }
    }

    private fun handleCardBrandSelected(
        action: VaultAddEditAction.ItemType.CardType.BrandSelect,
    ) {
        updateCardContent { it.copy(brand = action.brand) }
    }

    private fun handleCardCardHolderNameTextChange(
        action: VaultAddEditAction.ItemType.CardType.CardHolderNameTextChange,
    ) {
        updateCardContent { it.copy(cardHolderName = action.cardHolderName) }
    }

    private fun handleCardExpirationMonthSelected(
        action: VaultAddEditAction.ItemType.CardType.ExpirationMonthSelect,
    ) {
        updateCardContent { it.copy(expirationMonth = action.expirationMonth) }
    }

    private fun handleCardExpirationYearTextChange(
        action: VaultAddEditAction.ItemType.CardType.ExpirationYearTextChange,
    ) {
        updateCardContent { it.copy(expirationYear = action.expirationYear) }
    }

    private fun handleCardNumberTextChange(
        action: VaultAddEditAction.ItemType.CardType.NumberTextChange,
    ) {
        updateCardContent { it.copy(number = action.number) }
    }

    private fun handleCardSecurityCodeTextChange(
        action: VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange,
    ) {
        updateCardContent { it.copy(securityCode = action.securityCode) }
    }

    //endregion Card Type Handlers

    //region Internal Type Handlers

    private fun handleInternalActions(action: VaultAddEditAction.Internal) {
        when (action) {
            is VaultAddEditAction.Internal.CreateCipherResultReceive -> {
                handleCreateCipherResultReceive(action)
            }

            is VaultAddEditAction.Internal.UpdateCipherResultReceive -> {
                handleUpdateCipherResultReceive(action)
            }

            is VaultAddEditAction.Internal.TotpCodeReceive -> handleVaultTotpCodeReceive(action)
            is VaultAddEditAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultAddEditAction.Internal.GeneratorResultReceive -> {
                handleGeneratorResultReceive(action)
            }
        }
    }

    private fun handleCreateCipherResultReceive(
        action: VaultAddEditAction.Internal.CreateCipherResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        when (action.createCipherResult) {
            is CreateCipherResult.Error -> {
                // TODO Display error dialog BIT-501
                sendEvent(
                    event = VaultAddEditEvent.ShowToast(
                        message = "Save Item Failure".asText(),
                    ),
                )
            }

            is CreateCipherResult.Success -> {
                sendEvent(
                    event = VaultAddEditEvent.NavigateBack,
                )
            }
        }
    }

    private fun handleUpdateCipherResultReceive(
        action: VaultAddEditAction.Internal.UpdateCipherResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (val result = action.updateCipherResult) {
            is UpdateCipherResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultAddEditState.DialogState.Error(
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is UpdateCipherResult.Success -> {
                sendEvent(VaultAddEditEvent.NavigateBack)
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(action: VaultAddEditAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultAddEditState.ViewState.Error(
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
                            ?.toViewState(
                                isClone = it.isCloneMode,
                                resourceManager = resourceManager,
                            )
                            ?: VaultAddEditState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = VaultAddEditState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultAddEditState.ViewState.Error(
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
                            ?.toViewState(
                                isClone = it.isCloneMode,
                                resourceManager = resourceManager,
                            )
                            ?: VaultAddEditState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleVaultTotpCodeReceive(action: VaultAddEditAction.Internal.TotpCodeReceive) {
        when (action.totpResult) {
            is TotpCodeResult.Success -> {
                sendEvent(
                    event = VaultAddEditEvent.ShowToast(
                        message = R.string.authenticator_key_added.asText(),
                    ),
                )

                updateLoginContent { loginType ->
                    loginType.copy(totp = action.totpResult.code)
                }
            }

            TotpCodeResult.CodeScanningError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = VaultAddEditState.DialogState.Error(
                            R.string.authenticator_key_read_error.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleGeneratorResultReceive(
        action: VaultAddEditAction.Internal.GeneratorResultReceive,
    ) {
        when (action.generatorResult) {
            is GeneratorResult.Password -> {
                updateLoginContent { loginType ->
                    loginType.copy(
                        password = action.generatorResult.password,
                    )
                }
            }

            is GeneratorResult.Username -> {
                updateLoginContent { loginType ->
                    loginType.copy(
                        username = action.generatorResult.username,
                    )
                }
            }
        }
    }

    //endregion Internal Type Handlers

    //region Utility Functions

    private inline fun onContent(
        crossinline block: (VaultAddEditState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? VaultAddEditState.ViewState.Content)?.let(block)
    }

    private inline fun onEdit(
        crossinline block: (VaultAddEditType.EditItem) -> Unit,
    ) {
        (state.vaultAddEditType as? VaultAddEditType.EditItem)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            VaultAddEditState.ViewState.Content,
        ) -> VaultAddEditState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? VaultAddEditState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateCommonContent(
        crossinline block: (VaultAddEditState.ViewState.Content.Common) ->
        VaultAddEditState.ViewState.Content.Common,
    ) {
        updateContent { currentContent ->
            currentContent.copy(common = block(currentContent.common))
        }
    }

    private inline fun updateLoginContent(
        crossinline block: (VaultAddEditState.ViewState.Content.ItemType.Login) ->
        VaultAddEditState.ViewState.Content.ItemType.Login,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddEditState.ViewState.Content.ItemType.Login)
                ?.let { currentContent.copy(type = block(it)) }
        }
    }

    private inline fun updateIdentityContent(
        crossinline block: (VaultAddEditState.ViewState.Content.ItemType.Identity) ->
        VaultAddEditState.ViewState.Content.ItemType.Identity,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddEditState.ViewState.Content.ItemType.Identity)
                ?.let { currentContent.copy(type = block(it)) }
        }
    }

    private inline fun updateCardContent(
        crossinline block: (VaultAddEditState.ViewState.Content.ItemType.Card) ->
        VaultAddEditState.ViewState.Content.ItemType.Card,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddEditState.ViewState.Content.ItemType.Card)?.let {
                currentContent.copy(
                    type = block(it),
                )
            }
        }
    }

    private fun VaultAddEditState.ViewState.Content.clearNonSharedData():
        VaultAddEditState.ViewState.Content.Common =
        common.copy(
            customFieldData = common.customFieldData
                .filterNot { it is VaultAddEditState.Custom.LinkedField },
        )

    private fun VaultAddEditState.ViewState.Content.toUpdatedPreviousItemTypes():
        Map<VaultAddEditState.ItemTypeOption, VaultAddEditState.ViewState.Content.ItemType> =
        previousItemTypes
            .toMutableMap()
            .apply { set(type.itemTypeOption, type) }

    private fun VaultAddEditState.ViewState.Content.previousItemTypeOrDefault(
        itemType: VaultAddEditState.ItemTypeOption,
    ): VaultAddEditState.ViewState.Content.ItemType =
        previousItemTypes.getOrDefault(
            key = itemType,
            defaultValue = when (itemType) {
                VaultAddEditState.ItemTypeOption.LOGIN -> {
                    VaultAddEditState.ViewState.Content.ItemType.Login()
                }

                VaultAddEditState.ItemTypeOption.CARD -> {
                    VaultAddEditState.ViewState.Content.ItemType.Card()
                }

                VaultAddEditState.ItemTypeOption.IDENTITY -> {
                    VaultAddEditState.ViewState.Content.ItemType.Identity()
                }

                VaultAddEditState.ItemTypeOption.SECURE_NOTES -> {
                    VaultAddEditState.ViewState.Content.ItemType.SecureNotes
                }
            },
        )

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
data class VaultAddEditState(
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
            is VaultAddEditType.CloneItem -> R.string.add_item.asText()
        }

    /**
     * Whether or not the cipher is in a collection.
     */
    val isCipherInCollection: Boolean
        get() = (viewState as? ViewState.Content)
            ?.common
            ?.originalCipher
            ?.collectionIds
            ?.isNotEmpty()
            ?: false

    /**
     * Helper to determine if the UI should display the content in add item mode.
     */
    val isAddItemMode: Boolean get() = vaultAddEditType == VaultAddEditType.AddItem

    /**
     * Helper to determine if the UI should display the content in clone mode.
     */
    val isCloneMode: Boolean get() = vaultAddEditType is VaultAddEditType.CloneItem

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
     * Represents the specific view states for the [VaultAddEditScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultAddEditScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Loading state for the [VaultAddEditScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultAddEditScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val type: ItemType,
            val previousItemTypes: Map<ItemTypeOption, ItemType> = emptyMap(),
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
                abstract val itemTypeOption: ItemTypeOption

                /**
                 * Represents the login item information.
                 *
                 * @property username The username required for the login item.
                 * @property password The password required for the login item.
                 * @property uri The URI associated with the login item.
                 * @property totp The current TOTP (if applicable).
                 * @property canViewPassword Indicates whether the current user can view and copy
                 * passwords associated with the login item.
                 */
                @Parcelize
                data class Login(
                    val username: String = "",
                    val password: String = "",
                    val uri: String = "",
                    val totp: String? = null,
                    val canViewPassword: Boolean = true,
                ) : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.LOGIN
                }

                /**
                 * Represents the `Card` item type.
                 *
                 * @property cardHolderName The card holder name for the card item.
                 * @property number The number for the card item.
                 * @property brand The brand for the card item.
                 * @property expirationMonth The expiration month for the card item.
                 * @property expirationYear The expiration year for the card item.
                 * @property securityCode The security code for the card item.
                 */
                @Parcelize
                data class Card(
                    val cardHolderName: String = "",
                    val number: String = "",
                    val brand: VaultCardBrand = VaultCardBrand.SELECT,
                    val expirationMonth: VaultCardExpirationMonth = VaultCardExpirationMonth.SELECT,
                    val expirationYear: String = "",
                    val securityCode: String = "",
                ) : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.CARD
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
                    val selectedTitle: VaultIdentityTitle = VaultIdentityTitle.SELECT,
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

                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.IDENTITY
                }

                /**
                 * Represents the `SecureNotes` item type.
                 */
                @Parcelize
                data object SecureNotes : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.SECURE_NOTES
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
         * The name of the custom field.
         */
        abstract val name: String

        /**
         * Represents the data for displaying a custom text field.
         */
        @Parcelize
        data class TextField(
            override val itemId: String,
            override val name: String,
            val value: String,
        ) : Custom()

        /**
         * Represents the data for displaying a custom hidden text field.
         */
        @Parcelize
        data class HiddenField(
            override val itemId: String,
            override val name: String,
            val value: String,
        ) : Custom()

        /**
         * Represents the data for displaying a custom boolean property field.
         */
        @Parcelize
        data class BooleanField(
            override val itemId: String,
            override val name: String,
            val value: Boolean,
        ) : Custom()

        /**
         * Represents the data for displaying a custom linked field.
         */
        @Parcelize
        data class LinkedField(
            override val itemId: String,
            override val name: String,
            val vaultLinkedFieldType: VaultLinkedFieldType?,
        ) : Custom()
    }

    /**
     * Displays a dialog.
     */
    @Parcelize
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
sealed class VaultAddEditEvent {
    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: Text) : VaultAddEditEvent()

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : VaultAddEditEvent()

    /**
     * Navigate to attachments screen.
     */
    data class NavigateToAttachments(
        val cipherId: String,
    ) : VaultAddEditEvent()

    /**
     * Navigates to the move to organization screen.
     */
    data class NavigateToMoveToOrganization(
        val cipherId: String,
    ) : VaultAddEditEvent()

    /**
     * Navigates to the collections screen.
     */
    data class NavigateToCollections(
        val cipherId: String,
    ) : VaultAddEditEvent()

    /**
     * Navigate to the QR code scan screen.
     */
    data object NavigateToQrCodeScan : VaultAddEditEvent()

    /**
     * Navigate to the manual code entry screen.
     */
    data object NavigateToManualCodeEntry : VaultAddEditEvent()

    /**
     * Navigate to the generator modal.
     */
    data class NavigateToGeneratorModal(
        val generatorMode: GeneratorMode.Modal,
    ) : VaultAddEditEvent()
}

/**
 * Represents a set of actions related to the process of adding an item to the vault.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class VaultAddEditAction {

    /**
     * Represents actions common across all item types.
     */
    sealed class Common : VaultAddEditAction() {

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
         * The user has clicked the attachments overflow option.
         */
        data object AttachmentsClick : Common()

        /**
         * The user has clicked the move to organization overflow option.
         */
        data object MoveToOrganizationClick : Common()

        /**
         * The user has clicked the collections overflow option.
         */
        data object CollectionsClick : Common()

        /**
         * Represents the action when a type option is selected.
         *
         * @property typeOption The selected type option.
         */
        data class TypeOptionSelect(
            val typeOption: VaultAddEditState.ItemTypeOption,
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
        data class CustomFieldValueChange(val customField: VaultAddEditState.Custom) : Common()

        /**
         *  Fired when the custom field data is changed.
         */
        data class CustomFieldActionSelect(
            val customFieldAction: CustomFieldAction,
            val customField: VaultAddEditState.Custom,
        ) : Common()

        /**
         * Represents the action to open tooltip
         */
        data object TooltipClick : Common()
    }

    /**
     * Represents actions specific to an item type.
     */
    sealed class ItemType : VaultAddEditAction() {

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
             * @property isGranted the status of the camera permission.
             */
            data class SetupTotpClick(val isGranted: Boolean) : LoginType()

            /**
             * Represents the action to copy the totp code to the clipboard.
             *
             * @property totpKey the totp key being copied.
             */
            data class CopyTotpKeyClick(val totpKey: String) : LoginType()

            /**
             * Represents the action to clear the totp code.
             */
            data object ClearTotpKeyClick : LoginType()

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
            data class TitleSelect(
                val title: VaultIdentityTitle,
            ) : IdentityType()
        }

        /**
         * Represents actions specific to the Card type.
         */
        sealed class CardType : ItemType() {

            /**
             * Fired when the card holder name text input is changed.
             *
             * @property cardHolderName The new card holder name text.
             */
            data class CardHolderNameTextChange(val cardHolderName: String) : CardType()

            /**
             * Fired when the number text input is changed.
             *
             * @property number The new number text.
             */
            data class NumberTextChange(val number: String) : CardType()

            /**
             * Fired when the brand input is selected.
             *
             * @property brand The selected brand.
             */
            data class BrandSelect(
                val brand: VaultCardBrand,
            ) : CardType()

            /**
             * Fired when the expiration month input is selected.
             *
             * @property expirationMonth The selected expiration month.
             */
            @Suppress("MaxLineLength")
            data class ExpirationMonthSelect(
                val expirationMonth: VaultCardExpirationMonth,
            ) : CardType()

            /**
             * Fired when the expiration year text input is changed.
             *
             * @property expirationYear The new expiration year text.
             */
            data class ExpirationYearTextChange(val expirationYear: String) : CardType()

            /**
             * Fired when the security code text input is changed.
             *
             * @property securityCode The new security code text.
             */
            data class SecurityCodeTextChange(val securityCode: String) : CardType()
        }
    }

    /**
     * Models actions that the [VaultAddEditViewModel] itself might send.
     */
    sealed class Internal : VaultAddEditAction() {

        /**
         * Indicates that the vault totp code result has been received.
         */
        data class TotpCodeReceive(val totpResult: TotpCodeResult) : Internal()

        /**
         * Indicates that the vault totp code result has been received.
         */
        data class GeneratorResultReceive(
            val generatorResult: GeneratorResult,
        ) : Internal()

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
