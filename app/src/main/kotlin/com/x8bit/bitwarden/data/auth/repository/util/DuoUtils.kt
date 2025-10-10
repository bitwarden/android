package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import com.bitwarden.annotation.OmitFromCoverage

private const val DUO_HOST: String = "duo-callback"

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
    val localData = data
    return if (action == Intent.ACTION_VIEW && localData != null && localData.host == DUO_HOST) {
        localData.getDuoCallbackTokenResult()
    } else {
        null
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
