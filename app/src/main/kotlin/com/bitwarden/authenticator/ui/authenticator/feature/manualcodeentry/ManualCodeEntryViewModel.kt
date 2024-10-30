package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.isSyncWithBitwardenEnabled
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.isBase32
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
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
@Suppress("TooManyFunctions")
class ManualCodeEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authenticatorRepository: AuthenticatorRepository,
    private val authenticatorBridgeManager: AuthenticatorBridgeManager,
    settingsRepository: SettingsRepository,
) : BaseViewModel<ManualCodeEntryState, ManualCodeEntryEvent, ManualCodeEntryAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ManualCodeEntryState(
            code = "",
            issuer = "",
            dialog = null,
            buttonState = deriveButtonState(
                sharedCodesState = authenticatorRepository.sharedCodesStateFlow.value,
                defaultSaveOption = settingsRepository.defaultSaveOption,
            ),
        ),
) {
    override fun handleAction(action: ManualCodeEntryAction) {
        when (action) {
            is ManualCodeEntryAction.CloseClick -> handleCloseClick()
            is ManualCodeEntryAction.CodeTextChange -> handleCodeTextChange(action)
            is ManualCodeEntryAction.IssuerTextChange -> handleIssuerTextChange(action)
            is ManualCodeEntryAction.ScanQrCodeTextClick -> handleScanQrCodeTextClick()
            is ManualCodeEntryAction.SettingsClick -> handleSettingsClick()
            ManualCodeEntryAction.DismissDialog -> {
                handleDialogDismiss()
            }

            ManualCodeEntryAction.SaveLocallyClick -> handleSaveLocallyClick()
            ManualCodeEntryAction.SaveToBitwardenClick -> handleSaveToBitwardenClick()
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialog = null) }
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

    private fun handleSaveLocallyClick() = handleCodeSubmit(saveToBitwarden = false)

    private fun handleSaveToBitwardenClick() = handleCodeSubmit(saveToBitwarden = true)

    private fun handleCodeSubmit(saveToBitwarden: Boolean) {
        val isSteamCode = state.code.startsWith(TotpCodeManager.STEAM_CODE_PREFIX)
        val sanitizedCode = state.code
            .replace(" ", "")
            .replace(TotpCodeManager.STEAM_CODE_PREFIX, "")
        if (sanitizedCode.isBlank()) {
            showErrorDialog(R.string.key_is_required.asText())
            return
        }

        if (!sanitizedCode.isBase32()) {
            showErrorDialog(R.string.key_is_invalid.asText())
            return
        }

        if (state.issuer.isBlank()) {
            showErrorDialog(R.string.name_is_required.asText())
            return
        }

        if (saveToBitwarden) {
            // Save to Bitwarden by kicking off save to Bitwarden flow:
            saveValidCodeToBitwarden(sanitizedCode)
        } else {
            // Save locally by giving entity to AuthRepository and navigating back:
            saveValidCodeLocally(sanitizedCode, isSteamCode)
        }
    }

    private fun saveValidCodeToBitwarden(sanitizedCode: String) {
        val didLaunchSaveToBitwarden = authenticatorBridgeManager
            .startAddTotpLoginItemFlow(
                totpUri = "otpauth://totp/?secret=$sanitizedCode&issuer=${state.issuer}",
            )
        if (!didLaunchSaveToBitwarden) {
            mutableStateFlow.update {
                it.copy(
                    dialog = ManualCodeEntryState.DialogState.Error(
                        title = R.string.something_went_wrong.asText(),
                        message = R.string.please_try_again.asText(),
                    ),
                )
            }
        } else {
            sendEvent(ManualCodeEntryEvent.NavigateBack)
        }
    }

    private fun saveValidCodeLocally(
        sanitizedCode: String,
        isSteamCode: Boolean,
    ) {
        viewModelScope.launch {
            authenticatorRepository.createItem(
                AuthenticatorItemEntity(
                    id = UUID.randomUUID().toString(),
                    key = sanitizedCode,
                    issuer = state.issuer,
                    accountName = "",
                    userId = null,
                    type = if (isSteamCode) {
                        AuthenticatorItemType.STEAM
                    } else {
                        AuthenticatorItemType.TOTP
                    },
                    favorite = false,
                ),
            )
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

    private fun handleScanQrCodeTextClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToQrCodeScreen)
    }

    private fun handleSettingsClick() {
        sendEvent(ManualCodeEntryEvent.NavigateToAppSettings)
    }

    private fun showErrorDialog(message: Text) {
        mutableStateFlow.update {
            it.copy(
                dialog = ManualCodeEntryState.DialogState.Error(
                    message = message,
                ),
            )
        }
    }
}

private fun deriveButtonState(
    sharedCodesState: SharedVerificationCodesState,
    defaultSaveOption: DefaultSaveOption,
): ManualCodeEntryState.ButtonState {
    // If syncing with Bitwarden is not enabled, show local save only:
    if (!sharedCodesState.isSyncWithBitwardenEnabled) {
        return ManualCodeEntryState.ButtonState.LocalOnly
    }
    // Otherwise, show save options based on user's preferences:
    return when (defaultSaveOption) {
        DefaultSaveOption.NONE -> ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary
        DefaultSaveOption.BITWARDEN_APP -> ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary
        DefaultSaveOption.LOCAL -> ManualCodeEntryState.ButtonState.SaveLocallyPrimary
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
    val buttonState: ButtonState,
) : Parcelable {

    /**
     * Models dialog states for [ManualCodeEntryViewModel].
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * Show an error dialog with an optional [title], and a [message].
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Show a loading dialog.
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

    /**
     * Models what variation of button states should be shown.
     */
    @Parcelize
    sealed class ButtonState : Parcelable {

        /**
         * Show only save locally option.
         */
        @Parcelize
        data object LocalOnly : ButtonState()

        /**
         * Show both save locally and save to Bitwarden, with Bitwarden being the primary option.
         */
        @Parcelize
        data object SaveToBitwardenPrimary : ButtonState()

        /**
         * Show both save locally and save to Bitwarden, with locally being the primary option.
         */
        @Parcelize
        data object SaveLocallyPrimary : ButtonState()
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
     * The user clicked the save locally button.
     */
    data object SaveLocallyClick : ManualCodeEntryAction()

    /**
     * Th user clicked the save to Bitwarden button.
     */
    data object SaveToBitwardenClick : ManualCodeEntryAction()

    /**
     * The user has changed the code text.
     */
    data class CodeTextChange(val code: String) : ManualCodeEntryAction()

    /**
     * The use has changed the issuer text.
     */
    data class IssuerTextChange(val issuer: String) : ManualCodeEntryAction()

    /**
     * The text to switch to QR code scanning is clicked.
     */
    data object ScanQrCodeTextClick : ManualCodeEntryAction()

    /**
     * The action for the user clicking the settings button.
     */
    data object SettingsClick : ManualCodeEntryAction()

    /**
     * The user has dismissed the dialog.
     */
    data object DismissDialog : ManualCodeEntryAction()
}
