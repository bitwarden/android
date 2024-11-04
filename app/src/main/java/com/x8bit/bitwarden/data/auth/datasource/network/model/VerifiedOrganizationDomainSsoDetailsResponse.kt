package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when requesting organization verified domain SSO details.
 *
 * @property verifiedOrganizationDomainSsoDetails The list of verified organization domain SSO
 * details.
 */
@Serializable
data class VerifiedOrganizationDomainSsoDetailsResponse(
    @SerialName("data")
    val verifiedOrganizationDomainSsoDetails: List<VerifiedOrganizationDomainSsoDetail>,
) {
    /**
     * Response body for an organization verified domain SSO details.
     *
     * @property organizationName The name of the organization.
     * @property organizationIdentifier The identifier of the organization.
     * @property domainName The name of the domain.
     */
    @Serializable
    data class VerifiedOrganizationDomainSsoDetail(
        @SerialName("organizationName")
        val organizationName: String,

        @SerialName("organizationIdentifier")
        val organizationIdentifier: String,

        @SerialName("domainName")
        val domainName: String,
    )
}
