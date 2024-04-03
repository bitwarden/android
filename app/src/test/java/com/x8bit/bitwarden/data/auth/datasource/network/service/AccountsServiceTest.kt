package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson.PBKDF2_SHA256
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class AccountsServiceTest : BaseServiceTest() {

    private val accountsApi: AccountsApi = retrofit.create()
    private val authenticatedAccountsApi: AuthenticatedAccountsApi = retrofit.create()
    private val service = AccountsServiceImpl(
        accountsApi = accountsApi,
        authenticatedAccountsApi = authenticatedAccountsApi,
        json = Json {
            ignoreUnknownKeys = true
        },
    )

    @Test
    fun `deleteAccount with empty response is success`() = runTest {
        val masterPasswordHash = "37y4d8r379r4789nt387r39k3dr87nr93"
        val json = ""
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertTrue(service.deleteAccount(masterPasswordHash).isSuccess)
    }

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
        assertTrue(service.preLogin(EMAIL).isFailure)
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
        assertTrue(service.preLogin(EMAIL).isFailure)
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
        assertTrue(service.preLogin(EMAIL).isFailure)
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
        assertEquals(expectedResponse.asSuccess(), service.preLogin(EMAIL))
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
        assertEquals(expectedResponse.asSuccess(), service.preLogin(EMAIL))
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
        assertEquals(expectedResponse.asSuccess(), service.register(registerRequestBody))
    }

    @Test
    fun `register failure with Invalid json should be Invalid`() = runTest {
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
        val result = service.register(registerRequestBody)
        assertEquals(
            RegisterResponseJson.Invalid(
                message = "The model state is invalid.",
                validationErrors = mapOf("" to listOf("Email '' is already taken.")),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `register failure with Error json should return Error`() = runTest {
        val json = """
            {
              "Object": "error",
              "Message": "Slow down! Too many requests. Try again soon."
            }
        """.trimIndent()
        val response = MockResponse().setResponseCode(429).setBody(json)
        server.enqueue(response)
        val result = service.register(registerRequestBody)
        assertEquals(
            RegisterResponseJson.Error(
                message = "Slow down! Too many requests. Try again soon.",
            ),
            result.getOrThrow(),
        )
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
        assertEquals(expectedResponse.asSuccess(), service.register(registerRequestBody))
    }

    @Test
    fun `requestPasswordHint success should return Success`() = runTest {
        val email = "test@example.com"
        val response = MockResponse().setResponseCode(200).setBody("{}")
        server.enqueue(response)

        val result = service.requestPasswordHint(email)

        assertTrue(result.isSuccess)
        assertEquals(PasswordHintResponseJson.Success, result.getOrNull())
    }

    @Test
    fun `resendVerificationCodeEmail with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resendVerificationCodeEmail(
            body = ResendEmailRequestJson(
                deviceIdentifier = "3",
                email = "example@email.com",
                passwordHash = "37y4d8r379r4789nt387r39k3dr87nr93",
                ssoToken = null,
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resetPassword with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resetPassword(
            body = ResetPasswordRequestJson(
                currentPasswordHash = "",
                newPasswordHash = "",
                passwordHint = null,
                key = "",
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resetPassword with empty response and null current password is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resetPassword(
            body = ResetPasswordRequestJson(
                currentPasswordHash = null,
                newPasswordHash = "",
                passwordHint = null,
                key = "",
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `setPassword with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.setPassword(
            body = SetPasswordRequestJson(
                passwordHash = "passwordHash",
                passwordHint = "passwordHint",
                organizationIdentifier = "organizationId",
                kdfIterations = 7,
                kdfMemory = 1,
                kdfParallelism = 2,
                kdfType = null,
                key = "encryptedUserKey",
                keys = RegisterRequestJson.Keys(
                    publicKey = "public",
                    encryptedPrivateKey = "private",
                ),
            ),
        )
        assertTrue(result.isSuccess)
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