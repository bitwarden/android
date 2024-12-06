package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.RemindMeLaterClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Manages application state for the new device notice two factor screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class NewDeviceNoticeTwoFactorViewModel @Inject constructor(
    val environmentRepository: EnvironmentRepository,
) : BaseViewModel<Unit, NewDeviceNoticeTwoFactorEvent, NewDeviceNoticeTwoFactorAction>(
    initialState = Unit,
) {
    private val webTwoFactorUrl: String
        get() {
            val baseUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            return "$baseUrl/#/settings/security/two-factor"
        }

    private val webAccountUrl: String
        get() {
            val baseUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            return "$baseUrl/#/settings/account"
        }

    override fun handleAction(action: NewDeviceNoticeTwoFactorAction) {
        when (action) {
            ChangeAccountEmailClick -> sendEvent(
                NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail(
                    url = webAccountUrl,
                ),
            )

            TurnOnTwoFactorClick -> sendEvent(
                NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor(
                    url = webTwoFactorUrl,
                ),
            )

            RemindMeLaterClick -> {
                // TODO PM-8217: Add logic to remind me later
                sendEvent(NewDeviceNoticeTwoFactorEvent.NavigateBack)
            }
        }
    }
}

/**
 * Models events for the new device notice two factor screen.
 */
sealed class NewDeviceNoticeTwoFactorEvent {
    /**
     * Navigates to the turn on two factor url.
     * @param url The url to navigate to.
     */
    data class NavigateToTurnOnTwoFactor(val url: String) : NewDeviceNoticeTwoFactorEvent()

    /**
     * Navigates to the change account email url.
     * @param url The url to navigate to.
     */
    data class NavigateToChangeAccountEmail(val url: String) : NewDeviceNoticeTwoFactorEvent()

    /**
     * Navigates back.
     */
    data object NavigateBack : NewDeviceNoticeTwoFactorEvent()
}

/**
 * Models actions for the new device notice two factor screen.
 */
sealed class NewDeviceNoticeTwoFactorAction {
    /**
     * User tapped the turn on two factor button.
     */
    data object TurnOnTwoFactorClick : NewDeviceNoticeTwoFactorAction()

    /**
     * User tapped the change account email button.
     */
    data object ChangeAccountEmailClick : NewDeviceNoticeTwoFactorAction()

    /**
     * User tapped the remind me later button.
     */
    data object RemindMeLaterClick : NewDeviceNoticeTwoFactorAction()
}
