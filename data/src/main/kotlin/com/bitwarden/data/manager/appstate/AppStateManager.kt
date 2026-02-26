package com.bitwarden.data.manager.appstate

import com.bitwarden.data.manager.appstate.model.AppCreationState
import com.bitwarden.data.manager.appstate.model.AppForegroundState
import kotlinx.coroutines.flow.StateFlow

/**
 * A manager for tracking app foreground state changes.
 */
interface AppStateManager {
    /**
     * Emits whenever there are changes to the app creation state.
     *
     * This is required because the Accessibility Service will keep the app process alive when the
     * app would otherwise be destroyed.
     */
    val appCreatedStateFlow: StateFlow<AppCreationState>

    /**
     * Emits whenever there are changes to the app foreground state.
     */
    val appForegroundStateFlow: StateFlow<AppForegroundState>
}
