package com.bitwarden.network.interceptor

/**
 * Provides base URLs for different API groups.
 */
interface BaseUrlsProvider {
    /**
     * Gets the base URL for "/api" calls.
     */
    fun getBaseApiUrl(): String

    /**
     * Gets the base URL for "/identity" calls.
     */
    fun getBaseIdentityUrl(): String

    /**
     * Gets the base URL for "/events" calls.
     */
    fun getBaseEventsUrl(): String
}
