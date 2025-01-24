package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.security.KeyChain
import android.security.KeyChainException
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ImportPrivateKeyResult
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsCertificate
import com.x8bit.bitwarden.data.platform.datasource.disk.model.MutualTlsKeyHost
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class KeyManagerTest {
    private val mockContext = mockk<Context>()
    private val mockAndroidKeyStore = mockk<KeyStore>(name = "MockAndroidKeyStore")
    private val mockPkcs12KeyStore = mockk<KeyStore>(name = "MockPKCS12KeyStore")
    private val keyDiskSource = KeyManagerImpl(
        context = mockContext,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(KeyStore::class, KeyChain::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(KeyStore::class, KeyChain::class)
    }

    @Test
    fun `getMutualTlsCertificateChain should return null when MutualTlsKeyAlias is not found`() {
        // Verify null is returned when alias is not found in KeyChain
        setupMockAndroidKeyStore()
        every { KeyChain.getPrivateKey(mockContext, "mockAlias") } throws KeyChainException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = "mockAlias",
                host = MutualTlsKeyHost.KEY_CHAIN,
            ),
        )

        // Verify null is returned when alias is not found in AndroidKeyStore
        every { mockAndroidKeyStore.getKey("mockAlias", null) } throws UnrecoverableKeyException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = "mockAlias",
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return MutualTlsCertificateChain when using ANDROID KEY STORE and key is found`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"
        val mockPrivateKey = mockk<PrivateKey>()
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")
        every {
            mockAndroidKeyStore.getCertificateChain(mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } returns mockPrivateKey

        val result = keyDiskSource.getMutualTlsCertificateChain(
            alias = mockAlias,
            host = MutualTlsKeyHost.ANDROID_KEY_STORE,
        )

        assertEquals(
            MutualTlsCertificate(
                alias = mockAlias,
                certificateChain = listOf(mockCertificate1, mockCertificate2),
                privateKey = mockPrivateKey,
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return null when using ANDROID KEY STORE and key is not found`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")
        every {
            mockAndroidKeyStore.getCertificateChain(mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } returns null

        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return null when using ANDROID KEY STORE and certificate chain is invalid`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } returns mockk<PrivateKey>()

        // Verify null is returned when certificate chain is empty
        every {
            mockAndroidKeyStore.getCertificateChain(mockAlias)
        } returns emptyArray()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )

        // Verify null is returned when certificate chain contains non-X509Certificate objects
        every {
            mockAndroidKeyStore.getCertificateChain(mockAlias)
        } returns arrayOf(mockk<Certificate>())
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return null when using ANDROID KEY STORE and an exception occurs`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")
        every {
            mockAndroidKeyStore.getCertificateChain(mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)

        // Verify KeyStoreException is handled
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } throws KeyStoreException()

        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )

        // Verify UnrecoverableKeyException is handled
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } throws UnrecoverableKeyException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )

        // Verify NoSuchAlgorithmException is handled
        every {
            mockAndroidKeyStore.getKey(mockAlias, null)
        } throws NoSuchAlgorithmException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.ANDROID_KEY_STORE,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return MutualTlsCertificateChain when using KEY CHAIN and key is found`() {
        val mockAlias = "mockAlias"
        val mockPrivateKey = mockk<PrivateKey>()
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")
        every {
            KeyChain.getCertificateChain(mockContext, mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)
        every {
            KeyChain.getPrivateKey(mockContext, mockAlias)
        } returns mockPrivateKey

        val result = keyDiskSource.getMutualTlsCertificateChain(
            alias = mockAlias,
            host = MutualTlsKeyHost.KEY_CHAIN,
        )

        assertEquals(
            MutualTlsCertificate(
                alias = mockAlias,
                certificateChain = listOf(mockCertificate1, mockCertificate2),
                privateKey = mockPrivateKey,
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return null when using KEY CHAIN and key is not found`() {
        val mockAlias = "mockAlias"
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")
        every {
            KeyChain.getCertificateChain(mockContext, mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)
        every {
            KeyChain.getPrivateKey(mockContext, mockAlias)
        } returns null

        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.KEY_CHAIN,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getMutualTlsCertificateChain should return null when using KEY CHAIN and an exception occurs`() {
        val mockAlias = "mockAlias"
        val mockCertificate1 = mockk<X509Certificate>(name = "mockCertificate1")
        val mockCertificate2 = mockk<X509Certificate>(name = "mockCertificate2")

        every {
            KeyChain.getCertificateChain(mockContext, mockAlias)
        } returns arrayOf(mockCertificate1, mockCertificate2)

        // Verify KeyChainException from getPrivateKey is handled
        every {
            KeyChain.getPrivateKey(mockContext, mockAlias)
        } throws KeyChainException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.KEY_CHAIN,
            ),
        )

        // Verify KeyChainException from getCertificateChain is handled
        every { KeyChain.getPrivateKey(mockContext, mockAlias) } returns mockk()
        every { KeyChain.getCertificateChain(mockContext, mockAlias) } throws KeyChainException()
        assertNull(
            keyDiskSource.getMutualTlsCertificateChain(
                alias = mockAlias,
                host = MutualTlsKeyHost.KEY_CHAIN,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `removeMutualTlsKey should remove key from AndroidKeyStore when host is ANDROID_KEY_STORE`() {
        setupMockAndroidKeyStore()
        val mockAlias = "mockAlias"

        every { mockAndroidKeyStore.deleteEntry(mockAlias) } just runs

        keyDiskSource.removeMutualTlsKey(
            alias = mockAlias,
            host = MutualTlsKeyHost.ANDROID_KEY_STORE,
        )

        verify {
            mockAndroidKeyStore.deleteEntry(mockAlias)
        }
    }

    @Test
    fun `removeMutualTlsKey should do nothing when host is KEY_CHAIN`() {
        keyDiskSource.removeMutualTlsKey(
            alias = "mockAlias",
            host = MutualTlsKeyHost.KEY_CHAIN,
        )

        verify(exactly = 0) {
            mockAndroidKeyStore.deleteEntry(any())
        }
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
            keyDiskSource.importMutualTlsCertificate(
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
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws KeyStoreException()
        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "KeyStoreException was not handled correctly" }

        // Verify IOException is handled
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws IOException()
        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "IOException was not handled correctly" }

        // Verify IOException with UnrecoverableKeyException cause is handled
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws IOException(UnrecoverableKeyException())

        assertEquals(
            ImportPrivateKeyResult.Error.UnrecoverableKey,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )

        // Verify IOException with unexpected cause is handled
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws IOException(Exception())
        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "IOException with Unexpected exception cause was not handled correctly" }

        // Verify CertificateException is handled
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws CertificateException()
        assertEquals(
            ImportPrivateKeyResult.Error.InvalidCertificateChain,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "CertificateException was not handled correctly" }

        // Verify NoSuchAlgorithmException is handled
        every {
            mockPkcs12KeyStore.load(any(), any())
        } throws NoSuchAlgorithmException()
        assertEquals(
            ImportPrivateKeyResult.Error.UnsupportedKey,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        ) { "NoSuchAlgorithmException was not handled correctly" }
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
            ImportPrivateKeyResult.Error.UnsupportedKey,
            keyDiskSource.importMutualTlsCertificate(
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
        every {
            mockPkcs12KeyStore.getKey(
                "mockInternalAlias",
                password.toCharArray(),
            )
        } throws UnrecoverableKeyException()

        assertEquals(
            ImportPrivateKeyResult.Error.UnrecoverableKey,
            keyDiskSource.importMutualTlsCertificate(
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
            ImportPrivateKeyResult.Error.UnrecoverableKey,
            keyDiskSource.importMutualTlsCertificate(
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
            ImportPrivateKeyResult.Error.InvalidCertificateChain,
            keyDiskSource.importMutualTlsCertificate(
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
            ImportPrivateKeyResult.Error.InvalidCertificateChain,
            keyDiskSource.importMutualTlsCertificate(
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
        every {
            mockPkcs12KeyStore.getCertificateChain("mockInternalAlias")
        } returns arrayOf(mockk())

        every {
            mockAndroidKeyStore.setKeyEntry(
                expectedAlias,
                any(),
                any(),
                any(),
            )
        } throws KeyStoreException()

        assertEquals(
            ImportPrivateKeyResult.Error.KeyStoreOperationFailed,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `importMutualTlsCertificate should return DuplicateAlias when alias already exists in AndroidKeyStore`() {
        setupMockAndroidKeyStore()
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
        every {
            mockPkcs12KeyStore.getCertificateChain("mockInternalAlias")
        } returns arrayOf(mockk())

        every { mockAndroidKeyStore.containsAlias(expectedAlias) } returns true

        assertEquals(
            ImportPrivateKeyResult.Error.DuplicateAlias,
            keyDiskSource.importMutualTlsCertificate(
                key = pkcs12Bytes,
                alias = expectedAlias,
                password = password,
            ),
        )
    }

    private fun setupMockAndroidKeyStore() {
        every { KeyStore.getInstance("AndroidKeyStore") } returns mockAndroidKeyStore
        every { mockAndroidKeyStore.load(null) } just runs
    }

    private fun setupMockPkcs12KeyStore() {
        every { KeyStore.getInstance("pkcs12") } returns mockPkcs12KeyStore
        every { mockPkcs12KeyStore.load(any(), any()) } just runs
    }
}
