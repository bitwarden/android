package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.x8bit.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util.toViewState
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.asText
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.concat
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject

/**
 * View model responsible for handling user interactions with the item listing screen.
 */
@HiltViewModel
class ItemListingViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel<ItemListingState, ItemListingEvent, ItemListingAction>(
    initialState = ItemListingState(
        settingsRepository.authenticatorAlertThresholdSeconds,
        viewState = ItemListingState.ViewState.Loading,
        dialog = null
    )
) {

    init {

        settingsRepository
            .authenticatorAlertThresholdSecondsFlow
            .map { ItemListingAction.Internal.AlertThresholdSecondsReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        authenticatorRepository
            .getAuthCodesFlow()
            .map { ItemListingAction.Internal.AuthCodesUpdated(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        authenticatorRepository
            .totpCodeFlow
            .map { ItemListingAction.Internal.TotpCodeReceive(totpResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemListingAction) {
        when (action) {
            ItemListingAction.ScanQrCodeClick -> {
                sendEvent(ItemListingEvent.NavigateToQrCodeScanner)
            }

            ItemListingAction.EnterSetupKeyClick -> {
                sendEvent(ItemListingEvent.NavigateToManualAddItem)
            }

            ItemListingAction.BackClick -> {
                sendEvent(ItemListingEvent.NavigateBack)
            }

            is ItemListingAction.ItemClick -> {
                sendEvent(ItemListingEvent.NavigateToItem(action.id))
            }

            ItemListingAction.DialogDismiss -> {
                handleDialogDismiss()
            }

            is ItemListingAction.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleInternalAction(internalAction: ItemListingAction.Internal) {
        when (internalAction) {
            is ItemListingAction.Internal.AuthCodesUpdated -> {
                handleAuthenticatorDataReceive(internalAction)
            }

            is ItemListingAction.Internal.AlertThresholdSecondsReceive -> {
                handleAlertThresholdSecondsReceive(internalAction)
            }

            is ItemListingAction.Internal.TotpCodeReceive -> {
                handleTotpCodeReceive(internalAction)
            }

            is ItemListingAction.Internal.CreateItemResultReceive -> {
                handleCreateItemResultReceive(internalAction)
            }
        }
    }

    private fun handleCreateItemResultReceive(
        action: ItemListingAction.Internal.CreateItemResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }

        when (action.result) {
            CreateItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.authenticator_key_read_error.asText(),
                        )
                    )
                }
            }

            CreateItemResult.Success -> {
                sendEvent(
                    event = ItemListingEvent.ShowToast(
                        message = R.string.authenticator_key_added.asText(),
                    ),
                )
            }
        }
    }

    private fun handleTotpCodeReceive(action: ItemListingAction.Internal.TotpCodeReceive) {
        mutableStateFlow.update { it.copy(viewState = ItemListingState.ViewState.Loading) }

        viewModelScope.launch {
            when (val totpResult = action.totpResult) {
                TotpCodeResult.CodeScanningError -> {
                    sendAction(
                        action = ItemListingAction.Internal.CreateItemResultReceive(
                            result = CreateItemResult.Error,
                        ),
                    )
                }

                is TotpCodeResult.Success -> {

                    val item = totpResult.code.toAuthenticatorEntityOrNull()
                        ?: run {
                            sendAction(
                                action = ItemListingAction.Internal.CreateItemResultReceive(
                                    result = CreateItemResult.Error,
                                ),
                            )
                            return@launch
                        }

                    val result = authenticatorRepository.createItem(item)
                    sendAction(ItemListingAction.Internal.CreateItemResultReceive(result))
                }
            }
        }
    }

    private fun handleAlertThresholdSecondsReceive(
        action: ItemListingAction.Internal.AlertThresholdSecondsReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                alertThresholdSeconds = action.thresholdSeconds,
            )
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleAuthenticatorDataReceive(
        action: ItemListingAction.Internal.AuthCodesUpdated,
    ) {
        updateViewState(action.itemListingDataState)
    }

    private fun updateViewState(authenticatorData: DataState<List<VerificationCodeItem>>) {
        when (authenticatorData) {
            is DataState.Error -> authenticatorErrorReceive(authenticatorData)
            is DataState.Loaded -> authenticatorDataLoadedReceive(authenticatorData)
            is DataState.Loading -> authenticatorDataLoadingReceive()
            is DataState.NoNetwork -> authenticatorNoNetworkReceive(authenticatorData)
            is DataState.Pending -> authenticatorPendingReceive(authenticatorData)
        }
    }

    private fun authenticatorErrorReceive(authenticatorData: DataState.Error<List<VerificationCodeItem>>) {
        if (authenticatorData.data != null) {
            updateStateWithVerificationCodeItems(
                authenticatorData = authenticatorData.data,
                clearDialogState = true
            )
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = ItemListingState.ViewState.Error(
                        R.string.generic_error_message.asText(),
                    ),
                    dialog = null,
                )
            }
        }
    }

    private fun authenticatorDataLoadedReceive(
        authenticatorData: DataState.Loaded<List<VerificationCodeItem>>,
    ) {
        updateStateWithVerificationCodeItems(
            authenticatorData = authenticatorData.data,
            clearDialogState = true
        )
    }

    private fun authenticatorDataLoadingReceive() {
        mutableStateFlow.update {
            it.copy(
                viewState = ItemListingState.ViewState.Loading
            )
        }
    }

    private fun authenticatorNoNetworkReceive(state: DataState.NoNetwork<List<VerificationCodeItem>>) {
        if (state.data != null) {
            updateStateWithVerificationCodeItems(
                authenticatorData = state.data,
                clearDialogState = true
            )
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = ItemListingState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(R.string.internet_connection_required_message.asText())
                    ),
                    dialog = null,
                )
            }
        }
    }

    private fun authenticatorPendingReceive(
        action: DataState.Pending<List<VerificationCodeItem>>,
    ) {
        updateStateWithVerificationCodeItems(
            authenticatorData = action.data,
            clearDialogState = false
        )
    }

    private fun updateStateWithVerificationCodeItems(
        authenticatorData: List<VerificationCodeItem>,
        clearDialogState: Boolean,
    ) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = authenticatorData.toViewState(
                    alertThresholdSeconds = state.alertThresholdSeconds,
                ),
                dialog = currentState.dialog.takeUnless { clearDialogState },
            )
        }
    }

    private fun String.toAuthenticatorEntityOrNull(): AuthenticatorItemEntity? {
        try {
            val uri = Uri.parse(this)

            val type = AuthenticatorItemType
                .entries
                .find { it.name.lowercase() == uri.host }
                ?: return null

            val label = uri.pathSegments.firstOrNull() ?: return null

            val key = uri.getQueryParameter(SECRET) ?: return null

            val algorithm = AuthenticatorItemAlgorithm
                .entries
                .find { it.name == uri.getQueryParameter(ALGORITHM) }
                ?: AuthenticatorItemAlgorithm.SHA1

            val digits = uri.getQueryParameter(DIGITS)?.toInt() ?: 6

            val issuer = uri.getQueryParameter(ISSUER)

            val period = uri.getQueryParameter(PERIOD)?.toInt() ?: 30

            return AuthenticatorItemEntity(
                id = UUID.randomUUID().toString(),
                key = key,
                accountName = label,
                type = type,
                algorithm = algorithm,
                period = period,
                digits = digits,
                issuer = issuer,
                userId = null,
            )
        } catch (e: Throwable) {
            return null
        }
    }
}

private const val ALGORITHM = "algorithm"
private const val DIGITS = "digits"
private const val PERIOD = "period"
private const val SECRET = "secret"
private const val ISSUER = "issuer"
private const val TOTP_CODE_PREFIX = "otpauth://totp"

/**
 * Represents the state for displaying the item listing.
 *
 * @property viewState Current state of the [ItemListingScreen].
 * @property dialog State of the dialog show on the [ItemListingScreen]. `null` if no dialog is
 * shown.
 */
@Parcelize
data class ItemListingState(
    val alertThresholdSeconds: Int,
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
        data class AuthCodesUpdated(
            val itemListingDataState: DataState<List<VerificationCodeItem>>,
        ) : Internal()

        /**
         * Indicates authenticator item alert threshold seconds changes has been received.
         */
        data class AlertThresholdSecondsReceive(
            val thresholdSeconds: Int,
        ) : Internal()

        /**
         * Indicates a new TOTP code scan result has been received.
         */
        data class TotpCodeReceive(val totpResult: TotpCodeResult) : Internal()

        /**
         * Indicates a result for creating and item has been received.
         */
        data class CreateItemResultReceive(val result: CreateItemResult) : Internal()
    }
}

/**
 * The data for the verification code item to display.
 */
@Parcelize
data class VerificationCodeDisplayItem(
    val id: String,
    val label: String,
    val issuer: String?,
    val supportingLabel: String?,
    val timeLeftSeconds: Int,
    val periodSeconds: Int,
    val alertThresholdSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(R.drawable.ic_login_item),
) : Parcelable
