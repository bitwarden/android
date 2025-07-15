package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.sdk.CipherRepository

/**
 * Creates and manages sdk repositories.
 */
interface SdkRepositoryFactory {
    /**
     * Retrieves or creates a [CipherRepository] for use with the Bitwarden SDK.
     */
    fun getCipherRepository(userId: String): CipherRepository
}
