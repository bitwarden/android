package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.platform.manager.model.NetworkConnection
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNetworkConnectionManager(
    isNetworkConnected: Boolean,
    networkConnection: NetworkConnection,
) : NetworkConnectionManager {
    private val mutableIsNetworkConnectedStateFlow = MutableStateFlow(isNetworkConnected)
    private val mutableNetworkConnectionStateFlow = MutableStateFlow(networkConnection)

    var fakeIsNetworkConnected: Boolean
        get() = mutableIsNetworkConnectedStateFlow.value
        set(value) {
            mutableIsNetworkConnectedStateFlow.value = value
        }

    var fakeNetworkConnection: NetworkConnection
        get() = mutableNetworkConnectionStateFlow.value
        set(value) {
            mutableNetworkConnectionStateFlow.value = value
        }

    override val isNetworkConnected: Boolean get() = fakeIsNetworkConnected

    override val isNetworkConnectedFlow: StateFlow<Boolean> =
        mutableIsNetworkConnectedStateFlow.asStateFlow()

    override val networkConnection: NetworkConnection get() = fakeNetworkConnection

    override val networkConnectionFlow: StateFlow<NetworkConnection> =
        mutableNetworkConnectionStateFlow.asStateFlow()
}
