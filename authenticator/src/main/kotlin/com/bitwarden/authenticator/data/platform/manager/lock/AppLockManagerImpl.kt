package com.bitwarden.authenticator.data.platform.manager.lock

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The default implementation of the [AppLockManager].
 */
internal class AppLockManagerImpl(
    authDiskSource: AuthDiskSource,
    dispatcherManager: DispatcherManager,
) : AppLockManager {
    private val unconfinedScope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    private val internalHasUserUnlocked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val appLockStateFlow: StateFlow<AppLockState> = combine(
        internalHasUserUnlocked,
        authDiskSource.userBiometricUnlockKeyFlow,
    ) { hasUserUnlocked, userBiometricUnlockKey ->
        getAppLockState(
            hasUserUnlocked = hasUserUnlocked,
            userBiometricUnlockKey = userBiometricUnlockKey,
        )
    }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = getAppLockState(
                hasUserUnlocked = internalHasUserUnlocked.value,
                userBiometricUnlockKey = authDiskSource.getUserBiometricUnlockKey(),
            ),
        )

    override fun manualAppUnlock() {
        internalHasUserUnlocked.update { true }
    }

    private fun getAppLockState(
        hasUserUnlocked: Boolean,
        userBiometricUnlockKey: String?,
    ): AppLockState =
        if (hasUserUnlocked) {
            // The user has manually unlocked, so we are unlocked.
            AppLockState.UNLOCKED
        } else if (userBiometricUnlockKey != null) {
            // The user has not yet manually unlocked and we have a biometric key so we are locked.
            AppLockState.LOCKED
        } else {
            // The user has not yet setup biometrics, so we cannot be locked.
            AppLockState.UNLOCKED
        }
}
