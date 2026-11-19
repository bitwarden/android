package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult

/**
 * An interface to manage the user KDF settings.
 */
interface KdfManager {

    /**
     * Checks if the KDF settings of the user with the given [userId] are below the minimums and
     * need an update.
     */
    fun needsKdfUpdateToMinimums(userId: String): Boolean

    /**
     * Updates the KDF settings of the user with the given [userId] if below the minimums.
     *
     * The update is only performed when [userId] is the active user, otherwise
     * [UpdateKdfMinimumsResult.ActiveAccountNotFound] is returned.
     */
    suspend fun updateKdfToMinimumsIfNeeded(
        userId: String,
        password: String,
    ): UpdateKdfMinimumsResult
}
