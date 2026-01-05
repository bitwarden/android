package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.os.Parcelable
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.DateTime
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.takeUntilLoaded
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.TotpData
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePinResult
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.credentials.manager.BitwardenCredentialManager
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2RegisterCredentialResult
import com.x8bit.bitwarden.data.credentials.model.UserVerificationRequirement
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.CoachMarkTourType
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.util.toAutofillSaveItemOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toCreateCredentialRequestOrNull
import com.x8bit.bitwarden.data.platform.manager.util.toTotpDataOrNull
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratorResult
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldAction
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.toCustomField
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.appendFolderAndOwnerData
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toAvailableFolders
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toDefaultAddTypeContent
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.validateCipherOrReturnErrorState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.messageResourceId
import com.x8bit.bitwarden.ui.vault.feature.util.canAssignToCollections
import com.x8bit.bitwarden.ui.vault.feature.util.hasDeletePermissionInAtLeastOneCollection
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toCipherView
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.Clock
import java.util.Collections
import java.util.UUID
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
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList", "LongMethod")
class VaultAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    generatorRepository: GeneratorRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val toastManager: ToastManager,
    private val authRepository: AuthRepository,
    private val clipboardManager: BitwardenClipboardManager,
    private val policyManager: PolicyManager,
    private val vaultRepository: VaultRepository,
    private val bitwardenCredentialManager: BitwardenCredentialManager,
    private val settingsRepository: SettingsRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val resourceManager: ResourceManager,
    private val clock: Clock,
    private val organizationEventManager: OrganizationEventManager,
    private val networkConnectionManager: NetworkConnectionManager,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<VaultAddEditState, VaultAddEditEvent, VaultAddEditAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toVaultAddEditArgs()
            val vaultAddEditType = args.vaultAddEditType
            val vaultCipherType = args.vaultItemCipherType
            val selectedFolderId = args.selectedFolderId
            val selectedCollectionId = args.selectedCollectionId
            val isIndividualVaultDisabled = policyManager
                .getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
                .any()

            val specialCircumstance = specialCircumstanceManager.specialCircumstance
            // Check for autofill data to pre-populate
            val autofillSaveItem = specialCircumstance?.toAutofillSaveItemOrNull()
            val autofillSelectionData = specialCircumstance?.toAutofillSelectionDataOrNull()
            // Check for totp data to pre-populate
            val totpData = specialCircumstance?.toTotpDataOrNull()
            // Check for Fido2 or Password credential data to pre-populate
            val providerCreateCredentialRequest =
                specialCircumstance?.toCreateCredentialRequestOrNull()
            val fido2AttestationOptions = providerCreateCredentialRequest?.requestJson
                ?.let { request ->
                    bitwardenCredentialManager.getPasskeyAttestationOptionsOrNull(request)
                }

            // Exit on save if handling an autofill, Fido2 Attestation, or TOTP link
            val shouldExitOnSave = autofillSaveItem != null ||
                providerCreateCredentialRequest != null

            val dialogState = if (!settingsRepository.initialAutofillDialogShown &&
                vaultAddEditType is VaultAddEditType.AddItem &&
                autofillSelectionData == null
            ) {
                VaultAddEditState.DialogState.InitialAutofillPrompt
            } else {
                null
            }

            VaultAddEditState(
                vaultAddEditType = vaultAddEditType,
                cipherType = vaultCipherType,
                viewState = when (vaultAddEditType) {
                    is VaultAddEditType.AddItem -> {
                        autofillSelectionData
                            ?.toDefaultAddTypeContent(isIndividualVaultDisabled)
                            ?: autofillSaveItem?.toDefaultAddTypeContent(isIndividualVaultDisabled)
                            ?: providerCreateCredentialRequest?.toDefaultAddTypeContent(
                                attestationOptions = fido2AttestationOptions,
                                isIndividualVaultDisabled = isIndividualVaultDisabled,
                            )
                            ?: totpData?.toDefaultAddTypeContent(isIndividualVaultDisabled)
                            ?: VaultAddEditState.ViewState.Content(
                                common = VaultAddEditState.ViewState.Content.Common(
                                    selectedFolderId = selectedFolderId,
                                    selectedCollectionId = selectedCollectionId,
                                ),
                                isIndividualVaultDisabled = isIndividualVaultDisabled,
                                type = vaultCipherType.toItemType(),
                            )
                    }

                    is VaultAddEditType.EditItem -> VaultAddEditState.ViewState.Loading
                    is VaultAddEditType.CloneItem -> VaultAddEditState.ViewState.Loading
                },
                dialog = dialogState,
                bottomSheetState = null,
                totpData = totpData,
                createCredentialRequest = providerCreateCredentialRequest,
                // Set special conditions for autofill and fido2 save
                shouldShowCloseButton = autofillSaveItem == null &&
                    providerCreateCredentialRequest == null,
                shouldExitOnSave = shouldExitOnSave,
                shouldShowCoachMarkTour = false,
                shouldClearSpecialCircumstance = autofillSelectionData == null,
                defaultUriMatchType = settingsRepository.defaultUriMatchType,
            )
        },
) {

    //region Initialization and Overrides

    init {
        onEdit {
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientViewed(cipherId = it.vaultItemId),
            )
        }
        vaultRepository
            .vaultDataStateFlow
            .takeUntilLoaded()
            .map { vaultDataState ->
                VaultAddEditAction.Internal.VaultDataReceive(
                    vaultData = vaultDataState,
                    userData = authRepository.userStateFlow.value,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepository
            .totpCodeFlow
            .map { VaultAddEditAction.Internal.TotpCodeReceive(totpResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepository
            .foldersStateFlow
            .map { VaultAddEditAction.Internal.AvailableFoldersReceive(folderData = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        generatorRepository
            .generatorResultFlow
            .map { result ->
                // Wait until we have a Content screen to update
                mutableStateFlow.first {
                    it.viewState is VaultAddEditState.ViewState.Content
                }
                VaultAddEditAction.Internal.GeneratorResultReceive(generatorResult = result)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        firstTimeActionManager
            .shouldShowAddLoginCoachMarkFlow
            .map { shouldShowTour ->
                VaultAddEditAction.Internal.ShouldShowAddLoginCoachMarkValueChangeReceive(
                    shouldShowCoachMarkTour = shouldShowTour,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.CIPHER_MOVED_TO_ORGANIZATION)
            .map { VaultAddEditAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultAddEditAction) {
        when (action) {
            is VaultAddEditAction.Common -> handleCommonActions(action)
            is VaultAddEditAction.ItemType.LoginType -> handleAddLoginTypeAction(action)
            is VaultAddEditAction.ItemType.IdentityType -> handleIdentityTypeActions(action)
            is VaultAddEditAction.ItemType.CardType -> handleCardTypeActions(action)
            is VaultAddEditAction.ItemType.SshKeyType -> handleSshKeyTypeActions(action)
            is VaultAddEditAction.Internal -> handleInternalActions(action)
        }
    }

    //endregion Initialization and Overrides

    //region Common Handlers

    @Suppress("LongMethod")
    private fun handleCommonActions(action: VaultAddEditAction.Common) {
        when (action) {
            is VaultAddEditAction.Common.CustomFieldValueChange -> {
                handleCustomFieldValueChange(action)
            }

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
            is VaultAddEditAction.Common.ConfirmDeleteClick -> handleConfirmDeleteClick()
            is VaultAddEditAction.Common.CloseClick -> handleCloseClick()
            is VaultAddEditAction.Common.DismissDialog -> handleDismissDialog()
            is VaultAddEditAction.Common.SaveClick -> handleSaveClick()
            is VaultAddEditAction.Common.AddNewCustomFieldClick -> {
                handleAddNewCustomFieldClick(action)
            }

            is VaultAddEditAction.Common.TooltipClick -> handleTooltipClick()
            is VaultAddEditAction.Common.CustomFieldActionSelect -> {
                handleCustomFieldActionSelected(action)
            }

            is VaultAddEditAction.Common.CollectionSelect -> handleCollectionSelect(action)
            is VaultAddEditAction.Common.InitialAutofillDialogDismissed -> {
                handleInitialAutofillDialogDismissed()
            }

            is VaultAddEditAction.Common.HiddenFieldVisibilityChange -> {
                handleHiddenFieldVisibilityChange(action)
            }

            is VaultAddEditAction.Common.ConfirmOverwriteExistingPasskeyClick -> {
                handleConfirmOverwriteExistingPasskeyClick()
            }

            VaultAddEditAction.Common.UserVerificationSuccess -> {
                handleUserVerificationSuccess()
            }

            VaultAddEditAction.Common.UserVerificationLockOut -> {
                handleUserVerificationLockOut()
            }

            VaultAddEditAction.Common.UserVerificationFail -> {
                handleUserVerificationFail()
            }

            VaultAddEditAction.Common.UserVerificationCancelled -> {
                handleUserVerificationCancelled()
            }

            is VaultAddEditAction.Common.CredentialErrorDialogDismissed -> {
                handleCredentialErrorDialogDismissed(action)
            }

            VaultAddEditAction.Common.UserVerificationNotSupported -> {
                handleUserVerificationNotSupported()
            }

            is VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit -> {
                handleMasterPasswordFido2VerificationSubmit(action)
            }

            VaultAddEditAction.Common.RetryFido2PasswordVerificationClick -> {
                handleRetryFido2PasswordVerificationClick()
            }

            is VaultAddEditAction.Common.PinFido2VerificationSubmit -> {
                handlePinFido2VerificationSubmit(action)
            }

            VaultAddEditAction.Common.RetryFido2PinVerificationClick -> {
                handleRetryFido2PinVerificationClick()
            }

            is VaultAddEditAction.Common.PinFido2SetUpSubmit -> handlePinFido2SetUpSubmit(action)
            VaultAddEditAction.Common.PinFido2SetUpRetryClick -> handlePinFido2SetUpRetryClick()

            VaultAddEditAction.Common.DismissFido2VerificationDialogClick -> {
                handleDismissFido2VerificationDialogClick()
            }

            is VaultAddEditAction.Common.FolderChange -> handleFolderTextInputChange(action)

            VaultAddEditAction.Common.SelectOrAddFolderForItem -> {
                handleSelectOrAddFolderForItem()
            }

            is VaultAddEditAction.Common.AddNewFolder -> {
                handleAddNewFolder(action)
            }

            VaultAddEditAction.Common.SelectOwnerForItem -> {
                handleSelectOwnerForItem()
            }

            VaultAddEditAction.Common.DismissBottomSheet -> {
                handleDismissBottomSheet()
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleSaveClick() = onContent { content ->
        if (hasValidationErrors(content)) return@onContent

        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
            )
        }

        state.createCredentialRequest?.run {
            createPublicKeyCredentialRequest
                ?.let { createPublicKeyCredentialRequest ->
                    handleCreatePublicKeyCredentialRequest(
                        request = createPublicKeyCredentialRequest,
                        callingAppInfo = this.callingAppInfo,
                        cipherView = content.toCipherView(),
                    )
                    return@onContent
                }
        }

        viewModelScope.launch {
            when (val vaultAddEditType = state.vaultAddEditType) {
                is VaultAddEditType.AddItem -> {
                    val result = content.createCipherForAddAndCloneItemStates()
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
                    val result = content.createCipherForAddAndCloneItemStates()
                    sendAction(VaultAddEditAction.Internal.CreateCipherResultReceive(result))
                }
            }
        }
    }

    private fun hasValidationErrors(content: VaultAddEditState.ViewState.Content): Boolean =
        if (content.common.name.isBlank()) {
            showGenericErrorDialog(
                message = BitwardenString.validation_field_required
                    .asText(BitwardenString.name.asText()),
            )
            true
        } else if (
            content.common.selectedOwnerId != null &&
            content.common.selectedOwner?.collections?.all { !it.isSelected } == true
        ) {
            showGenericErrorDialog(
                message = BitwardenString.select_one_collection.asText(),
            )
            true
        } else if (
            !networkConnectionManager.isNetworkConnected
        ) {
            showDialog(
                dialogState = VaultAddEditState.DialogState.Generic(
                    title = BitwardenString.internet_connection_required_title.asText(),
                    message = BitwardenString.internet_connection_required_message.asText(),
                ),
            )
            true
        } else {
            false
        }

    private fun handleCreatePublicKeyCredentialRequest(
        callingAppInfo: CallingAppInfo,
        request: CreatePublicKeyCredentialRequest,
        cipherView: CipherView,
    ) {
        if (cipherView.isActiveWithFido2Credentials) {
            mutableStateFlow.update {
                it.copy(dialog = VaultAddEditState.DialogState.OverwritePasskeyConfirmationPrompt)
            }
            return
        }

        if (bitwardenCredentialManager.isUserVerified) {
            registerFido2CredentialToCipher(callingAppInfo, request, cipherView)
            return
        }

        when (
            val requirement =
                bitwardenCredentialManager.getUserVerificationRequirement(request)
        ) {
            UserVerificationRequirement.DISCOURAGED -> {
                registerFido2CredentialToCipher(callingAppInfo, request, cipherView)
            }

            else -> {
                sendEvent(
                    VaultAddEditEvent.Fido2UserVerification(
                        isRequired = requirement == UserVerificationRequirement.REQUIRED,
                    ),
                )
            }
        }
    }

    private fun registerFido2CredentialToCipher(
        callingAppInfo: CallingAppInfo,
        request: CreatePublicKeyCredentialRequest,
        cipherView: CipherView,
    ) {
        viewModelScope.launch {
            val userId = authRepository.activeUserId
                ?: run {
                    showCredentialErrorDialog(
                        BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                            .asText(),
                    )
                    return@launch
                }
            val result: Fido2RegisterCredentialResult =
                bitwardenCredentialManager.registerFido2Credential(
                    userId = userId,
                    callingAppInfo = callingAppInfo,
                    createPublicKeyCredentialRequest = request,
                    selectedCipherView = cipherView,
                )
            sendAction(
                VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive(result),
            )
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

    private fun handleConfirmDeleteClick() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddEditState.DialogState.Loading(
                    BitwardenString.soft_deleting.asText(),
                ),
            )
        }
        onContent { content ->
            if (content.common.originalCipher?.id != null) {
                viewModelScope.launch {
                    trySendAction(
                        VaultAddEditAction.Internal.DeleteCipherReceive(
                            result = vaultRepository.softDeleteCipher(
                                cipherId = content.common.originalCipher.id.toString(),
                                cipherView = content.common.originalCipher,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = VaultAddEditEvent.NavigateBack,
        )
    }

    private fun handleDismissDialog() {
        clearDialogState()
    }

    private fun handleInitialAutofillDialogDismissed() {
        settingsRepository.initialAutofillDialogShown = true
        clearDialogState()
    }

    private fun handleHiddenFieldVisibilityChange(
        action: VaultAddEditAction.Common.HiddenFieldVisibilityChange,
    ) {
        onEdit {
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledHiddenFieldVisible(
                        cipherId = it.vaultItemId,
                    ),
                )
            }
        }
    }

    private fun handleConfirmOverwriteExistingPasskeyClick() {
        state
            .createCredentialRequest
            ?.let { request ->
                request.createPublicKeyCredentialRequest
                    ?.let { createPublicKeyCredentialRequest ->
                        onContent { content ->
                            handleCreatePublicKeyCredentialRequest(
                                request = createPublicKeyCredentialRequest,
                                callingAppInfo = request.callingAppInfo,
                                cipherView = content.toCipherView(),
                            )
                        }
                    }
            }
            ?: showCredentialErrorDialog(
                BitwardenString.passkey_operation_failed_because_the_request_is_invalid.asText(),
            )
    }

    private fun handleUserVerificationLockOut() {
        bitwardenCredentialManager.isUserVerified = false
        showCredentialErrorDialog(
            BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
        )
    }

    private fun handleUserVerificationSuccess() {
        bitwardenCredentialManager.isUserVerified = true
        getRequestAndRegisterFido2Credential()
    }

    private fun getRequestAndRegisterFido2Credential() =
        state.createCredentialRequest
            ?.let { request ->
                request.createPublicKeyCredentialRequest
                    ?.let { createPublicKeyCredentialRequest ->
                        onContent { content ->
                            handleCreatePublicKeyCredentialRequest(
                                request = createPublicKeyCredentialRequest,
                                callingAppInfo = request.callingAppInfo,
                                cipherView = content.toCipherView(),
                            )
                        }
                    }
            }
            ?: showCredentialErrorDialog(
                BitwardenString.passkey_operation_failed_because_the_request_is_unsupported
                    .asText(),
            )

    private fun handleUserVerificationFail() {
        bitwardenCredentialManager.isUserVerified = false
        showCredentialErrorDialog(
            BitwardenString.passkey_operation_failed_because_user_could_not_be_verified.asText(),
        )
    }

    private fun handleCredentialErrorDialogDismissed(
        action: VaultAddEditAction.Common.CredentialErrorDialogDismissed,
    ) {
        bitwardenCredentialManager.isUserVerified = false
        clearDialogState()
        sendEvent(
            VaultAddEditEvent.CompleteCredentialRegistration(
                result = CreateCredentialResult.Error(action.message),
            ),
        )
    }

    private fun handleUserVerificationCancelled() {
        bitwardenCredentialManager.isUserVerified = false
        clearDialogState()
        sendEvent(
            VaultAddEditEvent.CompleteCredentialRegistration(
                result = CreateCredentialResult.Cancelled,
            ),
        )
    }

    private fun handleUserVerificationNotSupported() {
        bitwardenCredentialManager.isUserVerified = false

        val activeAccount = authRepository
            .userStateFlow
            .value
            ?.activeAccount
            ?: run {
                showCredentialErrorDialog(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
                return
            }

        if (settingsRepository.isUnlockWithPinEnabled) {
            mutableStateFlow.update {
                it.copy(dialog = VaultAddEditState.DialogState.Fido2PinPrompt)
            }
        } else if (activeAccount.hasMasterPassword) {
            mutableStateFlow.update {
                it.copy(dialog = VaultAddEditState.DialogState.Fido2MasterPasswordPrompt)
            }
        } else {
            // Prompt the user to set up a PIN for their account.
            mutableStateFlow.update {
                it.copy(dialog = VaultAddEditState.DialogState.Fido2PinSetUpPrompt)
            }
        }
    }

    private fun handleMasterPasswordFido2VerificationSubmit(
        action: VaultAddEditAction.Common.MasterPasswordFido2VerificationSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.password)
            sendAction(
                VaultAddEditAction.Internal.ValidateFido2PasswordResultReceive(
                    result = result,
                ),
            )
        }
    }

    private fun handleRetryFido2PasswordVerificationClick() {
        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2MasterPasswordPrompt)
        }
    }

    private fun handlePinFido2VerificationSubmit(
        action: VaultAddEditAction.Common.PinFido2VerificationSubmit,
    ) {
        viewModelScope.launch {
            val result = authRepository.validatePinUserKey(action.pin)
            sendAction(
                VaultAddEditAction.Internal.ValidateFido2PinResultReceive(
                    result = result,
                ),
            )
        }
    }

    private fun handleRetryFido2PinVerificationClick() {
        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinPrompt)
        }
    }

    private fun handlePinFido2SetUpSubmit(action: VaultAddEditAction.Common.PinFido2SetUpSubmit) {
        if (action.pin.isBlank()) {
            mutableStateFlow.update {
                it.copy(dialog = VaultAddEditState.DialogState.Fido2PinSetUpError)
            }
            return
        }

        // There's no need to ask the user whether or not they want to use their master password
        // on login, and shouldRequireMasterPasswordOnRestart is hardcoded to false, because the
        // user can only reach this part of the flow if they have no master password.
        settingsRepository.storeUnlockPin(
            pin = action.pin,
            shouldRequireMasterPasswordOnRestart = false,
        )

        // After storing the PIN, the user can proceed with their original FIDO 2 request.
        handleValidAuthentication()
    }

    private fun handlePinFido2SetUpRetryClick() {
        mutableStateFlow.update {
            it.copy(dialog = VaultAddEditState.DialogState.Fido2PinSetUpPrompt)
        }
    }

    private fun handleDismissFido2VerificationDialogClick() {
        showCredentialErrorDialog(
            BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                .asText(),
        )
    }

    private fun handleSelectOrAddFolderForItem() {
        mutableStateFlow.update {
            it.copy(
                bottomSheetState = VaultAddEditState.BottomSheetState.FolderSelection,
            )
        }
    }

    private fun handleSelectOwnerForItem() {
        mutableStateFlow.update {
            it.copy(
                bottomSheetState = VaultAddEditState.BottomSheetState.OwnerSelection,
            )
        }
    }

    private fun handleDismissBottomSheet() {
        mutableStateFlow.update {
            it.copy(
                bottomSheetState = null,
            )
        }
    }

    private fun handleAddNewFolder(action: VaultAddEditAction.Common.AddNewFolder) {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddEditState.DialogState.Loading(BitwardenString.saving.asText()),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.createFolder(
                FolderView(
                    name = action.newFolderName,
                    id = null,
                    revisionDate = DateTime.now(),
                ),
            )
            sendAction(VaultAddEditAction.Internal.AddFolderResultReceive(result = result))
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
                    (state.viewState as VaultAddEditState.ViewState.Content)
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
                    (state.viewState as VaultAddEditState.ViewState.Content)
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

            CustomFieldAction.REMOVE -> {
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
            commonContent.copy(selectedFolderId = action.folderId)
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
            commonContent.copy(selectedOwnerId = action.ownerId)
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
        sendEvent(VaultAddEditEvent.NavigateToTooltipUri)
    }

    private fun handleCollectionSelect(
        action: VaultAddEditAction.Common.CollectionSelect,
    ) {
        updateCommonContent { currentCommonContentState ->
            currentCommonContentState.copy(
                selectedOwnerId = currentCommonContentState.selectedOwner?.id,
                availableOwners = currentCommonContentState
                    .availableOwners
                    .toUpdatedOwners(
                        selectedCollectionId = action.collection.id,
                        selectedOwnerId = currentCommonContentState.selectedOwner?.id,
                    ),
            )
        }
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

            is VaultAddEditAction.ItemType.LoginType.UriValueChange -> {
                handleLoginUriValueInputChange(action)
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

            is VaultAddEditAction.ItemType.LoginType.RemoveUriClick -> {
                handleLoginRemoveUriClick(action)
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

            is VaultAddEditAction.ItemType.LoginType.PasswordVisibilityChange -> {
                handlePasswordVisibilityChange(action)
            }

            VaultAddEditAction.ItemType.LoginType.ClearFido2CredentialClick -> {
                handleLoginClearFido2Credential()
            }

            VaultAddEditAction.ItemType.LoginType.LearnAboutLoginsDismissed -> {
                handleLearnAboutLoginsDismissed()
            }

            VaultAddEditAction.ItemType.LoginType.StartLearnAboutLogins -> {
                handleStartLearnAboutLogins()
            }

            VaultAddEditAction.ItemType.LoginType.AuthenticatorHelpToolTipClick -> {
                handleAuthenticatorHelpToolTipClick()
            }

            VaultAddEditAction.ItemType.LoginType.LearnMoreClick -> {
                handleLearnMoreClick()
            }
        }
    }

    private fun handleLearnMoreClick() {
        sendEvent(VaultAddEditEvent.NavigateToLearnMore)
    }

    private fun handleStartLearnAboutLogins() {
        coachMarkTourCompleted()
        sendEvent(VaultAddEditEvent.StartAddLoginItemCoachMarkTour)
    }

    private fun handleLearnAboutLoginsDismissed() {
        coachMarkTourCompleted()
    }

    private fun coachMarkTourCompleted() {
        firstTimeActionManager.markCoachMarkTourCompleted(
            tourCompleted = CoachMarkTourType.ADD_LOGIN,
        )
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

    private fun handleLoginUriValueInputChange(
        action: VaultAddEditAction.ItemType.LoginType.UriValueChange,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(
                uriList = loginType
                    .uriList
                    .map { uriItem ->
                        if (uriItem.id == action.uriItem.id) {
                            action.uriItem
                        } else {
                            uriItem
                        }
                    },
            )
        }
    }

    private fun handleLoginRemoveUriClick(
        action: VaultAddEditAction.ItemType.LoginType.RemoveUriClick,
    ) {
        updateLoginContent { loginType ->
            loginType.copy(
                uriList = loginType.uriList.filter {
                    it != action.uriItem
                },
            )
        }
    }

    private fun handleLoginOpenUsernameGeneratorClick() {
        sendEvent(
            event = VaultAddEditEvent.NavigateToGeneratorModal(
                generatorMode = GeneratorMode.Modal.Username(
                    website = (state.viewState as? VaultAddEditState.ViewState.Content)
                        ?.website
                        .orEmpty(),
                ),
            ),
        )
    }

    private fun handleLoginPasswordCheckerClick() {
        onLoginType { loginType ->
            mutableStateFlow.update {
                it.copy(
                    dialog = VaultAddEditState.DialogState.Loading(
                        BitwardenString.loading.asText(),
                    ),
                )
            }

            viewModelScope.launch {
                val result = authRepository.getPasswordBreachCount(password = loginType.password)
                sendAction(VaultAddEditAction.Internal.PasswordBreachReceive(result))
            }
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
        clipboardManager.setText(
            text = action.totpKey,
            toastDescriptorOverride = BitwardenString.authenticator_key.asText(),
        )
    }

    private fun handleLoginClearTotpKey() {
        updateLoginContent { loginType ->
            loginType.copy(totp = null)
        }
    }

    private fun handleLoginClearFido2Credential() {
        updateLoginContent { loginType ->
            loginType.copy(fido2CredentialCreationDateTime = null)
        }
        sendEvent(event = VaultAddEditEvent.ShowSnackbar(BitwardenString.passkey_removed.asText()))
    }

    private fun handlePasswordVisibilityChange(
        action: VaultAddEditAction.ItemType.LoginType.PasswordVisibilityChange,
    ) {
        onEdit {
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledPasswordVisible(
                        cipherId = it.vaultItemId,
                    ),
                )
            }
        }
    }

    private fun handleLoginAddNewUriClick() {
        updateLoginContent { loginType ->
            loginType.copy(
                uriList = loginType.uriList + UriItem(
                    id = UUID.randomUUID().toString(),
                    uri = "",
                    match = null,
                    checksum = null,
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

            is VaultAddEditAction.ItemType.CardType.NumberVisibilityChange -> {
                handleNumberVisibilityChange(action)
            }

            is VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange -> {
                handleCardSecurityCodeTextChange(action)
            }

            is VaultAddEditAction.ItemType.CardType.SecurityCodeVisibilityChange -> {
                handleSecurityCodeVisibilityChange(action)
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

    private fun handleNumberVisibilityChange(
        action: VaultAddEditAction.ItemType.CardType.NumberVisibilityChange,
    ) {
        onEdit {
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardNumberVisible(
                        cipherId = it.vaultItemId,
                    ),
                )
            }
        }
    }

    private fun handleCardSecurityCodeTextChange(
        action: VaultAddEditAction.ItemType.CardType.SecurityCodeTextChange,
    ) {
        updateCardContent { it.copy(securityCode = action.securityCode) }
    }

    private fun handleSecurityCodeVisibilityChange(
        action: VaultAddEditAction.ItemType.CardType.SecurityCodeVisibilityChange,
    ) {
        onEdit {
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardCodeVisible(
                        cipherId = it.vaultItemId,
                    ),
                )
            }
        }
    }

    //endregion Card Type Handlers

    //region SSH Key Type Handlers

    private fun handleSshKeyTypeActions(action: VaultAddEditAction.ItemType.SshKeyType) {
        when (action) {
            is VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange -> {
                handlePrivateKeyVisibilityChange(action)
            }
        }
    }

    private fun handlePrivateKeyVisibilityChange(
        action: VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange,
    ) {
        updateSshKeyContent { it.copy(showPrivateKey = action.isVisible) }
    }

    //endregion SSH Key Type Handlers

    //region Internal Type Handlers

    private fun handleInternalActions(action: VaultAddEditAction.Internal) {
        when (action) {
            is VaultAddEditAction.Internal.CreateCipherResultReceive -> {
                handleCreateCipherResultReceive(action)
            }

            is VaultAddEditAction.Internal.UpdateCipherResultReceive -> {
                handleUpdateCipherResultReceive(action)
            }

            is VaultAddEditAction.Internal.DeleteCipherReceive -> handleDeleteCipherReceive(action)
            is VaultAddEditAction.Internal.TotpCodeReceive -> handleVaultTotpCodeReceive(action)
            is VaultAddEditAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultAddEditAction.Internal.DetermineContentStateResultReceive -> {
                handleDetermineContentStateResultReceive(action)
            }

            is VaultAddEditAction.Internal.GeneratorResultReceive -> {
                handleGeneratorResultReceive(action)
            }

            is VaultAddEditAction.Internal.PasswordBreachReceive -> {
                handlePasswordBreachReceive(action)
            }

            is VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive -> {
                handleFido2RegisterCredentialResultReceive(action)
            }

            is VaultAddEditAction.Internal.ValidateFido2PasswordResultReceive -> {
                handleValidateFido2PasswordResultReceive(action)
            }

            is VaultAddEditAction.Internal.ValidateFido2PinResultReceive -> {
                handleValidateFido2PinResultReceive(action)
            }

            is VaultAddEditAction.Internal.ShouldShowAddLoginCoachMarkValueChangeReceive -> {
                handleShouldShowAddLoginCoachMarkValueChange(action)
            }

            is VaultAddEditAction.Internal.AddFolderResultReceive -> handleAddFolderResult(action)
            is VaultAddEditAction.Internal.AvailableFoldersReceive -> {
                handleAvailableFoldersReceive(action)
            }

            is VaultAddEditAction.Internal.SnackbarDataReceived -> {
                handleSnackbarDataReceived(action)
            }
        }
    }

    private fun handleSnackbarDataReceived(
        action: VaultAddEditAction.Internal.SnackbarDataReceived,
    ) {
        sendEvent(VaultAddEditEvent.ShowSnackbar(action.data))
    }

    private fun handleAvailableFoldersReceive(
        action: VaultAddEditAction.Internal.AvailableFoldersReceive,
    ) {
        action
            .folderData
            .data
            ?.let {
                updateCommonContent { commonContent ->
                    commonContent.copy(
                        availableFolders = it.toAvailableFolders(resourceManager = resourceManager),
                    )
                }
            }
    }

    private fun handleShouldShowAddLoginCoachMarkValueChange(
        action: VaultAddEditAction.Internal.ShouldShowAddLoginCoachMarkValueChangeReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                shouldShowCoachMarkTour = action.shouldShowCoachMarkTour,
            )
        }
    }

    private fun handleAddFolderResult(action: VaultAddEditAction.Internal.AddFolderResultReceive) {
        mutableStateFlow.update {
            it.copy(
                dialog = null,
            )
        }
        updateCommonContent {
            it.copy(
                selectedFolderId = (action.result as? CreateFolderResult.Success)?.folderView?.id,
            )
        }
    }

    private fun handleCreateCipherResultReceive(
        action: VaultAddEditAction.Internal.CreateCipherResultReceive,
    ) {
        clearDialogState()

        when (val result = action.createCipherResult) {
            is CreateCipherResult.Error -> {
                showDialog(
                    dialogState = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = result
                            .errorMessage
                            ?.asText()
                            ?: BitwardenString.generic_error_message.asText(),
                        error = result.error,
                    ),
                )
            }

            is CreateCipherResult.Success -> {
                if (state.shouldClearSpecialCircumstance) {
                    specialCircumstanceManager.specialCircumstance = null
                }
                if (state.createCredentialRequest?.createPasswordCredentialRequest != null) {
                    sendEvent(
                        VaultAddEditEvent.CompleteCredentialRegistration(
                            CreateCredentialResult.Success.PasswordCreated,
                        ),
                    )
                } else if (state.shouldExitOnSave) {
                    sendEvent(event = VaultAddEditEvent.ExitApp)
                } else {
                    snackbarRelayManager.sendSnackbarData(
                        data = BitwardenSnackbarData(BitwardenString.new_item_created.asText()),
                        relay = SnackbarRelay.CIPHER_CREATED,
                    )
                    sendEvent(event = VaultAddEditEvent.NavigateBack)
                }
            }
        }
    }

    private fun handleUpdateCipherResultReceive(
        action: VaultAddEditAction.Internal.UpdateCipherResultReceive,
    ) {
        clearDialogState()
        when (val result = action.updateCipherResult) {
            is UpdateCipherResult.Error -> {
                showDialog(
                    dialogState = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = result
                            .errorMessage
                            ?.asText()
                            ?: BitwardenString.generic_error_message.asText(),
                        error = result.error,
                    ),
                )
            }

            is UpdateCipherResult.Success -> {
                specialCircumstanceManager.specialCircumstance = null
                if (state.shouldExitOnSave) {
                    sendEvent(event = VaultAddEditEvent.ExitApp)
                } else {
                    snackbarRelayManager.sendSnackbarData(
                        data = BitwardenSnackbarData(BitwardenString.item_updated.asText()),
                        relay = SnackbarRelay.CIPHER_UPDATED,
                    )
                    sendEvent(event = VaultAddEditEvent.NavigateBack)
                }
            }
        }
    }

    private fun handleDeleteCipherReceive(action: VaultAddEditAction.Internal.DeleteCipherReceive) {
        when (val result = action.result) {
            is DeleteCipherResult.Error -> {
                showDialog(
                    dialogState = VaultAddEditState.DialogState.Generic(
                        message = BitwardenString.generic_error_message.asText(),
                        error = result.error,
                    ),
                )
            }

            DeleteCipherResult.Success -> {
                clearDialogState()
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.item_soft_deleted.asText()),
                    relay = SnackbarRelay.CIPHER_DELETED_SOFT,
                )
                sendEvent(VaultAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleVaultDataReceive(action: VaultAddEditAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultData) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = VaultAddEditState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                viewModelScope.launch {
                    sendAction(
                        VaultAddEditAction.Internal.DetermineContentStateResultReceive(
                            vaultAddEditState = state.determineContentState(
                                vaultData = vaultDataState.data,
                                userData = action.userData,
                            ),
                        ),
                    )
                }
            }

            DataState.Loading -> {
                // Skip loading states for add modes, since this will blow away any initial content
                // or user-selected content.
                if (state.isAddItemMode) return

                mutableStateFlow.update {
                    it.copy(viewState = VaultAddEditState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                viewModelScope.launch {
                    sendAction(
                        VaultAddEditAction.Internal.DetermineContentStateResultReceive(
                            state.determineContentState(
                                vaultData = vaultDataState.data,
                                userData = action.userData,
                            ),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                viewModelScope.launch {
                    sendAction(
                        VaultAddEditAction.Internal.DetermineContentStateResultReceive(
                            state.determineContentState(
                                vaultData = vaultDataState.data,
                                userData = action.userData,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun VaultAddEditState.determineContentState(
        vaultData: VaultData?,
        userData: UserState?,
    ): VaultAddEditState {
        val internalVaultData = vaultData
            ?: VaultData(
                decryptCipherListResult = DecryptCipherListResult(
                    successes = emptyList(),
                    failures = emptyList(),
                ),
                collectionViewList = emptyList(),
                folderViewList = emptyList(),
                sendViewList = emptyList(),
            )
        val isIndividualVaultDisabled = policyManager
            .getActivePolicies(type = PolicyTypeJson.PERSONAL_OWNERSHIP)
            .any()
        return copy(
            viewState = internalVaultData.decryptCipherListResult.successes
                .find { it.id == vaultAddEditType.vaultItemId }
                ?.let {
                    val result = vaultRepository.getCipher(
                        vaultAddEditType.vaultItemId.orEmpty(),
                    )
                    when (result) {
                        GetCipherResult.CipherNotFound -> {
                            Timber.e("Cipher not found")
                            null
                        }

                        is GetCipherResult.Failure -> {
                            Timber.e(result.error, "Failed to decrypt cipher.")
                            null
                        }

                        is GetCipherResult.Success -> result.cipherView
                    }
                }
                .validateCipherOrReturnErrorState(
                    currentAccount = userData?.activeAccount,
                    vaultAddEditType = vaultAddEditType,
                ) { currentAccount, cipherView ->
                    val canDelete = if (cipherView?.permissions?.delete != null) {
                        cipherView.permissions?.delete == true
                    } else {
                        val needsManagePermission = cipherView
                            ?.organizationId
                            ?.let { orgId ->
                                currentAccount
                                    .organizations
                                    .firstOrNull { it.id == orgId }
                                    ?.limitItemDeletion
                            }

                        internalVaultData
                            .collectionViewList
                            .hasDeletePermissionInAtLeastOneCollection(
                                collectionIds = cipherView?.collectionIds,
                                needsManagePermission = needsManagePermission == true,
                            )
                    }

                    val canAssignToCollections = internalVaultData
                        .collectionViewList
                        .canAssignToCollections(cipherView?.collectionIds)

                    // Derive the view state from the current Cipher for Edit mode
                    // or use the current state for Add
                    (cipherView
                        ?.toViewState(
                            isClone = isCloneMode,
                            isIndividualVaultDisabled = isIndividualVaultDisabled,
                            totpData = totpData,
                            resourceManager = resourceManager,
                            clock = clock,
                            canDelete = canDelete,
                            canAssignToCollections = canAssignToCollections,
                        )
                        ?: viewState)
                        .appendFolderAndOwnerData(
                            folderViewList = internalVaultData.folderViewList,
                            collectionViewList = internalVaultData
                                .collectionViewList
                                .filter { !it.readOnly },
                            activeAccount = currentAccount,
                            isIndividualVaultDisabled = isIndividualVaultDisabled,
                            resourceManager = resourceManager,
                        )
                },
        )
    }

    private fun handleDetermineContentStateResultReceive(
        action: VaultAddEditAction.Internal.DetermineContentStateResultReceive,
    ) = mutableStateFlow.update { action.vaultAddEditState }

    private fun handleVaultTotpCodeReceive(action: VaultAddEditAction.Internal.TotpCodeReceive) {
        when (val result = action.totpResult) {
            is TotpCodeResult.Success -> {
                sendEvent(
                    event = VaultAddEditEvent.ShowSnackbar(
                        message = BitwardenString.authenticator_key_added.asText(),
                    ),
                )

                updateLoginContent { loginType ->
                    loginType.copy(totp = result.code)
                }
            }

            is TotpCodeResult.CodeScanningError -> {
                showDialog(
                    dialogState = VaultAddEditState.DialogState.Generic(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.authenticator_key_read_error.asText(),
                        error = result.error,
                    ),
                )
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

    private fun handlePasswordBreachReceive(
        action: VaultAddEditAction.Internal.PasswordBreachReceive,
    ) {
        showDialog(
            dialogState = when (val result = action.result) {
                is BreachCountResult.Error -> {
                    VaultAddEditState.DialogState.Generic(
                        message = BitwardenString.generic_error_message.asText(),
                        error = result.error,
                    )
                }

                is BreachCountResult.Success -> {
                    VaultAddEditState.DialogState.Generic(
                        message = if (result.breachCount > 0) {
                            BitwardenPlurals.password_exposed
                                .asPluralsText(
                                    quantity = result.breachCount,
                                    args = arrayOf(result.breachCount),
                                )
                        } else {
                            BitwardenString.password_safe.asText()
                        },
                    )
                }
            },
        )
    }

    private fun handleFido2RegisterCredentialResultReceive(
        action: VaultAddEditAction.Internal.Fido2RegisterCredentialResultReceive,
    ) {
        clearDialogState()
        when (action.result) {
            is Fido2RegisterCredentialResult.Error -> {
                // Use toast here because we are closing the activity.
                toastManager.show(BitwardenString.an_error_has_occurred)
                sendEvent(
                    VaultAddEditEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Error(
                            action.result.messageResourceId.asText(),
                        ),
                    ),
                )
            }

            is Fido2RegisterCredentialResult.Success -> {
                // Use toast here because we are closing the activity.
                toastManager.show(BitwardenString.item_updated)
                sendEvent(
                    VaultAddEditEvent.CompleteCredentialRegistration(
                        CreateCredentialResult.Success.Fido2CredentialRegistered(
                            responseJson = action.result.responseJson,
                        ),
                    ),
                )
            }
        }
    }

    private fun handleValidateFido2PasswordResultReceive(
        action: VaultAddEditAction.Internal.ValidateFido2PasswordResultReceive,
    ) {
        clearDialogState()

        when (action.result) {
            is ValidatePasswordResult.Error -> {
                showCredentialErrorDialog(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
            }

            is ValidatePasswordResult.Success -> {
                if (action.result.isValid) {
                    handleValidAuthentication()
                } else {
                    handleInvalidAuthentication(
                        errorDialogState = VaultAddEditState.DialogState.Fido2MasterPasswordError,
                    )
                }
            }
        }
    }

    private fun handleValidateFido2PinResultReceive(
        action: VaultAddEditAction.Internal.ValidateFido2PinResultReceive,
    ) {
        clearDialogState()

        when (action.result) {
            is ValidatePinResult.Error -> {
                showCredentialErrorDialog(
                    BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                        .asText(),
                )
            }

            is ValidatePinResult.Success -> {
                if (action.result.isValid) {
                    handleValidAuthentication()
                } else {
                    handleInvalidAuthentication(
                        errorDialogState = VaultAddEditState.DialogState.Fido2PinError,
                    )
                }
            }
        }
    }

    private fun handleInvalidAuthentication(errorDialogState: VaultAddEditState.DialogState) {
        bitwardenCredentialManager.authenticationAttempts += 1
        if (bitwardenCredentialManager.hasAuthenticationAttemptsRemaining()) {
            mutableStateFlow.update {
                it.copy(dialog = errorDialogState)
            }
        } else {
            showCredentialErrorDialog(
                BitwardenString.passkey_operation_failed_because_user_could_not_be_verified
                    .asText(),
            )
        }
    }

    private fun handleValidAuthentication() {
        bitwardenCredentialManager.isUserVerified = true
        bitwardenCredentialManager.authenticationAttempts = 0

        getRequestAndRegisterFido2Credential()
    }

    private fun handleAuthenticatorHelpToolTipClick() {
        sendEvent(VaultAddEditEvent.NavigateToAuthenticatorKeyTooltipUri)
    }
    //endregion Internal Type Handlers

    //region Utility Functions

    private fun clearDialogState() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun showCredentialErrorDialog(message: Text) {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultAddEditState.DialogState.CredentialError(message),
            )
        }
    }

    private fun showGenericErrorDialog(
        message: Text = BitwardenString.generic_error_message.asText(),
    ) {
        showDialog(
            dialogState = VaultAddEditState.DialogState.Generic(
                title = BitwardenString.an_error_has_occurred.asText(),
                message = message,
            ),
        )
    }

    private fun showDialog(dialogState: VaultAddEditState.DialogState?) {
        mutableStateFlow.update { it.copy(dialog = dialogState) }
    }

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

    private inline fun onLoginType(
        crossinline block: (VaultAddEditState.ViewState.Content.ItemType.Login) -> Unit,
    ) {
        onContent { (it.type as? VaultAddEditState.ViewState.Content.ItemType.Login)?.let(block) }
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

    private inline fun updateSshKeyContent(
        crossinline block: (VaultAddEditState.ViewState.Content.ItemType.SshKey) ->
        VaultAddEditState.ViewState.Content.ItemType.SshKey,
    ) {
        updateContent { currentContent ->
            (currentContent.type as? VaultAddEditState.ViewState.Content.ItemType.SshKey)?.let {
                currentContent.copy(
                    type = block(it),
                )
            }
        }
    }

    @Suppress("MaxLineLength")
    private suspend fun VaultAddEditState.ViewState.Content.createCipherForAddAndCloneItemStates(): CreateCipherResult {
        return common.selectedOwner?.collections
            ?.filter { it.isSelected }
            ?.map { it.id }
            ?.let {
                vaultRepository.createCipherInOrganization(
                    cipherView = toCipherView(),
                    collectionIds = it,
                )
            }
            ?: vaultRepository.createCipher(cipherView = toCipherView())
    }

    private fun List<VaultAddEditState.Owner>.toUpdatedOwners(
        selectedOwnerId: String?,
        selectedCollectionId: String,
    ): List<VaultAddEditState.Owner> =
        map { owner ->
            if (owner.id != selectedOwnerId) return@map owner
            owner.copy(
                collections = owner
                    .collections
                    .toUpdatedCollections(selectedCollectionId = selectedCollectionId),
            )
        }

    private fun List<VaultCollection>.toUpdatedCollections(
        selectedCollectionId: String,
    ): List<VaultCollection> =
        map { collection ->
            collection.copy(
                isSelected = if (selectedCollectionId == collection.id) {
                    !collection.isSelected
                } else {
                    collection.isSelected
                },
            )
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
data class VaultAddEditState(
    val vaultAddEditType: VaultAddEditType,
    val cipherType: VaultItemCipherType,
    val viewState: ViewState,
    val dialog: DialogState?,
    val bottomSheetState: BottomSheetState?,
    val shouldShowCloseButton: Boolean = true,
    // Internal
    val shouldExitOnSave: Boolean = false,
    val shouldClearSpecialCircumstance: Boolean = true,
    val totpData: TotpData? = null,
    val createCredentialRequest: CreateCredentialRequest? = null,
    val defaultUriMatchType: UriMatchType,
    private val shouldShowCoachMarkTour: Boolean,
) : Parcelable {

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (vaultAddEditType) {
            is VaultAddEditType.AddItem,
            is VaultAddEditType.CloneItem,
                -> when (cipherType) {
                VaultItemCipherType.LOGIN -> BitwardenString.new_login.asText()
                VaultItemCipherType.CARD -> BitwardenString.new_card.asText()
                VaultItemCipherType.IDENTITY -> BitwardenString.new_identity.asText()
                VaultItemCipherType.SECURE_NOTE -> BitwardenString.new_note.asText()
                VaultItemCipherType.SSH_KEY -> BitwardenString.new_ssh_key.asText()
            }

            is VaultAddEditType.EditItem -> when (cipherType) {
                VaultItemCipherType.LOGIN -> BitwardenString.edit_login.asText()
                VaultItemCipherType.CARD -> BitwardenString.edit_card.asText()
                VaultItemCipherType.IDENTITY -> BitwardenString.edit_identity.asText()
                VaultItemCipherType.SECURE_NOTE -> BitwardenString.edit_note.asText()
                VaultItemCipherType.SSH_KEY -> BitwardenString.edit_ssh_key.asText()
            }
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
    val isAddItemMode: Boolean get() = vaultAddEditType is VaultAddEditType.AddItem

    /**
     * Helper to determine if the UI should display the content in clone mode.
     */
    val isCloneMode: Boolean get() = vaultAddEditType is VaultAddEditType.CloneItem

    /**
     * Helper to determine if the UI should allow deletion of this item.
     */
    val canDelete: Boolean
        get() = (viewState as? ViewState.Content)
            ?.common
            ?.canDelete
            ?: false

    val canAssociateToCollections: Boolean
        get() = (viewState as? ViewState.Content)
            ?.common
            ?.canAssignToCollections
            ?: false

    val shouldShowLearnAboutNewLogins: Boolean
        get() = shouldShowCoachMarkTour &&
            ((viewState as? ViewState.Content)?.type is ViewState.Content.ItemType.Login) &&
            isAddItemMode

    val hasOrganizations: Boolean
        get() = (viewState as? ViewState.Content)
            ?.common
            ?.hasOrganizations
            ?: false

    val shouldShowMoveToOrganization: Boolean
        get() = !isAddItemMode &&
            !isCipherInCollection &&
            hasOrganizations

    /**
     * Enum representing the main type options for the vault, such as LOGIN, CARD, etc.
     *
     * @property labelRes The resource ID of the string that represents the label of each type.
     */
    enum class ItemTypeOption(val labelRes: Int) {
        LOGIN(BitwardenString.type_login),
        CARD(BitwardenString.type_card),
        IDENTITY(BitwardenString.type_identity),
        SECURE_NOTES(BitwardenString.type_secure_note),
        SSH_KEYS(BitwardenString.type_ssh_key),
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
            val isIndividualVaultDisabled: Boolean,
            val previousItemTypes: Map<ItemTypeOption, ItemType> = emptyMap(),
        ) : ViewState() {

            /**
             * Content data that is common for all item types.
             *
             * @property originalCipher The original cipher from the vault that the user is editing.
             * This is only present when editing a pre-existing cipher.
             * @property name Represents the name for the item type. This is an abstract property
             * that must be overridden to save the item.
             * @property isUnlockWithPasswordEnabled Indicates whether the user is allowed to
             * unlock with a password.
             * @property masterPasswordReprompt Indicates if a master password reprompt is required.
             * @property favorite Indicates whether this item is marked as a favorite.
             * @property customFieldData Additional custom fields associated with the item.
             * @property notes Any additional notes or comments associated with the item.
             * @property selectedCollectionId The ID of the collection that this item belongs to.
             * @property selectedFolderId The ID of the folder that this item belongs to.
             * @property availableFolders The list of folders that this item could be added too.
             * @property selectedOwnerId The ID of the owner associated with the item.
             * @property availableOwners A list of available owners.
             * @property hasOrganizations Indicates if the user is part of any organizations.
             * @property canDelete Indicates whether the current user can delete the item.
             * @property canAssignToCollections Indicates whether the current user can assign the
             * item to a collection.
             */
            @Parcelize
            data class Common(
                @IgnoredOnParcel
                val originalCipher: CipherView? = null,
                val name: String = "",
                val isUnlockWithPasswordEnabled: Boolean = true,
                val masterPasswordReprompt: Boolean = false,
                val favorite: Boolean = false,
                val customFieldData: List<Custom> = emptyList(),
                val notes: String = "",
                val selectedCollectionId: String? = null,
                val selectedFolderId: String? = null,
                val availableFolders: List<Folder> = emptyList(),
                val selectedOwnerId: String? = null,
                val availableOwners: List<Owner> = emptyList(),
                val hasOrganizations: Boolean = false,
                val canDelete: Boolean = true,
                val canAssignToCollections: Boolean = true,
            ) : Parcelable {

                /**
                 * Helper to provide the currently selected owner.
                 */
                val selectedOwner: Owner?
                    get() = availableOwners.find { it.id == selectedOwnerId }
                        ?: availableOwners.firstOrNull()

                /**
                 * Helper to provide the currently selected folder.
                 */
                val selectedFolder: Folder?
                    get() = availableFolders.find { it.id == selectedFolderId }
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
                 * A list of all the linked field types supported by this [ItemType].
                 */
                abstract val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>

                /**
                 * Represents the login item information.
                 *
                 * @property username The username required for the login item.
                 * @property password The password required for the login item.
                 * @property uriList The list of URIs associated with the login item.
                 * @property totp The current TOTP (if applicable).
                 * @property canViewPassword Indicates whether the current user can view and copy
                 * passwords associated with the login item.
                 * @property canEditItem Indicates whether the current user can edit the login item.
                 * @property fido2CredentialCreationDateTime Date and time the FIDO 2 credential was
                 * created.
                 */
                @Parcelize
                data class Login(
                    val username: String = "",
                    val password: String = "",
                    val totp: String? = null,
                    val canViewPassword: Boolean = true,
                    val canEditItem: Boolean = true,
                    val uriList: List<UriItem> = listOf(
                        UriItem(
                            id = UUID.randomUUID().toString(),
                            uri = "",
                            match = null,
                            checksum = null,
                        ),
                    ),
                    val fido2CredentialCreationDateTime: Text? = null,
                ) : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.LOGIN

                    override val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>
                        get() = persistentListOf(
                            VaultLinkedFieldType.PASSWORD,
                            VaultLinkedFieldType.USERNAME,
                        )

                    /**
                     * Indicates whether the passkey can or cannot be removed.
                     */
                    val canRemovePasskey: Boolean get() = this.canEditItem && this.canViewPassword
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

                    override val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>
                        get() = persistentListOf(
                            VaultLinkedFieldType.CARDHOLDER_NAME,
                            VaultLinkedFieldType.EXPIRATION_MONTH,
                            VaultLinkedFieldType.EXPIRATION_YEAR,
                            VaultLinkedFieldType.SECURITY_CODE,
                            VaultLinkedFieldType.BRAND,
                            VaultLinkedFieldType.NUMBER,
                        )
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

                    override val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>
                        get() = persistentListOf(
                            VaultLinkedFieldType.TITLE,
                            VaultLinkedFieldType.MIDDLE_NAME,
                            VaultLinkedFieldType.ADDRESS_1,
                            VaultLinkedFieldType.ADDRESS_2,
                            VaultLinkedFieldType.ADDRESS_3,
                            VaultLinkedFieldType.CITY,
                            VaultLinkedFieldType.STATE,
                            VaultLinkedFieldType.POSTAL_CODE,
                            VaultLinkedFieldType.COUNTRY,
                            VaultLinkedFieldType.COMPANY,
                            VaultLinkedFieldType.EMAIL,
                            VaultLinkedFieldType.PHONE,
                            VaultLinkedFieldType.SSN,
                            VaultLinkedFieldType.IDENTITY_USERNAME,
                            VaultLinkedFieldType.PASSPORT_NUMBER,
                            VaultLinkedFieldType.LICENSE_NUMBER,
                            VaultLinkedFieldType.FIRST_NAME,
                            VaultLinkedFieldType.LAST_NAME,
                            VaultLinkedFieldType.FULL_NAME,
                        )
                }

                /**
                 * Represents the `SecureNotes` item type.
                 */
                @Parcelize
                data object SecureNotes : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.SECURE_NOTES
                    override val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>
                        get() = persistentListOf()
                }

                /**
                 * Represents the `SshKey` item type.
                 *
                 * @property publicKey The public key for the SSH key item.
                 * @property privateKey The private key for the SSH key item.
                 * @property fingerprint The fingerprint for the SSH key item.
                 */
                @Parcelize
                data class SshKey(
                    val publicKey: String = "",
                    val privateKey: String = "",
                    val fingerprint: String = "",
                    val showPublicKey: Boolean = false,
                    val showPrivateKey: Boolean = false,
                    val showFingerprint: Boolean = false,
                ) : ItemType() {
                    override val itemTypeOption: ItemTypeOption get() = ItemTypeOption.SSH_KEYS
                    override val vaultLinkedFieldTypes: ImmutableList<VaultLinkedFieldType>
                        get() = persistentListOf()
                }
            }

            /**
             * The first website associated with this item, or null if none exists.
             */
            val website: String? get() = (type as? ItemType.Login)?.uriList?.firstOrNull()?.uri
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
     * Models a folder that can be chosen by the user.
     *
     * @property id the folder id.
     * @property name the folder name.
     */
    @Parcelize
    data class Folder(
        val id: String?,
        val name: String,
    ) : Parcelable

    /**
     * Models an owner that can be chosen by the user.
     *
     * @property id the id of the owner (nullable).
     * @property name the name of the owner.
     * @property collections the collections of the owner.
     */
    @Parcelize
    data class Owner(
        val id: String?,
        val name: String,
        val collections: List<VaultCollection>,
    ) : Parcelable

    /**
     * Displays a bottom sheet.
     */
    sealed class BottomSheetState : Parcelable {
        /**
         * Displays a folder selection bottom sheet.
         */
        @Parcelize
        data object FolderSelection : BottomSheetState()

        /**
         * Displays a owner selection bottom sheet.
         */
        @Parcelize
        data object OwnerSelection : BottomSheetState()
    }

    /**
     * Displays a dialog.
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * Displays a generic dialog to the user.
         */
        @Parcelize
        data class Generic(
            val title: Text? = null,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * Displays a loading dialog to the user.
         */
        @Parcelize
        data class Loading(val label: Text) : DialogState()

        /**
         * Displays the initial autofill dialog to the user.
         */
        @Parcelize
        data object InitialAutofillPrompt : DialogState()

        /**
         * Displays a credential operation error dialog to the user.
         */
        @Parcelize
        data class CredentialError(val message: Text) : DialogState()

        /**
         * Displays the overwrite passkey confirmation prompt to the user.
         */
        @Parcelize
        data object OverwritePasskeyConfirmationPrompt : DialogState()

        /**
         * Displays a dialog to prompt the user for their master password as part of the FIDO 2
         * user verification flow.
         */
        @Parcelize
        data object Fido2MasterPasswordPrompt : DialogState()

        /**
         * Displays a dialog to alert the user that their password for the FIDO 2 user
         * verification flow was incorrect and to retry.
         */
        @Parcelize
        data object Fido2MasterPasswordError : DialogState()

        /**
         * Displays a dialog to prompt the user for their PIN as part of the FIDO 2
         * user verification flow.
         */
        @Parcelize
        data object Fido2PinPrompt : DialogState()

        /**
         * Displays a dialog to alert the user that their PIN for the FIDO 2 user
         * verification flow was incorrect and to retry.
         */
        @Parcelize
        data object Fido2PinError : DialogState()

        /**
         * Displays a dialog to prompt the user to set up a PIN as part of the FIDO 2
         * user verification flow.
         */
        @Parcelize
        data object Fido2PinSetUpPrompt : DialogState()

        /**
         * Displays a dialog to alert the user that the PIN is a required field.
         */
        @Parcelize
        data object Fido2PinSetUpError : DialogState()
    }
}

/**
 * Represents a set of events that can be emitted during the process of adding an item to the vault.
 * Each subclass of this sealed class denotes a distinct event that can occur.
 */
sealed class VaultAddEditEvent {
    /**
     * Shows a snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : VaultAddEditEvent(), BackgroundEvent {
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
     * Leave the application.
     */
    data object ExitApp : VaultAddEditEvent()

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
     * Navigate the user to the tooltip URI.
     */
    data object NavigateToTooltipUri : VaultAddEditEvent()

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

    /**
     * Complete the current credential registration process.
     *
     * @property result the result of FIDO 2 credential registration.
     */
    data class CompleteCredentialRegistration(
        val result: CreateCredentialResult,
    ) : BackgroundEvent, VaultAddEditEvent()

    /**
     * Perform user verification for a FIDO 2 credential operation.
     *
     * @param isRequired When `false`, user verification can be bypassed if it is not supported.
     */
    data class Fido2UserVerification(
        val isRequired: Boolean,
    ) : BackgroundEvent, VaultAddEditEvent()

    /**
     * Start the coach mark guided tour of the add login content.
     */
    data object StartAddLoginItemCoachMarkTour : VaultAddEditEvent()

    /**
     * Navigate the user to the tooltip URI for Authenticator key help.
     */
    data object NavigateToAuthenticatorKeyTooltipUri : VaultAddEditEvent()

    /**
     * Navigate the user to the learn more help page
     */
    data object NavigateToLearnMore : VaultAddEditEvent()
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
         * The user has dismissed the initial autofill dialog.
         */
        data object InitialAutofillDialogDismissed : Common()

        /**
         * The user has clicked the move to organization overflow option.
         */
        data object MoveToOrganizationClick : Common()

        /**
         * The user has clicked the collections overflow option.
         */
        data object CollectionsClick : Common()

        /**
         * The user has confirmed to deleted the cipher.
         */
        data object ConfirmDeleteClick : Common()

        /**
         * The user has confirmed overwriting the existing passkey.
         */
        data object ConfirmOverwriteExistingPasskeyClick : Common()

        /**
         * Fired when the name text input is changed.
         *
         * @property name The new name text.
         */
        data class NameTextChange(val name: String) : Common()

        /**
         * Fired when the folder text input is changed.
         *
         * @property folderId The new folder id.
         */
        data class FolderChange(val folderId: String?) : Common()

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
         * Fired when the owner text input is changed.
         *
         * @property ownerId The new owner id.
         */
        data class OwnershipChange(val ownerId: String?) : Common()

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

        /**
         * The user has selected a collection.
         *
         * @property collection the collection selected.
         */
        data class CollectionSelect(
            val collection: VaultCollection,
        ) : Common()

        /**
         * The user has changed the visibility state of a hidden field.
         *
         * @property isVisible the new visibility state of the hidden field.
         */
        data class HiddenFieldVisibilityChange(val isVisible: Boolean) : Common()

        /**
         * The user has too many failed verification attempts for FIDO operations and can no longer
         * use biometric or device credential verification for some time.
         */
        data object UserVerificationLockOut : Common()

        /**
         * The user has failed verification for FIDO 2 operations.
         */
        data object UserVerificationFail : Common()

        /**
         * The user has successfully verified themself using device biometrics or credentials.
         */
        data object UserVerificationSuccess : Common()

        /**
         * The user has cancelled device verification.
         */
        data object UserVerificationCancelled : Common()

        /**
         * The user has dismissed the credential error dialog.
         */
        data class CredentialErrorDialogDismissed(val message: Text) : Common()

        /**
         * User verification cannot be performed with device biometrics or credentials.
         */
        data object UserVerificationNotSupported : Common()

        /**
         * The user has clicked to submit their master password for FIDO 2 verification.
         */
        data class MasterPasswordFido2VerificationSubmit(
            val password: String,
        ) : Common()

        /**
         * The user has clicked to retry their FIDO 2 password verification.
         */
        data object RetryFido2PasswordVerificationClick : Common()

        /**
         * The user has clicked to submit their PIN for FIDO 2 verification.
         */
        data class PinFido2VerificationSubmit(
            val pin: String,
        ) : Common()

        /**
         * The user has clicked to retry their FIDO 2 PIN verification.
         */
        data object RetryFido2PinVerificationClick : Common()

        /**
         * The user has clicked to submit a PIN to set up for the FIDO 2 user verification flow.
         */
        data class PinFido2SetUpSubmit(
            val pin: String,
        ) : Common()

        /**
         * The user has clicked to retry setting up a PIN for the FIDO 2 user verification flow.
         */
        data object PinFido2SetUpRetryClick : Common()

        /**
         * The user has clicked to dismiss the FIDO 2 password or PIN verification dialog.
         */
        data object DismissFido2VerificationDialogClick : Common()

        /**
         * The user has clicked on folder selection card for the item.
         */
        data object SelectOrAddFolderForItem : Common()

        /**
         * The user has clicked on owner selection card for the item.
         */
        data object SelectOwnerForItem : Common()

        /**
         * The user has dismissed the current bottom sheet.
         */
        data object DismissBottomSheet : Common()

        /**
         * The user has selected to add a new folder to associate with the item.
         */
        data class AddNewFolder(val newFolderName: String) : Common()
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
             * Fired when the URI is changed.
             *
             * @property uriItem The new URI.
             */
            data class UriValueChange(val uriItem: UriItem) : LoginType()

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
             * Represents the action of removing a URI item.
             */
            data class RemoveUriClick(val uriItem: UriItem) : LoginType()

            /**
             * Represents the action to add a new URI field.
             */
            data object AddNewUriClick : LoginType()

            /**
             * Fired when the password's visibility has changed.
             *
             * @property isVisible The new password visibility state.
             */
            data class PasswordVisibilityChange(val isVisible: Boolean) : LoginType()

            /**
             * Represents the action to clear the fido2 credential.
             */
            data object ClearFido2CredentialClick : LoginType()

            /**
             * User has clicked the call to action on the learn about logins card.
             */
            data object StartLearnAboutLogins : LoginType()

            /**
             * User has dismissed the learn about logins card.
             */
            data object LearnAboutLoginsDismissed : LoginType()

            /**
             * User has clicked the call to action on the authenticator help tooltip.
             */
            data object AuthenticatorHelpToolTipClick : LoginType()

            /**
             * User has clicked the call to action on the learn more help link.
             */
            data object LearnMoreClick : LoginType()
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
             * Fired when the number's visibility has changed.
             *
             * @property isVisible The new number visibility state.
             */
            data class NumberVisibilityChange(val isVisible: Boolean) : CardType()

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

            /**
             * Fired when the security code's visibility has changed.
             *
             * @property isVisible The new code visibility state.
             */
            data class SecurityCodeVisibilityChange(val isVisible: Boolean) : CardType()
        }

        /**
         * Represents actions specific to the SSH Key type.
         */
        sealed class SshKeyType : ItemType() {

            /**
             * Fired when the private key's visibility has changed.
             */
            data class PrivateKeyVisibilityChange(val isVisible: Boolean) : SshKeyType()
        }
    }

    /**
     * Models actions that the [VaultAddEditViewModel] itself might send.
     */
    sealed class Internal : VaultAddEditAction() {

        /**
         * Indicates that the password breach results have been received.
         */
        data class PasswordBreachReceive(val result: BreachCountResult) : Internal()

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
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates that the vault item data has been received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
            val userData: UserState?,
        ) : Internal()

        /**
         * Indicates that the vault add edit state has been updated on a background thread.
         */
        data class DetermineContentStateResultReceive(
            val vaultAddEditState: VaultAddEditState,
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

        /**
         * Indicates that the delete cipher result has been received.
         */
        data class DeleteCipherReceive(
            val result: DeleteCipherResult,
        ) : Internal()

        /**
         * Indicates that the FIDO 2 registration result has been received.
         */
        data class Fido2RegisterCredentialResultReceive(
            val result: Fido2RegisterCredentialResult,
        ) : Internal()

        /**
         * Indicates that the result for verifying the user's master password as part of the FIDO 2
         * user verification flow has been received.
         */
        data class ValidateFido2PasswordResultReceive(
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that the result for verifying the user's PIN as part of the FIDO 2
         * user verification flow has been received.
         */
        data class ValidateFido2PinResultReceive(
            val result: ValidatePinResult,
        ) : Internal()

        /**
         * The value for the shouldShowAddLoginCoachMark has changed.
         */
        data class ShouldShowAddLoginCoachMarkValueChangeReceive(
            val shouldShowCoachMarkTour: Boolean,
        ) : Internal()

        /**
         * Received a result for attempting to add a folder.
         */
        data class AddFolderResultReceive(
            val result: CreateFolderResult,
        ) : Internal()

        /**
         * Received an update to the available folders.
         */
        data class AvailableFoldersReceive(
            val folderData: DataState<List<FolderView>>,
        ) : Internal()
    }
}
