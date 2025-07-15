package com.x8bit.bitwarden.data.platform.manager.sdk

import com.bitwarden.sdk.CipherRepository
import com.x8bit.bitwarden.data.platform.manager.sdk.repository.SdkCipherRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource

/**
 * The default implementation for the [SdkRepositoryFactory].
 */
class SdkRepositoryFactoryImpl(
    private val vaultDiskSource: VaultDiskSource,
) : SdkRepositoryFactory {
    override fun getCipherRepository(
        userId: String,
    ): CipherRepository =
        SdkCipherRepository(
            userId = userId,
            vaultDiskSource = vaultDiskSource,
        )
}
