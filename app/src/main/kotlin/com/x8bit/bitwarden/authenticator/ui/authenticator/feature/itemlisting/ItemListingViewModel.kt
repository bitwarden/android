package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.ItemListingData
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
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
    settingsRepository: SettingsRepository,
) :
    BaseViewModel<ItemListingState, ItemListingEvent, ItemListingAction>(
        initialState = ItemListingState(
            viewState = ItemListingState.ViewState.Loading,
            dialog = null
        )
    ) {

    init {
        combine(
            authenticatorRepository.getAuthCodesFlow(),
            settingsRepository.authenticatorAlertThresholdSecondsFlow,
        ) { authCodeState, alertThresholdSeconds ->
            when (authCodeState) {
                is DataState.Error -> {
                    DataState.Error(
                        error = authCodeState.error,
                        data = ItemListingData(
                            alertThresholdSeconds = alertThresholdSeconds,
                            authenticatorData = authCodeState.data,
                        )
                    )
                }

                is DataState.Loaded -> {
                    DataState.Loaded(
                        ItemListingData(
                            alertThresholdSeconds = alertThresholdSeconds,
                            authCodeState.data
                        )
                    )
                }

                DataState.Loading -> {
                    DataState.Loading
                }

                is DataState.NoNetwork -> {
                    DataState.NoNetwork(
                        ItemListingData(
                            alertThresholdSeconds = alertThresholdSeconds,
                            authenticatorData = authCodeState.data ?: emptyList()
                        )
                    )
                }

                is DataState.Pending -> {
                    DataState.Pending(
                        ItemListingData(
                            alertThresholdSeconds = alertThresholdSeconds,
                            authCodeState.data
                        )
                    )
                }
            }
        }
            .onEach {
                sendAction(ItemListingAction.Internal.AuthenticatorDataReceive(it))
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemListingAction) {
        when (action) {
            ItemListingAction.ScanQrCodeClick -> sendEvent(ItemListingEvent.NavigateToQrCodeScanner)
            ItemListingAction.EnterSetupKeyClick -> sendEvent(ItemListingEvent.NavigateToManualAddItem)
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
        when (val viewState = action.itemListingDataState) {
            is DataState.Error -> authenticatorErrorReceive(viewState)
            is DataState.Loaded -> authenticatorDataLoadedReceive(viewState.data)
            is DataState.Loading -> authenticatorDataLoadingReceive()
            is DataState.NoNetwork -> authenticatorNoNetworkReceive(viewState.data)
            is DataState.Pending -> authenticatorPendingReceive()
        }
    }

    private fun authenticatorErrorReceive(authenticatorData: DataState.Error<ItemListingData>) {
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

    private fun authenticatorDataLoadedReceive(authenticatorData: ItemListingData) {
        if (state.dialog == ItemListingState.DialogState.Syncing) {
            sendEvent(ItemListingEvent.ShowToast(R.string.syncing_complete.asText()))
        }
        mutableStateFlow.update {
            it.copy(
                viewState = updateViewState(authenticatorData),
                dialog = null
            )
        }
    }

    private fun authenticatorDataLoadingReceive() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.Loading,
                dialog = ItemListingState.DialogState.Syncing
            )
        }
    }

    private fun authenticatorNoNetworkReceive(authenticatorData: ItemListingData?) {
        mutableStateFlow.update {
            it.copy(
                viewState = updateViewState(authenticatorData),
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

    private fun updateViewState(
        itemListingData: ItemListingData?,
    ): ItemListingState.ViewState {
        val items = itemListingData?.authenticatorData ?: return ItemListingState.ViewState.NoItems
        return ItemListingState.ViewState.Content(
            itemList = items.toDisplayItems(itemListingData.alertThresholdSeconds)
        )
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
     * Navigate to the QR Code Scanner screen.
     */
    data object NavigateToQrCodeScanner : ItemListingEvent()

    /**
     * Navigate to the Manual Add Item screen.
     */
    data object NavigateToManualAddItem : ItemListingEvent()

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
     * The user clicked the Scan QR Code button.
     */
    data object ScanQrCodeClick : ItemListingAction()

    /**
     * The user clicked the Enter Setup Key button.
     */
    data object EnterSetupKeyClick : ItemListingAction()

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
            val itemListingDataState: DataState<ItemListingData>,
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
    val alertThresholdSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(R.drawable.ic_login_item),
) : Parcelable

private fun List<VerificationCodeItem>.toDisplayItems(alertThresholdSeconds: Int) = this.map {
    VerificationCodeDisplayItem(
        id = it.id,
        label = it.name,
        authCode = it.code,
        supportingLabel = it.username,
        periodSeconds = it.periodSeconds,
        timeLeftSeconds = it.timeLeftSeconds,
        alertThresholdSeconds = alertThresholdSeconds
    )
}
