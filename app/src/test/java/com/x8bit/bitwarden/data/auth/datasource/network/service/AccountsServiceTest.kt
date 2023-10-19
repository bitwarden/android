package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson.PBKDF2_SHA256
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class AccountsServiceTest : BaseServiceTest() {

    private val accountsApi: AccountsApi = retrofit.create()
    private val service = AccountsServiceImpl(
        accountsApi = accountsApi,
        json = Json,
    )

    @Test
    fun `preLogin with unknown kdf type be failure`() = runTest {
        val json = """
            {
              "kdf": 2,
              "kdfIterations": 1,
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 without memory property should be failure`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfParallelism": 1
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 without parallelism property should be failure`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfMemory": 1
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 should be success`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfMemory": 1,
              "kdfParallelism": 1
            }
            """
        val expectedResponse = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Argon2ID(
                iterations = 1u,
                memory = 1u,
                parallelism = 1u,
            ),
        )
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.preLogin(EMAIL))
    }

    @Test
    fun `preLogin Pbkdf2 should be success`() = runTest {
        val json = """
            {
              "kdf": 0,
              "kdfIterations": 1
            }
            """
        val expectedResponse = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Pbkdf2(1u),
        )
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.preLogin(EMAIL))
    }

    @Test
    fun `register success json should be Success`() = runTest {
        val json = """
            {
              "captchaBypassToken": "mock_token"            
            }
            """
        val expectedResponse = RegisterResponseJson.Success(
            captchaBypassToken = "mock_token",
        )
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.register(registerRequestBody))
    }

    @Test
    fun `register failure json should be failure`() = runTest {
        val json = """
            {
              "message": "The model state is invalid.",
              "validationErrors": {
                "": [
                  "Email '' is already taken."
                ]
              },
              "exceptionMessage": null,
              "exceptionStackTrace": null,
              "innerExceptionMessage": null,
              "object": "error"
            }
            """
        val response = MockResponse().setResponseCode(400).setBody(json)
        server.enqueue(response)
        assertTrue(service.register(registerRequestBody).isFailure)
    }

    @Test
    fun `register captcha json should be CaptchaRequired`() = runTest {
        val json = """
            {
              "validationErrors": {
                "HCaptcha_SiteKey": [
                  "mock_token"
                ]
              }
            }
            """
        val expectedResponse = RegisterResponseJson.CaptchaRequired(
            validationErrors = RegisterResponseJson.CaptchaRequired.ValidationErrors(
                captchaKeys = listOf("mock_token"),
            ),
        )
        val response = MockResponse().setResponseCode(400).setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.register(registerRequestBody))
    }

    companion object {
        private const val EMAIL = "email"
        private val registerRequestBody = RegisterRequestJson(
            email = EMAIL,
            masterPasswordHash = "mockk_masterPasswordHash",
            masterPasswordHint = "mockk_masterPasswordHint",
            captchaResponse = "mockk_captchaResponse",
            key = "mockk_key",
            keys = RegisterRequestJson.Keys(
                publicKey = "mockk_publicKey",
                encryptedPrivateKey = "mockk_encryptedPrivateKey",
            ),
            kdfType = PBKDF2_SHA256,
            kdfIterations = 600000U,
        )
    }
}
