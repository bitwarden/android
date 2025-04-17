package com.x8bit.bitwarden.data.platform.datasource.network.ssl

import android.net.Uri
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.KeyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager

class SslManagerTest {

    private val mockEnvironment = mockk<Environment> {
        every { environmentUrlData } returns DEFAULT_ENV_URL_DATA
    }
    private val mockEnvironmentRepository = mockk<EnvironmentRepository> {
        every { environment } returns mockEnvironment
    }
    private val mockMutualTlsCertificate = mockk<MutualTlsCertificate> {
        every { alias } returns "mockAlias"
    }
    private val mockKeyManager = mockk<KeyManager> {
        every { getMutualTlsCertificateChain(any(), any()) } returns mockMutualTlsCertificate
    }
    private val mockTrustManagerFactory = mockk<TrustManagerFactory> {
        every { init(null as? KeyStore?) } just runs
        every { trustManagers } returns DEFAULT_TRUST_MANAGERS
    }

    private val sslManager: SslManagerImpl = SslManagerImpl(
        keyManager = mockKeyManager,
        environmentRepository = mockEnvironmentRepository,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(TrustManagerFactory::class)
        every {
            TrustManagerFactory.getDefaultAlgorithm()
        } returns "defaultAlgorithm"
        every {
            TrustManagerFactory.getInstance("defaultAlgorithm")
        } returns mockTrustManagerFactory
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(TrustManagerFactory::class, Uri::class, SSLContext::class)
    }

    @Test
    fun `sslContext should be initialized with default TLS protocol`() {
        setupMockUri()
        assertTrue(sslManager.sslContext.protocol == "TLS")
    }

    @Test
    fun `X509ExtendedKeyManagerImpl should initialize with mutualTlsCertificate`() {
        setupMockUri()
        mockkStatic(SSLContext::class)
        val keyManagersCaptor = slot<Array<X509ExtendedKeyManager>>()
        val trustManagersCaptor = slot<Array<TrustManager>>()
        every { SSLContext.getInstance("TLS") } returns mockk<SSLContext> {
            every {
                init(
                    capture(keyManagersCaptor),
                    capture(trustManagersCaptor),
                    any(),
                )
            } just runs
        }
        every { mockEnvironment.environmentUrlData } returns DEFAULT_ENV_URL_DATA
        every { mockMutualTlsCertificate.alias } returns "mockAlias"
        every { mockMutualTlsCertificate.certificateChain } returns listOf(
            mockk<X509Certificate>(name = "MockCertificate1"),
            mockk<X509Certificate>(name = "MockCertificate2"),
        )
        every {
            mockMutualTlsCertificate.privateKey
        } returns mockk<PrivateKey>()
        every {
            mockKeyManager.getMutualTlsCertificateChain(
                alias = "mockAlias",
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            )
        } returns mockMutualTlsCertificate

        assertNotNull(sslManager.sslContext)

        val keyManager = keyManagersCaptor.captured.first()
        assertEquals(
            mockMutualTlsCertificate.alias,
            keyManager.chooseClientAlias(null, null, null),
        )
        assertTrue(
            keyManager
                .getCertificateChain("mockAlias")
                .contentEquals(
                    mockMutualTlsCertificate
                        .certificateChain
                        .toTypedArray(),
                ),
        )
        assertEquals(
            mockMutualTlsCertificate.privateKey,
            keyManager.getPrivateKey("mockAlias"),
        )
    }

    @Test
    fun `mutualTlsCertificate should return null when keyUri is null`() {
        every {
            mockEnvironment.environmentUrlData
        } returns DEFAULT_ENV_URL_DATA.copy(keyUri = null)
        assertNull(sslManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should be null when host is invalid`() {
        setupMockUri(authority = "UNKNOWN_HOST")
        assertNull(sslManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should be null when alias is null`() {
        setupMockUri(path = null)
        assertNull(sslManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should trim path when it is not null`() {
        setupMockUri(path = "/mockAlias/")
        assertEquals("mockAlias", sslManager.mutualTlsCertificate?.alias)
    }

    @Test
    fun `mutualTlsCertificate should be null when alias is empty after trim`() {
        setupMockUri(path = "/")
        assertNull(sslManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should call keyManager with correct alias and host`() {
        // Set host to ANDROID_KEY_STORE
        setupMockUri()
        assertNotNull(sslManager.mutualTlsCertificate)
        verify {
            mockKeyManager.getMutualTlsCertificateChain(
                alias = "mockAlias",
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            )
        }

        // Set host to KEY_CHAIN
        setupMockUri(authority = "KEY_CHAIN")
        assertNotNull(sslManager.mutualTlsCertificate)
        verify {
            mockKeyManager.getMutualTlsCertificateChain(
                alias = "mockAlias",
                host = MutualTlsKeyHost.KEY_CHAIN,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `trustManagers should return TrustManager array initialized with default algorithm and null keystore`() {
        assertTrue(sslManager.trustManagers.contentEquals(DEFAULT_TRUST_MANAGERS))
        verify {
            TrustManagerFactory.getInstance("defaultAlgorithm")
            TrustManagerFactory.getDefaultAlgorithm()
            mockTrustManagerFactory.init(null as? KeyStore?)
        }
    }

    private fun setupMockUri(
        authority: String = "ANDROID_KEY_STORE",
        path: String? = "/mockAlias",
    ) {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.authority } returns authority
        every { uriMock.path } returns path
    }
}

private val DEFAULT_TRUST_MANAGERS = arrayOf<TrustManager>(
    mockk(name = "MockTrustManager1"),
    mockk(name = "MockTrustManager2"),
)

val DEFAULT_ENV_URL_DATA = EnvironmentUrlDataJson(
    base = "https://example.com",
    keyUri = "cert://ANDROID_KEY_STORE/mockAlias",
)
