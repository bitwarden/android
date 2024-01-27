package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /organizations API.
 */
interface OrganizationApi {
    /**
     * Checks for the claimed domain organization of an email for SSO purposes.
     */
    @POST("/organizations/domain/sso/details")
    suspend fun getClaimedDomainOrganizationDetails(
        @Body body: OrganizationDomainSsoDetailsRequestJson,
    ): Result<OrganizationDomainSsoDetailsResponseJson>
}
