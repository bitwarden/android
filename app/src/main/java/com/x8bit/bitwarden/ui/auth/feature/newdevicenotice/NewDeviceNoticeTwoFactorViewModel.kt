package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import android.os.Parcelable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.ContinueDialogClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.DismissDialogClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.RemindMeLaterClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeTwoFactorDialogState.TurnOnTwoFactorDialog
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
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
            ChangeAccountEmailClick -> updateDialogState(newState = ChangeAccountEmailDialog)

            TurnOnTwoFactorClick -> updateDialogState(newState = TurnOnTwoFactorDialog)

            RemindMeLaterClick -> handleRemindMeLater()

            DismissDialogClick -> updateDialogState(newState = null)

            ContinueDialogClick -> handleContinueDialog()
        }
    }

    private fun handleRemindMeLater() {
        authRepository.setNewDeviceNoticeState(
            NewDeviceNoticeState(
                displayStatus = NewDeviceNoticeDisplayStatus.HAS_SEEN,
                lastSeenDate = null,
            ),
        )
        sendEvent(NewDeviceNoticeTwoFactorEvent.NavigateBackToVault)
    }

    private fun handleContinueDialog() {
        when (state.dialogState) {
            is ChangeAccountEmailDialog -> {
                sendEvent(
                    NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail(url = webAccountUrl),
                )
                updateDialogState(newState = null)
            }

            is TurnOnTwoFactorDialog -> {
                sendEvent(
                    NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor(url = webTwoFactorUrl),
                )
                updateDialogState(newState = null)
            }

            null -> return
        }
    }

    private fun updateDialogState(newState: NewDeviceNoticeTwoFactorDialogState?) {
        mutableStateFlow.update {
            it.copy(dialogState = newState)
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

    /**
     * User tapped the dismiss dialog button.
     */
    data object DismissDialogClick : NewDeviceNoticeTwoFactorAction()

    /**
     * User tapped the continue dialog button.
     */
    data object ContinueDialogClick : NewDeviceNoticeTwoFactorAction()
}

/**
 * Models state of the new device notice two factor screen.
 */
@Parcelize
data class NewDeviceNoticeTwoFactorState(
    val dialogState: NewDeviceNoticeTwoFactorDialogState? = null,
    val shouldShowRemindMeLater: Boolean,
) : Parcelable

/**
 * Dialog states for the new device notice two factor screen.
 */
sealed class NewDeviceNoticeTwoFactorDialogState(
    val message: Text,
) : Parcelable {
    /**
     * Represents the turn on two factor dialog.
     */
    @Parcelize
    data object TurnOnTwoFactorDialog : NewDeviceNoticeTwoFactorDialogState(
        message = R.string.two_step_login_description_long.asText(),
    )

    /**
     * Represents the change account email dialog.
     */
    @Parcelize
    data object ChangeAccountEmailDialog : NewDeviceNoticeTwoFactorDialogState(
        R.string.you_can_change_your_account_email_on_the_bitwarden_web_app.asText(),
    )
}
