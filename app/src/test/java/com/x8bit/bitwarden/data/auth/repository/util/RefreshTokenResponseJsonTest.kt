package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.repository.model.JwtTokenDataJson
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefreshTokenResponseJsonTest {

    @BeforeEach
    fun beforeEach() {
        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
    }

    @Test
    fun `toUserState updates the previous state`() {
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN_UPDATED) } returns JWT_TOKEN_DATA

        assertEquals(
            SINGLE_USER_STATE_UPDATED,
            REFRESH_TOKEN_RESPONSE.toUserStateJson(
                userId = USER_ID_1,
                previousUserState = SINGLE_USER_STATE,
            ),
        )
    }

    @Test
    fun `toUserState updates the previous state for non-active user`() {
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN_UPDATED) } returns JWT_TOKEN_DATA

        assertEquals(
            MULTI_USER_STATE_UPDATED,
            REFRESH_TOKEN_RESPONSE.toUserStateJson(
                userId = USER_ID_1,
                previousUserState = MULTI_USER_STATE,
            ),
        )
    }
}

private const val ACCESS_TOKEN_UPDATED = "updatedAccessToken"
private const val REFRESH_TOKEN_UPDATED = "updatedRefreshToken"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"

private val JWT_TOKEN_DATA = JwtTokenDataJson(
    userId = USER_ID_1,
    email = "updated@bitwarden.com",
    isEmailVerified = false,
    name = "Updated Bitwarden Tester",
    expirationAsEpochTime = 1697495714,
    hasPremium = true,
    authenticationMethodsReference = listOf("Application"),
)

private val REFRESH_TOKEN_RESPONSE = RefreshTokenResponseJson(
    accessToken = ACCESS_TOKEN_UPDATED,
    expiresIn = 3600,
    refreshToken = REFRESH_TOKEN_UPDATED,
    tokenType = "Bearer",
)

private val ACCOUNT_1 = AccountJson(
    profile = AccountJson.Profile(
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
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val ACCOUNT_1_UPDATED = ACCOUNT_1.copy(
    profile = ACCOUNT_1.profile.copy(
        userId = JWT_TOKEN_DATA.userId,
        email = JWT_TOKEN_DATA.email,
        isEmailVerified = JWT_TOKEN_DATA.isEmailVerified,
        name = JWT_TOKEN_DATA.name,
        hasPremium = JWT_TOKEN_DATA.hasPremium,
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
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val SINGLE_USER_STATE = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
    ),
)

private val SINGLE_USER_STATE_UPDATED = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1_UPDATED,
    ),
)

private val MULTI_USER_STATE = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
        USER_ID_2 to ACCOUNT_2,
    ),
)

private val MULTI_USER_STATE_UPDATED = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1_UPDATED,
        USER_ID_2 to ACCOUNT_2,
    ),
)
