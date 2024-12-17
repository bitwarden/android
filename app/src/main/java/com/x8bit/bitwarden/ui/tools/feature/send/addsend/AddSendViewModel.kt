package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.util.getActivePolicies
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.platform.repository.util.takeUntilLoaded
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.BackgroundEvent
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.shouldFinishOnComplete
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendName
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toViewState
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The maximum size an upload-able file is allowed to be (100 MiB).
 */
private const val MAX_FILE_SIZE_BYTES: Long = 100 * 1024 * 1024

/**
 * View model for the new send screen.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class AddSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepo: AuthRepository,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepo: EnvironmentRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val vaultRepo: VaultRepository,
    private val policyManager: PolicyManager,
    private val networkConnectionManager: NetworkConnectionManager,
) : BaseViewModel<AddSendState, AddSendEvent, AddSendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        // Check to see if we are navigating here from an external source
        val specialCircumstance = specialCircumstanceManager.specialCircumstance
        val shareSendType = specialCircumstance.toSendType()
        val sendAddType = AddSendArgs(savedStateHandle).sendAddType
        AddSendState(
            shouldFinishOnComplete = specialCircumstance.shouldFinishOnComplete(),
            isShared = shareSendType != null,
            addSendType = sendAddType,
            viewState = when (sendAddType) {
                AddSendType.AddItem -> AddSendState.ViewState.Content(
                    common = AddSendState.ViewState.Content.Common(
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
                        deletionDate = ZonedDateTime
                            .now(clock)
                            // We want the default time to be midnight, so we remove all values
                            // beyond days
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusWeeks(1),
                        expirationDate = null,
                        sendUrl = null,
                        hasPassword = false,
                    ),
                    selectedType = shareSendType ?: AddSendState.ViewState.Content.SendType.Text(
                        input = "",
                        isHideByDefaultChecked = false,
                    ),
                )

                is AddSendType.EditItem -> AddSendState.ViewState.Loading
            },
            dialogState = null,
            isPremiumUser = authRepo.userStateFlow.value?.activeAccount?.isPremium == true,
            baseWebSendUrl = environmentRepo.environment.environmentUrlData.baseWebSendUrl,
            policyDisablesSend = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
                .any(),
        )
    },
) {

    init {
        when (val addSendType = state.addSendType) {
            AddSendType.AddItem -> Unit
            is AddSendType.EditItem -> {
                vaultRepo
                    .getSendStateFlow(addSendType.sendItemId)
                    // We'll stop getting updates as soon as we get some loaded data.
                    .takeUntilLoaded()
                    .map { AddSendAction.Internal.SendDataReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }
        }

        authRepo
            .userStateFlow
            .map { AddSendAction.Internal.UserStateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AddSendAction): Unit = when (action) {
        AddSendAction.CopyLinkClick -> handleCopyLinkClick()
        AddSendAction.DeleteClick -> handleDeleteClick()
        is AddSendAction.FileChoose -> handeFileChose(action)
        AddSendAction.RemovePasswordClick -> handleRemovePasswordClick()
        AddSendAction.ShareLinkClick -> handleShareLinkClick()
        is AddSendAction.CloseClick -> handleCloseClick()
        is AddSendAction.DeletionDateChange -> handleDeletionDateChange(action)
        is AddSendAction.ExpirationDateChange -> handleExpirationDateChange(action)
        AddSendAction.ClearExpirationDate -> handleClearExpirationDate()
        AddSendAction.DismissDialogClick -> handleDismissDialogClick()
        is AddSendAction.SaveClick -> handleSaveClick()
        is AddSendAction.FileTypeClick -> handleFileTypeClick()
        is AddSendAction.TextTypeClick -> handleTextTypeClick()
        is AddSendAction.ChooseFileClick -> handleChooseFileClick(action)
        is AddSendAction.NameChange -> handleNameChange(action)
        is AddSendAction.MaxAccessCountChange -> handleMaxAccessCountChange(action)
        is AddSendAction.TextChange -> handleTextChange(action)
        is AddSendAction.NoteChange -> handleNoteChange(action)
        is AddSendAction.PasswordChange -> handlePasswordChange(action)
        is AddSendAction.HideByDefaultToggle -> handleHideByDefaultToggle(action)
        is AddSendAction.DeactivateThisSendToggle -> handleDeactivateThisSendToggle(action)
        is AddSendAction.HideMyEmailToggle -> handleHideMyEmailToggle(action)
        is AddSendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: AddSendAction.Internal): Unit = when (action) {
        is AddSendAction.Internal.CreateSendResultReceive -> handleCreateSendResultReceive(action)
        is AddSendAction.Internal.UpdateSendResultReceive -> handleUpdateSendResultReceive(action)
        is AddSendAction.Internal.DeleteSendResultReceive -> handleDeleteSendResultReceive(action)
        is AddSendAction.Internal.RemovePasswordResultReceive -> handleRemovePasswordResultReceive(
            action,
        )

        is AddSendAction.Internal.UserStateReceive -> handleUserStateReceive(action)
        is AddSendAction.Internal.SendDataReceive -> handleSendDataReceive(action)
    }

    private fun handleCreateSendResultReceive(
        action: AddSendAction.Internal.CreateSendResultReceive,
    ) {
        when (val result = action.result) {
            is CreateSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = result.message?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is CreateSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                if (state.isShared) {
                    navigateBack()
                    clipboardManager.setText(result.sendView.toSendUrl(state.baseWebSendUrl))
                } else {
                    navigateBack()
                    sendEvent(
                        AddSendEvent.ShowShareSheet(
                            message = result.sendView.toSendUrl(state.baseWebSendUrl),
                        ),
                    )
                }
            }
        }
    }

    private fun handleUpdateSendResultReceive(
        action: AddSendAction.Internal.UpdateSendResultReceive,
    ) {
        when (val result = action.result) {
            is UpdateSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is UpdateSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                navigateBack()
                sendEvent(
                    AddSendEvent.ShowShareSheet(
                        message = result.sendView.toSendUrl(state.baseWebSendUrl),
                    ),
                )
            }
        }
    }

    private fun handleDeleteSendResultReceive(
        action: AddSendAction.Internal.DeleteSendResultReceive,
    ) {
        when (action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                navigateBack()
                sendEvent(AddSendEvent.ShowToast(message = R.string.send_deleted.asText()))
            }
        }
    }

    private fun handleRemovePasswordResultReceive(
        action: AddSendAction.Internal.RemovePasswordResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                updateCommonContent { it.copy(hasPassword = false) }
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(AddSendEvent.ShowToast(message = R.string.send_password_removed.asText()))
            }
        }
    }

    private fun handleUserStateReceive(action: AddSendAction.Internal.UserStateReceive) {
        mutableStateFlow.update {
            it.copy(isPremiumUser = action.userState?.activeAccount?.isPremium == true)
        }
    }

    @Suppress("LongMethod")
    private fun handleSendDataReceive(action: AddSendAction.Internal.SendDataReceive) {
        when (val sendDataState = action.sendDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = AddSendState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
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
                            ?: AddSendState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = AddSendState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = AddSendState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(
                                    " ".asText(),
                                    R.string.internet_connection_required_message.asText(),
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
                            ?: AddSendState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleCopyLinkClick() {
        onContent {
            it.common.sendUrl?.let { sendUrl -> clipboardManager.setText(text = sendUrl) }
        }
    }

    private fun handleDeleteClick() {
        onEdit {
            mutableStateFlow.update {
                it.copy(dialogState = AddSendState.DialogState.Loading(R.string.deleting.asText()))
            }
            viewModelScope.launch {
                val result = vaultRepo.deleteSend(it.sendItemId)
                sendAction(AddSendAction.Internal.DeleteSendResultReceive(result))
            }
        }
    }

    private fun handeFileChose(action: AddSendAction.FileChoose) {
        updateFileContent {
            it.copy(
                uri = action.fileData.uri,
                name = action.fileData.fileName,
                sizeBytes = action.fileData.sizeBytes,
            )
        }
    }

    private fun handleRemovePasswordClick() {
        onEdit {
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Loading(
                        message = R.string.removing_send_password.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = vaultRepo.removePasswordSend(it.sendItemId)
                sendAction(AddSendAction.Internal.RemovePasswordResultReceive(result))
            }
        }
    }

    private fun handleShareLinkClick() {
        onContent {
            it.common.sendUrl?.let { sendUrl ->
                sendEvent(AddSendEvent.ShowShareSheet(sendUrl))
            }
        }
    }

    private fun handlePasswordChange(action: AddSendAction.PasswordChange) {
        updateCommonContent {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleNoteChange(action: AddSendAction.NoteChange) {
        updateCommonContent {
            it.copy(noteInput = action.input)
        }
    }

    private fun handleHideMyEmailToggle(action: AddSendAction.HideMyEmailToggle) {
        updateCommonContent {
            it.copy(isHideEmailChecked = action.isChecked)
        }
    }

    private fun handleDeactivateThisSendToggle(action: AddSendAction.DeactivateThisSendToggle) {
        updateCommonContent {
            it.copy(isDeactivateChecked = action.isChecked)
        }
    }

    private fun handleCloseClick() = navigateBack()

    private fun handleDeletionDateChange(action: AddSendAction.DeletionDateChange) {
        updateCommonContent {
            it.copy(deletionDate = action.deletionDate)
        }
    }

    private fun handleExpirationDateChange(action: AddSendAction.ExpirationDateChange) {
        updateCommonContent {
            it.copy(expirationDate = action.expirationDate)
        }
    }

    private fun handleClearExpirationDate() {
        updateCommonContent { it.copy(expirationDate = null) }
    }

    @Suppress("LongMethod")
    private fun handleSaveClick() {
        onContent { content ->
            if (content.common.name.isBlank()) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.validation_field_required.asText(
                                R.string.name.asText(),
                            ),
                        ),
                    )
                }
                return@onContent
            }
            if (content.isFileType) {
                val fileType = content.selectedType as AddSendState.ViewState.Content.SendType.File
                if (fileType.name.isNullOrBlank()) {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = AddSendState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.validation_field_required.asText(
                                    R.string.file.asText(),
                                ),
                            ),
                        )
                    }
                    return@onContent
                }
                if ((fileType.sizeBytes ?: 0) > MAX_FILE_SIZE_BYTES) {
                    // Must be under 100 MB
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = AddSendState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.max_file_size.asText(),
                            ),
                        )
                    }
                    return@onContent
                }
            }
            if (!networkConnectionManager.isNetworkConnected) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.internet_connection_required_title.asText(),
                            message = R.string.internet_connection_required_message.asText(),
                        ),
                    )
                }
                return@onContent
            }
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                when (val addSendType = state.addSendType) {
                    AddSendType.AddItem -> {
                        val fileType = content
                            .selectedType
                            as? AddSendState.ViewState.Content.SendType.File
                        val result = vaultRepo.createSend(
                            sendView = content.toSendView(clock),
                            fileUri = fileType?.uri,
                        )
                        sendAction(AddSendAction.Internal.CreateSendResultReceive(result))
                    }

                    is AddSendType.EditItem -> {
                        val result = vaultRepo.updateSend(
                            sendId = addSendType.sendItemId,
                            sendView = content.toSendView(clock),
                        )
                        sendAction(AddSendAction.Internal.UpdateSendResultReceive(result))
                    }
                }
            }
        }
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleNameChange(action: AddSendAction.NameChange) {
        updateCommonContent {
            it.copy(name = action.input)
        }
    }

    private fun handleFileTypeClick() {
        if (state.policyDisablesSend) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Error(
                        title = null,
                        message = R.string.send_disabled_warning.asText(),
                    ),
                )
            }
            return
        }
        if (!state.isPremiumUser) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Error(
                        title = R.string.send.asText(),
                        message = R.string.send_file_premium_required.asText(),
                    ),
                )
            }
            return
        }
        updateContent {
            it.copy(
                selectedType = AddSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                ),
            )
        }
    }

    private fun handleTextTypeClick() {
        updateContent {
            it.copy(
                selectedType = AddSendState.ViewState.Content.SendType.Text(
                    input = "",
                    isHideByDefaultChecked = false,
                ),
            )
        }
    }

    private fun handleTextChange(action: AddSendAction.TextChange) {
        updateTextContent {
            it.copy(input = action.input)
        }
    }

    private fun handleHideByDefaultToggle(action: AddSendAction.HideByDefaultToggle) {
        updateTextContent {
            it.copy(isHideByDefaultChecked = action.isChecked)
        }
    }

    private fun handleChooseFileClick(action: AddSendAction.ChooseFileClick) {
        sendEvent(AddSendEvent.ShowChooserSheet(action.isCameraPermissionGranted))
    }

    private fun handleMaxAccessCountChange(action: AddSendAction.MaxAccessCountChange) {
        updateCommonContent { common ->
            common.copy(maxAccessCount = action.value.takeUnless { it == 0 })
        }
    }

    private fun navigateBack() {
        specialCircumstanceManager.specialCircumstance = null
        sendEvent(
            event = if (state.shouldFinishOnComplete) {
                AddSendEvent.ExitApp
            } else {
                AddSendEvent.NavigateBack
            },
        )
    }

    private val isHideEmailAddressEnabled: Boolean
        get() = !policyManager
            .getActivePolicies<PolicyInformation.SendOptions>()
            .any { it.shouldDisableHideEmail ?: false }

    private inline fun onContent(
        crossinline block: (AddSendState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? AddSendState.ViewState.Content)?.let(block)
    }

    private inline fun onEdit(
        crossinline block: (AddSendType.EditItem) -> Unit,
    ) {
        (state.addSendType as? AddSendType.EditItem)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            AddSendState.ViewState.Content,
        ) -> AddSendState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? AddSendState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateCommonContent(
        crossinline block: (
            AddSendState.ViewState.Content.Common,
        ) -> AddSendState.ViewState.Content.Common,
    ) {
        updateContent { it.copy(common = block(it.common)) }
    }

    private inline fun updateFileContent(
        crossinline block: (
            AddSendState.ViewState.Content.SendType.File,
        ) -> AddSendState.ViewState.Content.SendType.File,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddSendState.ViewState.Content.SendType.File)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }

    private inline fun updateTextContent(
        crossinline block: (
            AddSendState.ViewState.Content.SendType.Text,
        ) -> AddSendState.ViewState.Content.SendType.Text,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddSendState.ViewState.Content.SendType.Text)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }
}

/**
 * Models state for the new send screen.
 */
@Parcelize
data class AddSendState(
    val addSendType: AddSendType,
    val dialogState: DialogState?,
    val viewState: ViewState,
    val shouldFinishOnComplete: Boolean,
    val isPremiumUser: Boolean,
    val isShared: Boolean,
    val baseWebSendUrl: String,
    val policyDisablesSend: Boolean,
) : Parcelable {

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (addSendType) {
            AddSendType.AddItem -> R.string.add_send.asText()
            is AddSendType.EditItem -> R.string.edit_send.asText()
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
    val isAddMode: Boolean get() = addSendType is AddSendType.AddItem

    /**
     * Helper to determine if the currently displayed send has a password already set.
     */
    val hasPassword: Boolean
        get() = (viewState as? ViewState.Content)?.common?.hasPassword == true

    /**
     * Represents the specific view states for the [AddSendScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [AddSendScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [AddSendScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [AddSendScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val selectedType: SendType,
        ) : ViewState() {

            /**
             * Helper method to indicate if the selected type is [SendType.File].
             */
            val isFileType: Boolean get() = selectedType is SendType.File

            /**
             * Helper method to indicate if the selected type is [SendType.Text].
             */
            val isTextType: Boolean get() = selectedType is SendType.Text

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
            ) : Parcelable {
                val dateFormatPattern: String get() = "M/d/yyyy"

                val timeFormatPattern: String get() = "hh:mm a"
            }

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
 * Models events for the new send screen.
 */
sealed class AddSendEvent {
    /**
     * Closes the app.
     */
    data object ExitApp : AddSendEvent()

    /**
     * Navigate back.
     */
    data object NavigateBack : AddSendEvent()

    /**
     * Show file chooser sheet.
     */
    data class ShowChooserSheet(val withCameraOption: Boolean) : AddSendEvent()

    /**
     * Show share sheet.
     */
    data class ShowShareSheet(
        val message: String,
    ) : BackgroundEvent, AddSendEvent()

    /**
     * Show Toast.
     */
    data class ShowToast(val message: Text) : AddSendEvent()
}

/**
 * Models actions for the new send screen.
 */
sealed class AddSendAction {

    /**
     * User has chosen a file to be part of the send.
     */
    data class FileChoose(val fileData: IntentManager.FileData) : AddSendAction()

    /**
     * User clicked the remove password button.
     */
    data object RemovePasswordClick : AddSendAction()

    /**
     * User clicked the copy link button.
     */
    data object CopyLinkClick : AddSendAction()

    /**
     * User clicked the share link button.
     */
    data object ShareLinkClick : AddSendAction()

    /**
     * User clicked the delete button.
     */
    data object DeleteClick : AddSendAction()

    /**
     * User clicked the close button.
     */
    data object CloseClick : AddSendAction()

    /**
     * User clicked to dismiss the current dialog.
     */
    data object DismissDialogClick : AddSendAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : AddSendAction()

    /**
     * Value of the name field was updated.
     */
    data class NameChange(val input: String) : AddSendAction()

    /**
     * User clicked the file type segmented button.
     */
    data object FileTypeClick : AddSendAction()

    /**
     * User clicked the text type segmented button.
     */
    data object TextTypeClick : AddSendAction()

    /**
     * Value of the send text field updated.
     */
    data class TextChange(val input: String) : AddSendAction()

    /**
     * Value of the password field updated.
     */
    data class PasswordChange(val input: String) : AddSendAction()

    /**
     * Value of the note text field updated.
     */
    data class NoteChange(val input: String) : AddSendAction()

    /**
     * User clicked the choose file button.
     */
    data class ChooseFileClick(
        val isCameraPermissionGranted: Boolean,
    ) : AddSendAction()

    /**
     * User toggled the "hide text by default" toggle.
     */
    data class HideByDefaultToggle(val isChecked: Boolean) : AddSendAction()

    /**
     * User incremented the max access count.
     */
    data class MaxAccessCountChange(val value: Int) : AddSendAction()

    /**
     * User toggled the "hide my email" toggle.
     */
    data class HideMyEmailToggle(val isChecked: Boolean) : AddSendAction()

    /**
     * User toggled the "deactivate this send" toggle.
     */
    data class DeactivateThisSendToggle(val isChecked: Boolean) : AddSendAction()

    /**
     * The user changed the deletion date.
     */
    data class DeletionDateChange(val deletionDate: ZonedDateTime) : AddSendAction()

    /**
     * The user changed the expiration date.
     */
    data class ExpirationDateChange(val expirationDate: ZonedDateTime?) : AddSendAction()

    /**
     * The user has cleared the expiration date.
     */
    data object ClearExpirationDate : AddSendAction()

    /**
     * Models actions that the [AddSendViewModel] itself might send.
     */
    sealed class Internal : AddSendAction() {
        /**
         * Indicates what the current [userState] is.
         */
        data class UserStateReceive(val userState: UserState?) : Internal()

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
