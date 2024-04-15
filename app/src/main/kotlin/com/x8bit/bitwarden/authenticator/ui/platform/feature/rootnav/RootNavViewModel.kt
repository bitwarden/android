package com.x8bit.bitwarden.authenticator.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash
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
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleHasSeenWelcomeTutorialChange(hasSeenWelcomeGuide: Boolean) {
        if (hasSeenWelcomeGuide) {
            mutableStateFlow.update { RootNavState.ItemListing }
        } else {
            mutableStateFlow.update { RootNavState.Tutorial }
        }
    }
}

/**
 * Models root level destinations for the app.
 */
sealed class RootNavState : Parcelable {

    /**
     * App should display the Splash nav graph.
     */
    @Parcelize
    data object Splash : RootNavState()

    /**
     * App should display the Tutorial nav graph.
     */
    @Parcelize
    data object Tutorial : RootNavState()

    /**
     * App should display the Account List nav graph.
     */
    @Parcelize
    data object ItemListing : RootNavState()
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
         * Indicates an update in the welcome guide being seen has been received.
         */
        data class HasSeenWelcomeTutorialChange(val hasSeenWelcomeGuide: Boolean) : Internal()
    }
}
