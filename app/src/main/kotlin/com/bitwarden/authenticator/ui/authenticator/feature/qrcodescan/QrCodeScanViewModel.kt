package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import android.net.Uri
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.isBase32
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Handles [QrCodeScanAction],
 * and launches [QrCodeScanEvent] for the [QrCodeScanScreen].
 */
@HiltViewModel
class QrCodeScanViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
) : BaseViewModel<Unit, QrCodeScanEvent, QrCodeScanAction>(
    initialState = Unit,
) {
    override fun handleAction(action: QrCodeScanAction) {
        when (action) {
            is QrCodeScanAction.CloseClick -> handleCloseClick()
            is QrCodeScanAction.ManualEntryTextClick -> handleManualEntryTextClick()
            is QrCodeScanAction.CameraSetupErrorReceive -> handleCameraErrorReceive()
            is QrCodeScanAction.QrCodeScanReceive -> handleQrCodeScanReceive(action)
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

    // For more information: https://bitwarden.com/help/authenticator-keys/#support-for-more-parameters
    private fun handleQrCodeScanReceive(action: QrCodeScanAction.QrCodeScanReceive) {
        var result: TotpCodeResult = TotpCodeResult.Success(action.qrCode)
        val scannedCode = action.qrCode

        if (scannedCode.isBlank() || !scannedCode.startsWith(TotpCodeManager.TOTP_CODE_PREFIX)) {
            authenticatorRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError)
            sendEvent(QrCodeScanEvent.NavigateBack)
            return
        }

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
        if (!areParametersValid(scannedCode, values)) {
            result = TotpCodeResult.CodeScanningError
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
}
