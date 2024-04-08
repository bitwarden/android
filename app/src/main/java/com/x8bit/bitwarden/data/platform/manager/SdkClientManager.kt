package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.sdk.Client

/**
 * Manages the creation, caching, and destruction of SDK [Client] instances on a per-user basis.
 */
interface SdkClientManager {

    /**
     * Returns the cached [Client] instance for the given [userId], otherwise creates and caches
     * a new one and returns it.
     */
    suspend fun getOrCreateClient(userId: String?): Client

    /**
     * Clears any resources from the [Client] associated with the given [userId] and removes it
     * from the internal cache.
     */
    fun destroyClient(userId: String?)
}
