package com.x8bit.bitwarden.data.auth.datasource.network.model

/**
 * Hold the authentication information for different login methods.
 */
sealed class IdentityTokenAuthModel {
    /**
     * The type of authentication.
     */
    abstract val grantType: String

    /**
     * The username for login with password.
     */
    abstract val username: String?

    /**
     * The password for login with password.
     */
    abstract val password: String?

    /**
     * The sso code for login with single sign on.
     */
    abstract val ssoCode: String?

    /**
     * The sso code verifier for login with single sign on.
     */
    abstract val ssoCodeVerifier: String?

    /**
     * The sso redirect uri for login with single sign on.
     */
    abstract val ssoRedirectUri: String?

    /**
     * The data for logging in with a username and password.
     */
    data class MasterPassword(
        override val username: String,
        override val password: String,
    ) : IdentityTokenAuthModel() {
        override val grantType: String get() = "password"
        override val ssoCode: String? get() = null
        override val ssoCodeVerifier: String? get() = null
        override val ssoRedirectUri: String? get() = null
    }

    /**
     * The data for logging in with single sign on credentials.
     */
    data class SingleSignOn(
        override val ssoCode: String,
        override val ssoCodeVerifier: String,
        override val ssoRedirectUri: String,
    ) : IdentityTokenAuthModel() {
        override val grantType: String get() = "authorization_code"
        override val username: String? get() = null
        override val password: String? get() = null
    }
}
