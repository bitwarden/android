package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.network.provider.CustomHeadersProvider

/**
 * Responsible for managing the custom headers sent with requests to a self-hosted environment.
 *
 * The header values are kept in encrypted storage under an opaque identifier; only that
 * identifier is persisted with the environment data.
 */
interface CustomHeadersManager : CustomHeadersProvider {

    /**
     * Gets the custom headers stored under the given [id].
     *
     * @param id The identifier of the custom headers.
     * @return The custom headers, or null if none are stored.
     */
    fun getStoredCustomHeaders(id: String): Map<String, String>?

    /**
     * Stores the custom [headers] and returns the identifier under which they were saved.
     *
     * @param headers The custom headers to store.
     * @return The identifier of the stored custom headers.
     */
    fun saveCustomHeaders(headers: Map<String, String>): String

    /**
     * Removes the custom headers with the given [id] from storage if no environment still
     * references them.
     *
     * @param id The identifier of the custom headers to remove.
     */
    fun removeCustomHeaders(id: String)

    /**
     * Removes the custom headers referenced by the environment of the account with the given
     * [userId] if no other environment still references them. This must be called while the
     * account's data is still available.
     *
     * @param userId The user ID of the account being removed.
     */
    fun removeCustomHeadersForUser(userId: String)
}
