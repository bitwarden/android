package com.x8bit.bitwarden.ui.platform.feature.overlaynav

import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkPermissionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Manages the overlay navigation, hosting the root-navigation and any screen that can overlay it.
 */
@HiltViewModel
class OverlayNavViewModel @Inject constructor(
    cookieAcquisitionRequestManager: CookieAcquisitionRequestManager,
    networkPermissionManager: NetworkPermissionManager,
    settingsRepository: SettingsRepository,
) : BaseViewModel<Unit, OverlayNavEvent, OverlayNavAction>(initialState = Unit) {
    init {
        settingsRepository
            .hasShownAccessibilityDisclaimerFlow
            .filterNot { it }
            .map { OverlayNavAction.Internal.AccessibilityDisclosureRequired }
            .onEach(::trySendAction)
            .launchIn(viewModelScope)

        networkPermissionManager
            .isLocalNetworkAccessRequiredStateFlow
            .filter { it }
            .map { OverlayNavAction.Internal.LocalNetworkAccessRequired }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        cookieAcquisitionRequestManager
            .cookieAcquisitionRequestFlow
            .filterNotNull()
            .map { OverlayNavAction.Internal.CookieAcquisitionReady }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: OverlayNavAction) {
        when (action) {
            is OverlayNavAction.Internal -> handleInternal(action)
        }
    }

    private fun handleInternal(action: OverlayNavAction.Internal) {
        when (action) {
            OverlayNavAction.Internal.AccessibilityDisclosureRequired -> {
                handleAccessibilityDisclosureRequired()
            }

            OverlayNavAction.Internal.CookieAcquisitionReady -> handleCookieAcquisitionReady()
            OverlayNavAction.Internal.LocalNetworkAccessRequired -> {
                handleLocalNetworkAccessRequired()
            }
        }
    }

    private fun handleAccessibilityDisclosureRequired() {
        sendEvent(OverlayNavEvent.NavigateToAccessibilityDisclosure)
    }

    private fun handleCookieAcquisitionReady() {
        sendEvent(OverlayNavEvent.NavigateToCookieAcquisition)
    }

    private fun handleLocalNetworkAccessRequired() {
        sendEvent(OverlayNavEvent.NavigateToLocalNetworkAccess)
    }
}

/**
 * Models events for the overlay navigation screen.
 */
sealed class OverlayNavEvent {
    /**
     * Navigate to the cookie acquisition screen.
     */
    data object NavigateToCookieAcquisition : OverlayNavEvent(), DeferredBackgroundEvent

    /**
     * Navigate to the local network access screen.
     */
    data object NavigateToLocalNetworkAccess : OverlayNavEvent(), DeferredBackgroundEvent

    /**
     * Navigate to the accessibility disclosure screen.
     */
    data object NavigateToAccessibilityDisclosure : OverlayNavEvent(), DeferredBackgroundEvent
}

/**
 * Models actions for the overlay navigation screen.
 */
sealed class OverlayNavAction {
    /**
     * Internal ViewModel actions.
     */
    sealed class Internal : OverlayNavAction() {

        /**
         * Indicates that the cookie acquisition conditions are met and navigation
         * should proceed.
         */
        data object CookieAcquisitionReady : Internal()

        /**
         * Indicates that the local network access is required.
         */
        data object LocalNetworkAccessRequired : Internal()

        /**
         * Indicates that the accessibility disclosure needs to be displayed.
         */
        data object AccessibilityDisclosureRequired : Internal()
    }
}
