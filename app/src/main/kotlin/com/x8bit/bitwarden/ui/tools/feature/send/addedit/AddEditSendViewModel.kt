package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.takeUntilLoaded
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AddEditSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.shouldFinishOnComplete
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toSendName
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.util.toViewState
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The maximum size an upload-able file is allowed to be (100 MiB).
 */
private const val MAX_FILE_SIZE_BYTES: Long = 100 * 1024 * 1024

/**
 * View model for the add/edit send screen.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class AddEditSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepo: AuthRepository,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepo: EnvironmentRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val vaultRepo: VaultRepository,
    private val policyManager: PolicyManager,
    private val networkConnectionManager: NetworkConnectionManager,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<AddEditSendState, AddEditSendEvent, AddEditSendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        // Check to see if we are navigating here from an external source
        val specialCircumstance = specialCircumstanceManager.specialCircumstance
        val shareSendType = specialCircumstance.toSendType()
        val args = savedStateHandle.toAddEditSendArgs()
        val sendType = args.sendType
        val addEditSendType = args.addEditSendType
        AddEditSendState(
            sendType = sendType,
            shouldFinishOnComplete = specialCircumstance.shouldFinishOnComplete(),
            isShared = shareSendType != null,
            addEditSendType = addEditSendType,
            viewState = when (addEditSendType) {
                AddEditSendType.AddItem -> AddEditSendState.ViewState.Content(
                    common = AddEditSendState.ViewState.Content.Common(
                        name = specialCircumstance.toSendName().orEmpty(),
                        currentAccessCount = null,
                        maxAccessCount = null,
                        passwordInput = "",
                        noteInput = "",
                        isHideEmailChecked = false,
                        isDeactivateChecked = false,
                        isHideEmailAddressEnabled = !policyManager
                            .getActivePolicies<PolicyInformation.SendOptions>()
                            .any { it.shouldDisableHideEmail ?: false },
                        deletionDate = ZonedDateTime.now(clock).plusWeeks(1),
                        expirationDate = null,
                        sendUrl = null,
                        hasPassword = false,
                    ),
                    selectedType = shareSendType ?: when (sendType) {
                        SendItemType.FILE -> {
                            AddEditSendState.ViewState.Content.SendType.File(
                                uri = null,
                                name = null,
                                displaySize = null,
                                sizeBytes = null,
                            )
                        }

                        SendItemType.TEXT -> {
                            AddEditSendState.ViewState.Content.SendType.Text(
                                input = "",
                                isHideByDefaultChecked = false,
                            )
                        }
                    },
                )

                is AddEditSendType.EditItem -> AddEditSendState.ViewState.Loading
            },
            dialogState = null,
            baseWebSendUrl = environmentRepo.environment.environmentUrlData.baseWebSendUrl,
            policyDisablesSend = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
                .any(),
            isPremium = authRepo.userStateFlow.value?.activeAccount?.isPremium == true,
        )
    },
) {

    init {
        when (val addSendType = state.addEditSendType) {
            AddEditSendType.AddItem -> Unit
            is AddEditSendType.EditItem -> {
                vaultRepo
                    .getSendStateFlow(addSendType.sendItemId)
                    // We'll stop getting updates as soon as we get some loaded data.
                    .takeUntilLoaded()
                    .map { AddEditSendAction.Internal.SendDataReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }
        }
    }

    override fun handleAction(action: AddEditSendAction): Unit = when (action) {
        AddEditSendAction.CopyLinkClick -> handleCopyLinkClick()
        AddEditSendAction.DeleteClick -> handleDeleteClick()
        is AddEditSendAction.FileChoose -> handeFileChose(action)
        AddEditSendAction.RemovePasswordClick -> handleRemovePasswordClick()
        AddEditSendAction.ShareLinkClick -> handleShareLinkClick()
        is AddEditSendAction.CloseClick -> handleCloseClick()
        is AddEditSendAction.DeletionDateChange -> handleDeletionDateChange(action)
        AddEditSendAction.DismissDialogClick -> handleDismissDialogClick()
        is AddEditSendAction.SaveClick -> handleSaveClick()
        is AddEditSendAction.ChooseFileClick -> handleChooseFileClick(action)
        is AddEditSendAction.NameChange -> handleNameChange(action)
        is AddEditSendAction.MaxAccessCountChange -> handleMaxAccessCountChange(action)
        is AddEditSendAction.TextChange -> handleTextChange(action)
        is AddEditSendAction.NoteChange -> handleNoteChange(action)
        is AddEditSendAction.PasswordChange -> handlePasswordChange(action)
        is AddEditSendAction.HideByDefaultToggle -> handleHideByDefaultToggle(action)
        is AddEditSendAction.DeactivateThisSendToggle -> handleDeactivateThisSendToggle(action)
        is AddEditSendAction.HideMyEmailToggle -> handleHideMyEmailToggle(action)
        is AddEditSendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: AddEditSendAction.Internal): Unit = when (action) {
        is AddEditSendAction.Internal.CreateSendResultReceive -> {
            handleCreateSendResultReceive(action)
        }

        is AddEditSendAction.Internal.UpdateSendResultReceive -> {
            handleUpdateSendResultReceive(action)
        }

        is AddEditSendAction.Internal.DeleteSendResultReceive -> {
            handleDeleteSendResultReceive(action)
        }

        is AddEditSendAction.Internal.RemovePasswordResultReceive -> {
            handleRemovePasswordResultReceive(action)
        }

        is AddEditSendAction.Internal.SendDataReceive -> handleSendDataReceive(action)
    }

    private fun handleCreateSendResultReceive(
        action: AddEditSendAction.Internal.CreateSendResultReceive,
    ) {
        when (val result = action.result) {
            is CreateSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result.message?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is CreateSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                navigateBack()
                sendEvent(
                    AddEditSendEvent.ShowShareSheet(
                        message = result.sendView.toSendUrl(state.baseWebSendUrl),
                    ),
                )
            }
        }
    }

    private fun handleUpdateSendResultReceive(
        action: AddEditSendAction.Internal.UpdateSendResultReceive,
    ) {
        when (val result = action.result) {
            is UpdateSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is UpdateSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                navigateBack()
                sendEvent(
                    AddEditSendEvent.ShowShareSheet(
                        message = result.sendView.toSendUrl(state.baseWebSendUrl),
                    ),
                )
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.send_updated.asText()),
                    relay = SnackbarRelay.SEND_UPDATED,
                )
            }
        }
    }

    private fun handleDeleteSendResultReceive(
        action: AddEditSendAction.Internal.DeleteSendResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                navigateBack(isDeleted = true)
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.send_deleted.asText()),
                    relay = SnackbarRelay.SEND_DELETED,
                )
            }
        }
    }

    private fun handleRemovePasswordResultReceive(
        action: AddEditSendAction.Internal.RemovePasswordResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            result.error,
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                updateCommonContent { it.copy(hasPassword = false) }
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    AddEditSendEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.password_removed.asText(),
                        ),
                    ),
                )
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleSendDataReceive(action: AddEditSendAction.Internal.SendDataReceive) {
        when (val sendDataState = action.sendDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = AddEditSendState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = sendDataState
                            .data
                            ?.toViewState(
                                clock = clock,
                                baseWebSendUrl = environmentRepo
                                    .environment
                                    .environmentUrlData
                                    .baseWebSendUrl,
                                isHideEmailAddressEnabled = isHideEmailAddressEnabled,
                            )
                            ?: AddEditSendState.ViewState.Error(
                                message = BitwardenString.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = AddEditSendState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = AddEditSendState.ViewState.Error(
                            message = BitwardenString.internet_connection_required_title
                                .asText()
                                .concat(
                                    " ".asText(),
                                    BitwardenString.internet_connection_required_message.asText(),
                                ),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = sendDataState
                            .data
                            ?.toViewState(
                                clock = clock,
                                baseWebSendUrl = environmentRepo
                                    .environment
                                    .environmentUrlData
                                    .baseWebSendUrl,
                                isHideEmailAddressEnabled = isHideEmailAddressEnabled,
                            )
                            ?: AddEditSendState.ViewState.Error(
                                message = BitwardenString.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleCopyLinkClick() {
        onContent {
            it.common.sendUrl?.let { sendUrl ->
                clipboardManager.setText(
                    text = sendUrl,
                    toastDescriptorOverride = BitwardenString.send_link.asText(),
                )
            }
        }
    }

    private fun handleDeleteClick() {
        onEdit { editItem ->
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        BitwardenString.deleting.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = vaultRepo.deleteSend(editItem.sendItemId)
                sendAction(AddEditSendAction.Internal.DeleteSendResultReceive(result))
            }
        }
    }

    private fun handeFileChose(action: AddEditSendAction.FileChoose) {
        updateFileContent {
            it.copy(
                uri = action.fileData.uri,
                name = action.fileData.fileName,
                sizeBytes = action.fileData.sizeBytes,
            )
        }
    }

    private fun handleRemovePasswordClick() {
        onEdit { editItem ->
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        message = BitwardenString.removing_send_password.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = vaultRepo.removePasswordSend(editItem.sendItemId)
                sendAction(AddEditSendAction.Internal.RemovePasswordResultReceive(result))
            }
        }
    }

    private fun handleShareLinkClick() {
        onContent {
            it.common.sendUrl?.let { sendUrl ->
                sendEvent(AddEditSendEvent.ShowShareSheet(sendUrl))
            }
        }
    }

    private fun handlePasswordChange(action: AddEditSendAction.PasswordChange) {
        updateCommonContent {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleNoteChange(action: AddEditSendAction.NoteChange) {
        updateCommonContent {
            it.copy(noteInput = action.input)
        }
    }

    private fun handleHideMyEmailToggle(action: AddEditSendAction.HideMyEmailToggle) {
        updateCommonContent {
            it.copy(isHideEmailChecked = action.isChecked)
        }
    }

    private fun handleDeactivateThisSendToggle(action: AddEditSendAction.DeactivateThisSendToggle) {
        updateCommonContent {
            it.copy(isDeactivateChecked = action.isChecked)
        }
    }

    private fun handleCloseClick() = navigateBack()

    private fun handleDeletionDateChange(action: AddEditSendAction.DeletionDateChange) {
        updateCommonContent {
            it.copy(deletionDate = action.deletionDate)
        }
    }

    @Suppress("LongMethod")
    private fun handleSaveClick() {
        onContent { content ->
            if (content.common.name.isBlank()) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.validation_field_required.asText(
                                BitwardenString.name.asText(),
                            ),
                        ),
                    )
                }
                return@onContent
            }
            (content.selectedType as? AddEditSendState.ViewState.Content.SendType.File)
                ?.let { fileType ->
                    if (!state.isPremium) {
                        // We should never get here without a premium account, but we do one last
                        // check just in case.
                        mutableStateFlow.update {
                            it.copy(
                                dialogState = AddEditSendState.DialogState.Error(
                                    title = BitwardenString.send.asText(),
                                    message = BitwardenString.send_file_premium_required.asText(),
                                ),
                            )
                        }
                        return@onContent
                    }
                    if (fileType.name.isNullOrBlank()) {
                        mutableStateFlow.update {
                            it.copy(
                                dialogState = AddEditSendState.DialogState.Error(
                                    title = BitwardenString.an_error_has_occurred.asText(),
                                    message = BitwardenString
                                        .you_must_attach_a_file_to_save_this_send
                                        .asText(),
                                ),
                            )
                        }
                        return@onContent
                    }
                    if ((fileType.sizeBytes ?: 0) > MAX_FILE_SIZE_BYTES) {
                        // Must be under 100 MB
                        mutableStateFlow.update {
                            it.copy(
                                dialogState = AddEditSendState.DialogState.Error(
                                    title = BitwardenString.an_error_has_occurred.asText(),
                                    message = BitwardenString.max_file_size.asText(),
                                ),
                            )
                        }
                        return@onContent
                    }
                }
            if (!networkConnectionManager.isNetworkConnected) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddEditSendState.DialogState.Error(
                            title = BitwardenString.internet_connection_required_title.asText(),
                            message = BitwardenString.internet_connection_required_message.asText(),
                        ),
                    )
                }
                return@onContent
            }
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddEditSendState.DialogState.Loading(
                        message = BitwardenString.saving.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                when (val addSendType = state.addEditSendType) {
                    AddEditSendType.AddItem -> {
                        val fileType = content
                            .selectedType as? AddEditSendState.ViewState.Content.SendType.File
                        val result = vaultRepo.createSend(
                            sendView = content.toSendView(clock),
                            fileUri = fileType?.uri,
                        )
                        sendAction(AddEditSendAction.Internal.CreateSendResultReceive(result))
                    }

                    is AddEditSendType.EditItem -> {
                        val result = vaultRepo.updateSend(
                            sendId = addSendType.sendItemId,
                            sendView = content.toSendView(clock),
                        )
                        sendAction(AddEditSendAction.Internal.UpdateSendResultReceive(result))
                    }
                }
            }
        }
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleNameChange(action: AddEditSendAction.NameChange) {
        updateCommonContent {
            it.copy(name = action.input)
        }
    }

    private fun handleTextChange(action: AddEditSendAction.TextChange) {
        updateTextContent {
            it.copy(input = action.input)
        }
    }

    private fun handleHideByDefaultToggle(action: AddEditSendAction.HideByDefaultToggle) {
        updateTextContent {
            it.copy(isHideByDefaultChecked = action.isChecked)
        }
    }

    private fun handleChooseFileClick(action: AddEditSendAction.ChooseFileClick) {
        sendEvent(AddEditSendEvent.ShowChooserSheet(action.isCameraPermissionGranted))
    }

    private fun handleMaxAccessCountChange(action: AddEditSendAction.MaxAccessCountChange) {
        updateCommonContent { common ->
            common.copy(maxAccessCount = action.value.takeUnless { it == 0 })
        }
    }

    private fun navigateBack(isDeleted: Boolean = false) {
        specialCircumstanceManager.specialCircumstance = null
        sendEvent(
            event = if (state.shouldFinishOnComplete) {
                AddEditSendEvent.ExitApp
            } else if (isDeleted) {
                // We need to make sure we don't land on the View Send screen
                // since it has now been deleted.
                AddEditSendEvent.NavigateUpToSearchOrRoot
            } else {
                AddEditSendEvent.NavigateBack
            },
        )
    }

    private val isHideEmailAddressEnabled: Boolean
        get() = !policyManager
            .getActivePolicies<PolicyInformation.SendOptions>()
            .any { it.shouldDisableHideEmail ?: false }

    private inline fun onContent(
        crossinline block: (AddEditSendState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? AddEditSendState.ViewState.Content)?.let(block)
    }

    private inline fun onEdit(
        crossinline block: (AddEditSendType.EditItem) -> Unit,
    ) {
        (state.addEditSendType as? AddEditSendType.EditItem)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            AddEditSendState.ViewState.Content,
        ) -> AddEditSendState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? AddEditSendState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateCommonContent(
        crossinline block: (
            AddEditSendState.ViewState.Content.Common,
        ) -> AddEditSendState.ViewState.Content.Common,
    ) {
        updateContent { it.copy(common = block(it.common)) }
    }

    private inline fun updateFileContent(
        crossinline block: (
            AddEditSendState.ViewState.Content.SendType.File,
        ) -> AddEditSendState.ViewState.Content.SendType.File,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddEditSendState.ViewState.Content.SendType.File)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }

    private inline fun updateTextContent(
        crossinline block: (
            AddEditSendState.ViewState.Content.SendType.Text,
        ) -> AddEditSendState.ViewState.Content.SendType.Text,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddEditSendState.ViewState.Content.SendType.Text)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }
}

/**
 * Models state for the add/edit send screen.
 */
@Parcelize
data class AddEditSendState(
    val sendType: SendItemType,
    val addEditSendType: AddEditSendType,
    val dialogState: DialogState?,
    val viewState: ViewState,
    val shouldFinishOnComplete: Boolean,
    val isShared: Boolean,
    val baseWebSendUrl: String,
    val policyDisablesSend: Boolean,
    val isPremium: Boolean,
) : Parcelable {

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (addEditSendType) {
            AddEditSendType.AddItem -> when (sendType) {
                SendItemType.FILE -> BitwardenString.add_file_send.asText()
                SendItemType.TEXT -> BitwardenString.add_text_send.asText()
            }

            is AddEditSendType.EditItem -> when (sendType) {
                SendItemType.FILE -> BitwardenString.edit_file_send.asText()
                SendItemType.TEXT -> BitwardenString.edit_text_send.asText()
            }
        }

    /**
     * Helper to determine if the policy notice should be displayed.
     */
    val shouldDisplayPolicyWarning: Boolean
        get() = !policyDisablesSend &&
            (viewState as? ViewState.Content)?.common?.isHideEmailAddressEnabled != true

    /**
     * Helper to determine if the UI should display the content in add send mode.
     */
    val isAddMode: Boolean get() = addEditSendType is AddEditSendType.AddItem

    /**
     * Helper to determine if the currently displayed send has a password already set.
     */
    val hasPassword: Boolean
        get() = (viewState as? ViewState.Content)?.common?.hasPassword == true

    /**
     * Represents the specific view states for the [AddEditSendScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [AddEditSendScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [AddEditSendScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [AddEditSendScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val selectedType: SendType,
        ) : ViewState() {
            /**
             * Content data that is common for all item types.
             */
            @Parcelize
            data class Common(
                @IgnoredOnParcel
                val originalSendView: SendView? = null,
                val name: String,
                val currentAccessCount: Int?,
                val maxAccessCount: Int?,
                val passwordInput: String,
                val noteInput: String,
                val isHideEmailChecked: Boolean,
                val isDeactivateChecked: Boolean,
                val isHideEmailAddressEnabled: Boolean,
                val deletionDate: ZonedDateTime,
                val expirationDate: ZonedDateTime?,
                val sendUrl: String?,
                val hasPassword: Boolean,
            ) : Parcelable

            /**
             * Models what type the user is trying to send.
             */
            sealed class SendType : Parcelable {
                /**
                 * Sending a file.
                 */
                @Parcelize
                data class File(
                    val uri: Uri?,
                    val name: String?,
                    val displaySize: String?,
                    val sizeBytes: Long?,
                ) : SendType()

                /**
                 * Sending text.
                 */
                @Parcelize
                data class Text(
                    val input: String,
                    val isHideByDefaultChecked: Boolean,
                ) : SendType()
            }
        }
    }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the add/edit send screen.
 */
sealed class AddEditSendEvent {
    /**
     * Closes the app.
     */
    data object ExitApp : AddEditSendEvent()

    /**
     * Navigate back.
     */
    data object NavigateBack : AddEditSendEvent()

    /**
     * Navigate up to the search screen or the root screen depending where you came from.
     */
    data object NavigateUpToSearchOrRoot : AddEditSendEvent()

    /**
     * Show file chooser sheet.
     */
    data class ShowChooserSheet(val withCameraOption: Boolean) : AddEditSendEvent()

    /**
     * Show share sheet.
     */
    data class ShowShareSheet(
        val message: String,
    ) : BackgroundEvent, AddEditSendEvent()

    /**
     * Show a snackbar.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : AddEditSendEvent()
}

/**
 * Models actions for the add/edit send screen.
 */
sealed class AddEditSendAction {

    /**
     * User has chosen a file to be part of the send.
     */
    data class FileChoose(val fileData: FileData) : AddEditSendAction()

    /**
     * User clicked the remove password button.
     */
    data object RemovePasswordClick : AddEditSendAction()

    /**
     * User clicked the copy link button.
     */
    data object CopyLinkClick : AddEditSendAction()

    /**
     * User clicked the share link button.
     */
    data object ShareLinkClick : AddEditSendAction()

    /**
     * User clicked the delete button.
     */
    data object DeleteClick : AddEditSendAction()

    /**
     * User clicked the close button.
     */
    data object CloseClick : AddEditSendAction()

    /**
     * User clicked to dismiss the current dialog.
     */
    data object DismissDialogClick : AddEditSendAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : AddEditSendAction()

    /**
     * Value of the name field was updated.
     */
    data class NameChange(val input: String) : AddEditSendAction()

    /**
     * Value of the send text field updated.
     */
    data class TextChange(val input: String) : AddEditSendAction()

    /**
     * Value of the password field updated.
     */
    data class PasswordChange(val input: String) : AddEditSendAction()

    /**
     * Value of the note text field updated.
     */
    data class NoteChange(val input: String) : AddEditSendAction()

    /**
     * User clicked the choose file button.
     */
    data class ChooseFileClick(
        val isCameraPermissionGranted: Boolean,
    ) : AddEditSendAction()

    /**
     * User toggled the "hide text by default" toggle.
     */
    data class HideByDefaultToggle(val isChecked: Boolean) : AddEditSendAction()

    /**
     * User incremented the max access count.
     */
    data class MaxAccessCountChange(val value: Int) : AddEditSendAction()

    /**
     * User toggled the "hide my email" toggle.
     */
    data class HideMyEmailToggle(val isChecked: Boolean) : AddEditSendAction()

    /**
     * User toggled the "deactivate this send" toggle.
     */
    data class DeactivateThisSendToggle(val isChecked: Boolean) : AddEditSendAction()

    /**
     * The user changed the deletion date.
     */
    data class DeletionDateChange(val deletionDate: ZonedDateTime) : AddEditSendAction()

    /**
     * Models actions that the [AddEditSendViewModel] itself might send.
     */
    sealed class Internal : AddEditSendAction() {
        /**
         * Indicates a result for creating a send has been received.
         */
        data class CreateSendResultReceive(val result: CreateSendResult) : Internal()

        /**
         * Indicates a result for updating a send has been received.
         */
        data class UpdateSendResultReceive(val result: UpdateSendResult) : Internal()

        /**
         * Indicates a result for removing the password from a send has been received.
         */
        data class RemovePasswordResultReceive(val result: RemovePasswordSendResult) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(val result: DeleteSendResult) : Internal()

        /**
         * Indicates that the send item data has been received.
         */
        data class SendDataReceive(val sendDataState: DataState<SendView?>) : Internal()
    }
}
