package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedOrganizationApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedOrganizationApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationAutoEnrollStatusResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationDomainSsoDetailsResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.OrganizationKeysResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifiedOrganizationDomainSsoDetailsResponse
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create
import java.time.ZonedDateTime

class OrganizationServiceTest : BaseServiceTest() {
    private val authenticatedOrganizationApi: AuthenticatedOrganizationApi = retrofit.create()
    private val unauthenticatedOrganizationApi: UnauthenticatedOrganizationApi = retrofit.create()

    private val organizationService = OrganizationServiceImpl(
        authenticatedOrganizationApi = authenticatedOrganizationApi,
        unauthenticatedOrganizationApi = unauthenticatedOrganizationApi,
    )

    @Test
    fun `organizationResetPasswordEnroll when response is success should return Unit as success`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200))
            val result = organizationService.organizationResetPasswordEnroll(
                organizationId = "orgId",
                userId = "userId",
                passwordHash = "passwordHash",
                resetPasswordKey = "resetPasswordKey",
            )
            assertEquals(Unit.asSuccess(), result)
        }

    @Test
    fun `organizationResetPasswordEnroll when response is an error should return an error`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400))
            val result = organizationService.organizationResetPasswordEnroll(
                organizationId = "orgId",
                userId = "userId",
                passwordHash = "passwordHash",
                resetPasswordKey = "resetPasswordKey",
            )
            assertTrue(result.isFailure)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrganizationDomainSsoDetails when response is success should return PrevalidateSsoResponseJson`() =
        runTest {
            val email = "test@gmail.com"
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(ORGANIZATION_DOMAIN_SSO_DETAILS_JSON),
            )
            val result = organizationService.getOrganizationDomainSsoDetails(email)
            assertEquals(ORGANIZATION_DOMAIN_SSO_BODY.asSuccess(), result)
        }

    @Test
    fun `getOrganizationDomainSsoDetails when response is an error should return an error`() =
        runTest {
            val email = "test@gmail.com"
            server.enqueue(MockResponse().setResponseCode(400))
            val result = organizationService.getOrganizationDomainSsoDetails(email)
            assertTrue(result.isFailure)
        }

    @Test
    fun `getOrganizationAutoEnrollStatus when response is success should return valid response`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(ORGANIZATION_AUTO_ENROLL_STATUS_JSON),
            )
            val result = organizationService.getOrganizationAutoEnrollStatus("orgId")
            assertEquals(ORGANIZATION_AUTO_ENROLL_STATUS_RESPONSE.asSuccess(), result)
        }

    @Test
    fun `getOrganizationAutoEnrollStatus when response is an error should return an error`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400))
            val result = organizationService.getOrganizationAutoEnrollStatus("orgId")
            assertTrue(result.isFailure)
        }

    @Test
    fun `getOrganizationKeys when response is success should return valid response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(ORGANIZATION_KEYS_JSON),
        )
        val result = organizationService.getOrganizationKeys("orgId")
        assertEquals(ORGANIZATION_KEYS_RESPONSE.asSuccess(), result)
    }

    @Test
    fun `getOrganizationKeys when response is an error should return an error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = organizationService.getOrganizationKeys("orgId")
        assertTrue(result.isFailure)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getVerifiedOrganizationDomainSsoDetails when response is success should return valid response`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(ORGANIZATION_VERIFIED_DOMAIN_SSO_DETAILS_JSON),
            )
            val result =
                organizationService.getVerifiedOrganizationDomainSsoDetails("example@bitwarden.com")
            assertEquals(ORGANIZATION_VERIFIED_DOMAIN_SSO_DETAILS_RESPONSE.asSuccess(), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getVerifiedOrganizationDomainSsoDetails when response is an error should return an error`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400))
            val result =
                organizationService.getVerifiedOrganizationDomainSsoDetails("example@bitwarden.com")
            assertTrue(result.isFailure)
        }
}

private const val ORGANIZATION_AUTO_ENROLL_STATUS_JSON = """
{
  "id": "orgId",
  "resetPasswordEnabled": true
}
"""

private val ORGANIZATION_AUTO_ENROLL_STATUS_RESPONSE = OrganizationAutoEnrollStatusResponseJson(
    organizationId = "orgId",
    isResetPasswordEnabled = true,
)

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
    verifiedDate = ZonedDateTime.parse("2024-09-13T00:00:00.000Z"),
)

private const val ORGANIZATION_KEYS_JSON = """
{
  "privateKey": "privateKey",
  "publicKey": "publicKey"
}
"""

private val ORGANIZATION_KEYS_RESPONSE = OrganizationKeysResponseJson(
    privateKey = "privateKey",
    publicKey = "publicKey",
)

private const val ORGANIZATION_VERIFIED_DOMAIN_SSO_DETAILS_JSON = """
{
  "data": [
    {
      "organizationIdentifier": "Test Identifier",
      "organizationName": "Bitwarden",
      "domainName": "bitwarden.com"
    }
  ]
}
"""

private val ORGANIZATION_VERIFIED_DOMAIN_SSO_DETAILS_RESPONSE =
    VerifiedOrganizationDomainSsoDetailsResponse(
        verifiedOrganizationDomainSsoDetails = listOf(
            VerifiedOrganizationDomainSsoDetailsResponse.VerifiedOrganizationDomainSsoDetail(
                organizationIdentifier = "Test Identifier",
                organizationName = "Bitwarden",
                domainName = "bitwarden.com",
            ),
        ),
    )
