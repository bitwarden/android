package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model.ItemData
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model responsible for handling user interaction with the authenticator item screen.
 */
@HiltViewModel
class ItemViewModel @Inject constructor(
    authenticatorRepository: AuthenticatorRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ItemState, ItemEvent, ItemAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ItemState(
        itemId = ItemArgs(savedStateHandle).itemId,
        viewState = ItemState.ViewState.Loading,
        dialog = null,
    )
) {

    init {
        combine(
            authenticatorRepository.getItemStateFlow(state.itemId),
            authenticatorRepository.getAuthCodeFlow(state.itemId)
        ) { itemState, authCodeState ->
            val totpData = authCodeState.data?.let {
                TotpCodeItemData(
                    periodSeconds = it.periodSeconds,
                    timeLeftSeconds = it.timeLeftSeconds,
                    totpCode = it.totpCode,
                    verificationCode = it.code
                )
            }
            ItemAction.Internal.ItemDataReceive(
                item = itemState.data,
                itemDataState = combineDataStates(itemState, authCodeState) { item, _ ->
                    ItemData(
                        accountName = itemState.data?.name.orEmpty(),
                        totpCodeItemData = totpData
                    )
                }
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemAction) {
        when (action) {
            is ItemAction.CloseClick -> handleCloseClick()
            is ItemAction.ConfirmDeleteClick -> handleConfirmDeleteClick(action)
            is ItemAction.CopyTotpClick -> handleTotpCopyClick(action)
            is ItemAction.DeleteClick -> handleDeleteClick(action)
            is ItemAction.DismissDialogClick -> handleDismissDialogClick()
            is ItemAction.EditClick -> handleEditClick(action)
            is ItemAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleCloseClick() {
        sendEvent(ItemEvent.NavigateBack)
    }

    private fun handleConfirmDeleteClick(action: ItemAction.ConfirmDeleteClick) {
        TODO("Not yet implemented")
    }

    private fun handleTotpCopyClick(action: ItemAction.CopyTotpClick) {
        TODO("Not yet implemented")
    }

    private fun handleDeleteClick(action: ItemAction.DeleteClick) {
        TODO("Not yet implemented")
    }

    private fun handleDismissDialogClick() {
        TODO("Not yet implemented")
    }

    private fun handleEditClick(action: ItemAction.EditClick) {
        TODO("Not yet implemented")
    }

    private fun handleInternalAction(action: ItemAction.Internal) {
        when (action) {
            is ItemAction.Internal.CopyValue -> handleCopyValueAction(action.value)
            is ItemAction.Internal.DeleteItemReceive -> handleDeleteItemReceive(action.deleteItemResult)
            is ItemAction.Internal.ItemDataReceive -> handleItemDataReceive(action)
        }
    }

    private fun handleCopyValueAction(value: String) {
        TODO("Not yet implemented")
    }

    private fun handleDeleteItemReceive(deleteItemResult: DeleteItemResult) {
        TODO("Not yet implemented")
    }

    private fun handleItemDataReceive(action: ItemAction.Internal.ItemDataReceive) {
        val totpItemData = action.itemDataState.data?.totpCodeItemData ?: return
        mutableStateFlow.update {
            it.copy(
                itemId = action.item?.id.orEmpty(),
                viewState = ItemState.ViewState.Content(
                    totpCodeItemData = totpItemData
                )
            )
        }
    }
}

/**
 * Represents the state for displaying an item in the authenticator.
 *
 * @property itemId ID of the item displayed.
 * @property viewState Current state of the [ItemScreen].
 * @property dialog State of the dialog on the [ItemScreen]. `null` if no dialog is shown.
 */
@Parcelize
data class ItemState(
    val itemId: String,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Represents the different view states for the [ItemScreen].
     */
    sealed class ViewState : Parcelable {

        /**
         * Represents an error state for the [ItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Represents the [ItemScreen] content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [ItemScreen].
         */
        @Parcelize
        data class Content(
            val totpCodeItemData: TotpCodeItemData,
        ) : ViewState()
    }

    /**
     * Displays a dialog on the [ItemScreen].
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a generic dialog to the user.
         */
        @Parcelize
        data class Generic(
            val message: String,
        ) : DialogState()

        /**
         * Displays the loading dialog with a [message].
         */
        @Parcelize
        data class Loading(
            val message: String,
        ) : DialogState()

        /**
         * Displays the delete confirmation dialog prompt.
         */
        @Parcelize
        data class DeleteConfirmationPrompt(
            val message: String,
        ) : DialogState()
    }
}

/**
 * Represents a set of events related to viewing an authenticator item.
 */
sealed class ItemEvent {

    /**
     * Navigates back.
     */
    data object NavigateBack : ItemEvent()

    /**
     * Navigate to the edit screen.
     */
    data class NavigateToEdit(val itemId: String) : ItemEvent()

}

/**
 * Represents a set of actions related to viewing an authenticator item.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class ItemAction {

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : ItemAction()

    /**
     * The user has clicked the delete button.
     */
    data object DeleteClick : ItemAction()

    /**
     * The user has confirmed deletion of the item.
     */
    data object ConfirmDeleteClick : ItemAction()

    /**
     * The user has dismissed the displayed item.
     */
    data object DismissDialogClick : ItemAction()

    /**
     * The user has clicked the edit button.
     */
    data object EditClick : ItemAction()

    /**
     * The user has clicked the copy TOTP code button.
     */
    data object CopyTotpClick : ItemAction()

    /**
     * Models actions the [ItemScreen] itself may send.
     */
    sealed class Internal : ItemAction() {

        /**
         * Copies the given value to the clipboard.
         */
        data class CopyValue(val value: String) : Internal()

        /**
         * Indicates that the item data has been received.
         */
        data class ItemDataReceive(
            val item: CipherView?,
            val itemDataState: DataState<ItemData?>,
        ) : Internal()

        /**
         * Indicates that the delete item result has been received.
         */
        data class DeleteItemReceive(
            val deleteItemResult: DeleteItemResult,
        ) : Internal()
    }
}
