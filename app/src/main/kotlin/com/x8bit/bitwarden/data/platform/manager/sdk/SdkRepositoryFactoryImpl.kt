package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.sdk.CipherRepository
import com.bitwarden.sdk.ServerCommunicationConfigRepository
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.CookieDiskSource
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkCipherRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkTokenRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.ServerCommunicationConfigRepositoryImpl
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource

/**
 * The default implementation for the [SdkRepositoryFactory].
 */
class SdkRepositoryFactoryImpl(
    private val vaultDiskSource: VaultDiskSource,
    private val cookieDiskSource: CookieDiskSource,
    private val configDiskSource: ConfigDiskSource,
    private val authDiskSource: AuthDiskSource,
) : SdkRepositoryFactory {
    override fun getCipherRepository(
        userId: String,
    ): CipherRepository =
        SdkCipherRepository(
            userId = userId,
            vaultDiskSource = vaultDiskSource,
        )

    override fun getClientManagedTokens(
        userId: String?,
    ): ClientManagedTokens =
        SdkTokenRepository(
            userId = userId,
            authDiskSource = authDiskSource,
        )

    override fun getServerCommunicationConfigRepository(): ServerCommunicationConfigRepository =
        ServerCommunicationConfigRepositoryImpl(
            cookieDiskSource = cookieDiskSource,
            configDiskSource = configDiskSource,
        )
}
