package com.x8bit.bitwarden.data.auth.repository.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains data that can be parsed from a valid JWT token.
 *
 * @property userId The ID of the user.
 * @property email The user's email address.
 * @property isEmailVerified Whether or not the user's email is verified.
 * @property name The user's name.
 * @property expirationAsEpochTime The expiration time measured as an epoch time in seconds.
 * @property hasPremium True if the user has a premium account.
 * @property authenticationMethodsReference A list of the authentication methods used during
 * authentication.
 */
@Serializable
data class JwtTokenDataJson(
    @SerialName("sub")
    val userId: String,

    @SerialName("email")
    val email: String,

    @SerialName("email_verified")
    val isEmailVerified: Boolean,

    @SerialName("name")
    val name: String?,

    @SerialName("exp")
    val expirationAsEpochTime: Int,

    @SerialName("premium")
    val hasPremium: Boolean,

    @SerialName("amr")
    val authenticationMethodsReference: List<String>,
) {
    /**
     * Indicates that this is an external user. Mainly used for SSO users with a key connector.
     */
    val isExternal: Boolean
        get() = authenticationMethodsReference.any { it == "external" }
}
