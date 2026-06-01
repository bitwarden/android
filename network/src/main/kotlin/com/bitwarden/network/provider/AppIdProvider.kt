package com.bitwarden.network.provider

/**
 * A provider for all the functionality needed to uniquely identify the app installation.
 */
interface AppIdProvider {

    /**
     * The unique app ID.
     */
    val uniqueAppId: String
}
