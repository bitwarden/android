@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.util.isValidPattern
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.util.validateUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the blocked autofill URIs screen.
 */
@HiltViewModel
class BlockAutoFillViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<BlockAutoFillState, BlockAutoFillEvent, BlockAutoFillAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: BlockAutoFillState(
            dialog = null,
            viewState = BlockAutoFillState.ViewState.Empty,
        ),
) {
    init {
        updateContentWithUris(
            uris = settingsRepository.blockedAutofillUris,
        )
    }

    private fun updateContentWithUris(uris: List<String>) {
        mutableStateFlow.update { currentState ->
            if (uris.isNotEmpty()) {
                currentState.copy(
                    viewState = BlockAutoFillState.ViewState.Content(uris.map { it }),
                )
            } else {
                currentState.copy(
                    viewState = BlockAutoFillState.ViewState.Empty,
                )
            }
        }
    }

    override fun handleAction(action: BlockAutoFillAction) {
        when (action) {
            BlockAutoFillAction.BackClick -> handleCloseClick()
            BlockAutoFillAction.AddUriClick -> handleAddUriClick()
            is BlockAutoFillAction.UriTextChange -> handleUriTextChange(action)
            BlockAutoFillAction.DismissDialog -> handleDismissDialog()
            is BlockAutoFillAction.EditUriClick -> handleEditUriClick(action)
            is BlockAutoFillAction.RemoveUriClick -> handleRemoveUriClick(action)
            is BlockAutoFillAction.SaveUri -> handleSaveUri(action)
        }
    }

    private fun handleAddUriClick() {
        mutableStateFlow.update {
            it.copy(
                dialog = BlockAutoFillState.DialogState.AddEdit(uri = ""),
            )
        }
    }

    private fun handleUriTextChange(action: BlockAutoFillAction.UriTextChange) {
        mutableStateFlow.update { currentState ->
            val currentDialog =
                currentState.dialog as? BlockAutoFillState.DialogState.AddEdit
            currentState.copy(
                dialog = BlockAutoFillState.DialogState.AddEdit(
                    uri = action.uri,
                    originalUri = currentDialog?.originalUri,
                ),
            )
        }
    }

    private fun handleEditUriClick(action: BlockAutoFillAction.EditUriClick) {
        mutableStateFlow.update {
            it.copy(
                dialog = BlockAutoFillState.DialogState.AddEdit(
                    uri = action.uri,
                    originalUri = action.uri,
                ),
            )
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleSaveUri(action: BlockAutoFillAction.SaveUri) {
        val uriList = action.newUri.split(",").map { it.trim() }

        val errorText = uriList
            .filter { uri ->
                uri in settingsRepository.blockedAutofillUris || !uri.isValidPattern()
            }
            .firstNotNullOfOrNull { uri ->
                uri.validateUri(settingsRepository.blockedAutofillUris)
            }

        if (errorText != null) {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    dialog = BlockAutoFillState.DialogState.AddEdit(
                        uri = action.newUri,
                        errorMessage = errorText,
                    ),
                )
            }
            return
        }

        val currentUris = settingsRepository.blockedAutofillUris.toMutableList()
        uriList.forEach { newUri ->
            val uriIndex = currentUris.indexOf(newUri)
            if (uriIndex != -1) {
                currentUris[uriIndex] = newUri
            } else {
                currentUris.add(newUri)
            }
        }

        settingsRepository.blockedAutofillUris = currentUris
        updateContentWithUris(currentUris)
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleRemoveUriClick(action: BlockAutoFillAction.RemoveUriClick) {
        val currentUris = settingsRepository.blockedAutofillUris.toMutableList()
        currentUris.remove(action.uri)

        settingsRepository.blockedAutofillUris = currentUris
        updateContentWithUris(currentUris)
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = BlockAutoFillEvent.NavigateBack,
        )
    }
}

/**
 * Represents the state for block auto fill.
 *
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class BlockAutoFillState(
    val dialog: DialogState? = null,
    val viewState: ViewState,
) : Parcelable {

    /**
     * Representation of the dialog to display on BlockAutoFillScreen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Allows the user to confirm adding or editing URI.
         */
        @Parcelize
        data class AddEdit(
            val uri: String,
            val originalUri: String? = null,
            val errorMessage: Text? = null,
        ) : DialogState() {
            val isEdit: Boolean get() = originalUri != null
        }
    }

    /**
     * Represents the specific view states for the [BlockAutoFillScreen].
     */
    sealed class ViewState : Parcelable {

        /**
         * Represents a content state for the [BlockAutoFillScreen].
         *
         * @property blockedUris The list of blocked URIs.
         */
        @Parcelize
        data class Content(
            val blockedUris: List<String> = emptyList(),
        ) : ViewState()

        /**
         * Represents an empty content state for the [BlockAutoFillScreen].
         */
        @Parcelize
        data object Empty : ViewState()
    }
}

/**
 * Represents a set of events that can be emitted for the block auto fill screen.
 * Each subclass of this sealed class denotes a distinct event that can occur.
 */
sealed class BlockAutoFillEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : BlockAutoFillEvent()
}

/**
 * Represents a set of actions related to the block auto fill screen.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class BlockAutoFillAction {

    /**
     * User clicked BlockAutoFillListItem.
     */
    data class EditUriClick(val uri: String) : BlockAutoFillAction()

    /**
     * User clicked Add uri.
     */
    data object AddUriClick : BlockAutoFillAction()

    /**
     * User updated uri text.
     */
    data class UriTextChange(val uri: String) : BlockAutoFillAction()

    /**
     * User clicked close.
     */
    data object BackClick : BlockAutoFillAction()

    /**
     * User click to save or edit a URI.
     */
    data class SaveUri(val newUri: String) : BlockAutoFillAction()

    /**
     * User click to remove URI.
     */
    data class RemoveUriClick(val uri: String) : BlockAutoFillAction()

    /**
     * User dismissed the currently displayed dialog.
     */
    data object DismissDialog : BlockAutoFillAction()
}
