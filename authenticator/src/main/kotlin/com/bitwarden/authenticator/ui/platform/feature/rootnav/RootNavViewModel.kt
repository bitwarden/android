package com.bitwarden.authenticator.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state for the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState(
        navState = RootNavState.NavState.Splash,
    ),
) {
    init {
        combine(
            authRepository.appLockStateFlow,
            settingsRepository.hasSeenWelcomeTutorialFlow,
        ) { appLockState, hasSeenWelcomeTutorial ->
            RootNavAction.Internal.StateReceived(
                appLockState = appLockState,
                hasSeenWelcomeTutorial = hasSeenWelcomeTutorial,
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            RootNavAction.BackStackUpdate -> handleBackStackUpdate()
            is RootNavAction.BiometricSupportChanged -> handleBiometricSupportChanged(action)
            is RootNavAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: RootNavAction.Internal) {
        when (action) {
            is RootNavAction.Internal.StateReceived -> handleStateReceived(action)
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleBiometricSupportChanged(
        action: RootNavAction.BiometricSupportChanged,
    ) {
        if (!action.isBiometricsSupported) {
            authRepository.clearBiometrics()
            // State-based navigation should trigger an unlock now.
        }
    }

    private fun handleStateReceived(action: RootNavAction.Internal.StateReceived) {
        val newState = if (action.hasSeenWelcomeTutorial) {
            when (action.appLockState) {
                AppLockState.LOCKED -> RootNavState.NavState.Locked
                AppLockState.UNLOCKED -> RootNavState.NavState.Unlocked
            }
        } else {
            RootNavState.NavState.Tutorial
        }
        mutableStateFlow.update { it.copy(navState = newState) }
    }
}

/**
 * Models root level navigation state for the app.
 *
 * @property navState Current destination state of the app.
 */
@Parcelize
data class RootNavState(
    val navState: NavState,
) : Parcelable {

    /**
     * Models root level destinations for the app.
     */
    @Parcelize
    sealed class NavState : Parcelable {
        /**
         * App should display the Splash nav graph.
         */
        @Parcelize
        data object Splash : NavState()

        /**
         * App should display the Unlock screen.
         */
        @Parcelize
        data object Locked : NavState()

        /**
         * App should display the Tutorial nav graph.
         */
        @Parcelize
        data object Tutorial : NavState()

        /**
         * App should display the Account List nav graph.
         */
        @Parcelize
        data object Unlocked : NavState()
    }
}

/**
 * Models root navigation actions.
 */
sealed class RootNavAction {
    /**
     * Indicates the backstack has changed.
     */
    data object BackStackUpdate : RootNavAction()

    /**
     * Indicates an update on device biometrics support.
     */
    data class BiometricSupportChanged(val isBiometricsSupported: Boolean) : RootNavAction()

    /**
     * Models actions the [RootNavViewModel] itself may send.
     */
    sealed class Internal : RootNavAction() {
        /**
         * Received all data required to determine the state of the top-level UI.
         */
        data class StateReceived(
            val appLockState: AppLockState,
            val hasSeenWelcomeTutorial: Boolean,
        ) : Internal()
    }
}
