package com.bitwarden.testharness.ui.platform.feature.rootnav

import android.os.Parcelable
import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state for the test harness application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor() : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {

    init {
        mutableStateFlow.update { RootNavState.Landing }
    }

    override fun handleAction(action: RootNavAction) {
        // Handle actions if necessary
    }
}

/**
 * Models root level navigation destinations for the test harness.
 */
sealed class RootNavState : Parcelable {

    /**
     * App should show splash nav graph.
     */
    @Parcelize
    data object Splash : RootNavState()

    /**
     * App should show the main landing screen.
     */
    @Parcelize
    data object Landing : RootNavState()
}

/**
 * Models root navigation actions.
 */
sealed class RootNavAction
