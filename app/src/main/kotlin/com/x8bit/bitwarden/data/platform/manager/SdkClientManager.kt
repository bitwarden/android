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
    suspend fun getOrCreateClient(userId: String): Client

    /**
     * Helper function to retrieve a new instance of the [Client] and use it in the given [block].
     * This client is never persisted after the [block] completes.
     *
     * @param userId The used to create the [Client]. If null, the SDK is unassociated with a user.
     * @param accessToken The access token used in network requests.
     */
    suspend fun <T> singleUseClient(
        userId: String? = null,
        accessToken: String? = null,
        block: suspend Client.() -> T,
    ): T

    /**
     * Clears any resources from the [Client] associated with the given [userId] and removes it
     * from the internal cache.
     */
    fun destroyClient(userId: String?)
}
