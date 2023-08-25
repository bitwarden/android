package com.x8bit.bitwarden.ui.feature.rootnav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<RootNavState>(RootNavState.Splash)
    val state: StateFlow<RootNavState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(1000)
            _state.value = RootNavState.Login
        }
    }
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
