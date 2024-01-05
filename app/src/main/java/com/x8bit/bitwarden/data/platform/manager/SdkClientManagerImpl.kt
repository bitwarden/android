package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.sdk.Client

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val clientProvider: () -> Client = { Client(null) },
) : SdkClientManager {
    private val userIdToClientMap = mutableMapOf<String, Client>()

    override fun getOrCreateClient(
        userId: String,
    ): Client =
        userIdToClientMap.getOrPut(key = userId) { clientProvider() }

    override fun destroyClient(
        userId: String,
    ) {
        userIdToClientMap
            .remove(key = userId)
            ?.close()
    }
}
