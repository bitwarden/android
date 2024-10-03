package com.bitwarden.authenticatorbridge.manager

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
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
import com.bitwarden.authenticatorbridge.util.isBuildVersionBelow
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

    private val context = mockk<Context> {
        every { applicationContext } returns this
        every {
            packageManager.getPackageInfo("com.x8bit.bitwarden.dev", 0)
        } returns mockk()
    }
    private val mockBridgeService: IAuthenticatorBridgeService = mockk()
    private val fakeLifecycleOwner = FakeLifecycleOwner()
    private val fakeSymmetricKeyStorageProvider = FakeSymmetricKeyStorageProvider()
    private val testAuthenticatorBridgeCallbackProvider = TestAuthenticatorBridgeCallbackProvider()

    private lateinit var manager: AuthenticatorBridgeManagerImpl

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionBelow)
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns false
        mockkConstructor(Intent::class)
        mockkStatic(IAuthenticatorBridgeService.Stub::class)
        mockkStatic(EncryptedSharedAccountData::decrypt)
        manager = AuthenticatorBridgeManagerImpl(
            context = context,
            connectionType = AuthenticatorBridgeConnectionType.DEV,
            symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
            callbackProvider = testAuthenticatorBridgeCallbackProvider,
            processLifecycleOwner = fakeLifecycleOwner,
        )
    }

    @AfterEach
    fun teardown() {
        mockkStatic(::isBuildVersionBelow)
        unmockkConstructor(Intent::class)
        unmockkStatic(IAuthenticatorBridgeService.Stub::class)
        unmockkStatic(EncryptedSharedAccountData::decrypt)
    }

    @Test
    fun `initial AccountSyncState should be Loading when Bitwarden app is present`() {
        assertEquals(AccountSyncState.Loading, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `initial AccountSyncState should be AppNotInstalled when Bitwarden app is not present`() {
        every {
            context.packageManager.getPackageInfo("com.x8bit.bitwarden.dev", 0)
        } throws NameNotFoundException()
        val manager = AuthenticatorBridgeManagerImpl(
            context = context,
            connectionType = AuthenticatorBridgeConnectionType.DEV,
            symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
            callbackProvider = testAuthenticatorBridgeCallbackProvider,
            processLifecycleOwner = fakeLifecycleOwner,
        )
        assertEquals(AccountSyncState.AppNotInstalled, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `initial AccountSyncState should be OsVersionNotSupported when OS level is below S`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns true
        val manager = AuthenticatorBridgeManagerImpl(
            context = context,
            connectionType = AuthenticatorBridgeConnectionType.DEV,
            symmetricKeyStorageProvider = fakeSymmetricKeyStorageProvider,
            callbackProvider = testAuthenticatorBridgeCallbackProvider,
            processLifecycleOwner = fakeLifecycleOwner,
        )
        assertEquals(AccountSyncState.OsVersionNotSupported, manager.accountSyncStateFlow.value)
    }

    @Test
    fun `onStart when OS level is below S should set state to OsVersionNotSupported`() {
        every { isBuildVersionBelow(Build.VERSION_CODES.S) } returns true
        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        assertEquals(AccountSyncState.OsVersionNotSupported, manager.accountSyncStateFlow.value)
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
    fun `onStart when Bitwarden app is not present should set state to AppNotInstalled`() {
        val mockIntent: Intent = mockk()
        every {
            context.packageManager.getPackageInfo("com.x8bit.bitwarden.dev", 0)
        } throws NameNotFoundException()
        every {
            anyConstructed<Intent>().setComponent(any())
        } returns mockIntent

        every {
            context.bindService(any(), any(), Context.BIND_AUTO_CREATE)
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
            context.bindService(any(), any(), Context.BIND_AUTO_CREATE)
        } throws SecurityException()

        fakeLifecycleOwner.lifecycle.dispatchOnStart()

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    fun `onStart when Bitwarden app is present and bindService succeeds should set state to Loading before service calls back`() {
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
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does match should not query for symmetric key`() {
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
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onServiceConnected when symmetric key does not match and query for key fails state should be error`() {
        fakeSymmetricKeyStorageProvider.symmetricKey = SYMMETRIC_KEY
        every {
            mockBridgeService.checkSymmetricEncryptionKeyFingerprint(
                SYMMETRIC_KEY.toFingerprint().getOrNull()
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
                Context.BIND_AUTO_CREATE
            )
        } returns true

        fakeLifecycleOwner.lifecycle.dispatchOnStart()
        serviceConnection.captured.onServiceConnected(mockk(), mockk())

        assertEquals(AccountSyncState.Error, manager.accountSyncStateFlow.value)
        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { context.unbindService(any()) }
        verify { mockBridgeService.symmetricEncryptionKeyData }
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

        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
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

        assertEquals(SYMMETRIC_KEY, fakeSymmetricKeyStorageProvider.symmetricKey)
        verify(exactly = 0) { mockBridgeService.symmetricEncryptionKeyData }
        verify { context.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        verify { mockBridgeService.registerBridgeServiceCallback(any()) }
        verify { mockBridgeService.syncAccounts() }
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
