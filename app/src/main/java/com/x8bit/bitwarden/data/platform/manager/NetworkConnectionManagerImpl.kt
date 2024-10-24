package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Primary implementation of [NetworkConnectionManager].
 */
class NetworkConnectionManagerImpl(
    context: Context,
    private val externalScope: CoroutineScope
) : NetworkConnectionManager {
    private val connectivityManager: ConnectivityManager = context
        .applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isNetworkConnected: Boolean
        get() = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false

    override val isNetworkConnectedFlow: StateFlow<Boolean>
        get() = _connectedFlow
            .stateIn(
                scope = externalScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = isNetworkConnected
            )

    private val _connectedFlow = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network : Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    trySend(true)
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
