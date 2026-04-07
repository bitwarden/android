package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.JwtTokenDataJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.TrustedDeviceUserDecryptionOptionsJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockAccountKeysJson
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetTokenResponseExtensionsTest {

    @BeforeEach
    fun beforeEach() {
        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
    }

    @Test
    fun `toUserState with a null previous state creates a new single user state`() {
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN_1) } returns JWT_TOKEN_DATA

        assertEquals(
            SINGLE_USER_STATE_1,
            GET_TOKEN_RESPONSE_SUCCESS.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            ),
        )
    }

    @Test
    fun `toUserState with a non-null previous state updates the previous state`() {
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN_1) } returns JWT_TOKEN_DATA

        assertEquals(
            MULTI_USER_STATE,
            GET_TOKEN_RESPONSE_SUCCESS.toUserState(
                previousUserState = SINGLE_USER_STATE_2,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            ),
        )
    }

    @Test
    fun `toUserState with userDecryptionOptions creates a new single user state`() {
        val tokenResponse = GET_TOKEN_RESPONSE_SUCCESS.copy(
            userDecryptionOptions = USER_DECRYPTION_OPTIONS,
        )
        val expectedState = SINGLE_USER_STATE_1.copy(
            accounts = mapOf(
                USER_ID_1 to ACCOUNT_1.copy(
                    profile = PROFILE_1.copy(
                        forcePasswordResetReason = ForcePasswordResetReason
                            .TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
                        userDecryptionOptions = USER_DECRYPTION_OPTIONS,
                    ),
                ),
            ),
        )
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN_1) } returns JWT_TOKEN_DATA

        assertEquals(
            expectedState,
            tokenResponse.toUserState(
                previousUserState = null,
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            ),
        )
    }
}

private const val ACCESS_TOKEN_1 = "accessToken1"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"

private val JWT_TOKEN_DATA = JwtTokenDataJson(
    userId = "2a135b23-e1fb-42c9-bec3-573857bc8181",
    email = "test@bitwarden.com",
    isEmailVerified = true,
    name = "Bitwarden Tester",
    expirationAsEpochTime = 1697495714,
    hasPremium = false,
    authenticationMethodsReference = listOf("Application"),
)

private val GET_TOKEN_RESPONSE_SUCCESS = GetTokenResponseJson.Success(
    accessToken = ACCESS_TOKEN_1,
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
    shouldForcePasswordReset = false,
    twoFactorToken = null,
    masterPasswordPolicyOptions = null,
    userDecryptionOptions = null,
    keyConnectorUrl = null,
)
private val USER_DECRYPTION_OPTIONS = UserDecryptionOptionsJson(
    hasMasterPassword = false,
    trustedDeviceUserDecryptionOptions = TrustedDeviceUserDecryptionOptionsJson(
        encryptedUserKey = "encryptedUserKey",
        encryptedPrivateKey = "encryptedPrivateKey",
        hasAdminApproval = true,
        hasLoginApprovingDevice = true,
        hasManageResetPasswordPermission = true,
    ),
    keyConnectorUserDecryptionOptions = null,
    masterPasswordUnlock = null,
)
private val PROFILE_1 = AccountJson.Profile(
    userId = USER_ID_1,
    email = "test@bitwarden.com",
    isEmailVerified = true,
    name = "Bitwarden Tester",
    hasPremium = false,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    forcePasswordResetReason = null,
    kdfType = KdfTypeJson.ARGON2_ID,
    kdfIterations = 600000,
    kdfMemory = 16,
    kdfParallelism = 4,
    userDecryptionOptions = null,
    isTwoFactorEnabled = null,
    creationDate = null,
)
private val ACCOUNT_1 = AccountJson(
    profile = PROFILE_1,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)
private val ACCOUNT_2 = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID_2,
        email = "test2@bitwarden.com",
        isEmailVerified = true,
        name = "Bitwarden Tester 2",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        kdfIterations = 400000,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        isTwoFactorEnabled = null,
        creationDate = null,
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)
private val SINGLE_USER_STATE_1 = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
    ),
)
private val SINGLE_USER_STATE_2 = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_2 to ACCOUNT_2,
    ),
)
private val MULTI_USER_STATE = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
        USER_ID_2 to ACCOUNT_2,
    ),
)
