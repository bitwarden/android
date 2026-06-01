package com.x8bit.bitwarden.data.platform.datasource.network.ssl

import android.content.Context
import android.net.Uri
import android.security.KeyChain
import android.security.KeyChainException
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import com.x8bit.bitwarden.data.platform.manager.CertificateManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Vector
import javax.net.ssl.SSLContext

@Suppress("LargeClass")
class CertificateManagerTest {

    private val mockContext = mockk<Context>()
    private val mockAndroidKeyStore = mockk<KeyStore>(name = "MockAndroidKeyStore")
    private val mockPkcs12KeyStore = mockk<KeyStore>(name = "MockPKCS12KeyStore")
    private val mockEnvironment = mockk<Environment> {
        every { environmentUrlData } returns DEFAULT_ENV_URL_DATA
    }
    private val mockEnvironmentRepository = mockk<EnvironmentRepository> {
        every { environment } returns mockEnvironment
    }

    private val certificateManager: CertificateManagerImpl = CertificateManagerImpl(
        environmentRepository = mockEnvironmentRepository,
        context = mockContext,
    )
    @BeforeEach
    fun setUp() {
        mockkStatic(KeyStore::class, KeyChain::class)
        mockkConstructor(MissingPropertyException::class)
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            KeyStore::class,
            KeyChain::class,
            Uri::class,
            SSLContext::class,
        )
        unmockkConstructor(MissingPropertyException::class)
    }

    @Test
    fun `mutualTlsCertificate should return certificate stored in AndroidKeyStore`() {
        setupMockUri()
        setupMockAndroidKeyStore()

        val mockPrivateKey = mockk<PrivateKey>()
        val mockCertificateChain = listOf(
            mockk<X509Certificate>(),
        )

        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } returns mockPrivateKey
        every {
            mockAndroidKeyStore.getCertificateChain("mockAlias")
        } returns mockCertificateChain.toTypedArray()

        assertEquals(
            MutualTlsCertificate(
                alias = "mockAlias",
                privateKey = mockPrivateKey,
                certificateChain = mockCertificateChain,
            ),
            certificateManager.mutualTlsCertificate,
        )
    }

    @Test
    fun `mutualTlsCertificate should return certificate stored in KeyChain`() {
        setupMockUri(authority = MutualTlsKeyHost.KEY_CHAIN.name)
        setupMockPkcs12KeyStore()
        val mockPrivateKey = mockk<PrivateKey>()
        val mockCertificateChain = listOf(
            mockk<X509Certificate>(),
        )

        every {
            KeyChain.getPrivateKey(mockContext, "mockAlias")
        } returns mockPrivateKey
        every {
            KeyChain.getCertificateChain(mockContext, "mockAlias")
        } returns mockCertificateChain.toTypedArray()

        assertEquals(
            MutualTlsCertificate(
                alias = "mockAlias",
                privateKey = mockPrivateKey,
                certificateChain = mockCertificateChain.toList(),
            ),
            certificateManager.mutualTlsCertificate,
        )
    }

    @Test
    fun `mutualTlsCertificate should return null when keyUri is invalid`() {
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when host is invalid`() {
        setupMockUri(authority = "UNKNOWN_HOST")
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when alias is null`() {
        setupMockUri(path = null)
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when alias is empty after trim`() {
        setupMockUri(path = "/")
        assertEquals(
            "",
            certificateManager.chooseClientAlias(null, null, null),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when KeyChain throws KeyChainException for private key`() {
        setupMockUri(authority = MutualTlsKeyHost.KEY_CHAIN.name)
        val keyChainException = KeyChainException("Alias not found")
        every {
            KeyChain.getPrivateKey(mockContext, "mockAlias")
        } throws keyChainException
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when KeyChain throws KeyChainException for certificate chain`() {
        setupMockUri(authority = MutualTlsKeyHost.KEY_CHAIN.name)
        val keyChainException = KeyChainException("Unable to access certificate chain")
        every {
            KeyChain.getPrivateKey(mockContext, "mockAlias")
        } returns mockk<PrivateKey>()
        every {
            KeyChain.getCertificateChain(mockContext, "mockAlias")
        } throws keyChainException
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when KeyChain returns null private key`() {
        setupMockUri(authority = MutualTlsKeyHost.KEY_CHAIN.name)
        every {
            KeyChain.getPrivateKey(mockContext, "mockAlias")
        } returns null
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when KeyChain returns null certificate chain`() {
        setupMockUri(authority = MutualTlsKeyHost.KEY_CHAIN.name)
        every {
            KeyChain.getPrivateKey(mockContext, "mockAlias")
        } returns mockk<PrivateKey>()

        every {
            KeyChain.getCertificateChain(mockContext, "mockAlias")
        } returns null
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `mutualTlsCertificate should return null when AndroidKeyStore returns null private key`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } returns null
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when AndroidKeyStore returns empty certificate chain`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } returns mockk<PrivateKey>()
        every {
            mockAndroidKeyStore.getCertificateChain("mockAlias")
        } returns emptyArray()
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when AndroidKeyStore throws KeyStoreException`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        val keyStoreException = KeyStoreException()
        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } throws keyStoreException
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when AndroidKeyStore throws UnrecoverableKeyException`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } throws UnrecoverableKeyException()
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `mutualTlsCertificate should return null when AndroidKeyStore throws NoSuchAlgorithmException`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        val noSuchAlgorithmException = NoSuchAlgorithmException()
        every {
            mockAndroidKeyStore.getKey("mockAlias", null)
        } throws noSuchAlgorithmException
        assertNull(certificateManager.mutualTlsCertificate)
    }

    @Test
    fun `importMutualTlsCertificate should return Success when key is imported successfully`() {
        setupMockAndroidKeyStore()
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val internalAlias = "mockInternalAlias"
        val privateKey = mockk<PrivateKey>()
        val certChain = arrayOf(mockk<X509Certificate>())
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"
        every { mockPkcs12KeyStore.aliases() } returns mockk {
            every { hasMoreElements() } returns true
            every { nextElement() } returns internalAlias
        }
        every {
            mockPkcs12KeyStore.setKeyEntry(
                internalAlias,
                privateKey,
                null,
                certChain,
            )
        } just runs
        every {
            mockPkcs12KeyStore.getKey(
                internalAlias,
                password.toCharArray(),
            )
        } returns privateKey
        every {
            mockPkcs12KeyStore.getCertificateChain(internalAlias)
        } returns certChain
        every {
            mockAndroidKeyStore.containsAlias(expectedAlias)
        } returns false
        every {
            mockAndroidKeyStore.setKeyEntry(expectedAlias, privateKey, null, certChain)
        } just runs

        assertEquals(
            ImportPrivateKeyResult.Success(alias = expectedAlias),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Test
    fun `importMutualTlsCertificate should return Error when loading PKCS12 throws an exception`() {
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"

        // Verify KeyStoreException is handled
        val keystoreError = KeyStoreException()
        every { mockPkcs12KeyStore.load(any(), any()) } throws keystoreError
        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey(throwable = keystoreError),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "KeyStoreException was not handled correctly" }

        // Verify IOException is handled
        val keystoreOperationIoError = IOException()
        every { mockPkcs12KeyStore.load(any(), any()) } throws keystoreOperationIoError
        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed(
                throwable = keystoreOperationIoError,
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "IOException was not handled correctly" }

        // Verify IOException with UnrecoverableKeyException cause is handled
        val unrecoverableKeyError = IOException(UnrecoverableKeyException())
        every { mockPkcs12KeyStore.load(any(), any()) } throws unrecoverableKeyError
        assertEquals(
            ImportPrivateKeyResult.Error.UnrecoverableKey(throwable = unrecoverableKeyError),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )

        // Verify IOException with unexpected cause is handled
        val keystoreOperationError = IOException(Exception())
        every { mockPkcs12KeyStore.load(any(), any()) } throws keystoreOperationError
        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed(
                throwable = keystoreOperationError,
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "IOException with Unexpected exception cause was not handled correctly" }

        // Verify CertificateException is handled
        val certificateError = CertificateException()
        every { mockPkcs12KeyStore.load(any(), any()) } throws certificateError
        assertEquals(
            ImportPrivateKeyResult.Error.InvalidCertificateChain(throwable = certificateError),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "CertificateException was not handled correctly" }

        // Verify NoSuchAlgorithmException is handled
        val algorithmError = NoSuchAlgorithmException()
        every { mockPkcs12KeyStore.load(any(), any()) } throws algorithmError
        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey(throwable = algorithmError),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "NoSuchAlgorithmException was not handled correctly" }
    }

    @Test
    fun `importMutualTlsCertificate should return UnsupportedKey when aliases is null`() {
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"

        every { mockPkcs12KeyStore.aliases() } returns null

        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey(
                throwable = MissingPropertyException("Internal Alias"),
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Test
    fun `importMutualTlsCertificate should return UnsupportedKey when key store is empty`() {
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"

        every { mockPkcs12KeyStore.aliases() } returns mockk {
            every { hasMoreElements() } returns false
        }

        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey(
                throwable = MissingPropertyException("Internal Alias"),
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `importMutualTlsCertificate should return UnrecoverableKey when unable to retrieve private key`() {
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"

        every {
            mockPkcs12KeyStore.aliases()
        } returns mockk {
            every { hasMoreElements() } returns true
            every { nextElement() } returns "mockInternalAlias"
        }
        val error = UnrecoverableKeyException()
        every {
            mockPkcs12KeyStore.getKey(
                "mockInternalAlias",
                password.toCharArray(),
            )
        } throws error

        assertEquals(
            ImportPrivateKeyResult.Error.UnrecoverableKey(throwable = error),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )

        every {
            mockPkcs12KeyStore.getKey(
                "mockInternalAlias",
                password.toCharArray(),
            )
        } returns null
        assertEquals(
            ImportPrivateKeyResult.Error.UnrecoverableKey(
                throwable = MissingPropertyException("Private Key"),
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `importMutualTlsCertificate should return InvalidCertificateChain when certificate chain is empty`() {
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"

        every { mockPkcs12KeyStore.aliases() } returns mockk {
            every { hasMoreElements() } returns true
            every { nextElement() } returns "mockInternalAlias"
        }
        every {
            mockPkcs12KeyStore.getKey(
                "mockInternalAlias",
                password.toCharArray(),
            )
        } returns mockk()

        // Verify empty certificate chain is handled
        every {
            mockPkcs12KeyStore.getCertificateChain("mockInternalAlias")
        } returns emptyArray()
        assertEquals(
            ImportPrivateKeyResult.Error.InvalidCertificateChain(
                throwable = MissingPropertyException("Certificate Chain"),
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )

        // Verify null certificate chain is handled
        every {
            mockPkcs12KeyStore.getCertificateChain("mockInternalAlias")
        } returns null
        assertEquals(
            ImportPrivateKeyResult.Error.InvalidCertificateChain(
                throwable = MissingPropertyException("Certificate Chain"),
            ),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `importMutualTlsCertificate should return KeyStoreOperationFailed when saving to Android KeyStore throws KeyStoreException`() {
        setupMockAndroidKeyStore()
        setupMockPkcs12KeyStore()
        val expectedAlias = "mockAlias"
        val pkcs12Bytes = "key.p12".toByteArray()
        val password = "password"
        val error = KeyStoreException()

        every { mockPkcs12KeyStore.aliases() } returns mockk {
            every { hasMoreElements() } returns true
            every { nextElement() } returns "mockInternalAlias"
        }

        every {
            mockPkcs12KeyStore.getKey(
                "mockInternalAlias",
                password.toCharArray(),
            )
        } returns mockk()
        every {
            mockPkcs12KeyStore.getCertificateChain("mockInternalAlias")
        } returns arrayOf(mockk())

        every { mockAndroidKeyStore.containsAlias(expectedAlias) } returns false

        every {
            mockAndroidKeyStore.setKeyEntry(
                expectedAlias,
                any(),
                any(),
                any(),
            )
        } throws error

        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed(throwable = error),
            certificateManager.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeMutualTlsKey should remove key from AndroidKeyStore when host is ANDROID_KEY_STORE`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"

        every { mockAndroidKeyStore.deleteEntry(mockAlias) } just runs

        certificateManager.removeMutualTlsKey(
            alias = mockAlias,
            host = MutualTlsKeyHost.ANDROID_KEY_STORE,
        )

        verify {
            mockAndroidKeyStore.deleteEntry(mockAlias)
        }
    }

    @Test
    fun `removeMutualTlsKey should do nothing when host is KEY_CHAIN`() {
        certificateManager.removeMutualTlsKey(
            alias = "mockAlias",
            host = MutualTlsKeyHost.KEY_CHAIN,
        )

        verify(exactly = 0) {
            mockAndroidKeyStore.deleteEntry(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeMutualTlsKey should catch KeyStoreException when deleting key from AndroidKeyStore fails`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"
        val mockException = KeyStoreException()

        every { mockAndroidKeyStore.deleteEntry(mockAlias) } throws mockException

        certificateManager.removeMutualTlsKey(
            alias = mockAlias,
            host = MutualTlsKeyHost.ANDROID_KEY_STORE,
        )

        verify {
            mockAndroidKeyStore.deleteEntry(mockAlias)
        }
    }

    @Test
    fun `chooseClientAlias should return alias from mutualTlsCertificate`() {
        setupMockUri()
        setupMockAndroidKeyStore()

        every { mockAndroidKeyStore.getKey("mockAlias", null) } returns mockk<PrivateKey>()
        every { mockAndroidKeyStore.getCertificateChain("mockAlias") } returns arrayOf(
            mockk<X509Certificate>(),
        )

        assertEquals(
            "mockAlias",
            certificateManager.chooseClientAlias(
                arrayOf("TLS"),
                emptyArray(),
                null,
            ),
        )
    }

    @Test
    fun `getCertificateChain should return certificate chain from mutualTlsCertificate`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        val mockCertChain = arrayOf(mockk<X509Certificate>())
        every { mockAndroidKeyStore.getKey("mockAlias", null) } returns mockk<PrivateKey>()
        every { mockAndroidKeyStore.getCertificateChain("mockAlias") } returns mockCertChain

        assertTrue(
            certificateManager
                .getCertificateChain("mockAlias")
                .contentEquals(mockCertChain),
        )
    }

    @Test
    fun `getCertificateChain should return null when mutualTlsCertificate is null`() {
        assertNull(certificateManager.getCertificateChain("mockAlias"))
    }

    @Test
    fun `getPrivateKey should return private key from mutualTlsCertificate`() {
        setupMockUri()
        setupMockAndroidKeyStore()
        val mockPrivateKey = mockk<PrivateKey>()
        every { mockAndroidKeyStore.getKey("mockAlias", null) } returns mockPrivateKey
        every { mockAndroidKeyStore.getCertificateChain("mockAlias") } returns arrayOf(
            mockk<X509Certificate>(),
        )
        assertEquals(mockPrivateKey, certificateManager.getPrivateKey("mockAlias"))
    }

    @Test
    fun `getPrivateKey should return null when mutualTlsCertificate is null`() {
        assertNull(certificateManager.getPrivateKey("mockAlias"))
    }

    @Test
    fun `getMutualTlsKeyAliases should return aliases from AndroidKeyStore`() {
        setupMockAndroidKeyStore()
        val mockAliases = listOf("alias1", "alias2")
        every { mockAndroidKeyStore.aliases() } returns Vector(mockAliases).elements()
        assertEquals(mockAliases, certificateManager.getMutualTlsKeyAliases())
    }

    //region Helper functions
    private fun setupMockAndroidKeyStore() {
        every { KeyStore.getInstance("AndroidKeyStore") } returns mockAndroidKeyStore
        every { mockAndroidKeyStore.load(null) } just runs
    }

    private fun setupMockPkcs12KeyStore() {
        every { KeyStore.getInstance("pkcs12") } returns mockPkcs12KeyStore
        every { mockPkcs12KeyStore.load(any(), any()) } just runs
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
    //endregion Helper functions
}

val DEFAULT_ENV_URL_DATA = EnvironmentUrlDataJson(
    base = "https://example.com",
    keyUri = "cert://ANDROID_KEY_STORE/mockAlias",
)
