package com.x8bit.bitwarden.data.platform.util

import android.os.Build
import android.security.KeyStoreException
import androidx.annotation.RequiresApi
import com.bitwarden.core.util.isBuildVersionAtLeast
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

/**
 * Returns true when this [Throwable] or one of its causes is an Android Keystore exception that
 * reports [KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED].
 *
 * The public numeric-error API is only queried on API 33+.
 */
fun Throwable.isKeystoreUserAuthenticationRequired(): Boolean {
    if (!isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU)) {
        return false
    }
    return hasUserAuthenticationRequiredKeystoreCause()
}

/**
 * Returns true when this [Throwable] or one of its causes is a [KeyStoreException] reporting
 * [KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Throwable.hasUserAuthenticationRequiredKeystoreCause(): Boolean {
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = this
    while (current != null && seen.add(current)) {
        if (
            current is KeyStoreException &&
            current.numericErrorCode == KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
