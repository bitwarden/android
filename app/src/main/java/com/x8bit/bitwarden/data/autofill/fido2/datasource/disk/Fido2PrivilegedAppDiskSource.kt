package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk

import com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity.Fido2PrivilegedAppInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to privileged apps trusted for FIDO2
 * operations.
 */
interface Fido2PrivilegedAppDiskSource {

    /**
     * Flow of the user's trusted privileged apps.
     */
    val userTrustedPrivilegedAppsFlow: Flow<List<Fido2PrivilegedAppInfoEntity>>

    /**
     * Retrieves all the user's trusted privileged apps.
     */
    suspend fun getAllUserTrustedPrivilegedApps(): List<Fido2PrivilegedAppInfoEntity>

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
