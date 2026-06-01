package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult

/**
 * An interface to manage the user KDF settings.
 */
interface KdfManager {

    /**
     * Checks if user's current KDF settings are below the minimums and needs update
     */
    fun needsKdfUpdateToMinimums(): Boolean

    /**
     * Updates the user's KDF settings if below the minimums
     */
    suspend fun updateKdfToMinimumsIfNeeded(password: String): UpdateKdfMinimumsResult
}
