package com.x8bit.bitwarden.data.platform.manager

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SdkClientManagerTest {

    private val sdkClientManager = SdkClientManagerImpl(
        clientProvider = { mockk(relaxed = true) },
        featureFlagManager = mockk(),
    )

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateClient should create a new client for each userId and return a cached client for subsequent calls`() =
        runTest {
            val userId = "userId"
            val firstClient = sdkClientManager.getOrCreateClient(userId = userId)

            // Additional calls for the same userId return the same value
            val secondClient = sdkClientManager.getOrCreateClient(userId = userId)
            assertEquals(firstClient, secondClient)

            // Additional calls for different userIds should return different values
            val otherUserId = "otherUserId"
            val thirdClient = sdkClientManager.getOrCreateClient(userId = otherUserId)
            assertNotEquals(firstClient, thirdClient)
        }

    @Test
    fun `destroyClient should call close on the Client and remove it from the cache`() = runTest {
        val userId = "userId"
        val firstClient = sdkClientManager.getOrCreateClient(userId = userId)

        sdkClientManager.destroyClient(userId = userId)

        verify { firstClient.close() }

        // New calls for the same userId should return different values
        val secondClient = sdkClientManager.getOrCreateClient(userId = userId)
        assertNotEquals(firstClient, secondClient)
    }
}
