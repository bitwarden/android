package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body object when retrieving organization verified domain SSO info.
 *
 * @param email The email address to check against.
 */
@Serializable
data class VerifiedOrganizationDomainSsoDetailsRequest(
    @SerialName("email") val email: String,
)
