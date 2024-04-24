package com.bitwarden.authenticator.ui.platform.feature.rootnav

import android.os.Parcelable
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState(
        hasSeenWelcomeGuide = settingsRepository.hasSeenWelcomeTutorial,
        navState = RootNavState.NavState.Splash,
    )
) {

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
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleHasSeenWelcomeTutorialChange(hasSeenWelcomeGuide: Boolean) {
        if (hasSeenWelcomeGuide) {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.ItemListing) }
        } else {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Tutorial) }
        }
    }

    private fun handleTutorialFinished() {
        settingsRepository.hasSeenWelcomeTutorial = true
        mutableStateFlow.update { it.copy(navState = RootNavState.NavState.ItemListing) }
    }

    private fun handleSplashScreenDismissed() {
        if (settingsRepository.hasSeenWelcomeTutorial) {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.ItemListing) }
        } else {
            mutableStateFlow.update { it.copy(navState = RootNavState.NavState.Tutorial) }
        }
    }
}

/**
 * Models root level destinations for the app.
 */
@Parcelize
data class RootNavState(
    val hasSeenWelcomeGuide: Boolean,
    val navState: NavState,
) : Parcelable {

    @Parcelize
    sealed class NavState : Parcelable {
        /**
         * App should display the Splash nav graph.
         */
        @Parcelize
        data object Splash : NavState()

        /**
         * App should display the Tutorial nav graph.
         */
        @Parcelize
        data object Tutorial : NavState()

        /**
         * App should display the Account List nav graph.
         */
        @Parcelize
        data object ItemListing : NavState()
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

        data object SplashScreenDismissed : Internal()

        data object TutorialFinished : Internal()

        /**
         * Indicates an update in the welcome guide being seen has been received.
         */
        data class HasSeenWelcomeTutorialChange(val hasSeenWelcomeGuide: Boolean) : Internal()
    }
}
