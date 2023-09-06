package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_NAV_DESTINATION = "nav_state"

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<RootNavState, Unit, Unit>(
    initialState = RootNavState.Splash,
) {

    private var savedRootNavState: RootNavState?
        get() = savedStateHandle[KEY_NAV_DESTINATION]
        set(value) {
            savedStateHandle[KEY_NAV_DESTINATION] = value
        }

    init {
        savedRootNavState?.let { savedState: RootNavState ->
            mutableStateFlow.update { savedState }
        }
        // Every time the nav state changes, update saved state handle:
        stateFlow
            .onEach { savedRootNavState = it }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            @Suppress("MagicNumber")
            delay(1000)
            mutableStateFlow.update { RootNavState.Auth }
        }
    }

    override fun handleAction(action: Unit) = Unit
}

/**
 * Models root level destinations for the app.
 */
sealed class RootNavState : Parcelable {
    /**
     * App should show auth nav graph.
     */
    @Parcelize
    data object Auth : RootNavState()

    /**
     * App should show splash nav graph.
     */
    @Parcelize
    data object Splash : RootNavState()

    /**
     * App should show vault unlocked nav graph.
     */
    @Parcelize
    data object VaultUnlocked : RootNavState()
}
