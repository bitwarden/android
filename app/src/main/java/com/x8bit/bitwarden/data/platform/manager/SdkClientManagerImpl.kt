package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.sdk.Client

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val featureFlagManager: FeatureFlagManager,
    private val clientProvider: suspend () -> Client = {
        Client(settings = null).apply {
            platform().loadFlags(featureFlagManager.sdkFeatureFlags)
        }
    },
) : SdkClientManager {
    private val userIdToClientMap = mutableMapOf<String?, Client>()

    override suspend fun getOrCreateClient(
        userId: String?,
    ): Client = userIdToClientMap.getOrPut(key = userId) { clientProvider() }

    override fun destroyClient(
        userId: String?,
    ) {
        userIdToClientMap
            .remove(key = userId)
            ?.close()
    }
}
