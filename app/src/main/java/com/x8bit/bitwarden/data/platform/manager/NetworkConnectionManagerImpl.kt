package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Primary implementation of [NetworkConnectionManager].
 */
class NetworkConnectionManagerImpl(
    context: Context,
) : NetworkConnectionManager {
    private val connectivityManager: ConnectivityManager = context
        .applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isNetworkConnected: Boolean
        get() = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
}
