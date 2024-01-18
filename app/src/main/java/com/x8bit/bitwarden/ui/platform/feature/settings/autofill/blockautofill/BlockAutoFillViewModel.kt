package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the blocked autofill URIs screen.
 */
@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class BlockAutoFillViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<BlockAutoFillState, BlockAutoFillEvent, BlockAutoFillAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: BlockAutoFillState(
            viewState = BlockAutoFillState.ViewState.Empty,
        ),
) {

    override fun handleAction(action: BlockAutoFillAction) {
        when (action) {
            BlockAutoFillAction.BackClick -> handleCloseClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = BlockAutoFillEvent.NavigateBack,
        )
    }
}

/**
 * Represents the state for block auto fill.
 *
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class BlockAutoFillState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [BlockAutoFillScreen].
     */
    sealed class ViewState : Parcelable {

        /**
         * Represents an empty content state for the [BlockAutoFillScreen].
         */
        @Parcelize
        data object Empty : ViewState()
    }
}

/**
 * Represents a set of events that can be emitted for the block auto fill screen.
 * Each subclass of this sealed class denotes a distinct event that can occur.
 */
sealed class BlockAutoFillEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : BlockAutoFillEvent()
}

/**
 * Represents a set of actions related to the block auto fill screen.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class BlockAutoFillAction {

    /**
     * User clicked close.
     */
    data object BackClick : BlockAutoFillAction()
}
