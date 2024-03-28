package com.x8bit.bitwarden.authenticator.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash
) {

    init {
        viewModelScope.launch {
            delay(250)
            trySendAction(RootNavAction.Internal.StateUpdate)
        }
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            RootNavAction.BackStackUpdate -> handleBackStackUpdate()
            RootNavAction.Internal.StateUpdate -> handleStateUpdate()
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleStateUpdate() {
        mutableStateFlow.update { RootNavState.ItemListing }
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

    sealed class Internal : RootNavAction() {
        data object StateUpdate : Internal()
    }
}
