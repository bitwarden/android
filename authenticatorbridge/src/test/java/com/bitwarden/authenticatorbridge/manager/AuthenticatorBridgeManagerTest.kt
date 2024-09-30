package com.bitwarden.authenticatorbridge.manager

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeServiceCallback
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.util.FakeLifecycleOwner
import com.bitwarden.authenticatorbridge.util.FakeSymmetricKeyStorageProvider
import com.bitwarden.authenticatorbridge.util.TestAuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.util.decrypt
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.authenticatorbridge.util.toFingerprint
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData
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
import org.junit.jupiter.api.Test

class AuthenticatorBridgeManagerTest {

    private val context = mockk<Context>()
    private val mockBridgeService: IAuthenticatorBridgeService = mockk()
    private val fakeLifecycleOwner = FakeLifecycleOwner()
    private val fakeSymmetricKeyStorageProvider = FakeSymmetricKeyStorageProvider()
    private val testAuthenticatorBridgeCallbackProvider = TestAuthenticatorBridgeCallbackProvider()

    private val manager: AuthenticatorBridgeManagerImpl = AuthenticatorBridgeManagerImpl(
        applicationContext = context,
        connectionType = AuthenticatorBridgeConnectionType.DEV,
        symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
        callbackProvider = testAuthenticatorBridgeCallbackProvider,
        processLifecycleOwner = fakeLifecycleOwner,
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(Intent::class)
        mockkStatic(IAuthenticatorBridgeService.Stub::class)
        mockkStatic(EncryptedSharedAccountData::decrypt)
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(Intent::class)
        unmockkStatic(IAuthenticatorBridgeService.Stub::class)
        unmockkStatic(EncryptedSharedAccountData::decrypt)
    }

    @Test
    fun `initial state should be Loading`() {
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `onStart when bindService fails should set state to error`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) } returns false

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    fun `onStart when bindService throws security exception should set state to error`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(any(), any(), Context.BIND_AUTO_CREATE)
        } throws SecurityException()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    fun `onStart when bindService succeeds state should be Loading before service calls back`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key is not present and service returns null symmetric key state should be SyncNotEnabled and should unbind service`() {
        val serviceConnection = slot<ServiceConnection>()
        val mockIntent: Intent = mockk()
        fakeSymmetricKeyStorageProvider.symmetricKey = null
        every { IAuthenticatorBridgeService.Stub.asInterface(any()) } returns mockBridgeService
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent
        every { context.unbindService(any()) } just runs
        every {
            context.bindService(
                any(),
                capture(serviceConnection),
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.SyncNotEnabled, manager.accountSyncStateFlow.value)
        verify { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        assertEquals(fakeSymmetricKeyStorageProvider.symmetricKey, SYMMETRIC_KEY)
        verify { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does match not query for symmetric key`() {
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull()
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        assertEquals(fakeSymmetricKeyStorageProvider.symmetricKey, SYMMETRIC_KEY)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAccountsSync should set AccountSyncState to decrypted response`() {
        val expectedAccounts = listOf<SharedAccountData.Account>(
            mockk()
        )
        val encryptedAccounts: EncryptedSharedAccountData = mockk()
        val decryptedAccounts: SharedAccountData = mockk {
            every { accounts } returns expectedAccounts
        }
        mockkStatic(EncryptedSharedAccountData::decrypt)
        every { encryptedAccounts.decrypt(SYMMETRIC_KEY) } returns Result.success(decryptedAccounts)
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull()
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Success(expectedAccounts), manager.accountSyncStateFlow.value)

        assertEquals(fakeSymmetricKeyStorageProvider.symmetricKey, SYMMETRIC_KEY)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    fun `onAccountsSync when symmetric key is missing should not set state to Success`() {
        val encryptedAccounts: EncryptedSharedAccountData = mockk()
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull()
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        fakeSymmetricKeyStorageProvider.symmetricKey = null
        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)

        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAccountsSync when decryption fails state should be error`() {
        val encryptedAccounts: EncryptedSharedAccountData = mockk()
        mockkStatic(EncryptedSharedAccountData::decrypt)
        every { encryptedAccounts.decrypt(SYMMETRIC_KEY) } returns Result.failure(RuntimeException())
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull()
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)

        callback.captured.onAccountsSync(encryptedAccounts)
        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)

        assertEquals(fakeSymmetricKeyStorageProvider.symmetricKey, SYMMETRIC_KEY)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    fun `onStop when no service was bound should not call unBindService`() {
        fakeLifecycleOwner.lifecycle.dispatchOnStop()
        verify(exactly = 0) { context.unbindService(any()) }
    }

    @Test
    fun `onStop when service has been started should unbind service`() {
        val mockIntent: Intent = mockk()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) } returns true
        every { context.unbindService(any()) } just runs

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        fakeLifecycleOwner.lifecycle.dispatchOnStop()

        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { context.unbindService(any()) }
    }
}

private val SYMMETRIC_KEY = generateSecretKey()
    .getOrThrow()
    .encoded
    .toSymmetricEncryptionKeyData()