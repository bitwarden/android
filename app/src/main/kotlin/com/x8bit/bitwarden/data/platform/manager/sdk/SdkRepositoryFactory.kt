package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.sdk.CipherRepository

/**
 * Creates and manages sdk repositories.
 */
interface SdkRepositoryFactory {
    /**
     * Retrieves or creates a [CipherRepository] for use with the Bitwarden SDK.
     */
    fun getCipherRepository(userId: String): CipherRepository

    /**
     * Retrieves or creates a [ClientManagedTokens] for use with the Bitwarden SDK.
     */
    fun getClientManagedTokens(userId: String?): ClientManagedTokens
}
