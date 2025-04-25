package com.x8bit.bitwarden.data.platform.manager.network

import com.x8bit.bitwarden.data.platform.manager.model.NetworkConnection
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager to detect and handle changes to network connectivity.
 */
interface NetworkConnectionManager {
    /**
     * Returns `true` if the application has a network connection and access to the Internet is
     * available.
     */
    val isNetworkConnected: Boolean

    /**
     * Emits `true` when the application has a network connection and access to the Internet is
     * available.
     */
    val isNetworkConnectedFlow: StateFlow<Boolean>

    /**
     * Returns the current network connection.
     */
    val networkConnection: NetworkConnection

    /**
     * Emits the current [NetworkConnection] indicating what type of network the app is currently
     * using to connect to the internet.
     */
    val networkConnectionFlow: StateFlow<NetworkConnection>
}
