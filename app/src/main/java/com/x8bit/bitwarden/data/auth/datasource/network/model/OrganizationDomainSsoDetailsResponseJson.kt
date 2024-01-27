package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Response object returned when requesting organization domain SSO details.
 *
 * @property isSsoAvailable Whether or not SSO is available for this domain.
 * @property domainName The organization's domain name.
 * @property organizationIdentifier The organization's identifier.
 * @property isSsoRequired Whether or not SSO is required.
 * @property verifiedDate The date these details were verified.
 */
@Serializable
data class OrganizationDomainSsoDetailsResponseJson(
    @SerialName("ssoAvailable") val isSsoAvailable: Boolean,
    @SerialName("domainName") val domainName: String,
    @SerialName("organizationIdentifier") val organizationIdentifier: String,
    @SerialName("ssoRequired") val isSsoRequired: Boolean,
    @Contextual
    @SerialName("verifiedDate") val verifiedDate: ZonedDateTime?,
)
