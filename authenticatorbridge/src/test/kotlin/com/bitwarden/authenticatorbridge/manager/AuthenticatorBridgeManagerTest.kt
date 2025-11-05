package com.bitwarden.authenticatorbridge.manager

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.util.FakeLifecycleOwner
import com.bitwarden.authenticatorbridge.util.FakeSymmetricKeyStorageProvider
import com.bitwarden.authenticatorbridge.util.PasswordManagerSignatureVerifier
import com.bitwarden.authenticatorbridge.util.TestAuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.util.decrypt
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.authenticatorbridge.util.toFingerprint
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData
import com.bitwarden.core.util.isBuildVersionAtLeast
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AuthenticatorBridgeManagerTest {

    private val context = mockk<Context> {
        every { applicationContext } returns this
        every {
            packageManager.getPackageInfo("com.x8bit.bitwarden.dev", 0)
        } returns mockk()
    }
    private val mockBridgeService: IAuthenticatorBridgeService = mockk()
    private val mockPasswordManagerSignatureVerifier = mockk<PasswordManagerSignatureVerifier> {
        every { isValidPasswordManagerApp(any()) } returns true
    }
    private val fakeLifecycleOwner = FakeLifecycleOwner()
    private val fakeSymmetricKeyStorageProvider = FakeSymmetricKeyStorageProvider()
    private val testAuthenticatorBridgeCallbackProvider = TestAuthenticatorBridgeCallbackProvider()

    private lateinit var manager: AuthenticatorBridgeManagerImpl

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionAtLeast)
        mockkConstructor(Intent::class)
        mockkStatic(IAuthenticatorBridgeService.Stub::class)
        mockkStatic(EncryptedSharedAccountData::decrypt)

        // Set default test API level to at least 34
        every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns true
        every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns true

        manager = AuthenticatorBridgeManagerImpl(
            context = context,
            connectionType = AuthenticatorBridgeConnectionType.DEV,
            symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
            passwordManagerSignatureVerifier = mockPasswordManagerSignatureVerifier,
            callbackProvider = testAuthenticatorBridgeCallbackProvider,
            processLifecycleOwner = fakeLifecycleOwner,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Intent::class)
        unmockkStatic(IAuthenticatorBridgeService.Stub::class)
        unmockkStatic(EncryptedSharedAccountData::decrypt)
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `initial AccountSyncState should be Loading when Bitwarden app is present`() {
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `initial AccountSyncState should be AppNotInstalled when Bitwarden app is not present`() {
        every {
            mockPasswordManagerSignatureVerifier.isValidPasswordManagerApp(any())
        } returns false
        val manager = AuthenticatorBridgeManagerImpl(
            context = context,
            connectionType = AuthenticatorBridgeConnectionType.DEV,
            symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
            passwordManagerSignatureVerifier = mockPasswordManagerSignatureVerifier,
            callbackProvider = testAuthenticatorBridgeCallbackProvider,
            processLifecycleOwner = fakeLifecycleOwner,
        )
        assertEquals(AccountSyncState.AppNotInstalled, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `onStart when bindService fails should set state to error`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns false

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
    }

    @Test
    fun `onStart when Bitwarden app is not present should set state to AppNotInstalled`() {
        // Mock verifier to return false (app not valid/installed)
        every {
            mockPasswordManagerSignatureVerifier.isValidPasswordManagerApp(any())
        } returns false

        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } throws SecurityException()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.AppNotInstalled, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `onStart when bindService throws security exception should set state to error`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } throws SecurityException()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `onStart when Bitwarden app is present and bindService succeeds should set state to Loading before service calls back`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key is not present and service returns null symmetric key state should be SyncNotEnabled and should unbind service`() {
        val serviceConnection = slot<ServiceConnection>()
        val mockIntent: Intent = mockk()
        fakeSymmetricKeyStorageProvider.symmetricKey = null
        every { mockBridgeService.symmetricEncryptionKeyData } returns null
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.SyncNotEnabled, manager.accountSyncStateFlow.value)
        verify { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { context.unbindService(any()) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does not match should set symmetric key from service`() {
        fakeSymmetricKeyStorageProvider.symmetricKey = null
        every { mockBridgeService.symmetricEncryptionKeyData } returns SYMMETRIC_KEY
        val serviceConnection = slot<ServiceConnection>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does match should not query for symmetric key`() {
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull(),
            )
        } returns true
        val serviceConnection = slot<ServiceConnection>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does not match and query for key fails state should be error`() {
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull(),
            )
        } returns false
        every {
            mockBridgeService.symmetricEncryptionKeyData
        } throws RuntimeException()
        val serviceConnection = slot<ServiceConnection>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { context.unbindService(any()) }
        verify { mockBridgeService.symmetricEncryptionKeyData }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAccountsSync should set AccountSyncState to decrypted response`() {
        val expectedAccounts = listOf<SharedAccountData.Account>(
            mockk(),
        )
        val encryptedAccounts: EncryptedSharedAccountData = mockk(relaxed = true)
        val decryptedAccounts: SharedAccountData = mockk {
            every { accounts } returns expectedAccounts
        }
        every { encryptedAccounts.decrypt(SYMMETRIC_KEY) } returns Result.success(decryptedAccounts)
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull(),
            )
        } returns true
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        val serviceConnection = slot<ServiceConnection>()
        val callback = slot<IAuthenticatorBridgeServiceCallback>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every { mockBridgeService.registerBridgeServiceCallback(capture(callback)) } just runs
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Success(expectedAccounts), manager.accountSyncStateFlow.value)

        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    fun `onAccountsSync when symmetric key is missing should not set state to Success`() {
        val encryptedAccounts: EncryptedSharedAccountData = mockk()
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull(),
            )
        } returns true
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        val serviceConnection = slot<ServiceConnection>()
        val callback = slot<IAuthenticatorBridgeServiceCallback>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every { mockBridgeService.registerBridgeServiceCallback(capture(callback)) } just runs
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        fakeSymmetricKeyStorageProvider.symmetricKey = null
        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)

        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAccountsSync when decryption fails state should be error`() {
        val encryptedAccounts: EncryptedSharedAccountData = mockk()
        every { encryptedAccounts.decrypt(SYMMETRIC_KEY) } returns Result.failure(RuntimeException())
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull(),
            )
        } returns true
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        val serviceConnection = slot<ServiceConnection>()
        val callback = slot<IAuthenticatorBridgeServiceCallback>()
        val mockIntent: Intent = mockk()
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every { mockBridgeService.registerBridgeServiceCallback(capture(callback)) } just runs
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)

        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    fun `onStop when service has been started should unbind service`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        } returns true
        every { context.unbindService(any()) } just runs

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        fakeLifecycleOwner.lifecycle.dispatchOnStop()

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        verify {
            context.bindService(
                any(),
                any(),
                Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS,
            )
        }
        verify { context.unbindService(any()) }
    }

    /**
     * Tests to verify behavior between API levels 31 and 33.
     */
    @Nested
    inner class PostApi31AuthenticatorBridgeManagerTest {
        @BeforeEach
        fun setup() {
            every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns true
            every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns false
        }

        @Test
        fun `correct flags should be passed to bindService when API level is between 31 and 33`() {
            val mockIntent: Intent = mockk()
            every {
                anyConstructed<Intent>().setComponent(any())
            } returns mockIntent

            every { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) } returns true

            fakeLifecycleOwner.lifecycle.dispatchOnStart()

            verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        }
    }

    /**
     * Tests to verify behavior between API levels below 31.
     */
    @Nested
    inner class PreApi31AuthenticatorBridgeManagerTest {
        @BeforeEach
        fun setup() {
            every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns false
            every { isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) } returns false
        }

        @Test
        fun `initial AccountSyncState should be OsVersionNotSupported when OS level is below S`() {
            val manager = AuthenticatorBridgeManagerImpl(
                context = context,
                connectionType = AuthenticatorBridgeConnectionType.DEV,
                symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
                passwordManagerSignatureVerifier = mockPasswordManagerSignatureVerifier,
                callbackProvider = testAuthenticatorBridgeCallbackProvider,
                processLifecycleOwner = fakeLifecycleOwner,
            )
            assertEquals(AccountSyncState.OsVersionNotSupported, manager.accountSyncStateFlow.value)
        }

        @Test
        fun `onStart when OS level is below S should set state to OsVersionNotSupported`() {
            every { isBuildVersionAtLeast(Build.VERSION_CODES.S) } returns false
            fakeLifecycleOwner.lifecycle.dispatchOnStart()
            assertEquals(AccountSyncState.OsVersionNotSupported, manager.accountSyncStateFlow.value)
        }
    }
}

private val SYMMETRIC_KEY = generateSecretKey()
    .getOrThrow()
    .encoded
    .toSymmetricEncryptionKeyData()
