package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.sdk.CipherRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkCipherRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkTokenRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource

/**
 * The default implementation for the [SdkRepositoryFactory].
 */
class SdkRepositoryFactoryImpl(
    private val vaultDiskSource: VaultDiskSource,
    private val bitwardenServiceClient: BitwardenServiceClient,
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
            tokenProvider = bitwardenServiceClient.tokenProvider,
        )
}
