package com.x8bit.bitwarden.data.platform.datasource.disk

/**
 * Disk source for persisting the custom headers sent with requests to a self-hosted environment.
 */
interface CustomHeadersDiskSource {

    /**
     * Gets the custom headers stored under the given [id].
     *
     * @param id The identifier of the custom headers.
     * @return The custom headers, or null if none are stored.
     */
    fun getCustomHeaders(id: String): Map<String, String>?

    /**
     * Stores the custom [headers] under the given [id]. Pass `null` to delete the stored headers.
     *
     * @param id The identifier to store the custom headers under.
     * @param headers The custom headers to persist, or `null` to delete.
     */
    fun storeCustomHeaders(id: String, headers: Map<String, String>?)
}
