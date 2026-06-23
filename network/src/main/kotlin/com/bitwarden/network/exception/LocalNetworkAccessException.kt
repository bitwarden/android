package com.bitwarden.network.exception

import android.Manifest
import java.io.IOException

/**
 * Thrown when a user attempts to make a network request to a device on the local network but does
 * not have the [Manifest.permission.ACCESS_LOCAL_NETWORK] permission.
 */
class LocalNetworkAccessException(message: String) : IOException(message)
