package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body to verify the email token.
 *
 * @param email The email being used to create the account.
 * @param emailVerificationToken The token used to verify the email.
 */
@Serializable
data class VerifyEmailTokenRequestJson(
    @SerialName("email")
    val email: String,

    @SerialName("emailVerificationToken")
    val emailVerificationToken: String?,
)
