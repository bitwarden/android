package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the [VaultMoveToOrganizationScreen].
 *
 * @param savedStateHandle Handles the navigation arguments of this ViewModel.
 */
@HiltViewModel
@Suppress("MaxLineLength")
class VaultMoveToOrganizationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultMoveToOrganizationState, VaultMoveToOrganizationEvent, VaultMoveToOrganizationAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            VaultMoveToOrganizationState(
                vaultItemId = VaultMoveToOrganizationArgs(savedStateHandle).vaultItemId,
                viewState = VaultMoveToOrganizationState.ViewState.Loading,
            )
        },
) {

    override fun handleAction(action: VaultMoveToOrganizationAction) {
        when (action) {
            is VaultMoveToOrganizationAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(VaultMoveToOrganizationEvent.NavigateBack)
    }
}

/**
 * Models state for the [VaultMoveToOrganizationScreen].
 *
 * @property vaultItemId Indicates whether the VM is in add or edit mode.
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class VaultMoveToOrganizationState(
    val vaultItemId: String,
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [VaultMoveToOrganizationScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultMoveToOrganizationScreen].
         *
         * @property message the error message to display.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Represents a loading state for the [VaultMoveToOrganizationScreen].
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultMoveToOrganizationScreen].
         */
        @Parcelize
        data object Content : ViewState()
    }
}

/**
 * Models events for the [VaultMoveToOrganizationScreen].
 */
sealed class VaultMoveToOrganizationEvent {

    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : VaultMoveToOrganizationEvent()

    /**
     * Show a toast with the given message.
     *
     * @property text the text to display.
     */
    data class ShowToast(val text: Text) : VaultMoveToOrganizationEvent()
}

/**
 * Models actions for the [VaultMoveToOrganizationScreen].
 */
sealed class VaultMoveToOrganizationAction {

    /**
     * Click the back button.
     */
    data object BackClick : VaultMoveToOrganizationAction()
}
