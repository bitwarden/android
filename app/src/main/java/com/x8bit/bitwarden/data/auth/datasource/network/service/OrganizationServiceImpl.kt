package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.bitwarden.network.api.AuthenticatedOrganizationApi
import com.bitwarden.network.model.OrganizationAutoEnrollStatusResponseJson
import com.bitwarden.network.model.OrganizationKeysResponseJson
import com.bitwarden.network.model.OrganizationResetPasswordEnrollRequestJson
import com.bitwarden.network.util.toResult
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedOrganizationApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifiedOrganizationDomainSsoDetailsRequest
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifiedOrganizationDomainSsoDetailsResponse

/**
 * Default implementation of [OrganizationService].
 */
class OrganizationServiceImpl(
    private val authenticatedOrganizationApi: AuthenticatedOrganizationApi,
    private val unauthenticatedOrganizationApi: UnauthenticatedOrganizationApi,
) : OrganizationService {
    override suspend fun organizationResetPasswordEnroll(
        organizationId: String,
        userId: String,
        passwordHash: String?,
        resetPasswordKey: String,
    ): Result<Unit> = authenticatedOrganizationApi
        .organizationResetPasswordEnroll(
            organizationId = organizationId,
            userId = userId,
            body = OrganizationResetPasswordEnrollRequestJson(
                passwordHash = passwordHash,
                resetPasswordKey = resetPasswordKey,
            ),
        )
        .toResult()

    override suspend fun getOrganizationDomainSsoDetails(
        email: String,
    ): Result<OrganizationDomainSsoDetailsResponseJson> = unauthenticatedOrganizationApi
        .getClaimedDomainOrganizationDetails(
            body = OrganizationDomainSsoDetailsRequestJson(
                email = email,
            ),
        )
        .toResult()

    override suspend fun getOrganizationAutoEnrollStatus(
        organizationIdentifier: String,
    ): Result<OrganizationAutoEnrollStatusResponseJson> = authenticatedOrganizationApi
        .getOrganizationAutoEnrollResponse(
            organizationIdentifier = organizationIdentifier,
        )
        .toResult()

    override suspend fun getOrganizationKeys(
        organizationId: String,
    ): Result<OrganizationKeysResponseJson> = authenticatedOrganizationApi
        .getOrganizationKeys(
            organizationId = organizationId,
        )
        .toResult()

    override suspend fun getVerifiedOrganizationDomainSsoDetails(
        email: String,
    ): Result<VerifiedOrganizationDomainSsoDetailsResponse> = unauthenticatedOrganizationApi
        .getVerifiedOrganizationDomainsByEmail(
            body = VerifiedOrganizationDomainSsoDetailsRequest(
                email = email,
            ),
        )
        .toResult()
}
