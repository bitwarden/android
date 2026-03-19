package com.x8bit.bitwarden.ui.vault.feature.qrcodescan

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import com.bitwarden.ui.platform.util.getTotpDataOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.TotpCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Handles [QrCodeScanAction] and launches [QrCodeScanEvent] for the [QrCodeScanScreen].
 */
@HiltViewModel
class QrCodeScanViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<QrCodeScanState, QrCodeScanEvent, QrCodeScanAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: QrCodeScanState(hasHandledScan = false),
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
        sendEvent(QrCodeScanEvent.NavigateBack)
    }

    private fun handleManualEntryTextClick() {
        sendEvent(QrCodeScanEvent.NavigateToManualCodeEntry)
    }

    // For more information: https://bitwarden.com/help/authenticator-keys/#support-for-more-parameters
    private fun handleQrCodeScanReceive(action: QrCodeScanAction.QrCodeScanReceive) {
        if (state.hasHandledScan) {
            return
        }
        mutableStateFlow.update {
            it.copy(hasHandledScan = true)
        }
        val qrCode = action.qrCode
        qrCode
            .getTotpDataOrNull()
            ?.let { vaultRepository.emitTotpCodeResult(TotpCodeResult.Success(code = qrCode)) }
            ?: run { vaultRepository.emitTotpCodeResult(TotpCodeResult.CodeScanningError()) }
        sendEvent(QrCodeScanEvent.NavigateBack)
    }

    private fun handleCameraErrorReceive() {
        sendEvent(QrCodeScanEvent.NavigateToManualCodeEntry)
    }
}

/**
 * Models events for the [QrCodeScanScreen].
 */
sealed class QrCodeScanEvent {

    /**
     * Navigate back.
     * Added DeferredBackgroundEvent as QrCodeScan might be fired before events are consumed
     */
    data object NavigateBack : QrCodeScanEvent(), DeferredBackgroundEvent

    /**
     * Navigate to manual code entry screen.
     */
    data object NavigateToManualCodeEntry : QrCodeScanEvent()
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

/**
 * Represents the state of the QrCodeScan screen.
 */
@Parcelize
data class QrCodeScanState(
    val hasHandledScan: Boolean,
) : Parcelable
