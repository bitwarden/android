package com.x8bit.bitwarden.data.autofill.fido2.repository

import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppData
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing privileged apps trusted by the user.
 */
interface PrivilegedAppRepository {

    /**
     * Flow that represents the trusted privileged apps data.
     */
    val trustedAppDataStateFlow: StateFlow<DataState<PrivilegedAppData>>

    /**
     * Flow of the user's trusted privileged apps.
     */
    val userTrustedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>

    /**
     * Flow of the Google's trusted privileged apps.
     */
    val googleTrustedPrivilegedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>

    /**
     * Flow of the community's trusted privileged apps.
     */
    val communityTrustedAppsFlow: StateFlow<DataState<PrivilegedAppAllowListJson>>

    /**
     * List the user's trusted privileged apps.
     */
    suspend fun getUserTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson?

    /**
     * List Google's trusted privileged apps.
     */
    suspend fun getGoogleTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson?

    /**
     * List community's trusted privileged apps.
     */
    suspend fun getCommunityTrustedPrivilegedAppsOrNull(): PrivilegedAppAllowListJson?

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
