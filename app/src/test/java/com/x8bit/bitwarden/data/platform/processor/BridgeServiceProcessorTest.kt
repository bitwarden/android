package com.x8bit.bitwarden.data.platform.processor

import android.os.Build
import android.os.RemoteCallbackList
import com.bitwarden.bridge.IBridgeService
import com.bitwarden.bridge.IBridgeServiceCallback
import com.bitwarden.bridge.model.EncryptedSharedAccountData
import com.bitwarden.bridge.model.SharedAccountData
import com.bitwarden.bridge.util.NATIVE_BRIDGE_SDK_VERSION
import com.bitwarden.bridge.util.encrypt
import com.bitwarden.bridge.util.generateSecretKey
import com.bitwarden.bridge.util.toFingerprint
import com.bitwarden.bridge.util.toSymmetricEncryptionKeyData
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.BridgeRepository
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BridgeServiceProcessorTest {

    private val featureFlagManager = mockk<FeatureFlagManager>()
    private val bridgeRepository = mockk<BridgeRepository>()

    private lateinit var bridgeServiceProcessor: BridgeServiceProcessorImpl

    @BeforeEach
    fun setup() {
        bridgeServiceProcessor = BridgeServiceProcessorImpl(
            bridgeRepository = bridgeRepository,
            featureFlagManager = featureFlagManager,
            dispatcherManager = FakeDispatcherManager(),
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(::isBuildVersionBelow)
        unmockkStatic(SharedAccountData::encrypt)
    }

    @Test
    fun `when AuthenticatorSync feature flag is off, should return null binder`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns false
        assertNull(bridgeServiceProcessor.binder)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `when AuthenticatorSync feature flag is on and running Android level greater than S, should return non-null binder`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        assertNotNull(bridgeServiceProcessor.binder)
    }

    @Test
    fun `when below Android level S, should never return a binder regardless of feature flag`() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns true
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns false
        assertNull(bridgeServiceProcessor.binder)

        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        assertNull(bridgeServiceProcessor.binder)
    }

    @Test
    fun `versionNumber should match version of compiled bridge sdk`() {
        val binder = getDefaultBinder()
        assertEquals(
            NATIVE_BRIDGE_SDK_VERSION,
            binder.versionNumber,
        )
    }

    @Test
    @Suppress("MaxLineLength")
    fun `checkSymmetricEncryptionKeyFingerprint should return false when given fingerprint is null`() {
        val binder = getDefaultBinder()
        // Set disk symmetric key to null so that it is technically equal to given null fingerprint:
        every { bridgeRepository.authenticatorSyncSymmetricKey } returns null
        // Binder should still return false in this case:
        assertFalse(binder.checkSymmetricEncryptionKeyFingerprint(null))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `checkSymmetricEncryptionKeyFingerprint should return false if fingerprint doesn't match`() {
        val binder = getDefaultBinder()
        every { bridgeRepository.authenticatorSyncSymmetricKey } returns ByteArray(1)
        assertFalse(binder.checkSymmetricEncryptionKeyFingerprint(SYMMETRIC_KEY_FINGERPRINT))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `checkSymmetricEncryptionKeyFingerprint should return true if fingerprint does match`() {
        val binder = getDefaultBinder()
        every {
            bridgeRepository.authenticatorSyncSymmetricKey
        } returns SYMMETRIC_KEY.symmetricEncryptionKey.byteArray
        assert(binder.checkSymmetricEncryptionKeyFingerprint(SYMMETRIC_KEY_FINGERPRINT))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `getSymmetricEncryptionKeyData should return null when there is no symmetric key stored on disk`() {
        val binder = getDefaultBinder()
        every { bridgeRepository.authenticatorSyncSymmetricKey } returns null
        assertNull(binder.symmetricEncryptionKeyData)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `getSymmetricEncryptionKeyData should return the symmetric key stored on disk`() {
        val binder = getDefaultBinder()
        every {
            bridgeRepository.authenticatorSyncSymmetricKey
        } returns SYMMETRIC_KEY.symmetricEncryptionKey.byteArray
        assertEquals(SYMMETRIC_KEY, binder.symmetricEncryptionKeyData)
    }

    @Nested
    inner class SyncAccountsTest {

        private var lastAccountsSync: EncryptedSharedAccountData? = null

        private val serviceCallback = object : IBridgeServiceCallback.Stub() {
            override fun onAccountsSync(data: EncryptedSharedAccountData?) {
                lastAccountsSync = data
            }
        }

        @BeforeEach
        fun setup() {
            // Setup RemoteCallbackList to call back to serviceCallback:
            mockkConstructor(RemoteCallbackList::class)
            every {
                anyConstructed<RemoteCallbackList<IBridgeServiceCallback>>()
                    .register(serviceCallback)
            } returns true
            every {
                anyConstructed<RemoteCallbackList<IBridgeServiceCallback>>()
                    .beginBroadcast()
            } returns 1
            every {
                anyConstructed<RemoteCallbackList<IBridgeServiceCallback>>()
                    .getBroadcastItem(0)
            } returns serviceCallback
            lastAccountsSync = null
        }

        @Test
        fun `syncAccounts when symmetricEncryptionKeyData is null should do nothing`() {
            every { bridgeRepository.authenticatorSyncSymmetricKey } returns null
            getDefaultBinder().syncAccounts()
            assertNull(lastAccountsSync)
        }

        @Test
        fun `syncAccounts should encrypt result from BridgeRepository`() {
            val sharedAccountData = mockk<SharedAccountData>()
            val expected = mockk<EncryptedSharedAccountData>()
            every {
                bridgeRepository.authenticatorSyncSymmetricKey
            } returns SYMMETRIC_KEY.symmetricEncryptionKey.byteArray
            coEvery { bridgeRepository.getSharedAccounts() } returns sharedAccountData
            mockkStatic(SharedAccountData::encrypt)
            every { sharedAccountData.encrypt(SYMMETRIC_KEY) } returns expected.asSuccess()

            getDefaultBinder().syncAccounts()

            assertEquals(expected, lastAccountsSync)
            coVerify { bridgeRepository.getSharedAccounts() }
        }
    }

    /**
     * Helper function for accessing the default implementation of [IBridgeService.Stub]. This
     * is particularly useful because the binder is nullable on [BridgeServiceProcessor] behind
     * a feature flag.
     */
    private fun getDefaultBinder(): IBridgeService.Stub {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        every { featureFlagManager.getFeatureFlag(FlagKey.AuthenticatorSync) } returns true
        return bridgeServiceProcessor.binder!!
    }
}

/**
 * Symmetric encryption key that can be used for test.
 */
private val SYMMETRIC_KEY = generateSecretKey()
    .getOrThrow()
    .encoded
    .toSymmetricEncryptionKeyData()

/**
 * Fingerprint of [SYMMETRIC_KEY].
 */
private val SYMMETRIC_KEY_FINGERPRINT = SYMMETRIC_KEY.toFingerprint().getOrThrow()
