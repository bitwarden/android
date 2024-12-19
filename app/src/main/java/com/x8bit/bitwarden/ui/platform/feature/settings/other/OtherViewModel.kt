package com.x8bit.bitwarden.ui.platform.feature.settings.other

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

private const val KEY_STATE = "state"

private const val VAULT_LAST_SYNC_TIME_PATTERN: String = "M/d/yyyy h:mm a"

/**
 * View model for the other screen.
 */
@HiltViewModel
class OtherViewModel @Inject constructor(
    private val clock: Clock,
    private val settingsRepo: SettingsRepository,
    private val vaultRepo: VaultRepository,
    private val networkConnectionManager: NetworkConnectionManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<OtherState, OtherEvent, OtherAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: OtherState(
            allowScreenCapture = settingsRepo.isScreenCaptureAllowed,
            allowSyncOnRefresh = settingsRepo.getPullToRefreshEnabledFlow().value,
            clearClipboardFrequency = settingsRepo.clearClipboardFrequency,
            lastSyncTime = settingsRepo
                .vaultLastSync
                ?.toFormattedPattern(VAULT_LAST_SYNC_TIME_PATTERN, clock)
                .orEmpty(),
            dialogState = null,
        ),
) {
    init {
        settingsRepo
            .vaultLastSyncStateFlow
            .map { OtherAction.Internal.VaultLastSyncReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        settingsRepo
            .vaultLastSyncStateFlow
            .drop(1)
            .map { OtherAction.Internal.ManualVaultSyncReceive }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: OtherAction): Unit = when (action) {
        is OtherAction.AllowScreenCaptureToggle -> handleAllowScreenCaptureToggled(action)
        is OtherAction.AllowSyncToggle -> handleAllowSyncToggled(action)
        OtherAction.BackClick -> handleBackClicked()
        is OtherAction.ClearClipboardFrequencyChange -> handleClearClipboardFrequencyChanged(action)
        OtherAction.SyncNowButtonClick -> handleSyncNowButtonClicked()
        is OtherAction.Internal -> handleInternalAction(action)
        OtherAction.DismissDialog -> handleDismissDialog()
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }
    }

    private fun handleAllowScreenCaptureToggled(action: OtherAction.AllowScreenCaptureToggle) {
        settingsRepo.isScreenCaptureAllowed = action.isScreenCaptureEnabled
        mutableStateFlow.update { it.copy(allowScreenCapture = action.isScreenCaptureEnabled) }
    }

    private fun handleAllowSyncToggled(action: OtherAction.AllowSyncToggle) {
        settingsRepo.storePullToRefreshEnabled(action.isSyncEnabled)
        mutableStateFlow.update { it.copy(allowSyncOnRefresh = action.isSyncEnabled) }
    }

    private fun handleBackClicked() {
        sendEvent(OtherEvent.NavigateBack)
    }

    private fun handleClearClipboardFrequencyChanged(
        action: OtherAction.ClearClipboardFrequencyChange,
    ) {
        mutableStateFlow.update {
            it.copy(clearClipboardFrequency = action.clearClipboardFrequency)
        }
        settingsRepo.clearClipboardFrequency = action.clearClipboardFrequency
    }

    private fun handleSyncNowButtonClicked() {
        if (networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(dialogState = OtherState.DialogState.Loading(R.string.syncing.asText()))
            }
            vaultRepo.sync(forced = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialogState = OtherState.DialogState.Error(
                        R.string.internet_connection_required_title.asText(),
                        R.string.internet_connection_required_message.asText(),
                    ),
                )
            }
        }
    }

    private fun handleInternalAction(action: OtherAction.Internal) {
        when (action) {
            is OtherAction.Internal.VaultLastSyncReceive -> handleVaultDataReceive(action)
            is OtherAction.Internal.ManualVaultSyncReceive -> handleManualVaultSyncReceive()
        }
    }

    private fun handleVaultDataReceive(action: OtherAction.Internal.VaultLastSyncReceive) {
        mutableStateFlow.update {
            it.copy(
                lastSyncTime = action
                    .vaultLastSyncTime
                    ?.toFormattedPattern(VAULT_LAST_SYNC_TIME_PATTERN, clock)
                    .orEmpty(),
                dialogState = null,
            )
        }
    }

    private fun handleManualVaultSyncReceive() {
        sendEvent(OtherEvent.ShowToast(R.string.syncing_complete.asText()))
    }
}

/**
 * Models the state of the Other screen.
 */
@Parcelize
data class OtherState(
    val allowScreenCapture: Boolean,
    val allowSyncOnRefresh: Boolean,
    val clearClipboardFrequency: ClearClipboardFrequency,
    val lastSyncTime: String,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Represents an error dialog with the given [title] and [message].
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the other screen.
 */
sealed class OtherEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : OtherEvent()

    /**
     * Show a toast with the given message.
     */
    data class ShowToast(
        val message: Text,
    ) : OtherEvent()
}

/**
 * Models actions for the other screen.
 */
sealed class OtherAction {
    /**
     * Indicates that the user toggled the Allow screen capture switch to [isScreenCaptureEnabled].
     */
    data class AllowScreenCaptureToggle(
        val isScreenCaptureEnabled: Boolean,
    ) : OtherAction()

    /**
     * Indicates that the user toggled the Allow sync on refresh switch to [isSyncEnabled].
     */
    data class AllowSyncToggle(
        val isSyncEnabled: Boolean,
    ) : OtherAction()

    /**
     * User clicked back button.
     */
    data object BackClick : OtherAction()

    /**
     * Indicates that the user changed the clear clipboard frequency.
     */
    data class ClearClipboardFrequencyChange(
        val clearClipboardFrequency: ClearClipboardFrequency,
    ) : OtherAction()

    /**
     * Indicates that the user clicked the Sync Now button.
     */
    data object SyncNowButtonClick : OtherAction()

    /**
     * Indicates that the dialog should be dismissed.
     */
    data object DismissDialog : OtherAction()

    /**
     * Models actions that the [OtherViewModel] itself might send.
     */
    sealed class Internal : OtherAction() {
        /**
         * Indicates last sync time of the vault has been received.
         */
        data class VaultLastSyncReceive(
            val vaultLastSyncTime: Instant?,
        ) : Internal()

        /**
         * Indicates a manual vault sync has been received.
         */
        data object ManualVaultSyncReceive : Internal()
    }
}
