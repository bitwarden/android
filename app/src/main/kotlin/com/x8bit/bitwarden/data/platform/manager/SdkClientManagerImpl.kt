package com.x8bit.bitwarden.data.platform.manager

import android.os.Build
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.sdk.CipherRepository
import com.bitwarden.sdk.Client
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipherResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import kotlinx.coroutines.flow.firstOrNull

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val featureFlagManager: FeatureFlagManager,
    nativeLibraryManager: NativeLibraryManager,
    private val vaultDiskSource: VaultDiskSource,
    private val clientProvider: suspend (String?) -> Client = { userId ->
        Client(settings = null).apply {
            platform().loadFlags(featureFlagManager.sdkFeatureFlags)
            if (userId != null) {
                platform().state()
                    .registerCipherRepository(CipherRepositoryImpl(userId, vaultDiskSource));
            }
        }
    },
) : SdkClientManager {
    private val userIdToClientMap = mutableMapOf<String?, Client>()

    init {
        // The SDK requires access to Android APIs that were not made public until API 31. In order
        // to work around this limitation the SDK must be manually loaded prior to initializing any
        // [Client] instance.
        if (!isBuildVersionAtLeast(Build.VERSION_CODES.S)) {
            nativeLibraryManager.loadLibrary("bitwarden_uniffi")
        }
    }

    override suspend fun getOrCreateClient(
        userId: String?,
    ): Client = userIdToClientMap.getOrPut(key = userId) { clientProvider(userId) }

    override fun destroyClient(
        userId: String?,
    ) {
        userIdToClientMap
            .remove(key = userId)
            ?.close()
    }
}

// TODO: This should probably be moved somewhere else?
class CipherRepositoryImpl(private val userId: String, private val vaultDiskSource: VaultDiskSource): CipherRepository {
    override suspend fun get(id: String): Cipher? {
        return vaultDiskSource.getCiphers(userId).firstOrNull()
            .orEmpty().firstOrNull { it.id == id }?.toEncryptedSdkCipher()
    }

    override suspend fun has(id: String): Boolean {
        return this.get(id) != null
    }

    override suspend fun list(): List<Cipher> {
        return vaultDiskSource.getCiphers(userId).firstOrNull()
            .orEmpty().map { it.toEncryptedSdkCipher() }
    }

    override suspend fun set(id: String, value: Cipher) {
        assert(value.id == id)
        vaultDiskSource.saveCipher(userId, value.toEncryptedNetworkCipherResponse(userId))
    }

    override suspend fun remove(id: String) {
        vaultDiskSource.deleteCipher(userId, id)
    }
}
