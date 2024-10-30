package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import android.net.Uri
import android.os.Parcelable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.authenticator.repository.util.isSyncWithBitwardenEnabled
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.isBase32
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Handles [QrCodeScanAction],
 * and launches [QrCodeScanEvent] for the [QrCodeScanScreen].
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class QrCodeScanViewModel @Inject constructor(
    private val authenticatorBridgeManager: AuthenticatorBridgeManager,
    private val authenticatorRepository: AuthenticatorRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<QrCodeScanState, QrCodeScanEvent, QrCodeScanAction>(
    initialState = QrCodeScanState(dialog = null),
) {

    /**
     * Keeps track of a pending successful scan to support the case where the user is choosing
     * default save location.
     */
    private var pendingSuccessfulScan: TotpCodeResult.TotpCodeScan? = null

    override fun handleAction(action: QrCodeScanAction) {
        when (action) {
            is QrCodeScanAction.CloseClick -> handleCloseClick()
            is QrCodeScanAction.ManualEntryTextClick -> handleManualEntryTextClick()
            is QrCodeScanAction.CameraSetupErrorReceive -> handleCameraErrorReceive()
            is QrCodeScanAction.QrCodeScanReceive -> handleQrCodeScanReceive(action)
            QrCodeScanAction.SaveToBitwardenErrorDismiss -> handleSaveToBitwardenDismiss()
            is QrCodeScanAction.SaveLocallyClick -> handleSaveLocallyClick(action)
            is QrCodeScanAction.SaveToBitwardenClick -> handleSaveToBitwardenClick(action)
        }
    }

    private fun handleSaveToBitwardenClick(action: QrCodeScanAction.SaveToBitwardenClick) {
        if (action.saveAsDefault) {
            settingsRepository.defaultSaveOption = DefaultSaveOption.BITWARDEN_APP
        }
        pendingSuccessfulScan?.let {
            saveCodeToBitwardenAndNavigateBack(it)
        }
        pendingSuccessfulScan = null
    }

    private fun handleSaveLocallyClick(action: QrCodeScanAction.SaveLocallyClick) {
        if (action.saveAsDefault) {
            settingsRepository.defaultSaveOption = DefaultSaveOption.LOCAL
        }
        pendingSuccessfulScan?.let {
            saveCodeLocallyAndNavigateBack(it)
        }
        pendingSuccessfulScan = null
    }

    private fun handleSaveToBitwardenDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            QrCodeScanEvent.NavigateBack,
        )
    }

    private fun handleManualEntryTextClick() {
        sendEvent(
            QrCodeScanEvent.NavigateToManualCodeEntry,
        )
    }

    private fun handleQrCodeScanReceive(action: QrCodeScanAction.QrCodeScanReceive) {
        val scannedCode = action.qrCode
        if (scannedCode.startsWith(TotpCodeManager.TOTP_CODE_PREFIX)) {
            handleTotpUriReceive(scannedCode)
        } else if (scannedCode.startsWith(TotpCodeManager.GOOGLE_EXPORT_PREFIX)) {
            handleGoogleExportUriReceive(scannedCode)
        } else {
            authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError)
            sendEvent(QrCodeScanEvent.NavigateBack)
            return
        }
    }

    // For more information: https://bitwarden.com/help/authenticator-keys/#support-for-more-parameters
    private fun handleTotpUriReceive(scannedCode: String) {
        val result = TotpCodeResult.TotpCodeScan(scannedCode)
        val scannedCodeUri = Uri.parse(scannedCode)
        val secretValue = scannedCodeUri
            .getQueryParameter(TotpCodeManager.SECRET_PARAM)
            .orEmpty()
            .toUpperCase(Locale.current)

        if (secretValue.isEmpty() || !secretValue.isBase32()) {
            authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError)
            sendEvent(QrCodeScanEvent.NavigateBack)
            return
        }

        val values = scannedCodeUri.queryParameterNames
        // If the parameters are not valid,
        if (!areParametersValid(scannedCode, values)) {
            authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError)
            sendEvent(QrCodeScanEvent.NavigateBack)
            return
        }
        if (authenticatorRepository.sharedCodesStateFlow.value.isSyncWithBitwardenEnabled) {
            when (settingsRepository.defaultSaveOption) {
                DefaultSaveOption.BITWARDEN_APP -> saveCodeToBitwardenAndNavigateBack(result)
                DefaultSaveOption.LOCAL -> saveCodeLocallyAndNavigateBack(result)

                DefaultSaveOption.NONE -> {
                    pendingSuccessfulScan = result
                    mutableStateFlow.update {
                        it.copy(
                            dialog = QrCodeScanState.DialogState.ChooseSaveLocation,
                        )
                    }
                }
            }
        } else {
            // Syncing with Bitwarden not enabled, save code locally:
            saveCodeLocallyAndNavigateBack(result)
        }
    }

    private fun handleGoogleExportUriReceive(scannedCode: String) {
        val uri = Uri.parse(scannedCode)
        val encodedData = uri.getQueryParameter(TotpCodeManager.DATA_PARAM)
        val result: TotpCodeResult = if (encodedData.isNullOrEmpty()) {
            TotpCodeResult.CodeScanningError
        } else {
            TotpCodeResult.GoogleExportScan(encodedData)
        }
        authenticatorRepository.emitTotpCodeResult(result)
        sendEvent(QrCodeScanEvent.NavigateBack)
    }

    private fun handleCameraErrorReceive() {
        sendEvent(
            QrCodeScanEvent.NavigateToManualCodeEntry,
        )
    }

    @Suppress("NestedBlockDepth", "ReturnCount", "MagicNumber")
    private fun areParametersValid(scannedCode: String, parameters: Set<String>): Boolean {
        parameters.forEach { parameter ->
            Uri.parse(scannedCode).getQueryParameter(parameter)?.let { value ->
                when (parameter) {
                    TotpCodeManager.DIGITS_PARAM -> {
                        val digit = value.toInt()
                        if (digit > 10 || digit < 1) {
                            return false
                        }
                    }

                    TotpCodeManager.PERIOD_PARAM -> {
                        val period = value.toInt()
                        if (period < 1) {
                            return false
                        }
                    }

                    TotpCodeManager.ALGORITHM_PARAM -> {
                        val lowercaseAlgo = value.lowercase()
                        if (lowercaseAlgo != "sha1" &&
                            lowercaseAlgo != "sha256" &&
                            lowercaseAlgo != "sha512"
                        ) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }

    private fun saveCodeToBitwardenAndNavigateBack(result: TotpCodeResult.TotpCodeScan) {
        val didLaunchAddToBitwarden =
            authenticatorBridgeManager.startAddTotpLoginItemFlow(result.code)
        if (didLaunchAddToBitwarden) {
            sendEvent(QrCodeScanEvent.NavigateBack)
        } else {
            mutableStateFlow.update {
                it.copy(dialog = QrCodeScanState.DialogState.SaveToBitwardenError)
            }
        }
    }

    private fun saveCodeLocallyAndNavigateBack(result: TotpCodeResult.TotpCodeScan) {
        authenticatorRepository.emitTotpCodeResult(result)
        sendEvent(QrCodeScanEvent.NavigateBack)
    }
}

/**
 * Models state for [QrCodeScanViewModel].
 *
 * @param dialog Dialog to be shown, or `null` if no dialog should be shown.
 */
@Parcelize
data class QrCodeScanState(
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Models dialogs that can be shown on the QR Scan screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Displays a prompt to choose save location for a newly scanned code.
         */
        @Parcelize
        data object ChooseSaveLocation : DialogState()

        /**
         * Displays an error letting the user know that saving to bitwarden failed.
         */
        @Parcelize
        data object SaveToBitwardenError : DialogState()
    }
}

/**
 * Models events for the [QrCodeScanScreen].
 */
sealed class QrCodeScanEvent {

    /**
     * Navigate back.
     */
    data object NavigateBack : QrCodeScanEvent()

    /**
     * Navigate to manual code entry screen.
     */
    data object NavigateToManualCodeEntry : QrCodeScanEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : QrCodeScanEvent()
}

/**
 * Models actions for the [QrCodeScanScreen].
 */
sealed class QrCodeScanAction {

    /**
     * User clicked close.
     */
    data object CloseClick : QrCodeScanAction()

    /**
     * The user has scanned a QR code.
     */
    data class QrCodeScanReceive(val qrCode: String) : QrCodeScanAction()

    /**
     * The text to switch to manual entry is clicked.
     */
    data object ManualEntryTextClick : QrCodeScanAction()

    /**
     * The Camera is unable to be setup.
     */
    data object CameraSetupErrorReceive : QrCodeScanAction()

    /**
     * The user dismissed the Save to Bitwarden error dialog.
     */
    data object SaveToBitwardenErrorDismiss : QrCodeScanAction()

    /**
     * User clicked save to Bitwarden on the choose save location dialog.
     *
     * @param saveAsDefault Whether or not he user checked "Save as default".
     */
    data class SaveToBitwardenClick(val saveAsDefault: Boolean) : QrCodeScanAction()

    /**
     * User clicked save locally on the save to Bitwarden dialog.
     *
     * @param saveAsDefault Whether or not he user checked "Save as default".
     */
    data class SaveLocallyClick(val saveAsDefault: Boolean) : QrCodeScanAction()
}
