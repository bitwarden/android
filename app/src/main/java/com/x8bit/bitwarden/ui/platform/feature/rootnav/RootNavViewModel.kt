package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
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
            mutableStateFlow.value = RootNavState.Auth
        }
    }

    override fun handleAction(action: Unit) = Unit
}

/**
 * Models state of the root level navigation of the app.
 */
sealed class RootNavState {
    /**
     * Show the vault unlocked screen.
     */
    data object VaultUnlocked : RootNavState()

    /**
     * Show the auth screens.
     */
    data object Auth : RootNavState()

    /**
     * Show the splash screen.
     */
    data object Splash : RootNavState()
}
