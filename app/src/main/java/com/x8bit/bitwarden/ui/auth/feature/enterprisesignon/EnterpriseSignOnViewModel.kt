package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the enterprise single sign on screen.
 */
@HiltViewModel
class EnterpriseSignOnViewModel @Inject constructor(
    private val networkConnectionManager: NetworkConnectionManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<EnterpriseSignOnState, EnterpriseSignOnEvent, EnterpriseSignOnAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
        ),
) {

    override fun handleAction(action: EnterpriseSignOnAction) {
        when (action) {
            EnterpriseSignOnAction.CloseButtonClick -> handleCloseButtonClicked()
            EnterpriseSignOnAction.DialogDismiss -> handleDialogDismissed()
            EnterpriseSignOnAction.LogInClick -> handleLogInClicked()
            is EnterpriseSignOnAction.OrgIdentifierInputChange -> {
                handleOrgIdentifierInputChanged(action)
            }
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(EnterpriseSignOnEvent.NavigateBack)
    }

    private fun handleDialogDismissed() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleLogInClicked() {
        // TODO BIT-816: submit request for single sign on
        sendEvent(EnterpriseSignOnEvent.ShowToast("Not yet implemented."))

        if (!networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                )
            }
            return
        }

        if (state.orgIdentifierInput.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        message = R.string.validation_field_required.asText(
                            R.string.org_identifier.asText(),
                        ),
                    ),
                )
            }
        }
    }

    private fun handleOrgIdentifierInputChanged(
        action: EnterpriseSignOnAction.OrgIdentifierInputChange,
    ) {
        mutableStateFlow.update { it.copy(orgIdentifierInput = action.input) }
    }
}

/**
 * Models state of the enterprise sign on screen.
 */
@Parcelize
data class EnterpriseSignOnState(
    val dialogState: DialogState?,
    val orgIdentifierInput: String,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. It no title
         * is specified a default will be provided.
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the enterprise sign on screen.
 */
sealed class EnterpriseSignOnEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : EnterpriseSignOnEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : EnterpriseSignOnEvent()
}

/**
 * Models actions for the enterprise sign on screen.
 */
sealed class EnterpriseSignOnAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : EnterpriseSignOnAction()

    /**
     * Indicates that the current dialog has been dismissed.
     */
    data object DialogDismiss : EnterpriseSignOnAction()

    /**
     * Indicates that the Log In button has been clicked.
     */
    data object LogInClick : EnterpriseSignOnAction()

    /**
     * Indicates that the organization identifier input has changed.
     */
    data class OrgIdentifierInputChange(
        val input: String,
    ) : EnterpriseSignOnAction()
}
