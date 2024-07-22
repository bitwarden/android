package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.crypto.HashPurpose
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.manager.TrustedDeviceManager
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.manager.util.FakeAppForegroundManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultStateEvent
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import io.mockk.awaits
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class VaultLockManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeAppForegroundManager = FakeAppForegroundManager()
    private val authSdkSource: AuthSdkSource = mockk {
        coEvery {
            hashPassword(
                email = MOCK_PROFILE.email,
                password = "drowssap",
                kdf = MOCK_PROFILE.toSdkParams(),
                purpose = HashPurpose.LOCAL_AUTHORIZATION,
            )
        } returns "hashedPassword".asSuccess()
    }
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val userLogoutManager: UserLogoutManager = mockk {
        every { logout(any()) } just runs
        every { softLogout(any()) } just runs
    }
    private val trustedDeviceManager: TrustedDeviceManager = mockk()
    private val mutableVaultTimeoutStateFlow =
        MutableStateFlow<VaultTimeout>(VaultTimeout.ThirtyMinutes)
    private val mutableVaultTimeoutActionStateFlow = MutableStateFlow(VaultTimeoutAction.LOCK)
    private val settingsRepository: SettingsRepository = mockk {
        every { getVaultTimeoutStateFlow(any()) } returns mutableVaultTimeoutStateFlow
        every { getVaultTimeoutActionStateFlow(any()) } returns mutableVaultTimeoutActionStateFlow
    }

    private var elapsedRealtimeMillis = 123456789L

    private val vaultLockManager: VaultLockManager = VaultLockManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        authSdkSource = authSdkSource,
        vaultSdkSource = vaultSdkSource,
        settingsRepository = settingsRepository,
        appForegroundManager = fakeAppForegroundManager,
        userLogoutManager = userLogoutManager,
        trustedDeviceManager = trustedDeviceManager,
        dispatcherManager = FakeDispatcherManager(),
        elapsedRealtimeMillisProvider = { elapsedRealtimeMillis },
    )

    @Test
    fun `vaultStateEventFlow should emit Locked event when vault state changes to locked`() =
        runTest {
            // Ensure the vault is unlocked
            verifyUnlockedVault(userId = USER_ID)

            vaultLockManager.vaultStateEventFlow.test {
                vaultLockManager.lockVault(userId = USER_ID)
                assertEquals(VaultStateEvent.Locked(userId = USER_ID), awaitItem())
            }
        }

    @Test
    fun `vaultStateEventFlow should not emit Locked event when vault state remains locked`() =
        runTest {
            // Ensure the vault is locked
            vaultLockManager.lockVault(userId = USER_ID)

            vaultLockManager.vaultStateEventFlow.test {
                vaultLockManager.lockVault(userId = USER_ID)
                expectNoEvents()
            }
        }

    @Test
    fun `vaultStateEventFlow should emit Unlocked event when vault state changes to unlocked`() =
        runTest {
            // Ensure the vault is locked
            vaultLockManager.lockVault(userId = USER_ID)

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
    fun `app going into background should update the current user's last active time`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start in a foregrounded state
        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )

        elapsedRealtimeMillis = 123L
        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED

        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for Never timeout should clear existing times and not perform timeout action`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for OnAppRestart timeout should clear existing times and lock vaults if necessary`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.OnAppRestart

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for other timeout should clear existing times and lock vaults if necessary`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for non-Never timeout should clear existing times and perform timeout action`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Verify Checking for timeout should take place for a user with logged in state`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 1) { settingsRepository.getVaultTimeoutActionStateFlow(USER_ID) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Verify Checking for timeout should not take place for a user who is already in the soft logged out state`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = USER_ID)
        assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        verify(exactly = 0) { settingsRepository.getVaultTimeoutActionStateFlow(USER_ID) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground subsequent times should perform timeout action if necessary and not clear existing times`() {
        setAccountTokens()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start in a foregrounded state
        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = USER_ID,
            lastActiveTimeMillis = null,
        )

        // Set the last active time to 2 minutes and the current time to 8 minutes, so only times
        // beyond 6 minutes perform their action.
        val lastActiveTime = 2 * 60 * 1000L
        elapsedRealtimeMillis = 8 * 60 * 1000L

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            clearVerifications(userLogoutManager)
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
            fakeAuthDiskSource.storeLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = lastActiveTime,
            )
            verifyUnlockedVaultBlocking(userId = USER_ID)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

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

            verify(exactly = 0) { userLogoutManager.softLogout(any()) }
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = lastActiveTime,
            )
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

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
                    verify(exactly = 0) { userLogoutManager.softLogout(any()) }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
                    verify(exactly = 1) { userLogoutManager.softLogout(USER_ID) }
                }
            }

            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = lastActiveTime,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switching users should perform lock actions for each user if necessary and reset their last active times`() {
        val userId2 = "mockId-2"
        setAccountTokens(listOf(USER_ID, userId2))
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = USER_ID,
            accounts = mapOf(
                USER_ID to MOCK_ACCOUNT,
                userId2 to MOCK_ACCOUNT.copy(profile = MOCK_PROFILE.copy(userId = userId2)),
            ),
        )

        // Set the last active time to 2 minutes and the current time to 8 minutes, so only times
        // beyond 6 minutes perform their action.
        val lastActiveTime = 2 * 60 * 1000L
        elapsedRealtimeMillis = 8 * 60 * 1000L

        // Will be used within each loop to reset the test to a suitable initial state.
        fun resetTest(vaultTimeout: VaultTimeout) {
            clearVerifications(userLogoutManager)
            mutableVaultTimeoutStateFlow.value = vaultTimeout
            fakeAuthDiskSource.storeLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = lastActiveTime,
            )
            fakeAuthDiskSource.storeLastActiveTimeMillis(
                userId = userId2,
                lastActiveTimeMillis = lastActiveTime,
            )
            verifyUnlockedVaultBlocking(userId = USER_ID)
            verifyUnlockedVaultBlocking(userId = userId2)
            assertTrue(vaultLockManager.isVaultUnlocked(USER_ID))
            assertTrue(vaultLockManager.isVaultUnlocked(userId2))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAuthDiskSource.userState = fakeAuthDiskSource.userState?.copy(
                activeUserId = if (fakeAuthDiskSource.userState?.activeUserId == USER_ID) {
                    userId2
                } else {
                    USER_ID
                },
            )

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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId2))
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
                    assertFalse(vaultLockManager.isVaultUnlocked(userId2))
                }
            }

            verify(exactly = 0) { userLogoutManager.softLogout(any()) }
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = elapsedRealtimeMillis,
            )
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId2,
                lastActiveTimeMillis = elapsedRealtimeMillis,
            )
        }

        // Test Logout action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOGOUT
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAuthDiskSource.userState = fakeAuthDiskSource.userState?.copy(
                activeUserId = if (fakeAuthDiskSource.userState?.activeUserId == USER_ID) {
                    userId2
                } else {
                    USER_ID
                },
            )

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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId2))
                    verify(exactly = 0) { userLogoutManager.softLogout(any()) }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(USER_ID))
                    assertFalse(vaultLockManager.isVaultUnlocked(userId2))
                    verify(exactly = 1) { userLogoutManager.softLogout(USER_ID) }
                    verify(exactly = 1) { userLogoutManager.softLogout(userId2) }
                }
            }

            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = USER_ID,
                lastActiveTimeMillis = elapsedRealtimeMillis,
            )
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId2,
                lastActiveTimeMillis = elapsedRealtimeMillis,
            )
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
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    privateKey = privateKey,
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
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    privateKey = privateKey,
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

            val unlockingJob = async {
                verifyUnlockingVault(userId = USER_ID)
            }
            this.testScheduler.advanceUntilIdle()

            assertTrue(vaultLockManager.isVaultUnlocking(userId = USER_ID))

            unlockingJob.cancel()
            this.testScheduler.advanceUntilIdle()

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

            vaultLockManager.lockVault(userId = USER_ID)

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

            vaultLockManager.lockVault(userId = USER_ID)

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

            vaultLockManager.lockVaultForCurrentUser()

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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
                trustedDeviceManager.trustThisDeviceIfNecessary(userId = USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto success for a Never VaultTimeout should return Success, save the auto-unlock key, and clear invalid unlock attempts`() =
        runTest {
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val userAutoUnlockKey = "userAutoUnlockKey"
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError, result)
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.AuthenticationError, result)
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError, result)
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 1,
            )

            val result = vaultLockManager.unlockVault(
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            assertEquals(VaultUnlockResult.GenericError, result)
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
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            coEvery {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_ID,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            fakeAuthDiskSource.storeInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 4,
            )

            val result = vaultLockManager.unlockVault(
                userId = USER_ID,
                kdf = kdf,
                email = email,
                initUserCryptoMethod = InitUserCryptoMethod.Password(
                    password = masterPassword,
                    userKey = userKey,
                ),
                privateKey = privateKey,
                organizationKeys = organizationKeys,
            )

            fakeAuthDiskSource.assertInvalidUnlockAttempts(
                userId = USER_ID,
                invalidUnlockAttempts = 5,
            )
            verify { userLogoutManager.logout(userId = USER_ID) }

            assertEquals(VaultUnlockResult.GenericError, result)
            assertEquals(
                emptyList<VaultUnlockData>(),
                vaultLockManager.vaultUnlockDataStateFlow.value,
            )
            coVerify(exactly = 1) {
                vaultSdkSource.initializeCrypto(
                    userId = USER_ID,
                    request = InitUserCryptoRequest(
                        kdfParams = kdf,
                        email = email,
                        privateKey = privateKey,
                        method = InitUserCryptoMethod.Password(
                            password = masterPassword,
                            userKey = userKey,
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
     * Note that this call will actively hang.
     */
    private suspend fun verifyUnlockingVault(userId: String) {
        val kdf = MOCK_PROFILE.toSdkParams()
        val email = MOCK_PROFILE.email
        val masterPassword = "drowssap"
        val userKey = "12345"
        val privateKey = "54321"
        val organizationKeys = null
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
                    ),
                ),
            )
        } just awaits

        vaultLockManager.unlockVault(
            userId = userId,
            kdf = kdf,
            email = email,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
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
        val masterPassword = "drowssap"
        val userKey = "12345"
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
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
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
            userId = userId,
            kdf = kdf,
            email = email,
            privateKey = privateKey,
            initUserCryptoMethod = InitUserCryptoMethod.Password(
                password = masterPassword,
                userKey = userKey,
            ),
            organizationKeys = organizationKeys,
        )

        assertEquals(VaultUnlockResult.Success, result)
        coVerify(exactly = 1) {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = kdf,
                    email = email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.Password(
                        password = masterPassword,
                        userKey = userKey,
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
                userId,
                accountTokens = AccountTokensJson("access-$userId", "refresh-$userId"),
            )
        }
    }
}

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
