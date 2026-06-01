package com.x8bit.bitwarden.data.platform.manager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED
import android.net.NetworkRequest
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.manager.model.NetworkConnection
import com.x8bit.bitwarden.data.platform.manager.model.NetworkSignalStrength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * Primary implementation of [NetworkConnectionManager].
 */
class NetworkConnectionManagerImpl(
    context: Context,
    dispatcherManager: DispatcherManager,
) : NetworkConnectionManager {
    private val unconfinedScope = CoroutineScope(context = dispatcherManager.unconfined)
    private val networkChangeCallback = ConnectionChangeCallback()

    private val connectivityManager: ConnectivityManager = context
        .applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build(),
            networkChangeCallback,
        )
    }

    override val isNetworkConnected: Boolean
        get() = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false

    override val isNetworkConnectedFlow: StateFlow<Boolean> =
        networkChangeCallback
            .connectionChangeFlow
            .map { isNetworkConnected }
            .distinctUntilChanged()
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = isNetworkConnected,
            )

    override val networkConnection: NetworkConnection
        get() = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
            .networkConnection

    override val networkConnectionFlow: StateFlow<NetworkConnection> = networkChangeCallback
        .connectionChangeFlow
        .map { _ -> networkConnection }
        .distinctUntilChanged()
        .onEach { Timber.d("Network status change: $it") }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = networkConnection,
        )

    /**
     * A callback used to monitor the connection of a [Network].
     */
    private class ConnectionChangeCallback : ConnectivityManager.NetworkCallback() {
        private val mutableConnectionState: MutableSharedFlow<Unit> = bufferedMutableSharedFlow()

        /**
         * A [StateFlow] that emits when the connection state to a network changes.
         */
        val connectionChangeFlow: SharedFlow<Unit> = mutableConnectionState.asSharedFlow()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            mutableConnectionState.tryEmit(Unit)
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            mutableConnectionState.tryEmit(Unit)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            mutableConnectionState.tryEmit(Unit)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            mutableConnectionState.tryEmit(Unit)
        }
    }
}

/**
 * Converts the [NetworkCapabilities] to a [NetworkConnection].
 */
private val NetworkCapabilities?.networkConnection: NetworkConnection
    get() = this
        ?.let {
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                NetworkConnection.Wifi(it.networkStrength)
            } else if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                NetworkConnection.Cellular
            } else {
                NetworkConnection.Other
            }
        }
        ?: NetworkConnection.None

/**
 * Converts an integer value to an enum signal strength based on the RSSI standard.
 *
 * * -50 dBm: Excellent signal
 * * -60 to -75 dBm: Good signal
 * * -76 to -90 dBm: Fair signal
 * * -91 to -110 dBm: Weak signal
 * * -110 dBm and below: No signal
 */
@Suppress("MagicNumber")
private val NetworkCapabilities.networkStrength: NetworkSignalStrength
    get() {
        val strength = this.signalStrength
        return when {
            (strength <= SIGNAL_STRENGTH_UNSPECIFIED) -> NetworkSignalStrength.UNKNOWN
            (strength <= -110) -> NetworkSignalStrength.NONE
            (strength <= -91) -> NetworkSignalStrength.WEAK
            (strength <= -76) -> NetworkSignalStrength.FAIR
            (strength <= -60) -> NetworkSignalStrength.GOOD
            else -> NetworkSignalStrength.EXCELLENT
        }
    }
