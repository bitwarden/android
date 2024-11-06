package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Response object returned when requesting organization domain SSO details.
 *
 * @property isSsoAvailable Whether or not SSO is available for this domain.
 * @property organizationIdentifier The organization's identifier.
 * @property verifiedDate The date the domain was verified.
 */
@Serializable
data class OrganizationDomainSsoDetailsResponseJson(
    @SerialName("ssoAvailable")
    val isSsoAvailable: Boolean,

    @SerialName("organizationIdentifier")
    val organizationIdentifier: String,

    @SerialName("verifiedDate")
    @Contextual
    val verifiedDate: ZonedDateTime?,
)
