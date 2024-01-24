package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.MasterPasswordPolicyOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PrevalidateSsoResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class IdentityServiceTest : BaseServiceTest() {

    private val identityApi: IdentityApi = retrofit.create()
    private val deviceModelProvider = mockk<DeviceModelProvider>() {
        every { deviceModel } returns "Test Device"
    }

    private val identityService = IdentityServiceImpl(
        api = identityApi,
        json = Json,
        deviceModelProvider = deviceModelProvider,
    )

    @Test
    fun `getToken when request response is Success should return Success`() = runTest {
        server.enqueue(MockResponse().setBody(LOGIN_SUCCESS_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
            uniqueAppId = UNIQUE_APP_ID,
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
            uniqueAppId = UNIQUE_APP_ID,
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
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertEquals(Result.success(CAPTCHA_BODY), result)
    }

    @Test
    fun `getToken when response is a 400 with an error body should return Invalid`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody(INVALID_LOGIN_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
            uniqueAppId = UNIQUE_APP_ID,
        )
        assertEquals(Result.success(INVALID_LOGIN), result)
    }

    @Test
    fun `prevalidateSso when response is success should return PrevalidateSsoResponseJson`() =
        runTest {
            val organizationId = "organizationId"
            server.enqueue(MockResponse().setResponseCode(200).setBody(PREVALIDATE_SSO_JSON))
            val result = identityService.prevalidateSso(organizationId)
            assertEquals(Result.success(PREVALIDATE_SSO_BODY), result)
        }

    @Test
    fun `prevalidateSso when response is an error should return an error`() =
        runTest {
            val organizationId = "organizationId"
            server.enqueue(MockResponse().setResponseCode(400))
            val result = identityService.prevalidateSso(organizationId)
            assertTrue(result.isFailure)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `refreshTokenSynchronously when response is success should return RefreshTokenResponseJson`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(REFRESH_TOKEN_JSON))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertEquals(REFRESH_TOKEN_BODY.asSuccess(), result)
    }

    @Test
    fun `refreshTokenSynchronously when response is an error should return an error`() {
        server.enqueue(MockResponse().setResponseCode(400))
        val result = identityService.refreshTokenSynchronously(refreshToken = REFRESH_TOKEN)
        assertTrue(result.isFailure)
    }

    companion object {
        private const val UNIQUE_APP_ID = "testUniqueAppId"
        private const val REFRESH_TOKEN = "refreshToken"
        private const val EMAIL = "email"
        private const val PASSWORD_HASH = "passwordHash"
    }
}

private const val PREVALIDATE_SSO_JSON = """
{
  "token": "2ff00750-e2d6-47a6-ae54-67b981e78030"
}
"""

private val PREVALIDATE_SSO_BODY = PrevalidateSsoResponseJson(
    token = "2ff00750-e2d6-47a6-ae54-67b981e78030",
)

private const val REFRESH_TOKEN_JSON = """
{
  "access_token": "accessToken",
  "expires_in": 3600,
  "refresh_token": "refreshToken",
  "token_type": "Bearer"
}
"""

private val REFRESH_TOKEN_BODY = RefreshTokenResponseJson(
    accessToken = "accessToken",
    expiresIn = 3600,
    refreshToken = "refreshToken",
    tokenType = "Bearer",
)

private const val CAPTCHA_BODY_JSON = """
{
  "HCaptcha_SiteKey": "123"
}
"""
private val CAPTCHA_BODY = GetTokenResponseJson.CaptchaRequired("123")

private const val LOGIN_SUCCESS_JSON = """
{
  "access_token": "accessToken",
  "expires_in": 3600,
  "token_type": "Bearer",
  "refresh_token": "refreshToken",
  "PrivateKey": "privateKey",
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
  "ResetMasterPassword": true,
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
  }
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
    shouldForcePasswordReset = true,
    shouldResetMasterPassword = true,
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
    ),
)

private const val INVALID_LOGIN_JSON = """
{
  "ErrorModel": {
    "Message": "123"
  }
}
"""

private val INVALID_LOGIN = GetTokenResponseJson.Invalid(
    errorModel = GetTokenResponseJson.Invalid.ErrorModel(
        errorMessage = "123",
    ),
)
