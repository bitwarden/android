package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SdkTokenRepositoryTest {

    private val authDiskSource: AuthDiskSource = mockk()

    @Test
    fun `getAccessToken should return null when userId is null`() = runTest {
        val repository = createSdkTokenRepository(userId = null)
        assertNull(repository.getAccessToken())
        verify(exactly = 0) {
            authDiskSource.getAccountTokens(userId = any())
        }
    }

    @Test
    fun `getAccessToken should return null when userId is valid and tokens are null`() =
        runTest {
            every { authDiskSource.getAccountTokens(userId = USER_ID) } returns null
            val repository = createSdkTokenRepository()
            assertNull(repository.getAccessToken())
            verify(exactly = 1) {
                authDiskSource.getAccountTokens(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAccessToken should return null when userId is valid and accessToken is null`() =
        runTest {
            every {
                authDiskSource.getAccountTokens(userId = USER_ID)
            } returns AccountTokensJson(
                accessToken = null,
                refreshToken = "refreshToken",
            )
            val repository = createSdkTokenRepository()
            assertNull(repository.getAccessToken())
            verify(exactly = 1) {
                authDiskSource.getAccountTokens(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAccessToken should return access token when userId is valid and accessToken is present`() =
        runTest {
            val accessToken = "access_token"
            every {
                authDiskSource.getAccountTokens(userId = USER_ID)
            } returns AccountTokensJson(
                accessToken = accessToken,
                refreshToken = "refreshToken",
            )
            val repository = createSdkTokenRepository()
            assertEquals(accessToken, repository.getAccessToken())
            verify(exactly = 1) {
                authDiskSource.getAccountTokens(userId = USER_ID)
            }
        }

    private fun createSdkTokenRepository(
        userId: String? = USER_ID,
    ): SdkTokenRepository = SdkTokenRepository(
        userId = userId,
        authDiskSource = authDiskSource,
    )
}

private const val USER_ID: String = "userId"
