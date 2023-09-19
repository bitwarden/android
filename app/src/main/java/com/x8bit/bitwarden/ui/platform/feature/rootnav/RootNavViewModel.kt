package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_NAV_DESTINATION = "nav_state"

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
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
        authRepository
            .authStateFlow
            .onEach { trySendAction(RootNavAction.AuthStateUpdated(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is RootNavAction.AuthStateUpdated -> handleAuthStateUpdated(action)
        }
    }

    private fun handleAuthStateUpdated(action: RootNavAction.AuthStateUpdated) {
        when (action.newState) {
            is AuthState.Authenticated -> mutableStateFlow.update { RootNavState.VaultUnlocked }
            is AuthState.Unauthenticated -> mutableStateFlow.update { RootNavState.Auth }
            is AuthState.Uninitialized -> mutableStateFlow.update { RootNavState.Splash }
        }
    }
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

/**
 * Models root level navigation actions.
 */
sealed class RootNavAction {

    /**
     * Auth state in the repository layer changed.
     */
    data class AuthStateUpdated(val newState: AuthState) : RootNavAction()
}
