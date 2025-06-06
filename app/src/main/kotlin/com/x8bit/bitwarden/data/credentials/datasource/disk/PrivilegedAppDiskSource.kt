package com.x8bit.bitwarden.data.credentials.datasource.disk

import com.x8bit.bitwarden.data.credentials.datasource.disk.entity.PrivilegedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to privileged apps trusted for
 * Credential Manager operations.
 */
interface PrivilegedAppDiskSource {

    /**
     * Flow of the user's trusted privileged apps.
     */
    val userTrustedPrivilegedAppsFlow: Flow<List<PrivilegedAppEntity>>

    /**
     * Retrieves all the user's trusted privileged apps.
     */
    suspend fun getAllUserTrustedPrivilegedApps(): List<PrivilegedAppEntity>

    /**
     * Adds a privileged app to the user's trusted list.
     */
    suspend fun addTrustedPrivilegedApp(packageName: String, signature: String)

    /**
     * Removes a privileged app from the user's trusted list.
     */
    suspend fun removeTrustedPrivilegedApp(packageName: String, signature: String)

    /**
     * Checks if a privileged app is trusted.
     */
    suspend fun isPrivilegedAppTrustedByUser(packageName: String, signature: String): Boolean
}
