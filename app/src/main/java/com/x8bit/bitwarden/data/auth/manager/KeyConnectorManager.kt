package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson

/**
 * Manager used to interface with a key connector.
 */
interface KeyConnectorManager {
    /**
     * Retrieves the master key from the key connector.
     */
    suspend fun getMasterKeyFromKeyConnector(
        url: String,
        accessToken: String,
    ): Result<KeyConnectorMasterKeyResponseJson>

    /**
     * Migrates an existing user to use the key connector.
     */
    @Suppress("LongParameterList")
    suspend fun migrateExistingUserToKeyConnector(
        userId: String,
        url: String,
        userKeyEncrypted: String,
        email: String,
        masterPassword: String,
        kdf: Kdf,
    ): Result<Unit>

    /**
     * Migrates a new user to use the key connector.
     */
    @Suppress("LongParameterList")
    suspend fun migrateNewUserToKeyConnector(
        url: String,
        accessToken: String,
        kdfType: KdfTypeJson,
        kdfIterations: Int?,
        kdfMemory: Int?,
        kdfParallelism: Int?,
        organizationIdentifier: String,
    ): Result<KeyConnectorResponse>
}
