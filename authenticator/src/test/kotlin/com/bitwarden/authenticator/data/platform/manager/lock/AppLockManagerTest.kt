package com.bitwarden.authenticator.data.platform.manager.lock

import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppLockManagerTest {

    private val authDiskSource = FakeAuthDiskSource()

    private val appLockManager: AppLockManager = AppLockManagerImpl(
        authDiskSource = authDiskSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `appLockStateFlow should update according to internal state changes`() = runTest {
        // The app has a key, so it should start out locked.
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")

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

            // Resetting the biometric key should keep the app is unlocked.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = "biometricsKey")
            expectNoEvents()
        }
    }
}
