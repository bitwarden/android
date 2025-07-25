package com.x8bit.bitwarden.ui.vault.feature.manualcodeentry

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The ViewModel for handling user interactions in the manual code entry screen.
 *
 */
@HiltViewModel
class ManualCodeEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<ManualCodeEntryState, ManualCodeEntryEvent, ManualCodeEntryAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ManualCodeEntryState(
            code = "",
            dialog = null,
        ),
) {
    override fun handleAction(action: ManualCodeEntryAction) {
        when (action) {
            is ManualCodeEntryAction.CloseClick -> handleCloseClick()
            is ManualCodeEntryAction.CodeTextChange -> handleCodeTextChange(action)
            is ManualCodeEntryAction.CodeSubmit -> handleCodeSubmit()
            ManualCodeEntryAction.DialogDismiss -> handleDialogDismiss()
            is ManualCodeEntryAction.ScanQrCodeTextClick -> handleScanQrCodeTextClick()
            is ManualCodeEntryAction.SettingsClick -> handleSettingsClick()
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
        val code = state.code.trim()
        if (code.isEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = ManualCodeEntryState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.authenticator_key_read_error.asText(),
                    ),
                )
            }
            return
        }
        vaultRepository.emitTotpCodeResult(TotpCodeResult.Success(code))
        sendEvent(ManualCodeEntryEvent.NavigateBack)
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleScanQrCodeTextClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToQrCodeScreen)
    }

    private fun handleSettingsClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToAppSettings)
    }
}

/**
 * Models state of the manual entry screen.
 */
@Parcelize
data class ManualCodeEntryState(
    val code: String,
    val dialog: DialogState?,
) : Parcelable {
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
     * User dismissed the dialog.
     */
    data object DialogDismiss : ManualCodeEntryAction()

    /**
     * The text to switch to QR code scanning is clicked.
     */
    data object ScanQrCodeTextClick : ManualCodeEntryAction()

    /**
     * The action for the user clicking the settings button.
     */
    data object SettingsClick : ManualCodeEntryAction()
}
