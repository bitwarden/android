package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.UnauthenticatedIdentityApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.IdentityTokenAuthModel
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.KeyConnectorUserDecryptionOptionsJson
import com.bitwarden.network.model.MasterPasswordPolicyOptionsJson
import com.bitwarden.network.model.PreLoginResponseJson
import com.bitwarden.network.model.PrevalidateSsoResponseJson
import com.bitwarden.network.model.RefreshTokenResponseJson
import com.bitwarden.network.model.RegisterFinishRequestJson
import com.bitwarden.network.model.RegisterRequestJson
import com.bitwarden.network.model.RegisterResponseJson
import com.bitwarden.network.model.SendVerificationEmailRequestJson
import com.bitwarden.network.model.SendVerificationEmailResponseJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.VerifyEmailTokenRequestJson
import com.bitwarden.network.model.VerifyEmailTokenResponseJson
import com.bitwarden.network.model.createMockAccountKeysJson
import com.bitwarden.network.util.DeviceModelProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class IdentityServiceTest : BaseServiceTest() {

    private val unauthenticatedIdentityApi: UnauthenticatedIdentityApi = retrofit.create()
    private val deviceModelProvider = mockk<DeviceModelProvider> {
        every { deviceModel } returns "Test Device"
    }

    private val identityService = IdentityServiceImpl(
        unauthenticatedIdentityApi = unauthenticatedIdentityApi,
        json = json,
        deviceModelProvider = deviceModelProvider,
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
        assertTrue(identityService.preLogin(EMAIL).isFailure)
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
        assertTrue(identityService.preLogin(EMAIL).isFailure)
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
        assertTrue(identityService.preLogin(EMAIL).isFailure)
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
        assertEquals(
            expectedResponse.asSuccess(),
            identityService.preLogin(EMAIL),
        )
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
        assertEquals(
            expectedResponse.asSuccess(),
            identityService.preLogin(EMAIL),
        )
    }

    @Test
    fun `register success json should be Success`() = runTest {
        val expectedResponse = RegisterResponseJson.Success
        val response = MockResponse().setBody(LOGIN_SUCCESS_JSON)
        server.enqueue(response)
        assertEquals(
            expectedResponse.asSuccess(),
            identityService.register(registerRequestBody),
        )
    }

    @Test
    fun `register failure with Invalid json should be Invalid`() = runTest {
        val response = MockResponse().setResponseCode(400).setBody(
            INVALID_MODEL_STATE_EMAIL_TAKEN_ERROR_JSON,
        )
        server.enqueue(response)
        val result = identityService.register(registerRequestBody)
        assertEquals(
            RegisterResponseJson.Invalid(
                invalidMessage = "The model state is invalid.",
                validationErrors = mapOf("" to listOf("Email '' is already taken.")),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `register failure with Error json should return Error`() = runTest {
        val response = MockResponse().setResponseCode(429).setBody(TOO_MANY_REQUEST_ERROR_JSON)
        server.enqueue(response)
        val result = identityService.register(registerRequestBody)
        assertEquals(
            RegisterResponseJson.Invalid(
                invalidMessage = "Slow down! Too many requests. Try again soon.",
                validationErrors = null,
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `getToken when request response is Success should return Success`() = runTest {
        server.enqueue(MockResponse().setBody(LOGIN_SUCCESS_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            authModel = IdentityTokenAuthModel.MasterPassword(
                username = EMAIL,
                password = PASSWORD_HASH,
            ),
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertEquals(LOGIN_SUCCESS.asSuccess(), result)
    }

    @Test
    fun `getToken when request is error should return error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = identityService.getToken(
            email = EMAIL,
            authModel = IdentityTokenAuthModel.MasterPassword(
                username = EMAIL,
                password = PASSWORD_HASH,
            ),
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `getToken when response is TwoFactorRequired should return TwoFactorRequired`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody(TWO_FACTOR_BODY_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            authModel = IdentityTokenAuthModel.MasterPassword(
                username = EMAIL,
                password = PASSWORD_HASH,
            ),
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertEquals(TWO_FACTOR_BODY.asSuccess(), result)
    }

    @Test
    fun `getToken when response is a 400 with an error body should return Invalid`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody(INVALID_LOGIN_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            authModel = IdentityTokenAuthModel.MasterPassword(
                username = EMAIL,
                password = PASSWORD_HASH,
            ),
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertEquals(INVALID_LOGIN.asSuccess(), result)
    }

    @Test
    fun `getToken when response is a 400 with a legacy error body should return Invalid`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody(LEGACY_INVALID_LOGIN_JSON))
            val result = identityService.getToken(
                email = EMAIL,
                authModel = IdentityTokenAuthModel.MasterPassword(
                    username = EMAIL,
                    password = PASSWORD_HASH,
                ),
                uniqueAppId = UNIQUE_APP_ID,
            )
            assertEquals(INVALID_LOGIN.asSuccess(), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `prevalidateSso when response is success should return PrevalidateSsoResponseJson Success`() =
        runTest {
            val organizationId = "organizationId"
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(PREVALIDATE_SSO_SUCCESS_JSON),
            )
            val result = identityService.prevalidateSso(organizationId)
            assertEquals(PREVALIDATE_SSO_SUCCESS_BODY.asSuccess(), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `prevalidateSso when response is an error should return PrevalidateSsoResponseJson error`() =
        runTest {
            val organizationId = "organizationId"
            server.enqueue(MockResponse().setResponseCode(400).setBody(PREVALIDATE_SSO_ERROR_JSON))
            val result = identityService.prevalidateSso(organizationId)
            assertEquals(PREVALIDATE_SSO_ERROR_BODY.asSuccess(), result)
        }

    @Test
    fun `refreshTokenSynchronously when response is success should return Success`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REFRESH_TOKEN_SUCCESS_JSON))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN_SUCCESS_BODY.asSuccess(), result)
    }

    @Test
    fun `refreshTokenSynchronously when response is error should return Error`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody(REFRESH_TOKEN_ERROR_JSON))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN_ERROR_BODY.asSuccess(), result)
    }

    @Test
    fun `refreshTokenSynchronously when response is an error should return an error`() {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshTokenSynchronously when response is a 403 error should return an Forbidden`() {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertTrue(result.getOrThrow() is RefreshTokenResponseJson.Forbidden)
    }

    @Test
    fun `refreshTokenSynchronously when response is a 401 error should return an Unauthorized`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertTrue(result.getOrThrow() is RefreshTokenResponseJson.Unauthorized)
    }

    @Test
    fun `registerFinish success json should be Success`() = runTest {
        val expectedResponse = RegisterResponseJson.Success
        val response = MockResponse().setBody(LOGIN_SUCCESS_JSON)
        server.enqueue(response)
        assertEquals(
            expectedResponse.asSuccess(),
            identityService.registerFinish(registerFinishRequestBody),
        )
    }

    @Test
    fun `registerFinish failure with Invalid json should be Invalid`() = runTest {
        val response = MockResponse().setResponseCode(400).setBody(
            INVALID_MODEL_STATE_EMAIL_TAKEN_ERROR_JSON,
        )
        server.enqueue(response)
        val result = identityService.registerFinish(registerFinishRequestBody)
        assertEquals(
            RegisterResponseJson.Invalid(
                invalidMessage = "The model state is invalid.",
                validationErrors = mapOf("" to listOf("Email '' is already taken.")),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `registerFinish failure with Error json should return Error`() = runTest {
        val response = MockResponse().setResponseCode(429).setBody(TOO_MANY_REQUEST_ERROR_JSON)
        server.enqueue(response)
        val result = identityService.registerFinish(registerFinishRequestBody)
        assertEquals(
            RegisterResponseJson.Invalid(
                invalidMessage = "Slow down! Too many requests. Try again soon.",
                validationErrors = null,
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `sendVerificationEmail should return a string when response is populated success`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(EMAIL_TOKEN))
            val result = identityService.sendVerificationEmail(SEND_VERIFICATION_EMAIL_REQUEST)
            assertEquals(
                SendVerificationEmailResponseJson
                    .Success(JsonPrimitive(EMAIL_TOKEN).content)
                    .asSuccess(),
                result,
            )
        }

    @Test
    fun `sendVerificationEmail should return null when response is empty success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val result = identityService.sendVerificationEmail(SEND_VERIFICATION_EMAIL_REQUEST)
        assertEquals(SendVerificationEmailResponseJson.Success(null).asSuccess(), result)
    }

    @Test
    fun `sendVerificationEmail should return an error when response is an error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = identityService.sendVerificationEmail(SEND_VERIFICATION_EMAIL_REQUEST)
        assertTrue(result.isFailure)
    }

    @Test
    fun `verifyEmailToken should return Valid when response is success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val result = identityService.verifyEmailRegistrationToken(
            body = VerifyEmailTokenRequestJson(
                token = EMAIL_TOKEN,
                email = EMAIL,
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `verifyEmailToken should return TokenExpired when response is expired error`() = runTest {
        val json = """
            {
              "message": "Expired link. Please restart registration or try logging in. You may already have an account"
            }
        """.trimIndent()
        val response = MockResponse().setResponseCode(400).setBody(json)
        server.enqueue(response)
        val result = identityService.verifyEmailRegistrationToken(
            body = VerifyEmailTokenRequestJson(
                token = EMAIL_TOKEN,
                email = EMAIL,
            ),
        )
        assertTrue(result.isSuccess)
        assertEquals(
            VerifyEmailTokenResponseJson.TokenExpired,
            result.getOrThrow(),
        )
    }

    @Test
    fun `verifyEmailToken should return Invalid when response message is non expired error`() =
        runTest {
            val messageWithOutExpired = "message without expir... whoops"
            val json = """{ "message": "$messageWithOutExpired" }""".trimIndent()
            val response = MockResponse().setResponseCode(400).setBody(json)
            server.enqueue(response)
            val result = identityService.verifyEmailRegistrationToken(
                body = VerifyEmailTokenRequestJson(
                    token = EMAIL_TOKEN,
                    email = EMAIL,
                ),
            )
            assertTrue(result.isSuccess)
            assertEquals(
                VerifyEmailTokenResponseJson.Invalid(messageWithOutExpired),
                result.getOrThrow(),
            )
        }

    @Test
    fun `verifyEmailToken should return an error when response is an un-handled error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = identityService.verifyEmailRegistrationToken(
            body = VerifyEmailTokenRequestJson(
                email = EMAIL,
                token = EMAIL_TOKEN,
            ),
        )
        assertTrue(result.isFailure)
    }

    companion object {
        private const val UNIQUE_APP_ID = "testUniqueAppId"
        private const val REFRESH_TOKEN = "refreshToken"
        private const val EMAIL_TOKEN = "emailToken"
        private const val EMAIL = "email"
        private const val PASSWORD_HASH = "passwordHash"
        private val registerRequestBody = RegisterRequestJson(
            email = EMAIL,
            masterPasswordHash = "mockk_masterPasswordHash",
            masterPasswordHint = "mockk_masterPasswordHint",
            key = "mockk_key",
            keys = RegisterRequestJson.Keys(
                publicKey = "mockk_publicKey",
                encryptedPrivateKey = "mockk_encryptedPrivateKey",
            ),
            kdfType = KdfTypeJson.PBKDF2_SHA256,
            kdfIterations = 600000U,
        )
        private val registerFinishRequestBody = RegisterFinishRequestJson(
            email = EMAIL,
            masterPasswordHash = "mockk_masterPasswordHash",
            masterPasswordHint = "mockk_masterPasswordHint",
            emailVerificationToken = "mock_emailVerificationToken",
            userSymmetricKey = "mockk_key",
            userAsymmetricKeys = RegisterFinishRequestJson.Keys(
                publicKey = "mockk_publicKey",
                encryptedPrivateKey = "mockk_encryptedPrivateKey",
            ),
            kdfType = KdfTypeJson.PBKDF2_SHA256,
            kdfIterations = 600000U,
        )
    }
}

private const val PREVALIDATE_SSO_SUCCESS_JSON = """
{
  "token": "2ff00750-e2d6-47a6-ae54-67b981e78030"
}
"""

private const val PREVALIDATE_SSO_ERROR_JSON = """
{
  "message": "Organization not found from identifier."
}
"""

private val PREVALIDATE_SSO_SUCCESS_BODY = PrevalidateSsoResponseJson.Success(
    token = "2ff00750-e2d6-47a6-ae54-67b981e78030",
)

private val PREVALIDATE_SSO_ERROR_BODY = PrevalidateSsoResponseJson.Error(
    message = "Organization not found from identifier.",
)

private const val REFRESH_TOKEN_ERROR_JSON = """
{
  "error": "invalid_grant"
}
"""

private val REFRESH_TOKEN_ERROR_BODY = RefreshTokenResponseJson.Error(
    error = "invalid_grant",
)

private const val REFRESH_TOKEN_SUCCESS_JSON = """
{
  "access_token": "accessToken",
  "expires_in": 3600,
  "refresh_token": "refreshToken",
  "token_type": "Bearer"
}
"""

private val REFRESH_TOKEN_SUCCESS_BODY = RefreshTokenResponseJson.Success(
    accessToken = "accessToken",
    expiresIn = 3600,
    refreshToken = "refreshToken",
    tokenType = "Bearer",
)

private const val TWO_FACTOR_BODY_JSON = """
{
  "TwoFactorProviders2": {"1": {"Email": "ex***@email.com"}, "0": {"Email": null}},
  "SsoEmail2faSessionToken": "exampleToken",
  "TwoFactorProviders": ["1", "3", "0"]
}
"""
private val TWO_FACTOR_BODY = GetTokenResponseJson.TwoFactorRequired(
    authMethodsData = mapOf(
        TwoFactorAuthMethod.EMAIL to JsonObject(mapOf("Email" to JsonPrimitive("ex***@email.com"))),
        TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
    ),
    ssoToken = "exampleToken",
    twoFactorProviders = listOf("1", "3", "0"),
)

private const val LOGIN_SUCCESS_JSON = """
{
  "access_token": "accessToken",
  "expires_in": 3600,
  "token_type": "Bearer",
  "refresh_token": "refreshToken",
  "PrivateKey": "privateKey",
  "AccountKeys": {
      "signatureKeyPair": {
        "wrappedSigningKey": "mockWrappedSigningKey-1",
        "verifyingKey": "mockVerifyingKey-1"
      },
      "publicKeyEncryptionKeyPair": {
        "wrappedPrivateKey": "mockWrappedPrivateKey-1",
        "publicKey": "mockPublicKey-1",
        "signedPublicKey": "mockSignedPublicKey-1",
        "object": "publicKeyEncryptionKeyPair"
      },
      "securityState": {
        "securityState": "mockSecurityState-1",
        "securityVersion": 1
      },
      "object": "privateKeys"
  },
  "Key": "key",
  "MasterPasswordPolicy": {
    "MinComplexity": 10,
    "MinLength": 100,
    "RequireUpper": true,
    "RequireLower": true,
    "RequireNumbers": true,
    "RequireSpecial": true,
    "EnforceOnLogin": true
  },
  "ForcePasswordReset": true,
  "Kdf": 1,
  "KdfIterations": 600000,
  "KdfMemory": 16,
  "KdfParallelism": 4,
  "UserDecryptionOptions": {
    "HasMasterPassword": true,
    "TrustedDeviceOption": {
      "EncryptedPrivateKey": "encryptedPrivateKey",
      "EncryptedUserKey": "encryptedUserKey",
      "HasAdminApproval": true,
      "HasLoginApprovingDevice": true,
      "HasManageResetPasswordPermission": true
    },
    "KeyConnectorOption": {
      "KeyConnectorUrl": "keyConnectorUrl"
    }
  },
  "KeyConnectorUrl": "keyConnectorUrl"
}
"""

private val LOGIN_SUCCESS = GetTokenResponseJson.Success(
    accessToken = "accessToken",
    refreshToken = "refreshToken",
    tokenType = "Bearer",
    expiresInSeconds = 3600,
    key = "key",
    kdfType = KdfTypeJson.ARGON2_ID,
    kdfIterations = 600000,
    kdfMemory = 16,
    kdfParallelism = 4,
    privateKey = "privateKey",
    accountKeys = createMockAccountKeysJson(number = 1),
    shouldForcePasswordReset = true,
    twoFactorToken = null,
    masterPasswordPolicyOptions = MasterPasswordPolicyOptionsJson(
        minimumComplexity = 10,
        minimumLength = 100,
        shouldRequireUppercase = true,
        shouldRequireLowercase = true,
        shouldRequireNumbers = true,
        shouldRequireSpecialCharacters = true,
        shouldEnforceOnLogin = true,
    ),
    userDecryptionOptions = UserDecryptionOptionsJson(
        hasMasterPassword = true,
        trustedDeviceUserDecryptionOptions = TrustedDeviceUserDecryptionOptionsJson(
            encryptedPrivateKey = "encryptedPrivateKey",
            encryptedUserKey = "encryptedUserKey",
            hasAdminApproval = true,
            hasLoginApprovingDevice = true,
            hasManageResetPasswordPermission = true,
        ),
        keyConnectorUserDecryptionOptions = KeyConnectorUserDecryptionOptionsJson(
            keyConnectorUrl = "keyConnectorUrl",
        ),
        masterPasswordUnlock = null,
    ),
    keyConnectorUrl = "keyConnectorUrl",
)

private const val INVALID_LOGIN_JSON = """
{
  "ErrorModel": {
    "Message": "123"
  }
}
"""

private const val LEGACY_INVALID_LOGIN_JSON = """
{
  "errorModel": {
    "message": "123"
  }
}
"""

private const val TOO_MANY_REQUEST_ERROR_JSON = """
{
  "Object": "error",
  "Message": "Slow down! Too many requests. Try again soon."
}
"""

private const val INVALID_MODEL_STATE_EMAIL_TAKEN_ERROR_JSON = """
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

private val INVALID_LOGIN = GetTokenResponseJson.Invalid(
    errorModel = GetTokenResponseJson.Invalid.ErrorModel(
        errorMessage = "123",
    ),
)

private val SEND_VERIFICATION_EMAIL_REQUEST = SendVerificationEmailRequestJson(
    email = "email@example.com",
    name = "Name Example",
    receiveMarketingEmails = true,
)
