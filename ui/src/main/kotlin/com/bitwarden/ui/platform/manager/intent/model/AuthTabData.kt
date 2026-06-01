package com.bitwarden.ui.platform.manager.intent.model

import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Represents all data required to launch an [AuthTabIntent] or a fallback [CustomTabsIntent].
 */
sealed class AuthTabData {
    /**
     * The scheme being used for the callback.
     */
    abstract val callbackScheme: String

    /**
     * The url to be used for the callback.
     */
    abstract val callbackUrl: String

    /**
     * A representation of a custom "Bitwarden" scheme callback.
     */
    data class CustomScheme(
        override val callbackUrl: String,
        override val callbackScheme: String = "bitwarden",
    ) : AuthTabData()

    /**
     * A representation of a "https" app link scheme callback.
     */
    data class HttpsScheme(
        val host: String,
        val path: String,
    ) : AuthTabData() {
        override val callbackScheme: String = "https"
        override val callbackUrl: String = "$callbackScheme://$host/$path"
    }
}
