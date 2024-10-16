package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager for compiling the state of all first time actions and related information such
 * as counts of notifications to show, etc.
 */
interface FirstTimeActionManager {

    /**
     * Returns an observable count of the number of settings items that have a badge to display
     * for the current active user.
     */
    val allSettingsBadgeCountFlow: StateFlow<Int>

    /**
     * Returns an observable count of the number of security settings items that have a badge to
     * display for the current active user.
     */
    val allSecuritySettingsBadgeCountFlow: StateFlow<Int>

    /**
     * Returns an observable count of the number of autofill settings items that have a badge to
     * display for the current active user.
     */
    val allAutofillSettingsBadgeCountFlow: StateFlow<Int>

    /**
     * Returns an observable count of the number of vault settings items that have a badge to
     * display for the current active user.
     */
    val allVaultSettingsBadgeCountFlow: StateFlow<Int>

    /**
     * Returns a [Flow] that emits every time the active user's first time state is changed.
     */
    val firstTimeStateFlow: Flow<FirstTimeState>

    /**
     * Get the current [FirstTimeState] of the active user if available, otherwise return
     * a default configuration.
     */
    val currentOrDefaultUserFirstTimeState: FirstTimeState
}
