package com.x8bit.bitwarden.ui.auth.feature.checkemail

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Models logic for the check email screen.
 */
@HiltViewModel
class CheckEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CheckEmailState, CheckEmailEvent, CheckEmailAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: CheckEmailState(
            email = savedStateHandle.toCheckEmailArgs().emailAddress,
        ),
) {
    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CheckEmailAction) {
        when (action) {
            CheckEmailAction.BackClick -> handleBackClick()
            CheckEmailAction.OpenEmailClick -> handleOpenEmailClick()
            CheckEmailAction.ChangeEmailClick -> handleChangeEmailClick()
        }
    }

    private fun handleOpenEmailClick() = sendEvent(CheckEmailEvent.NavigateToEmailApp)

    private fun handleBackClick() = sendEvent(CheckEmailEvent.NavigateBack)

    private fun handleChangeEmailClick() = sendEvent(CheckEmailEvent.NavigateBack)
}

/**
 * UI state for the check email screen.
 */
@Parcelize
data class CheckEmailState(
    val email: String,
) : Parcelable

/**
 * Models events for the check email screen.
 */
sealed class CheckEmailEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CheckEmailEvent()

    /**
     * Navigate to email app.
     */
    data object NavigateToEmailApp : CheckEmailEvent()
}

/**
 * Models actions for the check email screen.
 */
sealed class CheckEmailAction {
    /**
     * User clicked close.
     */
    data object BackClick : CheckEmailAction()

    /**
     * User clicked change email.
     */
    data object ChangeEmailClick : CheckEmailAction()

    /**
     * User clicked open email.
     */
    data object OpenEmailClick : CheckEmailAction()
}
