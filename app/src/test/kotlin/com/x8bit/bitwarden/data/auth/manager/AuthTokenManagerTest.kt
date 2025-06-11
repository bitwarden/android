package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.ZonedDateTime

class AuthTokenManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val authTokenManager = AuthTokenManagerImpl(fakeAuthDiskSource)

    @Test
    fun `UserState is null`() {
        fakeAuthDiskSource.userState = null
        assertNull(authTokenManager.getActiveAccessTokenOrNull())
    }

    @Test
    fun `Account tokens are null`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
            .copy(
                accounts = mapOf(
                    USER_ID to ACCOUNT.copy(tokens = null),
                ),
            )
        assertNull(authTokenManager.getActiveAccessTokenOrNull())
    }

    @Test
    fun `Access token is null`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
            .copy(
                accounts = mapOf(
                    USER_ID to ACCOUNT.copy(
                        tokens = AccountTokensJson(
                            accessToken = null,
                            refreshToken = null,
                        ),
                    ),
                ),
            )
        assertNull(authTokenManager.getActiveAccessTokenOrNull())
    }

    @Test
    fun `getActiveAccessTokenOrNull should return active user access token`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        fakeAuthDiskSource.storeAccountTokens(
            userId = USER_ID,
            accountTokens = AccountTokensJson(
                accessToken = ACCESS_TOKEN,
                refreshToken = REFRESH_TOKEN,
            ),
        )
        assertEquals(
            ACCESS_TOKEN,
            authTokenManager.getActiveAccessTokenOrNull(),
        )
    }
}

private const val EMAIL: String = "test@bitwarden.com"
private const val USER_ID: String = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val ACCESS_TOKEN: String = "accessToken"
private const val REFRESH_TOKEN: String = "refreshToken"
private val ACCOUNT: AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID,
        email = EMAIL,
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
        isTwoFactorEnabled = false,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    tokens = AccountTokensJson(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)
private val SINGLE_USER_STATE: UserStateJson = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(
        USER_ID to ACCOUNT,
    ),
)
