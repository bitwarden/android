package com.x8bit.bitwarden.ui.feature.rootnav

import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor() :
    BaseViewModel<RootNavState, Unit, Unit>(
        initialState = RootNavState.Splash,
    ) {

    init {
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(1000)
            mutableStateFlow.value = RootNavState.Login
        }
    }

    override fun handleAction(action: Unit) = Unit
}

/**
 * Models state of the root level navigation of the app.
 */
sealed class RootNavState {
    /**
     * Show the login screen.
     */
    data object Login : RootNavState()

    /**
     * Show the splash screen.
     */
    data object Splash : RootNavState()
}
