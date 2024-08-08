package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for send verification email.
 *
 * @param email the email to be registered.
 * @param name the name to be registered.
 * @param receiveMarketingEmails the answer to receive marketing emails.
 */
@Serializable
data class SendVerificationEmailRequestJson(
    @SerialName("email")
    val email: String,

    @SerialName("name")
    val name: String?,

    @SerialName("receiveMarketingEmails")
    val receiveMarketingEmails: Boolean,
)
