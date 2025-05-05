package com.x8bit.bitwarden.data.auth.repository.model

import com.bitwarden.network.model.VerifiedOrganizationDomainSsoDetailsResponse.VerifiedOrganizationDomainSsoDetail

/**
 * Response types when checking for an email's claimed domain organization.
 */
sealed class VerifiedOrganizationDomainSsoDetailsResult {
    /**
     * The request was successful.
     *
     * @property verifiedOrganizationDomainSsoDetails The verified organization domain SSO details.
     */
    data class Success(
        val verifiedOrganizationDomainSsoDetails: List<VerifiedOrganizationDomainSsoDetail>,
    ) : VerifiedOrganizationDomainSsoDetailsResult()

    /**
     * The request failed.
     */
    data class Failure(
        val error: Throwable,
    ) : VerifiedOrganizationDomainSsoDetailsResult()
}
