package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import com.bitwarden.annotation.OmitFromCoverage

private const val BITWARDEN_EU_HOST: String = "bitwarden.eu"
private const val BITWARDEN_US_HOST: String = "bitwarden.com"
private const val APP_LINK_SCHEME: String = "https"
private const val DEEPLINK_SCHEME: String = "bitwarden"
private const val CALLBACK: String = "duo-callback"

/**
 * Retrieves a [DuoCallbackTokenResult] from an Intent. There are three possible cases.
 *
 * - `null`: Intent is not a Duo callback, or data is null.
 *
 * - [DuoCallbackTokenResult.MissingToken]: Intent is the Duo callback, but it's missing the code or
 * state value.
 *
 * - [DuoCallbackTokenResult.Success]: Intent is the Duo callback, and it has a token.
 */
fun Intent.getDuoCallbackTokenResult(): DuoCallbackTokenResult? {
    if (action != Intent.ACTION_VIEW) return null
    val localData = data ?: return null
    return when (localData.scheme) {
        DEEPLINK_SCHEME -> {
            if (localData.host == CALLBACK) {
                localData.getDuoCallbackTokenResult()
            } else {
                null
            }
        }

        APP_LINK_SCHEME -> {
            if ((localData.host == BITWARDEN_US_HOST || localData.host == BITWARDEN_EU_HOST) &&
                localData.path == "/$CALLBACK"
            ) {
                localData.getDuoCallbackTokenResult()
            } else {
                null
            }
        }

        else -> null
    }
}

/**
 * Retrieves a [DuoCallbackTokenResult] from an Intent. There are three possible cases.
 *
 * - `null`: Intent is not a Duo callback, or data is null.
 *
 * - [DuoCallbackTokenResult.MissingToken]: Intent is the Duo callback, but it's missing the code or
 * state value.
 *
 * - [DuoCallbackTokenResult.Success]: Intent is the Duo callback, and it has a token.
 */
@OmitFromCoverage
fun AuthTabIntent.AuthResult.getDuoCallbackTokenResult(): DuoCallbackTokenResult =
    when (this.resultCode) {
        AuthTabIntent.RESULT_OK -> this.resultUri.getDuoCallbackTokenResult()
        AuthTabIntent.RESULT_CANCELED -> DuoCallbackTokenResult.MissingToken
        AuthTabIntent.RESULT_UNKNOWN_CODE -> DuoCallbackTokenResult.MissingToken
        AuthTabIntent.RESULT_VERIFICATION_FAILED -> DuoCallbackTokenResult.MissingToken
        AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> DuoCallbackTokenResult.MissingToken
        else -> DuoCallbackTokenResult.MissingToken
    }

private fun Uri?.getDuoCallbackTokenResult(): DuoCallbackTokenResult {
    val code = this?.getQueryParameter("code")
    val state = this?.getQueryParameter("state")
    return if (code != null && state != null) {
        DuoCallbackTokenResult.Success(token = "$code|$state")
    } else {
        DuoCallbackTokenResult.MissingToken
    }
}

/**
 * Sealed class representing the result of Duo callback token extraction.
 */
sealed class DuoCallbackTokenResult {
    /**
     * Represents a missing token in the Duo callback.
     */
    data object MissingToken : DuoCallbackTokenResult()

    /**
     * Represents a token present in the Duo callback.
     */
    data class Success(val token: String) : DuoCallbackTokenResult()
}
