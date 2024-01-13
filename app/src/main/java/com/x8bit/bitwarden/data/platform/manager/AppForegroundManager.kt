package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.StateFlow

/**
 * A manager for tracking app foreground state changes.
 */
interface AppForegroundManager {

    /**
     * Emits whenever there are changes to the app foreground state.
     */
    val appForegroundStateFlow: StateFlow<AppForegroundState>
}
