package com.x8bit.bitwarden.data.platform.util

import android.os.Build
import android.security.KeyStoreException
import androidx.annotation.RequiresApi
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.exception.LocalNetworkAccessException
import com.bitwarden.network.model.SdkInvalidJsonResponse
import kotlinx.serialization.json.Json

/**
 * Reads SDK error bodies, which have fields that [SdkInvalidJsonResponse] does not model.
 */
private val sdkErrorBodyJson = Json { ignoreUnknownKeys = true }

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
 * Attempts to read the server error body at the end of this SDK [Throwable]'s message, as the
 * SDK reports a failed API request. Returns `null` when there is no readable body or it has no
 * message to show.
 *
 * This is only meant for [Throwable]s thrown by the SDK.
 */
fun Throwable.parseSdkErrorBodyOrNull(): SdkInvalidJsonResponse? {
    val rawMessage = message ?: return null
    val bodyStart = rawMessage.indexOf('{').takeIf { it >= 0 } ?: return null
    return sdkErrorBodyJson
        .decodeFromStringOrNull<SdkInvalidJsonResponse>(string = rawMessage.substring(bodyStart))
        ?.takeIf { it.firstValidationErrorMessage.isNotBlank() }
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
