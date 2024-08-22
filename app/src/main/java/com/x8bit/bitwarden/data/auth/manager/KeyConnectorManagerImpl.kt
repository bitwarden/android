package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource

/**
 * The default implementation of the [KeyConnectorManager].
 */
class KeyConnectorManagerImpl(
    private val accountsService: AccountsService,
    private val authSdkSource: AuthSdkSource,
    private val vaultSdkSource: VaultSdkSource,
) : KeyConnectorManager {
    override suspend fun getMasterKeyFromKeyConnector(
        url: String,
        accessToken: String,
    ): Result<KeyConnectorMasterKeyResponseJson> =
        accountsService.getMasterKeyFromKeyConnector(
            url = url,
            accessToken = accessToken,
        )

    override suspend fun migrateExistingUserToKeyConnector(
        userId: String,
        url: String,
        userKeyEncrypted: String,
        email: String,
        masterPassword: String,
        kdf: Kdf,
    ): Result<Unit> =
        vaultSdkSource
            .deriveKeyConnector(
                userId = userId,
                userKeyEncrypted = userKeyEncrypted,
                email = email,
                password = masterPassword,
                kdf = kdf,
            )
            .flatMap { masterKey ->
                accountsService.storeMasterKeyToKeyConnector(url = url, masterKey = masterKey)
            }
            .flatMap { accountsService.convertToKeyConnector() }

    override suspend fun migrateNewUserToKeyConnector(
        url: String,
        accessToken: String,
        kdfType: KdfTypeJson,
        kdfIterations: Int?,
        kdfMemory: Int?,
        kdfParallelism: Int?,
        organizationIdentifier: String,
    ): Result<KeyConnectorResponse> =
        authSdkSource
            .makeKeyConnectorKeys()
            .flatMap { keyConnectorResponse ->
                accountsService
                    .storeMasterKeyToKeyConnector(
                        url = url,
                        accessToken = accessToken,
                        masterKey = keyConnectorResponse.masterKey,
                    )
                    .flatMap {
                        accountsService.setKeyConnectorKey(
                            accessToken = accessToken,
                            body = KeyConnectorKeyRequestJson(
                                userKey = keyConnectorResponse.encryptedUserKey,
                                keys = KeyConnectorKeyRequestJson.Keys(
                                    publicKey = keyConnectorResponse.keys.public,
                                    encryptedPrivateKey = keyConnectorResponse.keys.private,
                                ),
                                kdfType = kdfType,
                                kdfIterations = kdfIterations,
                                kdfMemory = kdfMemory,
                                kdfParallelism = kdfParallelism,
                                organizationIdentifier = organizationIdentifier,
                            ),
                        )
                    }
                    .map { keyConnectorResponse }
            }
}
