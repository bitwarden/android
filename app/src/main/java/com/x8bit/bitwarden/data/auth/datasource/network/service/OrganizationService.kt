package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationAutoEnrollStatusResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationKeysResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifiedOrganizationDomainSsoDetailsResponse

/**
 * Provides an API for querying organization endpoints.
 */
interface OrganizationService {
    /**
     * Enrolls a user with the given [userId] in this organizations reset password functionality.
     */
    suspend fun organizationResetPasswordEnroll(
        organizationId: String,
        userId: String,
        passwordHash: String?,
        resetPasswordKey: String,
    ): Result<Unit>

    /**
     * Request claimed organization domain information for an [email] needed for SSO requests.
     */
    suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): Result<OrganizationDomainSsoDetailsResponseJson>

    /**
     * Gets info regarding whether this organization enforces reset password auto enrollment.
     */
    suspend fun getOrganizationAutoEnrollStatus(
        organizationIdentifier: String,
    ): Result<OrganizationAutoEnrollStatusResponseJson>

    /**
     * Gets the public and private keys for this organization.
     */
    suspend fun getOrganizationKeys(
        organizationId: String,
    ): Result<OrganizationKeysResponseJson>

    /**
     * Request organization verified domain details for an [email] needed for SSO
     * requests.
     */
    suspend fun getVerifiedOrganizationDomainSsoDetails(
        email: String,
    ): Result<VerifiedOrganizationDomainSsoDetailsResponse>
}
