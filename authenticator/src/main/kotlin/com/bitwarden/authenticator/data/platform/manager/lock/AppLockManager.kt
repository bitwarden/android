package com.bitwarden.authenticator.data.platform.manager.lock

import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for handling the app's lock state.
 */
interface AppLockManager {
    /**
     * Tracks the current state of the app lock.
     */
    val appLockStateFlow: StateFlow<AppLockState>

    /**
     * Performs a manual app unlock.
     */
    fun manualAppUnlock()
}
