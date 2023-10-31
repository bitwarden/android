package com.x8bit.bitwarden.ui.auth.feature.environment

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the self-hosted/custom environment screen.
 */
@HiltViewModel
class EnvironmentViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<EnvironmentState, EnvironmentEvent, EnvironmentAction>(
    // TODO: Pull non-saved state from EnvironmentRepository (BIT-817)
    initialState = savedStateHandle[KEY_STATE]
        ?: EnvironmentState(
            serverUrl = "",
            webVaultServerUrl = "",
            apiServerUrl = "",
            identityServerUrl = "",
            iconsServerUrl = "",
        ),
) {

    init {
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: EnvironmentAction): Unit = when (action) {
        is EnvironmentAction.CloseClick -> handleCloseClickAction()
        is EnvironmentAction.SaveClick -> handleSaveClickAction()
        is EnvironmentAction.ServerUrlChange -> handleServerUrlChangeAction(action)
        is EnvironmentAction.WebVaultServerUrlChange -> handleWebVaultServerUrlChangeAction(action)
        is EnvironmentAction.ApiServerUrlChange -> handleApiServerUrlChangeAction(action)
        is EnvironmentAction.IdentityServerUrlChange -> handleIdentityServerUrlChangeAction(action)
        is EnvironmentAction.IconsServerUrlChange -> handleIconsServerUrlChangeAction(action)
    }

    private fun handleCloseClickAction() {
        sendEvent(EnvironmentEvent.NavigateBack)
    }

    private fun handleSaveClickAction() {
        // TODO: Save custom value (BIT-817)
        sendEvent(EnvironmentEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleServerUrlChangeAction(
        action: EnvironmentAction.ServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(serverUrl = action.serverUrl)
        }
    }

    private fun handleWebVaultServerUrlChangeAction(
        action: EnvironmentAction.WebVaultServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(webVaultServerUrl = action.webVaultServerUrl)
        }
    }

    private fun handleApiServerUrlChangeAction(
        action: EnvironmentAction.ApiServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(apiServerUrl = action.apiServerUrl)
        }
    }

    private fun handleIdentityServerUrlChangeAction(
        action: EnvironmentAction.IdentityServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(identityServerUrl = action.identityServerUrl)
        }
    }

    private fun handleIconsServerUrlChangeAction(
        action: EnvironmentAction.IconsServerUrlChange,
    ) {
        mutableStateFlow.update {
            it.copy(iconsServerUrl = action.iconsServerUrl)
        }
    }
}

/**
 * Models the state of the environment screen.
 */
@Parcelize
data class EnvironmentState(
    val serverUrl: String,
    val webVaultServerUrl: String,
    val apiServerUrl: String,
    val identityServerUrl: String,
    val iconsServerUrl: String,
) : Parcelable

/**
 * Models events for the environment screen.
 */
sealed class EnvironmentEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : EnvironmentEvent()

    /**
     * Show a toast with the given message.
     */
    data class ShowToast(
        val message: Text,
    ) : EnvironmentEvent()
}

/**
 * Models actions for the environment screen.
 */
sealed class EnvironmentAction {
    /**
     * User clicked back button.
     */
    data object CloseClick : EnvironmentAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : EnvironmentAction()

    /**
     * Indicates that the overall server URL has changed.
     */
    data class ServerUrlChange(
        val serverUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the web vault server URL has changed.
     */
    data class WebVaultServerUrlChange(
        val webVaultServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the API server URL has changed.
     */
    data class ApiServerUrlChange(
        val apiServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the identity server URL has changed.
     */
    data class IdentityServerUrlChange(
        val identityServerUrl: String,
    ) : EnvironmentAction()

    /**
     * Indicates that the icons server URL has changed.
     */
    data class IconsServerUrlChange(
        val iconsServerUrl: String,
    ) : EnvironmentAction()
}
