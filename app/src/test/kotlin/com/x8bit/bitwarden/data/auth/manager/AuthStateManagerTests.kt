package com.x8bit.bitwarden.data.auth.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthStateManagerTests {

    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val authStateManager: AuthStateManager = AuthStateManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `authStateFlow should react to user state changes and account token changes`() = runTest {
        authStateManager.authStateFlow.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())

            // Store the tokens, nothing happens yet since there is technically no active user yet
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1,
            )
            expectNoEvents()
            // Update the active user, we are now authenticated
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
            assertEquals(AuthState.Authenticated(accessToken = ACCESS_TOKEN), awaitItem())

            // Adding a tokens for the non-active user does not update the state
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_2,
                accountTokens = ACCOUNT_TOKENS_2,
            )
            expectNoEvents()
            // Adding a non-active user does not update the state
            fakeAuthDiskSource.userState = MULTI_USER_STATE
            expectNoEvents()

            // Changing the active users tokens causes an update
            val newAccessToken = "new_access_token"
            fakeAuthDiskSource.storeAccountTokens(
                userId = USER_ID_1,
                accountTokens = ACCOUNT_TOKENS_1.copy(accessToken = newAccessToken),
            )
            assertEquals(AuthState.Authenticated(newAccessToken), awaitItem())

            // Change the active user causes an update
            fakeAuthDiskSource.userState = MULTI_USER_STATE.copy(activeUserId = USER_ID_2)
            assertEquals(AuthState.Authenticated(accessToken = ACCESS_TOKEN_2), awaitItem())

            // Clearing the tokens of the active state results in the Unauthenticated state
            fakeAuthDiskSource.storeAccountTokens(userId = USER_ID_2, accountTokens = null)
            assertEquals(AuthState.Unauthenticated, awaitItem())
        }
    }
}

private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"
private const val EMAIL = "test@bitwarden.com"
private const val EMAIL_2 = "test2@bitwarden.com"
private const val ACCESS_TOKEN = "accessToken"
private const val ACCESS_TOKEN_2 = "accessToken2"
private const val REFRESH_TOKEN = "refreshToken"

private val ACCOUNT_TOKENS_1: AccountTokensJson = AccountTokensJson(
    accessToken = ACCESS_TOKEN,
    refreshToken = REFRESH_TOKEN,
)
private val ACCOUNT_TOKENS_2: AccountTokensJson = AccountTokensJson(
    accessToken = ACCESS_TOKEN_2,
    refreshToken = "refreshToken",
)

private val ACCOUNT_1: AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID_1,
        email = EMAIL,
        isEmailVerified = true,
        name = "Bitwarden Tester",
        hasPremiumPersonally = false,
        hasPremiumFromOrganization = null,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.ARGON2_ID,
        kdfIterations = 600000,
        kdfMemory = 16,
        kdfParallelism = 4,
        isTwoFactorEnabled = false,
        creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
        userDecryptionOptions = UserDecryptionOptionsJson(
            hasMasterPassword = true,
            trustedDeviceUserDecryptionOptions = null,
            keyConnectorUserDecryptionOptions = null,
            masterPasswordUnlock = MasterPasswordUnlockDataJson(
                kdf = KdfJson(
                    kdfType = KdfTypeJson.ARGON2_ID,
                    iterations = 600000,
                    memory = 16,
                    parallelism = 4,
                ),
                masterKeyWrappedUserKey = "encryptedUserKey",
                salt = EMAIL,
            ),
        ),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)
private val ACCOUNT_2 = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID_2,
        email = EMAIL_2,
        isEmailVerified = true,
        name = "Bitwarden Tester 2",
        hasPremiumPersonally = false,
        hasPremiumFromOrganization = null,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        kdfIterations = 400000,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        isTwoFactorEnabled = true,
        creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
    ),
)
private val SINGLE_USER_STATE_1: UserStateJson = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(USER_ID_1 to ACCOUNT_1),
)
private val MULTI_USER_STATE = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
        USER_ID_2 to ACCOUNT_2,
    ),
)
