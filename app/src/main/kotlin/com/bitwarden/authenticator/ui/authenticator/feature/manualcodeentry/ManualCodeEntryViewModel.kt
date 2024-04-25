package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The ViewModel for handling user interactions in the manual code entry screen.
 *
 */
@HiltViewModel
class ManualCodeEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authenticatorRepository: AuthenticatorRepository,
) : BaseViewModel<ManualCodeEntryState, ManualCodeEntryEvent, ManualCodeEntryAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ManualCodeEntryState(code = "", issuer = "", dialog = null),
) {
    override fun handleAction(action: ManualCodeEntryAction) {
        when (action) {
            is ManualCodeEntryAction.CloseClick -> handleCloseClick()
            is ManualCodeEntryAction.CodeTextChange -> handleCodeTextChange(action)
            is ManualCodeEntryAction.IssuerTextChange -> handleIssuerTextChange(action)
            is ManualCodeEntryAction.CodeSubmit -> handleCodeSubmit()
            is ManualCodeEntryAction.ScanQrCodeTextClick -> handleScanQrCodeTextClick()
            is ManualCodeEntryAction.SettingsClick -> handleSettingsClick()
            is ManualCodeEntryAction.Internal.CreateItemResultReceive -> {
                handleCreateItemReceive(action)
            }
        }
    }

    private fun handleIssuerTextChange(action: ManualCodeEntryAction.IssuerTextChange) {
        mutableStateFlow.update {
            it.copy(issuer = action.issuer)
        }
    }

    private fun handleCloseClick() {
        sendEvent(ManualCodeEntryEvent.NavigateBack)
    }

    private fun handleCodeTextChange(action: ManualCodeEntryAction.CodeTextChange) {
        mutableStateFlow.update {
            it.copy(code = action.code)
        }
    }

    private fun handleCodeSubmit() {
        viewModelScope.launch {
            val result = authenticatorRepository.createItem(
                AuthenticatorItemEntity(
                    id = UUID.randomUUID().toString(),
                    key = state.code,
                    issuer = state.issuer,
                    accountName = "",
                    userId = null,
                    type = if (state.code.startsWith("steam://")) {
                        AuthenticatorItemType.STEAM
                    } else {
                        AuthenticatorItemType.TOTP
                    }
                )
            )
            sendAction(ManualCodeEntryAction.Internal.CreateItemResultReceive(result))
        }
    }

    private fun handleScanQrCodeTextClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToQrCodeScreen)
    }

    private fun handleSettingsClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToAppSettings)
    }

    private fun handleCreateItemReceive(
        action: ManualCodeEntryAction.Internal.CreateItemResultReceive,
    ) {
        when (action.result) {
            CreateItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ManualCodeEntryState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        )
                    )
                }
            }

            CreateItemResult.Success -> {
                sendEvent(
                    event = ManualCodeEntryEvent.ShowToast(
                        message = R.string.verification_code_added.asText(),
                    ),
                )
                sendEvent(
                    event = ManualCodeEntryEvent.NavigateBack,
                )
            }
        }
    }
}

/**
 * Models state of the manual entry screen.
 */
@Parcelize
data class ManualCodeEntryState(
    val code: String,
    val issuer: String,
    val dialog: DialogState?,
) : Parcelable {

    @Parcelize
    sealed class DialogState : Parcelable {

        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the [ManualCodeEntryScreen].
 */
sealed class ManualCodeEntryEvent {

    /**
     * Navigate back.
     */
    data object NavigateBack : ManualCodeEntryEvent()

    /**
     * Navigate to the Qr code screen.
     */
    data object NavigateToQrCodeScreen : ManualCodeEntryEvent()

    /**
     * Navigate to the app settings.
     */
    data object NavigateToAppSettings : ManualCodeEntryEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : ManualCodeEntryEvent()
}

/**
 * Models actions for the [ManualCodeEntryScreen].
 */
sealed class ManualCodeEntryAction {

    /**
     * User clicked close.
     */
    data object CloseClick : ManualCodeEntryAction()

    /**
     * The user has submitted a code.
     */
    data object CodeSubmit : ManualCodeEntryAction()

    /**
     * The user has changed the code text.
     */
    data class CodeTextChange(val code: String) : ManualCodeEntryAction()

    /**
     * The use has changed the issuer text.
     */
    data class IssuerTextChange(val issuer: String) : ManualCodeEntryAction()

    /**
     * Models actions that the [ManualCodeEntryViewModel] itself might send.
     */
    sealed class Internal : ManualCodeEntryAction() {

        /**
         * Indicates a result for creating an item has been received.
         */
        data class CreateItemResultReceive(val result: CreateItemResult) : Internal()
    }

    /**
     * The text to switch to QR code scanning is clicked.
     */
    data object ScanQrCodeTextClick : ManualCodeEntryAction()

    /**
     * The action for the user clicking the settings button.
     */
    data object SettingsClick : ManualCodeEntryAction()
}
