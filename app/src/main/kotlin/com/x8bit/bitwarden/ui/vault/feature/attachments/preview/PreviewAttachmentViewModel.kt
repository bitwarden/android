package com.x8bit.bitwarden.ui.vault.feature.attachments.preview

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.repository.util.takeUntilLoaded
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.platform.util.firstWithTimeoutOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val KEY_TEMP_ATTACHMENT = "tempAttachmentFile"
private const val GET_CIPHER_DURATION = 5_000L

/**
 * ViewModel responsible for handling user interactions in the attachments screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class PreviewAttachmentViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val vaultRepository: VaultRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<PreviewAttachmentState, PreviewAttachmentEvent, PreviewAttachmentAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = savedStateHandle.toPreviewAttachmentArgs()
        val isPreviewable = args.fileName.isPreviewable
        PreviewAttachmentState(
            cipherId = args.cipherId,
            attachmentId = args.attachmentId,
            fileName = args.fileName,
            isPreviewable = isPreviewable,
            viewState = if (isPreviewable) {
                PreviewAttachmentState.ViewState.Loading()
            } else {
                PreviewAttachmentState.ViewState.Error(
                    message = BitwardenString
                        .preview_not_available_for_files
                        .asText(args.fileName.fileExtension),
                )
            },
            dialogState = null,
        )
    },
) {
    /**
     * Reference to a temporary attachment saved in cache.
     */
    private var temporaryAttachmentData: File?
        get() = savedStateHandle[KEY_TEMP_ATTACHMENT]
        set(value) {
            savedStateHandle[KEY_TEMP_ATTACHMENT] = value
        }

    private val refreshDataFlow: MutableSharedFlow<Unit> =
        bufferedMutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        refreshDataFlow
            .filter {
                // Don't bother retrieving the file is we cannot display it.
                state.isPreviewable
            }
            .flatMapLatest {
                vaultRepository
                    .getVaultItemStateFlow(state.cipherId)
                    .takeUntilLoaded()
            }
            .map { PreviewAttachmentAction.Internal.CipherReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PreviewAttachmentAction) {
        when (action) {
            PreviewAttachmentAction.BackClick -> handleBackClick()
            PreviewAttachmentAction.CloseClick -> handleCloseClick()
            PreviewAttachmentAction.DismissDialog -> handleDismissDialog()
            PreviewAttachmentAction.DownloadClick -> handleDownloadClick()
            PreviewAttachmentAction.BitmapRenderComplete -> handleBitmapRenderComplete()
            PreviewAttachmentAction.BitmapRenderError -> handleBitmapRenderError()
            PreviewAttachmentAction.FileMissing -> handleFileMissing()
            is PreviewAttachmentAction.AttachmentFileLocationReceive -> {
                handleAttachmentFileLocationReceive(action)
            }

            PreviewAttachmentAction.NoAttachmentFileLocationReceive -> {
                handleNoAttachmentFileLocationReceive()
            }

            is PreviewAttachmentAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleAttachmentFileLocationReceive(
        action: PreviewAttachmentAction.AttachmentFileLocationReceive,
    ) {
        val file = temporaryAttachmentData ?: return
        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Loading(
                    message = BitwardenString.saving.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = fileManager.fileToUri(fileUri = action.uri, file = file)
            sendAction(
                PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk(
                    isSaved = result,
                    file = file,
                ),
            )
        }
    }

    private fun handleNoAttachmentFileLocationReceive() {
        viewModelScope.launch {
            temporaryAttachmentData?.let { fileManager.delete(it) }
        }

        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.unable_to_save_attachment.asText(),
                ),
            )
        }
    }

    private fun handleInternalAction(action: PreviewAttachmentAction.Internal) {
        when (action) {
            is PreviewAttachmentAction.Internal.AttachmentFileReceive -> {
                handleAttachmentFileReceive(action)
            }

            is PreviewAttachmentAction.Internal.DownloadAttachmentReceive -> {
                handleDownloadAttachmentReceive(action)
            }

            is PreviewAttachmentAction.Internal.CipherReceive -> handleCipherReceive(action)
            is PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk -> {
                handleAttachmentFinishedSavingToDisk(action)
            }
        }
    }

    private fun handleAttachmentFinishedSavingToDisk(
        action: PreviewAttachmentAction.Internal.AttachmentFinishedSavingToDisk,
    ) {
        viewModelScope.launch { fileManager.delete(action.file) }
        if (action.isSaved) {
            mutableStateFlow.update { it.copy(dialogState = null) }
            sendEvent(
                PreviewAttachmentEvent.ShowSnackbar(
                    message = BitwardenString.save_attachment_success.asText(),
                ),
            )
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialogState = PreviewAttachmentState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.unable_to_save_attachment.asText(),
                    ),
                )
            }
        }
    }

    private fun handleAttachmentFileReceive(
        action: PreviewAttachmentAction.Internal.AttachmentFileReceive,
    ) {
        when (val result = action.result) {
            is DownloadAttachmentResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = PreviewAttachmentState.ViewState.Error(
                            message = result.errorMessage?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DownloadAttachmentResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = PreviewAttachmentState.ViewState.Content(file = result.file),
                    )
                }
            }
        }
    }

    private fun handleDownloadAttachmentReceive(
        action: PreviewAttachmentAction.Internal.DownloadAttachmentReceive,
    ) {
        when (val result = action.result) {
            is DownloadAttachmentResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PreviewAttachmentState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.unable_to_download_file.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is DownloadAttachmentResult.Success -> {
                temporaryAttachmentData = result.file
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    PreviewAttachmentEvent.NavigateToSelectAttachmentSaveLocation(
                        fileName = state.fileName,
                    ),
                )
            }
        }
    }

    private fun handleCipherReceive(action: PreviewAttachmentAction.Internal.CipherReceive) {
        val viewState = when (val dataState = action.cipherDataState) {
            is DataState.Error<CipherView?> -> {
                action.cipherDataState.data ?: run {
                    mutableStateFlow.update {
                        it.copy(dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable)
                    }
                }
                dataState.data.toViewState(
                    errorMessage = BitwardenString.generic_error_message.asText(),
                )
            }

            is DataState.Loaded<CipherView?> -> {
                dataState.data.toViewState(
                    errorMessage = BitwardenString.generic_error_message.asText(),
                )
            }

            DataState.Loading -> PreviewAttachmentState.ViewState.Loading()

            is DataState.NoNetwork<CipherView?> -> {
                dataState.data.toViewState(
                    errorMessage = BitwardenString.internet_connection_required_title
                        .asText()
                        .concat(
                            " ".asText(),
                            BitwardenString.internet_connection_required_message.asText(),
                        ),
                )
            }

            is DataState.Pending<CipherView?> -> {
                dataState.data.toViewState(
                    errorMessage = BitwardenString.generic_error_message.asText(),
                )
            }
        }
        mutableStateFlow.update { it.copy(viewState = viewState) }

        action.cipherDataState.data?.let {
            viewModelScope.launch {
                val result = vaultRepository.downloadAttachment(
                    cipherView = it,
                    attachmentId = state.attachmentId,
                )
                sendAction(PreviewAttachmentAction.Internal.AttachmentFileReceive(result))
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(PreviewAttachmentEvent.NavigateBack)
    }

    private fun handleCloseClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(PreviewAttachmentEvent.NavigateBack)
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleDownloadClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Loading(
                    message = BitwardenString.downloading.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result = vaultRepository
                .getVaultItemStateFlow(itemId = state.cipherId)
                .mapNotNull { it.data }
                .firstWithTimeoutOrNull(timeMillis = GET_CIPHER_DURATION)
                ?.let {
                    vaultRepository.downloadAttachment(
                        cipherView = it,
                        attachmentId = state.attachmentId,
                    )
                }
                ?: DownloadAttachmentResult.Failure(IllegalStateException("Cipher was missing."))

            sendAction(PreviewAttachmentAction.Internal.DownloadAttachmentReceive(result = result))
        }
    }

    private fun handleBitmapRenderComplete() {
        onContent { content ->
            viewModelScope.launch { fileManager.delete(content.file) }
        }
    }

    private fun handleBitmapRenderError() {
        mutableStateFlow.update {
            it.copy(
                viewState = PreviewAttachmentState.ViewState.Error(
                    message = BitwardenString.preview_unavailable_for_this_file.asText(),
                    illustrationRes = BitwardenDrawable.ill_file_error,
                ),
            )
        }
    }

    private fun handleFileMissing() {
        onContent { content ->
            viewModelScope.launch { fileManager.delete(content.file) }
        }
        mutableStateFlow.update {
            it.copy(viewState = PreviewAttachmentState.ViewState.Loading())
        }
        refreshDataFlow.tryEmit(Unit)
    }

    private fun CipherView?.toViewState(
        errorMessage: Text,
    ): PreviewAttachmentState.ViewState =
        this
            ?.let { PreviewAttachmentState.ViewState.Loading() }
            ?: PreviewAttachmentState.ViewState.Error(message = errorMessage)

    private inline fun onContent(
        crossinline block: (PreviewAttachmentState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? PreviewAttachmentState.ViewState.Content)?.let(block)
    }
}

/**
 * Represents the state for previewing an attachment.
 */
@Parcelize
data class PreviewAttachmentState(
    val cipherId: String,
    val attachmentId: String,
    val fileName: String,
    val isPreviewable: Boolean,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the specific view states for the [PreviewAttachmentScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [PreviewAttachmentScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
            @field:DrawableRes val illustrationRes: Int = BitwardenDrawable.ill_file_not_found,
        ) : ViewState()

        /**
         * Loading state for the [PreviewAttachmentScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data class Loading(
            val message: Text = BitwardenString.loading.asText(),
        ) : ViewState()

        /**
         * Represents a loaded content state for the [PreviewAttachmentScreen].
         */
        @Parcelize
        data class Content(
            val file: File,
        ) : ViewState()
    }

    /**
     * Models dialogs that can be shown on the Preview Attachment screen.
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

        /**
         * Represents a failure dialog when unable to decrypt the cipher.
         */
        @Parcelize
        data object PreviewUnavailable : DialogState()
    }
}

/**
 * Represents a set of events related previewing an attachment.
 */
sealed class PreviewAttachmentEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : PreviewAttachmentEvent()

    /**
     * Navigates to save the downloaded file.
     */
    data class NavigateToSelectAttachmentSaveLocation(
        val fileName: String,
    ) : PreviewAttachmentEvent()

    /**
     * Displays the given [data] as a snackbar.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : PreviewAttachmentEvent() {
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
}

/**
 * Represents a set of actions related to previewing an attachment.
 */
sealed class PreviewAttachmentAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : PreviewAttachmentAction()

    /**
     * User clicked the close button on the preview unavailable dialog.
     */
    data object CloseClick : PreviewAttachmentAction()

    /**
     * User has dismissed a dialog.
     */
    data object DismissDialog : PreviewAttachmentAction()

    /**
     * User clicked the download button.
     */
    data object DownloadClick : PreviewAttachmentAction()

    /**
     * The bitmap has been rendered from file.
     */
    data object BitmapRenderComplete : PreviewAttachmentAction()

    /**
     * There was an error during rendering.
     */
    data object BitmapRenderError : PreviewAttachmentAction()

    /**
     * The file no linger exists.
     */
    data object FileMissing : PreviewAttachmentAction()

    /**
     * The user has selected a location to save the file.
     */
    data class AttachmentFileLocationReceive(
        val uri: Uri,
    ) : PreviewAttachmentAction()

    /**
     * The user skipped selecting a location for the attachment file.
     */
    data object NoAttachmentFileLocationReceive : PreviewAttachmentAction()

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal : PreviewAttachmentAction() {
        /**
         * The cipher data has been received.
         */
        data class CipherReceive(
            val cipherDataState: DataState<CipherView?>,
        ) : Internal()

        /**
         * The attachment file has been received for display purposes only.
         */
        data class AttachmentFileReceive(
            val result: DownloadAttachmentResult,
        ) : Internal()

        /**
         * The attachment file has been received saving.
         */
        data class DownloadAttachmentReceive(
            val result: DownloadAttachmentResult,
        ) : Internal()

        /**
         * The attempt to save the temporary [file] attachment to disk has finished. [isSaved]
         * indicates if it was successful.
         */
        data class AttachmentFinishedSavingToDisk(
            val isSaved: Boolean,
            val file: File,
        ) : Internal()
    }
}

private val String.fileExtension: String
    get() = this.lowercase().substringAfterLast(".").uppercase()

private val String.isPreviewable: Boolean
    get() {
        val lowercasedFileName = this.lowercase()
        return lowercasedFileName.endsWith(".png") ||
            lowercasedFileName.endsWith(".jpg") ||
            lowercasedFileName.endsWith(".jpeg") ||
            lowercasedFileName.endsWith(".gif") ||
            lowercasedFileName.endsWith(".webp") ||
            lowercasedFileName.endsWith(".bmp")
    }
