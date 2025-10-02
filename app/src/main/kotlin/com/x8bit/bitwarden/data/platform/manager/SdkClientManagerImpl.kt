package com.x8bit.bitwarden.data.platform.manager

import android.os.Build
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    nativeLibraryManager: NativeLibraryManager,
    sdkRepoFactory: SdkRepositoryFactory,
    private val featureFlagManager: FeatureFlagManager,
    private val clientProvider: suspend (userId: String?) -> Client = { userId ->
        Client(
            tokenProvider = sdkRepoFactory.getClientManagedTokens(userId = userId),
            settings = null,
        )
            .apply {
                platform().loadFlags(featureFlagManager.sdkFeatureFlags)
                userId?.let {
                    platform().state().apply {
                        registerCipherRepository(sdkRepoFactory.getCipherRepository(userId = it))
                    }
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
