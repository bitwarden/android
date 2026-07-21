package com.bitwarden.core.data.manager.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeystoreManagerTest {

    private val mockAndroidKeyStore = mockk<KeyStore>(name = "MockAndroidKeyStore")
    private val mockKeyGenerator = mockk<KeyGenerator>()
    private val mockKeyGenParameterSpec = mockk<KeyGenParameterSpec>()
    private val mockBuildInfoManager = mockk<BuildInfoManager> {
        every { applicationId } returns APPLICATION_ID
    }

    private val keystoreManager: KeystoreManager = KeystoreManagerImpl(
        buildInfoManager = mockBuildInfoManager,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(KeyStore::class, KeyGenerator::class)
        mockkConstructor(KeyGenParameterSpec.Builder::class)
        every { KeyStore.getInstance("AndroidKeyStore") } returns mockAndroidKeyStore
        every { mockAndroidKeyStore.load(null) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(KeyStore::class, KeyGenerator::class)
        unmockkConstructor(KeyGenParameterSpec.Builder::class)
    }

    @Test
    fun `getKey should return success with key stored in keystore`() {
        val mockSecretKey = mockk<SecretKey>()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns mockSecretKey

        assertEquals(mockSecretKey.asSuccess(), keystoreManager.getKeyOrNull(alias = ALIAS))
        verify(exactly = 1) {
            mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null)
        }
    }

    @Test
    fun `getKey should return success with null when no key exists`() {
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null

        assertEquals(null.asSuccess(), keystoreManager.getKeyOrNull(alias = ALIAS))
    }

    @Test
    fun `getKey should return failure when keystore throws KeyStoreException`() {
        val error = KeyStoreException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } throws error

        assertEquals(error.asFailure(), keystoreManager.getKeyOrNull(alias = ALIAS))
    }

    @Test
    fun `getKey should return failure when keystore throws NoSuchAlgorithmException`() {
        val error = NoSuchAlgorithmException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } throws error

        assertEquals(error.asFailure(), keystoreManager.getKeyOrNull(alias = ALIAS))
    }

    @Test
    fun `getKey should return failure when keystore throws UnrecoverableKeyException`() {
        val error = UnrecoverableKeyException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } throws error

        assertEquals(error.asFailure(), keystoreManager.getKeyOrNull(alias = ALIAS))
    }

    @Test
    fun `getOrCreateKey should return existing key without generating a new one`() {
        val mockSecretKey = mockk<SecretKey>()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns mockSecretKey

        assertEquals(mockSecretKey.asSuccess(), keystoreManager.getOrCreateKey(alias = ALIAS))
        verify(exactly = 0) {
            KeyGenerator.getInstance(any<String>(), any<String>())
        }
    }

    @Test
    fun `getOrCreateKey should return failure when retrieving the existing key fails`() {
        val error = KeyStoreException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
        verify(exactly = 0) {
            KeyGenerator.getInstance(any<String>(), any<String>())
        }
    }

    @Test
    fun `getOrCreateKey should generate and return a new AES key when none exists`() {
        val mockSecretKey = mockk<SecretKey>()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        setupMockKeyGenParameterSpecBuilder()
        setupMockKeyGenerator()
        every { mockKeyGenerator.generateKey() } returns mockSecretKey

        assertEquals(mockSecretKey.asSuccess(), keystoreManager.getOrCreateKey(alias = ALIAS))
        verify(exactly = 1) {
            anyConstructed<KeyGenParameterSpec.Builder>()
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            anyConstructed<KeyGenParameterSpec.Builder>()
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            anyConstructed<KeyGenParameterSpec.Builder>().setKeySize(256)
            mockKeyGenerator.init(mockKeyGenParameterSpec)
            mockKeyGenerator.generateKey()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateKey should return failure when key generator instance throws NoSuchAlgorithmException`() {
        val error = NoSuchAlgorithmException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        every {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateKey should return failure when key generator instance throws NoSuchProviderException`() {
        val error = NoSuchProviderException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        every {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateKey should return failure when key generator instance throws IllegalArgumentException`() {
        val error = IllegalArgumentException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        every {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getOrCreateKey should return failure when key generator init throws InvalidAlgorithmParameterException`() {
        val error = InvalidAlgorithmParameterException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        setupMockKeyGenParameterSpecBuilder()
        setupMockKeyGenerator()
        every { mockKeyGenerator.init(mockKeyGenParameterSpec) } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
    }

    @Test
    fun `getOrCreateKey should return failure when generateKey throws ProviderException`() {
        val error = ProviderException()
        every { mockAndroidKeyStore.getKey(NAMESPACED_ALIAS, null) } returns null
        setupMockKeyGenParameterSpecBuilder()
        setupMockKeyGenerator()
        every { mockKeyGenerator.generateKey() } throws error

        assertEquals(error.asFailure(), keystoreManager.getOrCreateKey(alias = ALIAS))
    }

    @Test
    fun `hasKey should return true when keystore contains alias`() {
        every { mockAndroidKeyStore.containsAlias(NAMESPACED_ALIAS) } returns true

        assertTrue(keystoreManager.hasKey(alias = ALIAS))
    }

    @Test
    fun `hasKey should return false when keystore does not contain alias`() {
        every { mockAndroidKeyStore.containsAlias(NAMESPACED_ALIAS) } returns false

        assertFalse(keystoreManager.hasKey(alias = ALIAS))
    }

    @Test
    fun `removeKey should delete entry and return true`() {
        every { mockAndroidKeyStore.deleteEntry(NAMESPACED_ALIAS) } just runs

        assertTrue(keystoreManager.removeKey(alias = ALIAS))
        verify(exactly = 1) {
            mockAndroidKeyStore.deleteEntry(NAMESPACED_ALIAS)
        }
    }

    @Test
    fun `removeKey should return false when keystore throws KeyStoreException`() {
        every { mockAndroidKeyStore.deleteEntry(NAMESPACED_ALIAS) } throws KeyStoreException()

        assertFalse(keystoreManager.removeKey(alias = ALIAS))
    }

    private fun setupMockKeyGenerator() {
        every {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } returns mockKeyGenerator
        every { mockKeyGenerator.init(mockKeyGenParameterSpec) } just runs
    }

    private fun setupMockKeyGenParameterSpecBuilder() {
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().setBlockModes(any())
        } answers { self as KeyGenParameterSpec.Builder }
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().setEncryptionPaddings(any())
        } answers { self as KeyGenParameterSpec.Builder }
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().setDigests(any())
        } answers { self as KeyGenParameterSpec.Builder }
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().build()
        } returns mockKeyGenParameterSpec
        every {
            anyConstructed<KeyGenParameterSpec.Builder>().setKeySize(any())
        } answers { self as KeyGenParameterSpec.Builder }
    }
}

private const val APPLICATION_ID: String = "com.mock.app"
private const val ALIAS: String = "mockAlias"
private const val NAMESPACED_ALIAS: String = "$APPLICATION_ID.$ALIAS"
