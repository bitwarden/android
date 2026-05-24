package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.core.ClientSettings
import com.bitwarden.sdk.Repositories
import com.bitwarden.sdk.ServerCommunicationConfigRepository

/**
 * Creates and manages sdk repositories.
 */
interface SdkRepositoryFactory {
    /**
     * Retrieves or creates a [Repositories] for use with the Bitwarden SDK.
     */
    fun getRepositories(userId: String?): Repositories

    /**
     * Retrieves or creates a [ClientManagedTokens] for use with the Bitwarden SDK.
     */
    fun getClientManagedTokens(
        userId: String?,
        accessToken: String?,
    ): ClientManagedTokens

    /**
     * Retrieves or creates a [ClientSettings] for use with the Bitwarden SDK.
     */
    fun getClientSettings(): ClientSettings

    /**
     * Retrieves or creates a [ServerCommunicationConfigRepository] for use with the Bitwarden SDK.
     */
    fun getServerCommunicationConfigRepository(): ServerCommunicationConfigRepository
}
