package com.x8bit.bitwarden.ui.platform.feature.localnetworkaccess

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.platform.manager.network.NetworkPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel for the Local Network Access screen.
 */
@HiltViewModel
class LocalNetworkAccessViewModel @Inject constructor(
    private val networkPermissionManager: NetworkPermissionManager,
) : BaseViewModel<LocalNetworkAccessState, LocalNetworkAccessEvent, LocalNetworkAccessAction>(
    initialState = LocalNetworkAccessState,
) {
    override fun handleAction(action: LocalNetworkAccessAction) {
        when (action) {
            LocalNetworkAccessAction.CloseClick -> handleCloseClick()
            LocalNetworkAccessAction.ContinueWithoutPermissionClick -> {
                handleContinueWithoutPermissionClick()
            }

            is LocalNetworkAccessAction.Resumed -> handleResumed(action)
            LocalNetworkAccessAction.SettingsClick -> handleSettingsClick()
        }
    }

    private fun handleCloseClick() {
        networkPermissionManager.clearIsLocalNetworkAccessRequired()
        sendEvent(LocalNetworkAccessEvent.NavigateBack)
    }

    private fun handleContinueWithoutPermissionClick() {
        networkPermissionManager.clearIsLocalNetworkAccessRequired()
        sendEvent(LocalNetworkAccessEvent.NavigateBack)
    }

    private fun handleResumed(action: LocalNetworkAccessAction.Resumed) {
        if (action.hasLocalNetworkAccessPermission) {
            networkPermissionManager.clearIsLocalNetworkAccessRequired()
            sendEvent(LocalNetworkAccessEvent.NavigateBack)
        }
    }

    private fun handleSettingsClick() {
        sendEvent(LocalNetworkAccessEvent.NavigateToSettings)
    }
}

/**
 * State for the Local Network Access screen.
 */
@Parcelize
data object LocalNetworkAccessState : Parcelable

/**
 * Events for the Local Network Access screen.
 */
sealed class LocalNetworkAccessEvent {
    /**
     * Navigate away from this screen.
     */
    data object NavigateBack : LocalNetworkAccessEvent()

    /**
     * Navigate to the OS settings.
     */
    data object NavigateToSettings : LocalNetworkAccessEvent()
}

/**
 * Actions for the Local Network Access screen.
 */
sealed class LocalNetworkAccessAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : LocalNetworkAccessAction()

    /**
     * The user has clicked the continue without permission button.
     */
    data object ContinueWithoutPermissionClick : LocalNetworkAccessAction()

    /**
     * The user has clicked the Settings button.
     */
    data object SettingsClick : LocalNetworkAccessAction()

    /**
     * The screen has resumed.
     */
    data class Resumed(
        val hasLocalNetworkAccessPermission: Boolean,
    ) : LocalNetworkAccessAction()
}
