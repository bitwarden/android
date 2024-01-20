package com.x8bit.bitwarden.data.platform.manager

/**
 * Manager to detect and handle changes to network connectivity.
 */
interface NetworkConnectionManager {
    /**
     * Returns `true` if the application has a network connection and access to the Internet is
     * available.
     */
    val isNetworkConnected: Boolean
}
