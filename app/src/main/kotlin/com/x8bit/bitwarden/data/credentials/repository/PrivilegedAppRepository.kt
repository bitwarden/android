package com.x8bit.bitwarden.data.credentials.repository

import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing privileged apps trusted by the user.
 */
interface PrivilegedAppRepository {

    /**
     * Flow of the user's trusted privileged apps.
     */
    val userTrustedPrivilegedAppsFlow: Flow<PrivilegedAppAllowListJson>

    /**
     * List the user's trusted privileged apps.
     */
    suspend fun getAllUserTrustedPrivilegedApps(): PrivilegedAppAllowListJson

    /**
     * Returns true if the given [packageName] and [signature] are trusted.
     */
    suspend fun isPrivilegedAppAllowed(packageName: String, signature: String): Boolean

    /**
     * Adds the given [packageName] and [signature] to the list of trusted privileged apps.
     */
    suspend fun addTrustedPrivilegedApp(packageName: String, signature: String)

    /**
     * Removes the given [packageName] and [signature] from the list of trusted privileged apps.
     */
    suspend fun removeTrustedPrivilegedApp(packageName: String, signature: String)

    /**
     * Returns the JSON representation of the user's trusted privileged apps.
     */
    suspend fun getUserTrustedAllowListJson(): String
}
