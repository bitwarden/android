package com.x8bit.bitwarden.data.platform.manager.model

/**
 * A representation of the current network connection.
 */
sealed class NetworkConnection {
    /**
     * Currently not connected to the internet.
     */
    data object None : NetworkConnection()

    /**
     * Currently connected to the internet via WiFi with a signal [strength] indication.
     */
    data class Wifi(
        val strength: NetworkSignalStrength,
    ) : NetworkConnection()

    /**
     * Currently connected to the internet via cellular connection.
     */
    data object Cellular : NetworkConnection()

    /**
     * Currently connected to the internet via an unknown connection.
     */
    data object Other : NetworkConnection()
}
