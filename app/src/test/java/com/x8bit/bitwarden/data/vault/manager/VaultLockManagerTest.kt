package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
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
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
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
    private val vaultSdkSource: VaultSdkSource = mockk {
        every { clearCrypto(userId = any()) } just runs
    }
    private val userLogoutManager: UserLogoutManager = mockk {
        every { logout(any()) } just runs
        every { softLogout(any()) } just runs
    }
    private val mutableVaultTimeoutStateFlow =
        MutableStateFlow<VaultTimeout>(VaultTimeout.ThirtyMinutes)
    private val mutableVaultTimeoutActionStateFlow =
        MutableStateFlow<VaultTimeoutAction>(VaultTimeoutAction.LOCK)
    private val settingsRepository: SettingsRepository = mockk {
        every { getVaultTimeoutStateFlow(any()) } returns mutableVaultTimeoutStateFlow
        every { getVaultTimeoutActionStateFlow(any()) } returns mutableVaultTimeoutActionStateFlow
    }

    private var elapsedRealtimeMillis = 123456789L

    private val vaultLockManager: VaultLockManager = VaultLockManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        vaultSdkSource = vaultSdkSource,
        settingsRepository = settingsRepository,
        appForegroundManager = fakeAppForegroundManager,
        userLogoutManager = userLogoutManager,
        dispatcherManager = FakeDispatcherManager(),
        elapsedRealtimeMillisProvider = { elapsedRealtimeMillis },
    )

    @Test
    fun `app going into background should update the current user's last active time`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start in a foregrounded state
        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = null,
        )

        elapsedRealtimeMillis = 123L
        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED

        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = 123L,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for Never timeout should clear existing times and not perform timeout action`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = userId)
        assertTrue(vaultLockManager.isVaultUnlocked(userId))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertTrue(vaultLockManager.isVaultUnlocked(userId))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for OnAppRestart timeout should clear existing times and lock vaults if necessary`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.OnAppRestart

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = userId)
        assertTrue(vaultLockManager.isVaultUnlocked(userId))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(userId))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for other timeout should clear existing times and lock vaults if necessary`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = userId)
        assertTrue(vaultLockManager.isVaultUnlocked(userId))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(userId))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground for the first time for non-Never timeout should clear existing times and perform timeout action`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAppForegroundManager.appForegroundState = AppForegroundState.BACKGROUNDED
        fakeAuthDiskSource.storeLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = 123L,
        )
        verifyUnlockedVaultBlocking(userId = userId)
        assertTrue(vaultLockManager.isVaultUnlocked(userId))

        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertFalse(vaultLockManager.isVaultUnlocked(userId))
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
            lastActiveTimeMillis = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `app coming into foreground subsequent times should perform timeout action if necessary and not clear existing times`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        // Start in a foregrounded state
        fakeAppForegroundManager.appForegroundState = AppForegroundState.FOREGROUNDED
        fakeAuthDiskSource.assertLastActiveTimeMillis(
            userId = userId,
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
                userId = userId,
                lastActiveTimeMillis = lastActiveTime,
            )
            verifyUnlockedVaultBlocking(userId = userId)
            assertTrue(vaultLockManager.isVaultUnlocked(userId))
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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId))
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(userId))
                }
            }

            verify(exactly = 0) { userLogoutManager.softLogout(any()) }
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId,
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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId))
                    verify(exactly = 0) { userLogoutManager.softLogout(any()) }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(userId))
                    verify(exactly = 1) { userLogoutManager.softLogout(userId) }
                }
            }

            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId,
                lastActiveTimeMillis = lastActiveTime,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `switching users should perform lock actions for each user if necessary and reset their last active times`() {
        val userId1 = "mockId-1"
        val userId2 = "mockId-2"
        fakeAuthDiskSource.userState = UserStateJson(
            activeUserId = userId1,
            accounts = mapOf(
                userId1 to MOCK_ACCOUNT,
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
                userId = userId1,
                lastActiveTimeMillis = lastActiveTime,
            )
            fakeAuthDiskSource.storeLastActiveTimeMillis(
                userId = userId2,
                lastActiveTimeMillis = lastActiveTime,
            )
            verifyUnlockedVaultBlocking(userId = userId1)
            verifyUnlockedVaultBlocking(userId = userId2)
            assertTrue(vaultLockManager.isVaultUnlocked(userId1))
            assertTrue(vaultLockManager.isVaultUnlocked(userId2))
        }

        // Test Lock action
        mutableVaultTimeoutActionStateFlow.value = VaultTimeoutAction.LOCK
        MOCK_TIMEOUTS.forEach { vaultTimeout ->
            resetTest(vaultTimeout = vaultTimeout)

            fakeAuthDiskSource.userState = fakeAuthDiskSource.userState?.copy(
                activeUserId = if (fakeAuthDiskSource.userState?.activeUserId == userId1) {
                    userId2
                } else {
                    userId1
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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId1))
                    assertTrue(vaultLockManager.isVaultUnlocked(userId2))
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(userId1))
                    assertFalse(vaultLockManager.isVaultUnlocked(userId2))
                }
            }

            verify(exactly = 0) { userLogoutManager.softLogout(any()) }
            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId1,
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
                activeUserId = if (fakeAuthDiskSource.userState?.activeUserId == userId1) {
                    userId2
                } else {
                    userId1
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
                    assertTrue(vaultLockManager.isVaultUnlocked(userId1))
                    assertTrue(vaultLockManager.isVaultUnlocked(userId2))
                    verify(exactly = 0) { userLogoutManager.softLogout(any()) }
                }

                // Before 6 minutes
                VaultTimeout.Immediately,
                VaultTimeout.OneMinute,
                VaultTimeout.FiveMinutes,
                -> {
                    assertFalse(vaultLockManager.isVaultUnlocked(userId1))
                    assertFalse(vaultLockManager.isVaultUnlocked(userId2))
                    verify(exactly = 1) { userLogoutManager.softLogout(userId1) }
                    verify(exactly = 1) { userLogoutManager.softLogout(userId2) }
                }
            }

            fakeAuthDiskSource.assertLastActiveTimeMillis(
                userId = userId1,
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
        val userId = "mockId-1"
        val userAutoUnlockKey = "userAutoUnlockKey"

        // Initialize Never state
        coEvery {
            vaultSdkSource.getUserEncryptionKey(userId = userId)
        } returns userAutoUnlockKey.asSuccess()
        verifyUnlockedVault(userId = userId)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = userAutoUnlockKey,
        )

        mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

        fakeAuthDiskSource.assertUserAutoUnlockKey(
            userId = userId,
            userAutoUnlockKey = null,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for an unlocked account should store the user's encrypted key`() =
        runTest {
            val userId = "mockId-1"
            val userAutoUnlockKey = "userAutoUnlockKey"
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = userId)
            } returns userAutoUnlockKey.asSuccess()

            verifyUnlockedVault(userId = userId)

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )

            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored private key should do nothing`() {
        val userId = "mockId-1"
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is no stored auto-unlock key should do nothing`() {
        val userId = "mockId-1"
        val privateKey = "privateKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
        }
        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vaultTimeout update to Never for a locked account when there is a stored auto-unlock key should unlock the vault`() {
        val userId = "mockId-1"
        val privateKey = "privateKey"
        val userAutoUnlockKey = "userAutoUnlockKey"
        fakeAuthDiskSource.apply {
            userState = MOCK_USER_STATE
            storePrivateKey(
                userId = userId,
                privateKey = privateKey,
            )
            storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
        }
        coEvery {
            vaultSdkSource.initializeCrypto(
                userId = userId,
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

        assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

        mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

        assertTrue(vaultLockManager.isVaultUnlocked(userId = userId))

        coVerify {
            vaultSdkSource.initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    kdfParams = MOCK_PROFILE.toSdkParams(),
                    email = MOCK_PROFILE.email,
                    privateKey = privateKey,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = userAutoUnlockKey,
                    ),
                ),
            )
        }
    }

    @Test
    fun `isVaultUnlocked should return the correct value based on the vault lock state`() =
        runTest {
            val userId = "userId"
            assertFalse(vaultLockManager.isVaultUnlocked(userId = userId))

            verifyUnlockedVault(userId = userId)

            assertTrue(vaultLockManager.isVaultUnlocked(userId = userId))
        }

    @Test
    fun `isVaultLocking should return the correct value based on the vault unlocking state`() =
        runTest {
            val userId = "userId"
            assertFalse(vaultLockManager.isVaultUnlocking(userId = userId))

            val unlockingJob = async {
                verifyUnlockingVault(userId = userId)
            }
            this.testScheduler.advanceUntilIdle()

            assertTrue(vaultLockManager.isVaultUnlocking(userId = userId))

            unlockingJob.cancel()
            this.testScheduler.advanceUntilIdle()

            assertFalse(vaultLockManager.isVaultUnlocking(userId = userId))
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVault when non-Never timeout should lock the given account if it is currently unlocked`() =
        runTest {
            val userId = "userId"
            verifyUnlockedVault(userId = userId)
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            vaultLockManager.lockVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = userId) }
        }

    @Test
    fun `lockVault when Never timeout should lock the given account if it is currently unlocked`() =
        runTest {
            val userId = "userId"
            verifyUnlockedVault(userId = userId)
            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            vaultLockManager.lockVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = userId) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `lockVaultForCurrentUser should lock the vault for the current user if it is currently unlocked`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            verifyUnlockedVault(userId = userId)

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            vaultLockManager.lockVaultForCurrentUser()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            verify { vaultSdkSource.clearCrypto(userId = userId) }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto success for a non-Never VaultTimeout should return Success`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.ThirtyMinutes
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto success for a Never VaultTimeout should return Success and save the auto-unlock key`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
            val userAutoUnlockKey = "userAutoUnlockKey"
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
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.Success.asSuccess()
            coEvery {
                vaultSdkSource.getUserEncryptionKey(userId = userId)
            } returns userAutoUnlockKey.asSuccess()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            mutableVaultTimeoutStateFlow.value = VaultTimeout.Never
            fakeAuthDiskSource.storeUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = null,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = setOf(userId),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            fakeAuthDiskSource.assertUserAutoUnlockKey(
                userId = userId,
                userAutoUnlockKey = userAutoUnlockKey,
            )
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
            coVerify {
                vaultSdkSource.getUserEncryptionKey(userId = userId)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for users should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
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

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVault with initializeCrypto authentication failure for orgs should return AuthenticationError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns InitializeCryptoResult.AuthenticationError.asSuccess()

            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            }
        }

    @Test
    fun `unlockVault with initializeCrypto failure for users should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
            } returns Throwable("Fail").asFailure()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
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

    @Test
    fun `unlockVault with initializeCrypto failure for orgs should return GenericError`() =
        runTest {
            val userId = "userId"
            val kdf = MOCK_PROFILE.toSdkParams()
            val email = MOCK_PROFILE.email
            val masterPassword = "drowssap"
            val userKey = "12345"
            val privateKey = "54321"
            val organizationKeys = mapOf("orgId1" to "orgKey1")
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
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
            } returns Throwable("Fail").asFailure()
            assertEquals(
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )

            val result = vaultLockManager.unlockVault(
                userId = userId,
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
                VaultState(
                    unlockedVaultUserIds = emptySet(),
                    unlockingVaultUserIds = emptySet(),
                ),
                vaultLockManager.vaultStateFlow.value,
            )
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
            coVerify(exactly = 1) {
                vaultSdkSource.initializeOrganizationCrypto(
                    userId = userId,
                    request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                )
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
}

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
    userId = "mockId-1",
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
    tokens = AccountJson.Tokens(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to MOCK_ACCOUNT,
    ),
)
