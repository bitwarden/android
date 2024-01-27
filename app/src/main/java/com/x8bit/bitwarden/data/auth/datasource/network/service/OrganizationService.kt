package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson

/**
 * Provides an API for querying organization endpoints.
 */
interface OrganizationService {
    /**
     * Request claimed organization domain information for an [email] needed for SSO requests.
     */
    suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): Result<OrganizationDomainSsoDetailsResponseJson>
}
