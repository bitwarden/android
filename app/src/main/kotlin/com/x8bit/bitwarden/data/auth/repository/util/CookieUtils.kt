package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** URI scheme for cookie vendor callback. */
private const val COOKIE_CALLBACK_SCHEME: String = "bitwarden"

/** URI host for cookie vendor callback. */
private const val COOKIE_CALLBACK_HOST: String = "sso_cookie_vendor"

/** Completeness marker parameter name (filtered from cookie extraction). */
private const val COMPLETENESS_MARKER_PARAM = "d"

/**
 * Extracts cookie callback result from Intent.
 * Handles both single and sharded cookie formats.
 * Filters out the 'd' completeness marker parameter.
 *
 * @return [CookieCallbackResult] if this is a cookie callback, null otherwise.
 */
fun Intent.getCookieCallbackResultOrNull(): CookieCallbackResult? {
    if (action != Intent.ACTION_VIEW) return null
    val uri = data ?: return null
    if (uri.scheme != COOKIE_CALLBACK_SCHEME) return null
    if (uri.host != COOKIE_CALLBACK_HOST) return null

    val cookies = uri.queryParameterNames
        .asSequence()
        .filter { it != COMPLETENESS_MARKER_PARAM }
        .mapNotNull { name ->
            uri.getQueryParameter(name)?.takeIf { it.isNotEmpty() }?.let { name to it }
        }
        .toMap()

    return if (cookies.isEmpty()) {
        CookieCallbackResult.MissingCookie
    } else {
        CookieCallbackResult.Success(cookies)
    }
}

/**
 * Represents the result of a cookie callback from a deep link.
 */
sealed class CookieCallbackResult : Parcelable {
    /**
     * The callback did not contain any cookies.
     */
    @Parcelize
    data object MissingCookie : CookieCallbackResult()

    /**
     * Successfully extracted cookies from the callback.
     * @param cookies Map of cookie name to cookie value. Supports sharded cookies.
     */
    @Parcelize
    data class Success(val cookies: Map<String, String>) : CookieCallbackResult()
}
