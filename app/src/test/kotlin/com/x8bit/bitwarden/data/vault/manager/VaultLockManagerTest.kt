package com.x8bit.bitwarden.data.vault.manager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.HashPurpose
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.KdfManager
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.manager.model.LogoutEvent
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.UpdateKdfMinimumsResult
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.manager.model.AppCreationState
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.manager.util.FakeAppStateManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class VaultLockManagerTest {
    private val broadcastReceiver = slot<BroadcastReceiver>()
    private val context: Context = mockk {
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        every { registerReceiver(capture(broadcastReceiver), any()) } returns null
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeAppStateManager = FakeAppStateManager()
    private val authSdkSource: AuthSdkSource = mockk {
        coEvery {
            hashPassword(
                email = MOCK_PROFILE.email,
                password = "mockValue",
                kdf = MOCK_PROFILE.toSdkParams(),
                purpose = HashPurpose.LOCAL_AUTHORIZATION,
            )
        } returns "hashedPassword".asSuccess()
    }
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }

    private val mutableLogoutResultFlow = MutableSharedFlow<LogoutEvent>()
    private val userLogoutManager: UserLogoutManager = mockk {
        every { logout(userId = any(), reason = any()) } just runs
        every { softLogout(userId = any(), reason = any()) } just runs
        every { logoutEventFlow } returns mutableLogoutResultFlow.asSharedFlow()
    }
    private val trustedDeviceManager: TrustedDeviceManager = mockk()
    private val mutableVaultTimeoutStateFlow =
        MutableStateFlow<VaultTimeout>(VaultTimeout.ThirtyMinutes)
    private val mutableVaultTimeoutActionStateFlow = MutableStateFlow(VaultTimeoutAction.LOCK)
    private val settingsRepository: SettingsRepository = mockk {
        every { getVaultTimeoutStateFlow(any()) } returns mutableVaultTimeoutStateFlow
        every { getVaultTimeoutActionStateFlow(any()) } returns mutableVaultTimeoutActionStateFlow
    }
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDispatcherManager = FakeDispatcherManager(unconfined = testDispatcher)
    private val realtimeManager: RealtimeManager = mockk {
        every { elapsedRealtimeMs } returns FIXED_CLOCK.millis()
    }
    private val kdfManager: KdfManager = mockk {
        every { needsKdfUpdateToMinimums() } returns false
        coEvery {
            updateKdfToMinimumsIfNeeded(password = any())
        } returns UpdateKdfMinimumsResult.Success
    }
    private val pinProtectedUserKeyManager: PinProtectedUserKeyManager = mockk {
        coEvery { migratePinProtectedUserKeyIfNeeded(userId = any()) } just runs
    }

    private val vaultLockManager: VaultLockManager = VaultLockManagerImpl(
        context = context,
        clock = FIXED_CLOCK,
        realtimeManager = realtimeManager,
        authDiskSource = fakeAuthDiskSource,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        settingsRepository = settingsRepository,
        appStateManager = fakeAppStateManager,
        userLogoutManager = userLogoutManager,
        trustedDeviceManager = trustedDeviceManager,
        dispatcherManager = fakeDispatcherManager,
        kdfManager = kdfManager,
        pinProtectedUserKeyManager = pinProtectedUserKeyManager,
    )

    @Test
    fun `broadcast receiver should be registered on initialization`() {
        verify(exactly = 1) {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(any(), any())
        }
    }

    @Test
    fun `broadcast intent should reset active job`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Setup state as unlocked
        mutableVaultTimeoutStateFlow.value = VaultTimeout.OneMinute
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        // Background the app
        fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED

        // Advance by 30 seconds (half of what is required to lock the app)
        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 30 * 1000L)

        // Still unlocked
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        // Receive the screen on event
        broadcastReceiver.captured.onReceive(context, Intent())

        // Still unlocked
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        // Because the test clock is fixed, this should mean that we need to advance the clock a
        // full minute to get the vault to lock.
        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 30 * 1000L)

        // Still unlocked
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 31 * 1000L)

        // Finally locked
        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
    }

    @Test
    fun `vaultStateEventFlow should emit Locked event when vault state changes to locked`() =
        runTest {
            // Ensure the vault is unlocked
            verifyUnlockedVault(userId = USER_ID)

            vaultLockManager.vaultStateEventFlow.test {
                vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)
                assertEquals(VaultStateEvent.Locked(userId = USER_ID), awaitItem())
                fakeAuthDiskSource.assertLastLockTimestamp(
                    userId = USER_ID,
                    expectedValue = FIXED_CLOCK.instant(),
                )
            }
        }

    @Test
    fun `vaultStateEventFlow should not emit Locked event when vault state remains locked`() =
        runTest {
            // Ensure the vault is locked
            vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)

            vaultLockManager.vaultStateEventFlow.test {
                vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)
                expectNoEvents()
            }
        }

    @Test
    fun `vaultStateEventFlow should emit Unlocked event when vault state changes to unlocked`() =
        runTest {
            // Ensure the vault is locked
            vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)

            vaultLockManager.vaultStateEventFlow.test {
                verifyUnlockedVault(userId = USER_ID)
                assertEquals(VaultStateEvent.Unlocked(userId = USER_ID), awaitItem())
            }
        }

    @Test
    fun `vaultStateEventFlow should not emit Unlocked event when vault state remains unlocked`() =
        runTest {
            // Ensure the vault is unlocked
            verifyUnlockedVault(userId = USER_ID)

            vaultLockManager.vaultStateEventFlow.test {
                // There is no great way to directly call the internal setVaultToUnlocked
                // but that will be called internally again when syncing.
                vaultLockManager.syncVaultState(userId = USER_ID)
                expectNoEvents()
            }
        }

    @Test
    fun `isActiveUserUnlockingFlow should emit according to the current lock state`() = runTest {
        // Ensure the vault is unlocked
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        vaultLockManager.isActiveUserUnlockingFlow.test {
            assertFalse(awaitItem())
            verifyUnlockedVault(userId = USER_ID)
            assertTrue(awaitItem())
            assertFalse(awaitItem())
            vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)
            expectNoEvents()
        }
    }

    @Test
    fun `app coming into background subsequent times should perform timeout action if necessary`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start in a foregrounded state.
        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            clearVerifications(userLogoutManager)
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED
            verifyUnlockedVaultBlocking(userId = USER_ID)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            when (vaultTimeout) {
                // After 6 minutes (or action should not be performed)
                VaultTimeout.Never,
                VaultTimeout.OnAppRestart,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.ThirtyMinutes,
                VaultTimeout.OneHour,
                VaultTimeout.FourHours,
                is VaultTimeout.Custom,
                    -> {
                    assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                    -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
                }
            }

            verify(exactly = 0) {
                userLogoutManager.softLogout(userId = any(), reason = any())
            }
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)
            fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            when (vaultTimeout) {
                // After 6 minutes (or action should not be performed)
                VaultTimeout.Never,
                VaultTimeout.OnAppRestart,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.ThirtyMinutes,
                VaultTimeout.OneHour,
                VaultTimeout.FourHours,
                is VaultTimeout.Custom,
                    -> {
                    verify(exactly = 0) {
                        userLogoutManager.softLogout(userId = any(), reason = any())
                    }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                    -> {
                    verify(exactly = 1) {
                        userLogoutManager.softLogout(
                            userId = USER_ID,
                            reason = LogoutReason.Timeout,
                        )
                    }
                }
            }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app being created for the first time for Never timeout should not perform timeout action`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAppStateManager.appCreationState = AppCreationState.Destroyed
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = true)

        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app being created for the first time for OnAppRestart timeout should lock vaults if necessary`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.OnAppRestart

        fakeAppStateManager.appCreationState = AppCreationState.Destroyed
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = false)

        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app being created for the first time for other timeouts should check timeout action `() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppStateManager.appCreationState = AppCreationState.Destroyed
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = true)

        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
    }

    @Test
    fun `Verify Checking for timeout should take place for a user with logged in state`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppStateManager.appCreationState = AppCreationState.Destroyed
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = false)

        verify(exactly = 1) { settingsRepository.getVaultTimeoutActionStateFlow(USER_ID) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Verify Checking for timeout should not take place for a user who is already in the soft logged out state`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 0) { settingsRepository.getVaultTimeoutActionStateFlow(USER_ID) }
    }

    @Test
    fun `app being created subsequent times should do nothing except for OnAppRestart`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // We want to skip the first time since that is different from subsequent creations
        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = false)

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            fakeAppStateManager.appCreationState = AppCreationState.Destroyed
            clearVerifications(userLogoutManager)
            verifyUnlockedVaultBlocking(userId = USER_ID)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppStateManager.appCreationState =
                AppCreationState.Created(isAutoFill = false)
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            when (vaultTimeout) {
                VaultTimeout.OnAppRestart -> assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
                is VaultTimeout.Custom,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.FiveMinutes,
                VaultTimeout.FourHours,
                VaultTimeout.Immediately,
                VaultTimeout.Never,
                VaultTimeout.OneHour,
                VaultTimeout.OneMinute,
                VaultTimeout.ThirtyMinutes,
                    -> {
                    assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
                }
            }
            verify(exactly = 0) { userLogoutManager.softLogout(userId = any(), reason = any()) }
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppStateManager.appCreationState =
                AppCreationState.Created(isAutoFill = false)
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
            when (vaultTimeout) {
                VaultTimeout.OnAppRestart -> {
                    verify(exactly = 1) {
                        userLogoutManager.softLogout(userId = any(), reason = LogoutReason.Timeout)
                    }
                }

                is VaultTimeout.Custom,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.FiveMinutes,
                VaultTimeout.FourHours,
                VaultTimeout.Immediately,
                VaultTimeout.Never,
                VaultTimeout.OneHour,
                VaultTimeout.OneMinute,
                VaultTimeout.ThirtyMinutes,
                    -> {
                    verify(exactly = 0) {
                        userLogoutManager.softLogout(userId = any(), reason = any())
                    }
                }
            }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app being created subsequent times for AutoFill should do nothing regardless of timeout`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // We want to skip the first time since that is different from subsequent foregrounds
        fakeAppStateManager.appCreationState = AppCreationState.Created(isAutoFill = false)

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            fakeAppStateManager.appCreationState = AppCreationState.Destroyed
            clearVerifications(userLogoutManager)
            verifyUnlockedVaultBlocking(userId = USER_ID)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppStateManager.appCreationState =
                AppCreationState.Created(isAutoFill = true)
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
            verify(exactly = 0) { userLogoutManager.softLogout(userId = any(), reason = any()) }
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppStateManager.appCreationState =
                AppCreationState.Created(isAutoFill = true)
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
            verify(exactly = 0) { userLogoutManager.softLogout(userId = any(), reason = any()) }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switching users should perform lock actions or start a timer for each user if necessary`() {
        val userId2 = "mockId-2"
        setAccountTokens(listOf(USER_ID, userId2))
        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID,
            accounts = mapOf(
                USER_ID to MOCK_ACCOUNT,
                userId2 to MOCK_ACCOUNT.copy(profile = MOCK_PROFILE.copy(userId = userId2)),
            ),
        )

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            clearVerifications(userLogoutManager)
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            verifyUnlockedVaultBlocking(userId = USER_ID)
            verifyUnlockedVaultBlocking(userId = userId2)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
            assertTrue(vaultLockManager.isVaultUnlocked(userId2))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            val activeUserCheck = fakeAuthDiskSource.userState?.activeUserId == USER_ID
            val activeUserId = if (activeUserCheck) userId2 else USER_ID
            val inactiveUserId = if (activeUserCheck) USER_ID else userId2
            fakeAuthDiskSource.userState = fakeAuthDiskSource.userState?.copy(
                activeUserId = activeUserId,
            )
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            when (vaultTimeout) {
                // After 6 minutes (or action should not be performed)
                VaultTimeout.Never,
                VaultTimeout.OnAppRestart,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.ThirtyMinutes,
                VaultTimeout.OneHour,
                VaultTimeout.FourHours,
                is VaultTimeout.Custom,
                    -> {
                    assertTrue(vaultLockManager.isVaultUnlocked(activeUserId))
                    assertTrue(vaultLockManager.isVaultUnlocked(inactiveUserId))
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                    -> {
                    assertTrue(vaultLockManager.isVaultUnlocked(activeUserId))
                    assertFalse(vaultLockManager.isVaultUnlocked(inactiveUserId))
                }
            }

            verify(exactly = 0) { userLogoutManager.softLogout(userId = any(), reason = any()) }
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            val activeUserCheck = fakeAuthDiskSource.userState?.activeUserId == USER_ID
            val activeUserId = if (activeUserCheck) userId2 else USER_ID
            val inactiveUserId = if (activeUserCheck) USER_ID else userId2
            fakeAuthDiskSource.userState = fakeAuthDiskSource.userState?.copy(
                activeUserId = activeUserId,
            )
            // Advance by 6 minutes. Only actions with a timeout less than this will be triggered.
            testDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 6 * 60 * 1000L)

            when (vaultTimeout) {
                // After 6 minutes (or action should not be performed)
                VaultTimeout.Never,
                VaultTimeout.OnAppRestart,
                VaultTimeout.FifteenMinutes,
                VaultTimeout.ThirtyMinutes,
                VaultTimeout.OneHour,
                VaultTimeout.FourHours,
                is VaultTimeout.Custom,
                    -> {
                    verify(exactly = 0) {
                        userLogoutManager.softLogout(userId = any(), reason = any())
                    }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                    -> {
                    verify(exactly = 0) {
                        userLogoutManager.softLogout(userId = activeUserId, reason = any())
                    }
                    verify(exactly = 1) {
                        userLogoutManager.softLogout(userId = inactiveUserId, reason = any())
                    }
                }
            }
        }
    }

    @Test
    fun `vaultTimeout updates to non-Never should clear the user's auto-unlock key`() = runTest {
        val userAutoUnlockKey = "userAutoUnlockKey"

        // Initialize Never state
        coEvery {
            vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
        } returns userAutoUnlockKey.asSuccess()
        verifyUnlockedVault(userId = USER_ID)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = USER_ID,
            userAutoUnlockKey = userAutoUnlockKey,
        )

        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = USER_ID,
            userAutoUnlockKey = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for an unlocked account should store the user's encrypted key`() =
        runTest {
            val userAutoUnlockKey = "userAutoUnlockKey"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns userAutoUnlockKey.asSuccess()

            verifyUnlockedVault(userId = USER_ID)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = null,
            )

            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored private key should do nothing`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored auto-unlock key should do nothing`() {
        val privateKey = "privateKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = USER_ID,
                privateKey = privateKey,
            )
        }
        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is a stored auto-unlock key should unlock the vault`() {
        val privateKey = "privateKey"
        val userAutoUnlockKey = "userAutoUnlockKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = USER_ID,
                privateKey = privateKey,
            )
            storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = USER_ID,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID,
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = userAutoUnlockKey,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        coEvery {
            trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
        } returns true.asSuccess()

        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertTrue(vaultLockManager.isVaultUnlocked(userId = USER_ID))

        coVerify {
            vaultSdkSource.initializeCrypto(
                userId = USER_ID,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_ID,
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = userAutoUnlockKey,
                    ),
                ),
            )
            trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
        }
    }

    @Test
    fun `isVaultUnlocked should return the correct value based on the vault lock state`() =
        runTest {
            assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

            verifyUnlockedVault(userId = USER_ID)

            assertTrue(vaultLockManager.isVaultUnlocked(userId = USER_ID))
        }

    @Test
    fun `isVaultLocking should return the correct value based on the vault unlocking state`() =
        runTest {
            assertFalse(vaultLockManager.isVaultUnlocking(userId = USER_ID))

            // The async call will hang for 500ms
            async { verifyUnlockingVault(userId = USER_ID) }

            // We fast-forward 300ms, enough that the vault should be unlocking
            this.testScheduler.advanceTimeBy(delayTimeMillis = 300L)
            assertTrue(vaultLockManager.isVaultUnlocking(userId = USER_ID))

            // We fast-forward another 300ms, enough that the vault should be done unlocking
            this.testScheduler.advanceTimeBy(delayTimeMillis = 300L)
            assertFalse(vaultLockManager.isVaultUnlocking(userId = USER_ID))
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVault when non-Never timeout should lock the given account if it is currently unlocked`() =
        runTest {
            verifyUnlockedVault(userId = USER_ID)
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = USER_ID) }
        }

    @Test
    fun `lockVault when Never timeout should lock the given account if it is currently unlocked`() =
        runTest {
            verifyUnlockedVault(userId = USER_ID)
            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            vaultLockManager.lockVault(userId = USER_ID, isUserInitiated = false)

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = USER_ID) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVaultForCurrentUser should lock the vault for the current user if it is currently unlocked`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            verifyUnlockedVault(userId = USER_ID)

            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            vaultLockManager.lockVaultForCurrentUser(isUserInitiated = true)

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = USER_ID) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto success for a non-Never VaultTimeout should return Success`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            } returns false.asSuccess()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = null,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.Success, result)
            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = null,
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID,
                passwordHash = "hashedPassword",
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
                kdfManager.updateKdfToMinimumsIfNeeded(masterPassword)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto success for a Never VaultTimeout should return Success, save the auto-unlock key, and clear invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val userAutoUnlockKey = "userAutoUnlockKey"
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns userAutoUnlockKey.asSuccess()
            coEvery {
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            } returns true.asSuccess()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never
            fakeAuthDiskSource.apply {
                storeUserAutoUnlockKey(
                    userId = USER_ID,
                    userAutoUnlockKey = null,
                )
                storeInvalidUnlockAttempts(
                    userId = USER_ID,
                    invalidUnlockAttempts = 4,
                )
            }

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.Success, result)
            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            fakeAuthDiskSource.apply {
                assertUserAutoUnlockKey(
                    userId = USER_ID,
                    userAutoUnlockKey = userAutoUnlockKey,
                )
                assertInvalidUnlockAttempts(
                    userId = USER_ID,
                    invalidUnlockAttempts = null,
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for users should return AuthenticationError and increment invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError(error = error).asSuccess()

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError(error = error), result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 2,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for orgs should return AuthenticationError and increment invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.AuthenticationError(error = error).asSuccess()

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError(error = error), result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 2,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto failure for users should return GenericError and increment invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns error.asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError(error = error), result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 2,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto failure for orgs should return GenericError and increment invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns error.asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError(error = error), result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 2,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault error when reaching the maximum number of invalid unlock attempts should log out the user`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns error.asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 4,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 5,
            )
            verify(exactly = 1) {
                userLogoutManager.logout(
                    userId = USER_ID,
                    reason = LogoutReason.TooManyUnlockAttempts,
                )
            }

            assertEquals(VaultUnlockResult.GenericError(error = error), result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            }
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Test
    fun `waitUntilUnlocked should suspend until the user's vault has unlocked`() = runTest {
        // Begin in a locked state
        assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

        val waitUntilUnlockedJob = async {
            vaultLockManager.waitUntilUnlocked(userId = USER_ID)
        }
        this.testScheduler.advanceUntilIdle()

        // Confirm waitUntilUnlocked has not yet completed
        assertFalse(waitUntilUnlockedJob.isCompleted)

        // Unlock vault
        verifyUnlockedVault(userId = USER_ID)
        this.testScheduler.advanceUntilIdle()

        // Confirm unlock call has now completed and that the vault is unlocked
        assertTrue(waitUntilUnlockedJob.isCompleted)
        assertTrue(vaultLockManager.isVaultUnlocked(userId = USER_ID))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncVaultState with getUserEncryptionKey failure should update the users vault state to locked`() =
        runTest {
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns Throwable().asFailure()

            // Begin in a locked state
            assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

            vaultLockManager.syncVaultState(userId = USER_ID)

            // Confirm the vault is still locked
            assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))
            coVerify(exactly = 1) {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncVaultState with getUserEncryptionKey success should update the users vault state to unlocked`() =
        runTest {
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            } returns "UserEncryptionKey".asSuccess()

            // Begin in a locked state
            assertFalse(vaultLockManager.isVaultUnlocked(userId = USER_ID))

            vaultLockManager.syncVaultState(userId = USER_ID)

            // Confirm the vault is unlocked
            assertTrue(vaultLockManager.isVaultUnlocked(userId = USER_ID))
            coVerify(exactly = 1) {
                vaultSdkSource.getUserEncryptionKey(userId = USER_ID)
            }
        }

    @Test
    fun `When new LogoutResult is observed set the vault to locked for that user`() = runTest {
        verifyUnlockedVault(USER_ID)

        vaultLockManager.vaultStateEventFlow.test {
            mutableLogoutResultFlow.emit(LogoutEvent(loggedOutUserId = USER_ID))
            assertEquals(VaultStateEvent.Locked(userId = USER_ID), awaitItem())
            assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
        }
        verify(exactly = 1) {
            vaultSdkSource.clearCrypto(USER_ID)
        }
    }

    @Test
    fun `unlockVault with initializeCrypto success should migrate pinProtectedUserKey`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            } returns false.asSuccess()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                    password = masterPassword,
                    masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                ),
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.Success, result)
            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = InitUserCryptoMethod.MasterPasswordUnlock(
                            password = masterPassword,
                            masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                        ),
                    ),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
                pinProtectedUserKeyManager.migratePinProtectedUserKeyIfNeeded(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initUserCryptoMethod masterPasswordUnlock success should hash and store master password`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "mockValue"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                password = masterPassword,
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            )
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = initUserCryptoMethod,
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            } returns false.asSuccess()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = null,
            )

            val result = vaultLockManager.unlockVault(
                accountCryptographicState = createWrappedAccountCryptographicState(
                    privateKey = privateKey,
                    securityState = null,
                    signedPublicKey = null,
                    signingKey = null,
                ),
                userId = USER_ID,
                email = email,
                kdf = kdf,
                initUserCryptoMethod = initUserCryptoMethod,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.Success, result)
            assertEquals(
                listOf(
                    VaultUnlockData(
                        userId = USER_ID,
                        status = VaultUnlockData.Status.UNLOCKED,
                    ),
                ),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = USER_ID,
                userAutoUnlockKey = null,
            )
            fakeAuthDiskSource.assertMasterPasswordHash(
                userId = USER_ID,
                passwordHash = "hashedPassword",
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = privateKey,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_ID,
                        kdfParams = kdf,
                        email = email,
                        method = initUserCryptoMethod,
                    ),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
                kdfManager.updateKdfToMinimumsIfNeeded(password = masterPassword)
            }
        }

    /**
     * Resets the verification call count for the given [mock] while leaving all other mocked
     * behavior in place.
     */
    private fun clearVerifications(mock: Any) {
        clearMocks(
            firstMock = mock,
            recordedCalls = true,
            answers = false,
            childMocks = false,
            verificationMarks = false,
            exclusionRules = false,
        )
    }

    /**
     * Helper to ensures that the vault for the user with the given [userId] is actively unlocking.
     * Note that this call will delay for 500 ms.
     */
    private suspend fun verifyUnlockingVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val userKey = "12345"
        val masterPassword = "mockValue"
        val privateKey = "54321"
        val organizationKeys = null
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    kdfParams = kdf,
                    email = email,
                    method = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = masterPassword,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                    ),
                ),
            )
        } coAnswers {
            delay(timeMillis = 500L)
            InitializeCryptoResult.AuthenticationError(error = Throwable()).asSuccess()
        }

        vaultLockManager.unlockVault(
            accountCryptographicState = createWrappedAccountCryptographicState(
                privateKey = privateKey,
                securityState = null,
                signedPublicKey = null,
                signingKey = null,
            ),
            userId = userId,
            email = email,
            kdf = kdf,
            initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                password = masterPassword,
                masterPasswordUnlock = MasterPasswordUnlockData(
                    kdf = kdf,
                    masterKeyWrappedUserKey = userKey,
                    salt = "mockSalt",
                ),
            ),
            organizationKeys = organizationKeys,
        )
    }

    /**
     * Helper to ensures that the vault for the user with the given [userId] is unlocked.
     */
    private suspend fun verifyUnlockedVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "mockValue"
        val privateKey = "54321"
        val organizationKeys = null
        val userAutoUnlockKey = "userAutoUnlockKey"
        // Clear recorded calls so this helper can be called multiple times and assert a unique
        // unlock has happened each time.
        clearVerifications(vaultSdkSource)
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    kdfParams = kdf,
                    email = email,
                    method = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = masterPassword,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        coEvery {
            vaultSdkSource.getUserEncryptionKey(userId = userId)
        } returns userAutoUnlockKey.asSuccess()
        coEvery {
            trustedDeviceManager.trustThisDeviceIfNecessary(userId = userId)
        } returns true.asSuccess()

        val result = vaultLockManager.unlockVault(
            accountCryptographicState = createWrappedAccountCryptographicState(
                privateKey = privateKey,
                securityState = null,
                signedPublicKey = null,
                signingKey = null,
            ),
            userId = userId,
            email = email,
            kdf = kdf,
            initUserCryptoMethod = InitUserCryptoMethod.MasterPasswordUnlock(
                password = masterPassword,
                masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
            ),
            organizationKeys = organizationKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = userId,
                    kdfParams = kdf,
                    email = email,
                    method = InitUserCryptoMethod.MasterPasswordUnlock(
                        password = masterPassword,
                        masterPasswordUnlock = MOCK_MASTER_PASSWORD_UNLOCK_DATA,
                    ),
                ),
            )
        }
    }

    private fun verifyUnlockedVaultBlocking(userId: String) {
        runBlocking { verifyUnlockedVault(userId = userId) }
    }

    // region helper functions
    private fun setAccountTokens(userIds: List<String> = listOf(USER_ID)) {
        userIds.forEach { userId ->
            fakeAuthDiskSource.storeAccountTokens(
                userId = userId,
                accountTokens = AccountTokensJson(
                    accessToken = "access-$userId",
                    refreshToken = "refresh-$userId",
                ),
            )
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val USER_ID = "mockId-1"

private val MOCK_TIMEOUTS = VaultTimeout.Type.entries.map {
    when (it) {
        VaultTimeout.Type.IMMEDIATELY -> VaultTimeout.Immediately
        VaultTimeout.Type.ONE_MINUTE -> VaultTimeout.OneMinute
        VaultTimeout.Type.FIVE_MINUTES -> VaultTimeout.FiveMinutes
        VaultTimeout.Type.FIFTEEN_MINUTES -> VaultTimeout.FifteenMinutes
        VaultTimeout.Type.THIRTY_MINUTES -> VaultTimeout.ThirtyMinutes
        VaultTimeout.Type.ONE_HOUR -> VaultTimeout.OneHour
        VaultTimeout.Type.FOUR_HOURS -> VaultTimeout.FourHours
        VaultTimeout.Type.ON_APP_RESTART -> VaultTimeout.OnAppRestart
        VaultTimeout.Type.NEVER -> VaultTimeout.Never
        VaultTimeout.Type.CUSTOM -> VaultTimeout.Custom(vaultTimeoutInMinutes = 123)
    }
}

private val MOCK_PROFILE = AccountJson.Profile(
    userId = USER_ID,
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(
        USER_ID to MOCK_ACCOUNT,
    ),
)

private val MOCK_MASTER_PASSWORD_UNLOCK_DATA = MasterPasswordUnlockData(
    kdf = MOCK_PROFILE.toSdkParams(),
    masterKeyWrappedUserKey = "12345",
    salt = "mockSalt",
)
