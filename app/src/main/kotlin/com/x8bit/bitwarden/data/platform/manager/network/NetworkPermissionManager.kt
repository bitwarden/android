package com.x8bit.bitwarden.data.platform.manager.network

import com.bitwarden.network.provider.PermissionProvider
import kotlinx.coroutines.flow.StateFlow

/**
 * A manager class for handling network permissions.
 */
interface NetworkPermissionManager : PermissionProvider {
    /**
     * StateFlow indicating if local network access is being requested at this moment.
     *
     * Emits `true` when local network access is required, `false` otherwise.
     */
    val isLocalNetworkAccessRequiredStateFlow: StateFlow<Boolean>

    /**
     * Sets the local network access required state to `false`.
     */
    fun clearIsLocalNetworkAccessRequired()
}
