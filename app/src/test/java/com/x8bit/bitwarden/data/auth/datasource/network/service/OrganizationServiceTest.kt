package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.OrganizationApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class OrganizationServiceTest : BaseServiceTest() {
    private val organizationApi: OrganizationApi = retrofit.create()

    private val organizationService = OrganizationServiceImpl(
        organizationApi = organizationApi,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `getOrganizationDomainSsoDetails when response is success should return PrevalidateSsoResponseJson`() =
        runTest {
            val email = "test@gmail.com"
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(ORGANIZATION_DOMAIN_SSO_DETAILS_JSON),
            )
            val result = organizationService.getOrganizationDomainSsoDetails(email)
            assertEquals(Result.success(ORGANIZATION_DOMAIN_SSO_BODY), result)
        }

    @Test
    fun `getOrganizationDomainSsoDetails when response is an error should return an error`() =
        runTest {
            val email = "test@gmail.com"
            server.enqueue(MockResponse().setResponseCode(400))
            val result = organizationService.getOrganizationDomainSsoDetails(email)
            assertTrue(result.isFailure)
        }
}

private const val ORGANIZATION_DOMAIN_SSO_DETAILS_JSON = """
{
  "ssoAvailable": true,
  "domainName": "bitwarden.com",
  "organizationIdentifier": "Test Org",
  "ssoRequired": false,
  "verifiedDate": "2024-09-13T00:00:00.000Z"
}
"""

private val ORGANIZATION_DOMAIN_SSO_BODY = OrganizationDomainSsoDetailsResponseJson(
    isSsoAvailable = true,
    organizationIdentifier = "Test Org",
    domainName = "bitwarden.com",
    isSsoRequired = false,
    verifiedDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
)
