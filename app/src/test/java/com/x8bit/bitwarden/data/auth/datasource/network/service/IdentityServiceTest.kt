package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class IdentityServiceTest : BaseServiceTest() {

    private val identityApi: IdentityApi = retrofit.create()

    private val identityService = IdentityServiceImpl(
        api = identityApi,
        json = Json,
        baseUrl = server.url("/").toString(),
    )

    @Test
    fun `getToken when request response is Success should return Success`() = runTest {
        server.enqueue(MockResponse().setBody(LOGIN_SUCCESS_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
        )
        assertEquals(Result.success(LOGIN_SUCCESS), result)
    }

    @Test
    fun `getToken when request is error should return error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `getToken when response is CaptchaRequired should return CaptchaRequired`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody(CAPTCHA_BODY_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
        )
        assertEquals(Result.success(CAPTCHA_BODY), result)
    }

    companion object {
        private const val EMAIL = "email"
        private const val PASSWORD_HASH = "passwordHash"
    }
}

private const val CAPTCHA_BODY_JSON = """
{
  "HCaptcha_SiteKey": "123"
}
"""
private val CAPTCHA_BODY = GetTokenResponseJson.CaptchaRequired("123")

private const val LOGIN_SUCCESS_JSON = """
{
  "access_token": "123"
}    
"""
private val LOGIN_SUCCESS = GetTokenResponseJson.Success("123")
