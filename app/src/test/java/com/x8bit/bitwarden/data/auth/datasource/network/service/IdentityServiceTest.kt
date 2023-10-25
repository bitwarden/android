package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.MasterPasswordPolicyOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
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

    @Test
    fun `getToken when response is a 400 with an error body should return Invalid`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody(INVALID_LOGIN_JSON))
        val result = identityService.getToken(
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            captchaToken = null,
        )
        assertEquals(Result.success(INVALID_LOGIN), result)
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
