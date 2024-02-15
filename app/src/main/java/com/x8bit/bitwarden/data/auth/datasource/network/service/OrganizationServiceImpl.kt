package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.OrganizationApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson

/**
 * Default implementation of [OrganizationService].
 */
class OrganizationServiceImpl(
    private val organizationApi: OrganizationApi,
) : OrganizationService {
    override suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): Result<OrganizationDomainSsoDetailsResponseJson> = organizationApi
        .getClaimedDomainOrganizationDetails(
            body = OrganizationDomainSsoDetailsRequestJson(
                email = email,
            ),
        )
}
