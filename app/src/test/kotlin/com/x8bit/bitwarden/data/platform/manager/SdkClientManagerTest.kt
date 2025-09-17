package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.manager.NativeLibraryManager
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkClientManagerTest {

    private val mockNativeLibraryManager = mockk<NativeLibraryManager> {
        every { loadLibrary(any()) } returns Result.success(Unit)
    }
    private val sdkRepoFactory: SdkRepositoryFactory = mockk {
        every { getCipherRepository(userId = any()) } returns mockk()
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `init should load the bitwarden_uniffi library when build version is below 31`() = runTest {
        every { isBuildVersionAtLeast(31) } returns false
        createSdkClientManager()
        verify { mockNativeLibraryManager.loadLibrary("bitwarden_uniffi") }
    }

    @Test
    fun `init should not load the bitwarden_uniffi library when build version is 31 or above`() =
        runTest {
            every { isBuildVersionAtLeast(31) } returns true
            createSdkClientManager()
            verify(exactly = 0) { mockNativeLibraryManager.loadLibrary("bitwarden_uniffi") }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateClient should create a new client for each userId and return a cached client for subsequent calls`() =
        runTest {
            val sdkClientManager = createSdkClientManager()
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
        val sdkClientManager = createSdkClientManager()
        val userId = "userId"
        val firstClient = sdkClientManager.getOrCreateClient(userId = userId)

        sdkClientManager.destroyClient(userId = userId)

        verify { firstClient.close() }

        // New calls for the same userId should return different values
        val secondClient = sdkClientManager.getOrCreateClient(userId = userId)
        assertNotEquals(firstClient, secondClient)
    }

    private fun createSdkClientManager(): SdkClientManagerImpl = SdkClientManagerImpl(
        clientProvider = { mockk(relaxed = true) },
        nativeLibraryManager = mockNativeLibraryManager,
        featureFlagManager = mockk(),
        sdkRepoFactory = sdkRepoFactory,
    )
}
