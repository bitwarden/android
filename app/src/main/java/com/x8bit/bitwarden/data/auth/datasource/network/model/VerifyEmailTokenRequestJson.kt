package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the request body for verify email token endpoint.
 *
 * @param email the email address of the user to verify.
 * @param token the provided email verification token.
 */
@Serializable
data class VerifyEmailTokenRequestJson(
    @SerialName("email")
    val email: String,
    @SerialName("emailVerificationToken")
    val token: String,
)
