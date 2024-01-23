package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent

private const val SSO_HOST: String = "sso-callback"

/**
 * Retrieves an [SsoCallbackResult] from an Intent. There are three possible cases.
 *
 * - `null`: Intent is not an SSO callback, or data is null.
 *
 * - [SsoCallbackResult.MissingCode]: Intent is the SSO callback, but it's missing the needed code.
 *
 * - [SsoCallbackResult.Success]: Intent is the SSO callback with required data.
 */
fun Intent.getSsoCallbackResult(): SsoCallbackResult? {
    val localData = data
    return if (action == Intent.ACTION_VIEW && localData?.host == SSO_HOST) {
        val state = localData.getQueryParameter("state")
        val code = localData.getQueryParameter("code")
        if (code != null) {
            SsoCallbackResult.Success(
                state = state,
                code = code,
            )
        } else {
            SsoCallbackResult.MissingCode
        }
    } else {
        null
    }
}

/**
 * Sealed class representing the result of an SSO callback data extraction.
 */
sealed class SsoCallbackResult {
    /**
     * Represents an SSO callback object with a missing code value.
     */
    data object MissingCode : SsoCallbackResult()

    /**
     * Represents an SSO callback object with the necessary [state] and [code]. `state` being
     * present doesn't guarantee it is correct, and should be checked against the known state before
     * being used.
     */
    data class Success(
        val state: String?,
        val code: String,
    ) : SsoCallbackResult()
}
