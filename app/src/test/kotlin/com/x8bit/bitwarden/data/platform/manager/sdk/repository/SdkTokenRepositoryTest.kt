package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.network.provider.TokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SdkTokenRepositoryTest {

    private val tokenProvider: TokenProvider = mockk()

    @Test
    fun `getAccessToken should return null when userId is null`() = runTest {
        val repository = createSdkTokenRepository(userId = null)
        assertNull(repository.getAccessToken())
        verify(exactly = 0) {
            tokenProvider.getAccessToken(userId = any())
        }
    }

    @Test
    fun `getAccessToken should return null when userId is valid and tokenProvider returns null`() =
        runTest {
            every { tokenProvider.getAccessToken(userId = USER_ID) } returns null
            val repository = createSdkTokenRepository()
            assertNull(repository.getAccessToken())
            verify(exactly = 1) {
                tokenProvider.getAccessToken(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getAccessToken should return access token when userId is valid and tokenProvider returns an access token`() =
        runTest {
            val accessToken = "access_token"
            every { tokenProvider.getAccessToken(userId = USER_ID) } returns accessToken
            val repository = createSdkTokenRepository()
            assertEquals(accessToken, repository.getAccessToken())
            verify(exactly = 1) {
                tokenProvider.getAccessToken(userId = USER_ID)
            }
        }

    private fun createSdkTokenRepository(
        userId: String? = USER_ID,
    ): SdkTokenRepository = SdkTokenRepository(
        userId = userId,
        tokenProvider = tokenProvider,
    )
}

private const val USER_ID: String = "userId"
