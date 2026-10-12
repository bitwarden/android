package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.SendTypeJson
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.EffectiveSendPolicy
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.model.SendPolicyRestriction
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.util.toViewSendViewStateContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the view send screen.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class ViewSendViewModel @Inject constructor(
    private val clipboardManager: BitwardenClipboardManager,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val clock: Clock,
    private val vaultRepository: VaultRepository,
    environmentRepository: EnvironmentRepository,
    featureFlagManager: FeatureFlagManager,
    policyManager: PolicyManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ViewSendState, ViewSendEvent, ViewSendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = savedStateHandle.toViewSendArgs()
        ViewSendState(
            sendType = args.sendType,
            sendId = args.sendId,
            viewState = ViewSendState.ViewState.Loading,
            dialogState = null,
            baseWebSendUrl = environmentRepository.environment.baseWebSendUrl,
            allowedSendTypes = null,
            isSendDisabled = false,
            isSendControlsEnabled = false,
            isSendControlsExistingSendsEnabled = false,
        )
    },
) {
    init {
        vaultRepository
            .getSendStateFlow(sendId = state.sendId)
            .map { ViewSendAction.Internal.SendDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.SEND_UPDATED)
            .map { ViewSendAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        // The effective policy itself depends on the feature flags, so all three are observed
        // together to keep the derived restriction consistent whenever any one of them changes.
        combine(
            policyManager.getEffectiveSendPolicyFlow(),
            featureFlagManager.getFeatureFlagFlow(key = FlagKey.SendControls),
            featureFlagManager.getFeatureFlagFlow(key = FlagKey.SendControlsExistingSends),
        ) { effectiveSendPolicy, isSendControlsEnabled, isSendControlsExistingSendsEnabled ->
            ViewSendAction.Internal.EffectiveSendPolicyReceive(
                effectiveSendPolicy = effectiveSendPolicy,
                isSendControlsEnabled = isSendControlsEnabled,
                isSendControlsExistingSendsEnabled = isSendControlsExistingSendsEnabled,
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ViewSendAction) {
        when (action) {
            ViewSendAction.CloseClick -> handleCloseClick()
            ViewSendAction.CopyClick -> handleCopyClick()
            ViewSendAction.CopyNotesClick -> handleCopyNotesClick()
            ViewSendAction.DeleteClick -> handleDeleteClick()
            ViewSendAction.DialogDismiss -> handleDialogDismiss()
            ViewSendAction.EditClick -> handleEditClick()
            ViewSendAction.MakeACopyClick -> handleMakeACopyClick()
            ViewSendAction.ShareClick -> handleShareClick()
            is ViewSendAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: ViewSendAction.Internal) {
        when (action) {
            is ViewSendAction.Internal.SendDataReceive -> handleSendDataReceive(action)
            is ViewSendAction.Internal.DeleteResultReceive -> handleDeleteResultReceive(action)
            is ViewSendAction.Internal.EffectiveSendPolicyReceive -> {
                handleEffectiveSendPolicyReceive(action)
            }

            is ViewSendAction.Internal.SnackbarDataReceived -> handleSnackbarDataReceived(action)
        }
    }

    private fun handleEffectiveSendPolicyReceive(
        action: ViewSendAction.Internal.EffectiveSendPolicyReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                allowedSendTypes = action.effectiveSendPolicy.allowedSendTypes,
                isSendControlsEnabled = action.isSendControlsEnabled,
                isSendControlsExistingSendsEnabled = action.isSendControlsExistingSendsEnabled,
            )
        }
    }

    private fun handleCloseClick() {
        sendEvent(ViewSendEvent.NavigateBack)
    }

    private fun handleCopyClick() {
        onContent { clipboardManager.setText(text = it.shareLink) }
    }

    private fun handleCopyNotesClick() {
        onContent { clipboardManager.setText(text = it.notes.orEmpty()) }
    }

    private fun handleDeleteClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ViewSendState.DialogState.Loading(
                    BitwardenString.deleting.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.deleteSend(sendId = state.sendId)
            sendAction(ViewSendAction.Internal.DeleteResultReceive(result))
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleEditClick() {
        sendEvent(ViewSendEvent.NavigateToEdit(sendType = state.sendType, sendId = state.sendId))
    }

    private fun handleMakeACopyClick() {
        sendEvent(ViewSendEvent.NavigateToCopy(sendType = state.sendType, sendId = state.sendId))
    }

    private fun handleShareClick() {
        onContent { sendEvent(ViewSendEvent.ShareText(text = it.shareLink.asText())) }
    }

    private fun handleDeleteResultReceive(
        action: ViewSendAction.Internal.DeleteResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ViewSendState.DialogState.Error(
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

            is DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.send_deleted.asText()),
                    relay = SnackbarRelay.SEND_DELETED,
                )
                sendEvent(ViewSendEvent.NavigateBack)
            }
        }
    }

    private fun handleSnackbarDataReceived(action: ViewSendAction.Internal.SnackbarDataReceived) {
        sendEvent(ViewSendEvent.ShowSnackbar(action.data))
    }

    private fun handleSendDataReceive(action: ViewSendAction.Internal.SendDataReceive) {
        when (val dataState = action.sendDataState) {
            is DataState.Error -> sendErrorReceive(dataState = dataState)
            is DataState.Loaded -> sendLoadedReceive(dataState = dataState)
            is DataState.Loading -> sendLoadingReceive()
            is DataState.NoNetwork -> sendNoNetworkReceive(dataState = dataState)
            is DataState.Pending -> sendPendingReceive(dataState = dataState)
        }
    }

    private fun sendLoadedReceive(dataState: DataState.Loaded<SendView?>) {
        dataState
            .data
            ?.let { updateStateWithSendView(sendView = it) }
            ?: updateStateWithErrorMessage(
                message = BitwardenString.missing_send_resync_your_vault.asText(),
            )
    }

    private fun sendLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = ViewSendState.ViewState.Loading) }
    }

    private fun sendErrorReceive(dataState: DataState.Error<SendView?>) {
        dataState
            .data
            ?.let { updateStateWithSendView(sendView = it) }
            ?: updateStateWithErrorMessage(message = BitwardenString.generic_error_message.asText())
    }

    private fun sendNoNetworkReceive(dataState: DataState.NoNetwork<SendView?>) {
        dataState
            .data
            ?.let { updateStateWithSendView(sendView = it) }
            ?: updateStateWithErrorMessage(
                message = BitwardenString.internet_connection_required_title
                    .asText()
                    .concat(
                        " ".asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
            )
    }

    private fun sendPendingReceive(dataState: DataState.Pending<SendView?>) {
        dataState
            .data
            ?.let { updateStateWithSendView(sendView = it) }
            ?: updateStateWithErrorMessage(message = BitwardenString.generic_error_message.asText())
    }

    private fun updateStateWithSendView(sendView: SendView) {
        mutableStateFlow.update {
            it.copy(
                isSendDisabled = sendView.disabled,
                viewState = sendView.toViewSendViewStateContent(
                    baseWebSendUrl = it.baseWebSendUrl,
                    clock = clock,
                ),
            )
        }
    }

    private fun updateStateWithErrorMessage(message: Text) {
        mutableStateFlow.update {
            it.copy(viewState = ViewSendState.ViewState.Error(message = message))
        }
    }

    private fun onContent(block: (ViewSendState.ViewState.Content) -> Unit) {
        (state.viewState as? ViewSendState.ViewState.Content)?.let(block = block)
    }
}

/**
 * Models state for the view send screen.
 *
 * @property allowedSendTypes The types of Sends that are allowed to exist, sourced from
 * [EffectiveSendPolicy.allowedSendTypes]. A `null` value means no type restriction is in effect.
 * @property isSendDisabled Whether the server has marked this Send disabled.
 * @property isSendControlsEnabled Whether the SendControls policy is in effect, which is what makes
 * a disabled Send attributable to that policy.
 * @property isSendControlsExistingSendsEnabled Whether enforcement against Sends that predate the
 * policy is enabled.
 */
@Parcelize
data class ViewSendState(
    val sendType: SendItemType,
    val sendId: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
    val baseWebSendUrl: String,
    val allowedSendTypes: List<SendTypeJson>?,
    val isSendDisabled: Boolean,
    val isSendControlsEnabled: Boolean,
    val isSendControlsExistingSendsEnabled: Boolean,
) : Parcelable {
    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (sendType) {
            SendItemType.FILE -> BitwardenString.view_file_send.asText()
            SendItemType.TEXT -> BitwardenString.view_text_send.asText()
        }

    /**
     * Helper to determine how the SendControls policy restricts this Send, or `null` when it is
     * unrestricted and the screen behaves as it always has.
     *
     * A Send is only treated as policy-restricted while both feature flags are enabled, since
     * `disabled` is also set when the Send is deactivated by hand on another client.
     */
    val policyRestriction: SendPolicyRestriction?
        get() = when {
            !isSendControlsExistingSendsEnabled || !isSendControlsEnabled -> null
            !isSendDisabled -> null
            // A file send is reported as simply non-compliant even when its type is also
            // disallowed, since its attachment cannot be uploaded again either way.
            sendType == SendItemType.FILE -> SendPolicyRestriction.FileNotCompliant
            !isSendTypeAllowed -> SendPolicyRestriction.TypeNotAllowed
            else -> SendPolicyRestriction.CopyRequired
        }

    /**
     * Whether the fab is visible. A policy-restricted Send cannot be edited, so it has no fab.
     */
    val isFabVisible: Boolean
        get() = viewState is ViewState.Content && policyRestriction == null

    /**
     * Whether this Send's type is still allowed by the policy.
     */
    private val isSendTypeAllowed: Boolean
        get() = allowedSendTypes
            ?.any { it.toSendItemType() == sendType }
            ?: true

    /**
     * Represents the specific view states for the view send screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the view send screen.
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the view send screen, signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the view send screen.
         */
        @Parcelize
        data class Content(
            val sendType: SendType,
            val shareLink: String,
            val sendName: String,
            val deletionDate: String,
            val maxAccessCount: Int?,
            val currentAccessCount: Int,
            val notes: String?,
        ) : ViewState() {
            /**
             * Content data specific to a send type.
             */
            sealed class SendType : Parcelable {
                /**
                 * Content data specific to a file send type.
                 */
                @Parcelize
                data class FileType(
                    val fileName: String,
                    val fileSize: String,
                ) : SendType()

                /**
                 * Content data specific to a text send type.
                 */
                @Parcelize
                data class TextType(
                    val textToShare: String,
                ) : SendType()
            }
        }
    }

    /**
     * Represents a dialog displayed on the view send screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog.
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog.
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the view send screen.
 */
sealed class ViewSendEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : ViewSendEvent()

    /**
     * Navigate to the add send screen, pre-filled from the current send.
     */
    data class NavigateToCopy(
        val sendType: SendItemType,
        val sendId: String,
    ) : ViewSendEvent()

    /**
     * Navigate to the edit send screen for the current send.
     */
    data class NavigateToEdit(
        val sendType: SendItemType,
        val sendId: String,
    ) : ViewSendEvent()

    /**
     * Shares the [text] via the share sheet.
     */
    data class ShareText(
        val text: Text,
    ) : ViewSendEvent()

    /**
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : ViewSendEvent(), BackgroundEvent
}

/**
 * Models actions for the view send screen.
 */
sealed class ViewSendAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : ViewSendAction()

    /**
     * The user has clicked the copy button.
     */
    data object CopyClick : ViewSendAction()

    /**
     * The user has clicked the copy notes button.
     */
    data object CopyNotesClick : ViewSendAction()

    /**
     * The user has clicked the delete button.
     */
    data object DeleteClick : ViewSendAction()

    /**
     * The user has dismissed the dialog.
     */
    data object DialogDismiss : ViewSendAction()

    /**
     * The user has clicked the edit button.
     */
    data object EditClick : ViewSendAction()

    /**
     * The user clicked the make a copy button on the policy restriction warning.
     */
    data object MakeACopyClick : ViewSendAction()

    /**
     * The user has clicked the share button.
     */
    data object ShareClick : ViewSendAction()

    /**
     * Models actions that the ViewModel itself might send.
     */
    sealed class Internal : ViewSendAction() {
        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteResultReceive(val result: DeleteSendResult) : Internal()

        /**
         * Indicates that the effective Send policy, or either feature flag it depends on, has
         * changed.
         */
        data class EffectiveSendPolicyReceive(
            val effectiveSendPolicy: EffectiveSendPolicy,
            val isSendControlsEnabled: Boolean,
            val isSendControlsExistingSendsEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates that the send item data has been received.
         */
        data class SendDataReceive(val sendDataState: DataState<SendView?>) : Internal()

        /**
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()
    }
}
