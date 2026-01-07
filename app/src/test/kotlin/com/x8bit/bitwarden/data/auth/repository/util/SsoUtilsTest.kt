package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class SsoUtilsTest {

    @Test
    fun `generateUriForSso should generate the correct URI`() {
        val identityBaseUrl = "https://identity.bitwarden.com"
        val organizationIdentifier = "Test Organization"
        val token = "Test Token"
        val state = "test_state"
        val codeVerifier = "test_code_verifier"
        val expectedUrl = "https://identity.bitwarden.com/connect/authorize" +
            "?client_id=mobile" +
            "&redirect_uri=bitwarden%3A%2F%2Fsso-callback" +
            "&response_type=code" +
            "&scope=api%20offline_access" +
            "&state=test_state" +
            "&code_challenge=Qq1fGD0HhxwbmeMrqaebgn1qhvKeguQPXqLdpmixaM4" +
            "&code_challenge_method=S256" +
            "&response_mode=query" +
            "&domain_hint=Test+Organization" +
            "&ssoToken=Test+Token"

        val uri = generateUriForSso(
            identityBaseUrl = identityBaseUrl,
            organizationIdentifier = organizationIdentifier,
            token = token,
            state = state,
            codeVerifier = codeVerifier,
        )

        assertEquals(Uri.parse(expectedUrl), uri)
    }

    @Test
    fun `getSsoCallbackResult should return null when data is null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getSsoCallbackResult()
        assertNull(result)
    }

    @Test
    fun `getSsoCallbackResult should return null when action is not Intent ACTION_VIEW`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_ANSWER
        }
        val result = intent.getSsoCallbackResult()
        assertNull(result)
    }

    @Test
    fun `getSsoCallbackResult for deeplink should return MissingCode with missing state code`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("state") } returns "myState"
            every { data?.getQueryParameter("code") } returns null
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "sso-callback"
            every { data?.scheme } returns "bitwarden"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(SsoCallbackResult.MissingCode, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getSsoCallbackResult for deeplink should return Success when code query parameter is present`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "myCode"
            every { data?.getQueryParameter("state") } returns "myState"
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "sso-callback"
            every { data?.scheme } returns "bitwarden"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(
            SsoCallbackResult.Success(state = "myState", code = "myCode"),
            result,
        )
    }

    @Test
    fun `getSsoCallbackResult for app link should return MissingCode with missing state code`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("state") } returns "myState"
            every { data?.getQueryParameter("code") } returns null
            every { action } returns Intent.ACTION_VIEW
            every { data?.scheme } returns "https"
            every { data?.host } returns "bitwarden.eu"
            every { data?.path } returns "/sso-callback"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(SsoCallbackResult.MissingCode, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getSsoCallbackResult for app link should return Success when code query parameter is present`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("code") } returns "myCode"
            every { data?.getQueryParameter("state") } returns "myState"
            every { action } returns Intent.ACTION_VIEW
            every { data?.scheme } returns "https"
            every { data?.host } returns "bitwarden.com"
            every { data?.path } returns "/sso-callback"
        }
        val result = intent.getSsoCallbackResult()
        assertEquals(
            SsoCallbackResult.Success(state = "myState", code = "myCode"),
            result,
        )
    }
}
