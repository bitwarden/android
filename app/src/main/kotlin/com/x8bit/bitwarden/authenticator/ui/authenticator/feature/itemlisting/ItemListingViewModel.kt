package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * View model responsible for handling user interactions with the item listing screen.
 */
@HiltViewModel
class ItemListingViewModel @Inject constructor(
    authenticatorRepository: AuthenticatorRepository,
) :
    BaseViewModel<ItemListingState, ItemListingEvent, ItemListingAction>(
        initialState = ItemListingState(
            viewState = ItemListingState.ViewState.Loading,
            dialog = null
        )
    ) {

    init {
        authenticatorRepository.getAuthCodesFlow()
            .onEach {
                sendAction(ItemListingAction.Internal.AuthenticatorDataReceive(it))
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemListingAction) {
        when (action) {
            ItemListingAction.AddItemClick -> sendEvent(ItemListingEvent.NavigateToAddItem)
            ItemListingAction.BackClick -> sendEvent(ItemListingEvent.NavigateBack)

            is ItemListingAction.ItemClick -> sendEvent(ItemListingEvent.NavigateToItem(action.id))
            ItemListingAction.DialogDismiss -> handleDialogDismiss()
            is ItemListingAction.Internal.AuthenticatorDataReceive -> handleAuthenticatorDataReceive(
                action
            )
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleAuthenticatorDataReceive(action: ItemListingAction.Internal.AuthenticatorDataReceive) {
        when (val authenticatorData = action.authenticatorData) {
            is DataState.Error -> authenticatorErrorReceive(authenticatorData)
            is DataState.Loaded -> authenticatorDataLoadedReceive(authenticatorData)
            DataState.Loading -> authenticatorDataLoadingReceive(authenticatorData)
            is DataState.NoNetwork -> authenticatorNoNetworkReceive(authenticatorData)
            is DataState.Pending -> authenticatorPendingReceive()
        }
    }

    private fun authenticatorErrorReceive(authenticatorData: DataState<List<VerificationCodeItem>>) {
        mutableStateFlow.update {
            if (authenticatorData.data != null) {
                val viewState = updateViewState(authenticatorData.data)

                it.copy(
                    viewState = viewState,
                    dialog = ItemListingState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    )
                )
            } else {
                it.copy(
                    viewState = ItemListingState.ViewState.Error(
                        R.string.generic_error_message.asText(),
                    )
                )
            }
        }
    }

    private fun authenticatorDataLoadedReceive(authenticatorData: DataState<List<VerificationCodeItem>>) {
        if (state.dialog == ItemListingState.DialogState.Syncing) {
            sendEvent(ItemListingEvent.ShowToast(R.string.syncing_complete.asText()))
        }
        mutableStateFlow.update {
            it.copy(
                viewState = updateViewState(authenticatorData.data),
                dialog = null
            )
        }
    }

    private fun authenticatorDataLoadingReceive(authenticatorData: DataState<List<VerificationCodeItem>>) {
        mutableStateFlow.update {
            it.copy(
                viewState = updateViewState(authenticatorData.data),
                dialog = ItemListingState.DialogState.Syncing
            )
        }
    }

    private fun authenticatorNoNetworkReceive(authenticatorData: DataState<List<VerificationCodeItem>>) {
        mutableStateFlow.update {
            it.copy(
                viewState = updateViewState(authenticatorData.data),
                dialog = ItemListingState.DialogState.Error(
                    title = R.string.internet_connection_required_title.asText(),
                    message = R.string.internet_connection_required_message.asText(),
                )
            )
        }
    }

    private fun authenticatorPendingReceive() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.Loading
            )
        }
    }

    private fun updateViewState(verificationCodeItems: List<VerificationCodeItem>?) =
        if (verificationCodeItems.isNullOrEmpty()) {
            ItemListingState.ViewState.NoItems
        } else {
            ItemListingState.ViewState.Content(verificationCodeItems.toDisplayItems())
        }
}

/**
 * Represents the state for displaying the item listing.
 *
 * @property viewState Current state of the [ItemListingScreen].
 * @property dialog State of the dialog show on the [ItemListingScreen]. `null` if no dialog is
 * shown.
 */
@Parcelize
data class ItemListingState(
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Represents the different view states of the [ItemListingScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Represents the [ItemListingScreen] data is processing.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a state where the [ItemListingScreen] has no items to display.
         */
        @Parcelize
        data object NoItems : ViewState()

        /**
         * Represents a loaded content state for the [ItemListingScreen].
         */
        @Parcelize
        data class Content(
            val itemList: List<VerificationCodeDisplayItem>,
        ) : ViewState()

        /**
         * Represents an error state for the [ItemListingScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()
    }

    /**
     * Display a dialog on the [ItemListingScreen].
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays the syncing dialog to the user.
         */
        @Parcelize
        data object Syncing : DialogState()

        /**
         * Displays a generic error dialog to the user.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Represents a set of events related to viewing the item listing.
 */
sealed class ItemListingEvent {
    /**
     * Dismisses the pull-to-refresh indicator.
     */
    data object DismissPullToRefresh : ItemListingEvent()

    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateBack : ItemListingEvent()

    /**
     * Navigate to the Add Item screen.
     */
    data object NavigateToAddItem : ItemListingEvent()

    /**
     * Navigate to the View Item screen.
     */
    data class NavigateToItem(
        val id: String,
    ) : ItemListingEvent()

    /**
     * Navigate to the Edit Item screen.
     */
    data class NavigateToEditItem(
        val id: String,
    ) : ItemListingEvent()

    /**
     * Show a Toast with [message].
     */
    data class ShowToast(
        val message: Text,
    ) : ItemListingEvent()
}

/**
 * Represents a set of actions related to viewing the authenticator item listing.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class ItemListingAction {

    /**
     * The user clicked the back button.
     */
    data object BackClick : ItemListingAction()

    /**
     * The user clicked the Add Item button.
     */
    data object AddItemClick : ItemListingAction()

    /**
     * The user clicked a list item.
     */
    data class ItemClick(val id: String) : ItemListingAction()

    /**
     * The user dismissed the dialog.
     */
    data object DialogDismiss : ItemListingAction()

    /**
     * Models actions that [ItemListingScreen] itself may send.
     */
    sealed class Internal : ItemListingAction() {

        /**
         * Indicates authenticator item listing data has been received.
         */
        data class AuthenticatorDataReceive(
            val authenticatorData: DataState<List<VerificationCodeItem>>,
        ) : Internal()
    }
}

/**
 * The data for the verification code item to displayed.
 */
@Parcelize
data class VerificationCodeDisplayItem(
    val id: String,
    val label: String,
    val supportingLabel: String?,
    val timeLeftSeconds: Int,
    val periodSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(R.drawable.ic_login_item),
) : Parcelable

private fun List<VerificationCodeItem>.toDisplayItems() = this.map {
    VerificationCodeDisplayItem(
        id = it.id,
        label = it.name,
        authCode = it.code,
        supportingLabel = it.username,
        periodSeconds = it.periodSeconds,
        timeLeftSeconds = it.timeLeftSeconds,
    )
}
