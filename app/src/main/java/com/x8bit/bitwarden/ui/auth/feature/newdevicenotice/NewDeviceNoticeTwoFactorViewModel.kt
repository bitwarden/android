package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import android.os.Parcelable
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.RemindMeLaterClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Manages application state for the new device notice two factor screen.
 */
@HiltViewModel
class NewDeviceNoticeTwoFactorViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val environmentRepository: EnvironmentRepository,
    val featureFlagManager: FeatureFlagManager,
) : BaseViewModel<
    NewDeviceNoticeTwoFactorState,
    NewDeviceNoticeTwoFactorEvent,
    NewDeviceNoticeTwoFactorAction,
    >(
    initialState = NewDeviceNoticeTwoFactorState(
        shouldShowRemindMeLater = !featureFlagManager.getFeatureFlag(
            FlagKey.NewDevicePermanentDismiss,
        ),
    ),
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
                authRepository.setNewDeviceNoticeState(
                    NewDeviceNoticeState(
                        displayStatus = NewDeviceNoticeDisplayStatus.HAS_SEEN,
                        delayDate = ZonedDateTime.now(),
                    ),
                )
                sendEvent(NewDeviceNoticeTwoFactorEvent.NavigateBackToVault)
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
     * Navigates back to vault.
     */
    data object NavigateBackToVault : NewDeviceNoticeTwoFactorEvent()
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

/**
 * Models state of the new device notice two factor screen.
 */
@Parcelize
data class NewDeviceNoticeTwoFactorState(
    val shouldShowRemindMeLater: Boolean,
) : Parcelable
