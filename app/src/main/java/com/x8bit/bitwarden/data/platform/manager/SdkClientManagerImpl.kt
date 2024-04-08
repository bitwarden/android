package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.vault.datasource.sdk.BitwardenFeatureFlagManager

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val featureFlagManager: BitwardenFeatureFlagManager,
    private val clientProvider: suspend () -> Client = {
        Client(settings = null).apply {
            platform().loadFlags(featureFlagManager.featureFlags)
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
