package com.bitwarden.network.api

import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.OrganizationDomainSsoDetailsRequestJson
import com.bitwarden.network.model.OrganizationDomainSsoDetailsResponseJson
import com.bitwarden.network.model.VerifiedOrganizationDomainSsoDetailsRequest
import com.bitwarden.network.model.VerifiedOrganizationDomainSsoDetailsResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /organizations API.
 */
internal interface UnauthenticatedOrganizationApi {
    /**
     * Checks for the claimed domain organization of an email for SSO purposes.
     */
    @POST("/organizations/domain/sso/details")
    suspend fun getClaimedDomainOrganizationDetails(
        @Body body: OrganizationDomainSsoDetailsRequestJson,
    ): NetworkResult<OrganizationDomainSsoDetailsResponseJson>

    /**
     * Checks for the verified organization domains of an email for SSO purposes.
     */
    @POST("/organizations/domain/sso/verified")
    suspend fun getVerifiedOrganizationDomainsByEmail(
        @Body body: VerifiedOrganizationDomainSsoDetailsRequest,
    ): NetworkResult<VerifiedOrganizationDomainSsoDetailsResponse>
}
