package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.autofill.accessibility.BitwardenAccessibilityService
import com.x8bit.bitwarden.data.platform.manager.model.AppCreationState
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.StateFlow

/**
 * A manager for tracking app foreground state changes.
 */
interface AppStateManager {
    /**
     * Emits whenever there are changes to the app creation state.
     *
     * This is required because the [BitwardenAccessibilityService] will keep the app process alive
     * when the app would otherwise be destroyed.
     */
    val appCreatedStateFlow: StateFlow<AppCreationState>

    /**
     * Emits whenever there are changes to the app foreground state.
     */
    val appForegroundStateFlow: StateFlow<AppForegroundState>
}
