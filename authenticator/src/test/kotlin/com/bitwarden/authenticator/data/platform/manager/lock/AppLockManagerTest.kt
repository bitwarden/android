package com.bitwarden.authenticator.data.platform.manager.lock

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppTimeout
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.data.manager.appstate.FakeAppStateManager
import com.bitwarden.data.manager.appstate.model.AppForegroundState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {

    private val broadcastReceiver = slot<BroadcastReceiver>()
    private val context: Context = mockk {
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        every { registerReceiver(capture(broadcastReceiver), any()) } returns null
    }
    private val unconfinedDispatcher = UnconfinedTestDispatcher()
    private val appStateManager = FakeAppStateManager(
        appForegroundState = AppForegroundState.FOREGROUNDED,
    )
    private val realtimeManager: RealtimeManager = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val authDiskSource = FakeAuthDiskSource()
    private val settingsDiskSource: SettingsDiskSource = mockk {
        every { appTimeoutInMinutes } returns -1
    }

    private fun createAppLockManager(): AppLockManager = AppLockManagerImpl(
        appStateManager = appStateManager,
        realtimeManager = realtimeManager,
        settingsRepository = settingsRepository,
        authDiskSource = authDiskSource,
        settingsDiskSource = settingsDiskSource,
        dispatcherManager = FakeDispatcherManager(unconfined = unconfinedDispatcher),
        context = context,
    )

    @Test
    fun `On initialization, should migrate user to OnAppRestart`() {
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        every { settingsDiskSource.appTimeoutInMinutes } returns null
        every { settingsRepository.appTimeoutState } returns AppTimeout.OnAppRestart
        every { settingsRepository.appTimeoutState = AppTimeout.OnAppRestart } just runs
        // We just need to make sure the init block runs
        createAppLockManager()

        verify(exactly = 1) {
            settingsRepository.appTimeoutState = AppTimeout.OnAppRestart
        }
    }

    @Test
    fun `appLockStateFlow should update according to internal state changes`() = runTest {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.appLockStateFlow.test {
            assertEquals(AppLockState.LOCKED, awaitItem())

            // Clearing biometric key means the app cannot be locked.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
            assertEquals(AppLockState.UNLOCKED, awaitItem())

            // Resetting the biometric key means the app is locked again.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
            assertEquals(AppLockState.LOCKED, awaitItem())

            // Manual unlock should update state to unlocked.
            appLockManager.manualAppUnlock()
            assertEquals(AppLockState.UNLOCKED, awaitItem())

            // Clearing the key should keep the state as unlocked.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
            expectNoEvents()

            // Resetting the biometric key should keep the app unlocked.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `appLockStateFlow initial state when timeout is Never should be UNLOCKED with biometric key`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.Never
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `appLockStateFlow initial state when timeout is not Never and biometric key exists should be LOCKED`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        assertEquals(AppLockState.LOCKED, appLockManager.appLockStateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `appLockStateFlow initial state when no biometric key should be UNLOCKED regardless of timeout`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        val appLockManager = createAppLockManager()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)
    }

    @Test
    fun `on background when timeout is Never should not start a lock timer`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.Never
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)

        appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
        unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = ONE_MINUTE_MS * 10)

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)
    }

    @Test
    fun `on background when timeout is OnAppRestart should not start a lock timer`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OnAppRestart
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.manualAppUnlock()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)

        appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
        unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = ONE_MINUTE_MS * 10)

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)
    }

    @Test
    fun `on background when timeout is Immediately should lock app`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.Immediately
        every { realtimeManager.elapsedRealtimeMs } returns 0L
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.manualAppUnlock()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)

        appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED

        assertEquals(AppLockState.LOCKED, appLockManager.appLockStateFlow.value)
    }

    @Test
    fun `on background when timeout is OneMinute should lock app after one minute`() = runTest {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        every { realtimeManager.elapsedRealtimeMs } returns 0L
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.appLockStateFlow.test {
            assertEquals(AppLockState.LOCKED, awaitItem())

            appLockManager.manualAppUnlock()
            assertEquals(AppLockState.UNLOCKED, awaitItem())

            appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
            expectNoEvents()

            unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = ONE_MINUTE_MS + 1L)
            assertEquals(AppLockState.LOCKED, awaitItem())
        }
    }

    @Test
    fun `on foreground before timeout fires should cancel pending lock`() {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        every { realtimeManager.elapsedRealtimeMs } returns 0L
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.manualAppUnlock()

        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)

        appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
        appStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        // Advance past the original timeout — job was canceled so no lock should occur.
        unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = ONE_MINUTE_MS)
        assertEquals(AppLockState.UNLOCKED, appLockManager.appLockStateFlow.value)
    }

    @Test
    fun `on screen on event should restart timeout with remaining duration`() = runTest {
        every { settingsRepository.appTimeoutState } returns AppTimeout.OneMinute
        every { realtimeManager.elapsedRealtimeMs } returns 0L
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
        val appLockManager = createAppLockManager()

        appLockManager.appLockStateFlow.test {
            assertEquals(AppLockState.LOCKED, awaitItem())

            appLockManager.manualAppUnlock()
            assertEquals(AppLockState.UNLOCKED, awaitItem())

            // App goes to background at realtime=0 ms, starting a 60 seconds lock timer.
            appStateManager.appForegroundState = AppForegroundState.BACKGROUNDED
            expectNoEvents()

            // Screen turns back on after 30 seconds of real time have elapsed.
            every { realtimeManager.elapsedRealtimeMs } returns THIRTY_SECONDS_MS
            broadcastReceiver.captured.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

            // Advancing 30 seconds should not trigger a lock, since it needs to surpass 30.
            unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = THIRTY_SECONDS_MS)
            expectNoEvents()

            // Advancing another millisecond should trigger the lock.
            unconfinedDispatcher.scheduler.advanceTimeBy(delayTimeMillis = 1L)
            assertEquals(AppLockState.LOCKED, awaitItem())
        }
    }
}

private const val ONE_MINUTE_MS: Long = 60_000L
private const val THIRTY_SECONDS_MS: Long = 30_000L
