package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body object when retrieving organization domain SSO info.
 *
 * @param email The email address to check against.
 */
@Serializable
data class OrganizationDomainSsoDetailsRequestJson(
    @SerialName("email") val email: String,
)
