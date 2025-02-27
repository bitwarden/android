package com.bitwarden.authenticator.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state for the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState(
        hasSeenWelcomeGuide = settingsRepository.hasSeenWelcomeTutorial,
        navState = RootNavState.NavState.Splash,
    ),
) {

    init {
        viewModelScope.launch {
            settingsRepository.hasSeenWelcomeTutorialFlow
                .map { RootNavAction.Internal.HasSeenWelcomeTutorialChange(it) }
                .onEach(::sendAction)
                .launchIn(viewModelScope)
        }
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            RootNavAction.BackStackUpdate -> {
                handleBackStackUpdate()
            }

            is RootNavAction.Internal.HasSeenWelcomeTutorialChange -> {
                handleHasSeenWelcomeTutorialChange(action.hasSeenWelcomeGuide)
            }

            RootNavAction.Internal.TutorialFinished -> {
                handleTutorialFinished()
            }

            RootNavAction.Internal.SplashScreenDismissed -> {
                handleSplashScreenDismissed()
            }

            RootNavAction.Internal.AppUnlocked -> {
                handleAppUnlocked()
            }
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleHasSeenWelcomeTutorialChange(hasSeenWelcomeGuide: Boolean) {
        settingsRepository.hasSeenWelcomeTutorial = hasSeenWelcomeGuide
        if (hasSeenWelcomeGuide) {
            if (settingsRepository.isUnlockWithBiometricsEnabled &&
                biometricsEncryptionManager.isBiometricIntegrityValid()) {
                mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Locked) }
            } else {
                mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Unlocked) }
            }
        } else {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Tutorial) }
        }
    }

    private fun handleTutorialFinished() {
        settingsRepository.hasSeenWelcomeTutorial = true
        mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Unlocked) }
    }

    private fun handleSplashScreenDismissed() {
        if (settingsRepository.hasSeenWelcomeTutorial) {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Unlocked) }
        } else {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Tutorial) }
        }
    }

    private fun handleAppUnlocked() {
        mutableStateFlow.update {
            it.copy(navState = RootNavState.NavState.Unlocked)
        }
    }
}

/**
 * Models root level navigation state for the app.
 *
 * @property hasSeenWelcomeGuide Indicates if the user has seen the Welcome Guide screen.
 * @property navState Current destination state of the app.
 */
@Parcelize
data class RootNavState(
    val hasSeenWelcomeGuide: Boolean,
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
     * Models actions the [RootNavViewModel] itself may send.
     */
    sealed class Internal : RootNavAction() {

        /**
         * Splash screen has been dismissed.
         */
        data object SplashScreenDismissed : Internal()

        /**
         * Indicates the user finished or skipped opening tutorial slides.
         */
        data object TutorialFinished : Internal()

        /**
         * Indicates the application has been unlocked.
         */
        data object AppUnlocked : Internal()

        /**
         * Indicates an update in the welcome guide being seen has been received.
         */
        data class HasSeenWelcomeTutorialChange(val hasSeenWelcomeGuide: Boolean) : Internal()
    }
}
