package com.x8bit.bitwarden.ui.platform.feature.settings.other

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the other screen.
 */
@HiltViewModel
class OtherViewModel @Inject constructor(
    private val vaultRepo: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<OtherState, OtherEvent, OtherAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: OtherState(
            allowScreenCapture = false,
            allowSyncOnRefresh = false,
            clearClipboardFrequency = OtherState.ClearClipboardFrequency.DEFAULT,
            lastSyncTime = "5/14/2023 4:52 PM",
        ),
) {
    override fun handleAction(action: OtherAction): Unit = when (action) {
        is OtherAction.AllowScreenCaptureToggle -> handleAllowScreenCaptureToggled(action)
        is OtherAction.AllowSyncToggle -> handleAllowSyncToggled(action)
        OtherAction.BackClick -> handleBackClicked()
        is OtherAction.ClearClipboardFrequencyChange -> handleClearClipboardFrequencyChanged(action)
        OtherAction.SyncNowButtonClick -> handleSyncNowButtonClicked()
    }

    private fun handleAllowScreenCaptureToggled(action: OtherAction.AllowScreenCaptureToggle) {
        // TODO BIT-805 implement screen capture setting
        mutableStateFlow.update { it.copy(allowScreenCapture = action.isScreenCaptureEnabled) }
    }

    private fun handleAllowSyncToggled(action: OtherAction.AllowSyncToggle) {
        // TODO BIT-461 hook up to pull-to-refresh feature
        mutableStateFlow.update { it.copy(allowSyncOnRefresh = action.isSyncEnabled) }
    }

    private fun handleBackClicked() {
        sendEvent(OtherEvent.NavigateBack)
    }

    private fun handleClearClipboardFrequencyChanged(
        action: OtherAction.ClearClipboardFrequencyChange,
    ) {
        // TODO BIT-1283 implement clear clipboard setting
        mutableStateFlow.update {
            it.copy(
                clearClipboardFrequency = action.clearClipboardFrequency,
            )
        }
    }

    private fun handleSyncNowButtonClicked() {
        // TODO BIT-1282 add full support and visual feedback
        vaultRepo.sync()
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
) : Parcelable {
    /**
     * Represents the different frequencies with which the user clipboard can be cleared.
     */
    enum class ClearClipboardFrequency(val text: Text) {
        DEFAULT(text = R.string.never.asText()),
        TEN_SECONDS(text = R.string.ten_seconds.asText()),
        TWENTY_SECONDS(text = R.string.twenty_seconds.asText()),
        THIRTY_SECONDS(text = R.string.thirty_seconds.asText()),
        ONE_MINUTE(text = R.string.one_minute.asText()),
        TWO_MINUTES(text = R.string.two_minutes.asText()),
        FIVE_MINUTES(text = R.string.five_minutes.asText()),
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
        val clearClipboardFrequency: OtherState.ClearClipboardFrequency,
    ) : OtherAction()

    /**
     * Indicates that the user clicked the Sync Now button.
     */
    data object SyncNowButtonClick : OtherAction()
}
