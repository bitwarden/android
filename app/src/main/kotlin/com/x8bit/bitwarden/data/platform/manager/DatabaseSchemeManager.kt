package com.x8bit.bitwarden.data.platform.manager

import kotlinx.coroutines.flow.Flow

/**
 * Manager for tracking changes to database scheme(s).
 */
interface DatabaseSchemeManager {
    /**
     * Clears the sync state for all users and emits on the [databaseSchemeChangeFlow].
     */
    fun clearSyncState()

    /**
     * Emits whenever the sync state hs been cleared.
     */
    val databaseSchemeChangeFlow: Flow<Unit>
}
