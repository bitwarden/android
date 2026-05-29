package com.bitwarden.network.provider

import android.Manifest

/**
 * A provider for network-related permissions.
 */
interface PermissionProvider {
    /**
     * The translated human-readable string to be displayed when the local network access
     * permission is the reason for a request failure.
     */
    val errorMessageString: String

    /**
     * Indicates if the app does or does not have the [Manifest.permission.ACCESS_LOCAL_NETWORK]
     * permission.
     */
    val hasLocalNetworkAccessPermission: Boolean

    /**
     * Signals that local network access permission is required for the current environment.
     */
    fun acquireLocalNetworkAccessPermission()
}
