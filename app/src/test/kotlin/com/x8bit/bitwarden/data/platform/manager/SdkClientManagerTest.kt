package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.api.base.ClientCertificate
import com.bitwarden.core.ClientSettings
import com.bitwarden.core.ValidateClientSettingsException
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.manager.NativeLibraryManager
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.LogLevel
import com.bitwarden.sdk.initLogger
import com.bitwarden.sdk.validateClientSettings
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.error.MutualTlsCertificateUnavailableException
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkPlatformApiFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.SdkRepositoryFactory
import com.x8bit.bitwarden.data.platform.manager.sdk.log.SdkLoggerFactory
import com.x8bit.bitwarden.data.platform.manager.util.toClientCertificateOrNull
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkClientManagerTest {

    private val mockNativeLibraryManager = mockk<NativeLibraryManager> {
        every { loadLibrary(any()) } returns Result.success(Unit)
    }
    private val sdkRepoFactory: SdkRepositoryFactory = mockk {
        every { getRepositories(userId = any()) } returns mockk()
    }
    private val sdkPlatformApiFactory: SdkPlatformApiFactory = mockk {
        every { getServerCommunicationConfigPlatformApi() } returns mockk()
    }
    private val certificateManager: CertificateManager = mockk()
    private val sdkLoggerFactory: SdkLoggerFactory = mockk {
        every { logLevel } returns LogLevel.INFO
        every { getLogCallback() } returns mockk()
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast, ::initLogger, ::validateClientSettings)
        mockkStatic(MutualTlsCertificate::toClientCertificateOrNull)
        every { isBuildVersionAtLeast(version = any()) } returns true
        every { initLogger(callback = any(), level = any(), platformLogger = any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast, ::initLogger, ::validateClientSettings)
        unmockkStatic(MutualTlsCertificate::toClientCertificateOrNull)
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
    fun `singleUseClient should create a new client everytime and run the lambda with it`() =
        runTest {
            val sdkClientManager = createSdkClientManager()
            val firstClient = sdkClientManager.singleUseClient { this }

            // Additional calls should always create a new client
            val secondClient = sdkClientManager.singleUseClient { this }
            assertNotEquals(firstClient, secondClient)
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

    @Test
    fun `validateClientSettings should pass the settings and certificate for the given URLs`() =
        runTest {
            val settings = mockk<ClientSettings>()
            val mutualTlsCertificate = mockk<MutualTlsCertificate>()
            val clientCertificate = mockk<ClientCertificate>()
            every {
                sdkRepoFactory.getClientSettings(environmentUrlData = ENVIRONMENT_URL_DATA)
            } returns settings
            every {
                certificateManager.getMutualTlsCertificate(keyUri = KEY_URI)
            } returns mutualTlsCertificate
            every { mutualTlsCertificate.toClientCertificateOrNull() } returns clientCertificate
            every {
                validateClientSettings(settings = settings, clientCertificate = clientCertificate)
            } just runs

            val result = createSdkClientManager().validateClientSettings(
                environmentUrlData = ENVIRONMENT_URL_DATA,
            )

            assertEquals(Unit.asSuccess(), result)
            verify(exactly = 1) {
                validateClientSettings(settings = settings, clientCertificate = clientCertificate)
            }
        }

    @Test
    fun `validateClientSettings should pass no certificate when there is no key URI`() = runTest {
        val environmentUrlData = ENVIRONMENT_URL_DATA.copy(keyUri = null)
        val settings = mockk<ClientSettings>()
        every {
            sdkRepoFactory.getClientSettings(environmentUrlData = environmentUrlData)
        } returns settings
        every { validateClientSettings(settings = settings, clientCertificate = null) } just runs

        val result = createSdkClientManager().validateClientSettings(
            environmentUrlData = environmentUrlData,
        )

        assertEquals(Unit.asSuccess(), result)
        verify(exactly = 0) { certificateManager.getMutualTlsCertificate(keyUri = any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `validateClientSettings should fail without calling the SDK when the certificate cannot be loaded`() =
        runTest {
            every { certificateManager.getMutualTlsCertificate(keyUri = KEY_URI) } returns null

            val result = createSdkClientManager().validateClientSettings(
                environmentUrlData = ENVIRONMENT_URL_DATA,
            )

            assertTrue(result.exceptionOrNull() is MutualTlsCertificateUnavailableException)
            verify(exactly = 0) { validateClientSettings(settings = any(), clientCertificate = any()) }
        }

    @Test
    fun `validateClientSettings should return the validation error as a failure`() = runTest {
        val error = ValidateClientSettingsException.IdentityUrl("invalid identity URL")
        every { sdkRepoFactory.getClientSettings(environmentUrlData = any()) } returns mockk()
        every {
            validateClientSettings(settings = any(), clientCertificate = null)
        } throws BitwardenException.ValidateClientSettings(v1 = error)

        val result = createSdkClientManager().validateClientSettings(
            environmentUrlData = ENVIRONMENT_URL_DATA.copy(keyUri = null),
        )

        assertEquals(error.asFailure(), result)
    }

    private fun createSdkClientManager(): SdkClientManagerImpl = SdkClientManagerImpl(
        dispatcherManager = FakeDispatcherManager(),
        clientProvider = { _, _ -> mockk(relaxed = true) },
        nativeLibraryManager = mockNativeLibraryManager,
        featureFlagManager = mockk(),
        sdkRepoFactory = sdkRepoFactory,
        sdkPlatformApiFactory = sdkPlatformApiFactory,
        sdkLoggerFactory = sdkLoggerFactory,
        certificateManager = certificateManager,
    )
}

private const val KEY_URI: String = "cert://ANDROID_KEY_STORE/alias"

private val ENVIRONMENT_URL_DATA: EnvironmentUrlDataJson = EnvironmentUrlDataJson(
    base = "https://vault.example.com",
    keyUri = KEY_URI,
)
