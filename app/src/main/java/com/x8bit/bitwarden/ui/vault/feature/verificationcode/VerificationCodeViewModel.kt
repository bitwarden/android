package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Handles [VerificationCodeAction],
 * and launches [VerificationCodeEvent] for the [VerificationCodeScreen].
 */
@HiltViewModel
class VerificationCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VerificationCodeState, VerificationCodeEvent, VerificationCodeAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: VerificationCodeState(
            viewState = VerificationCodeState.ViewState.Empty,
        ),
) {

    override fun handleAction(action: VerificationCodeAction) {
        when (action) {
            is VerificationCodeAction.BackClick -> handleBackClick()
            is VerificationCodeAction.ItemClick -> handleItemClick(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(
            event = VerificationCodeEvent.NavigateBack,
        )
    }

    private fun handleItemClick(action: VerificationCodeAction.ItemClick) {
        sendEvent(
            VerificationCodeEvent.NavigateToVaultItem(action.id),
        )
    }
}

/**
 * Models state of the verification code screen.
 *
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class VerificationCodeState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [VerificationCodeScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Represents an empty content state for the [VerificationCodeScreen].
         */
        @Parcelize
        data object Empty : ViewState()
    }
}

/**
 * Models events for the [VerificationCodeScreen].
 */
sealed class VerificationCodeEvent {

    /**
     * Navigate back.
     */
    data object NavigateBack : VerificationCodeEvent()

    /**
     * Navigates to the VaultItemScreen.
     *
     * @property id the id of the item to navigate to.
     */
    data class NavigateToVaultItem(val id: String) : VerificationCodeEvent()
}

/**
 * Models actions for the [VerificationCodeScreen].
 */
sealed class VerificationCodeAction {

    /**
     * Click the back button.
     */
    data object BackClick : VerificationCodeAction()

    /**
     * Navigates to an item.
     *
     * @property id the id of the item to navigate to.
     */
    data class ItemClick(val id: String) : VerificationCodeAction()
}
