package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.exception.LocalNetworkAccessException

/**
 * Returns a user-friendly error message if this [Throwable] is an allow-listed
 * exception type that carries one, or `null` otherwise.
 */
val Throwable.userFriendlyMessage: String?
    get() = when (this) {
        is LocalNetworkAccessException -> message
        is CookieRedirectException -> message
        else -> null
    }
