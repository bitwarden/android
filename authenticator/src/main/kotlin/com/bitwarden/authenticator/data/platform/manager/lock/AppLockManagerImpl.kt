package com.bitwarden.authenticator.data.platform.manager.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppTimeout
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.realtime.RealtimeManager
import com.bitwarden.data.manager.appstate.AppStateManager
import com.bitwarden.data.manager.appstate.model.AppForegroundState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * The default implementation of the [AppLockManager].
 */
@Suppress("LongParameterList")
internal class AppLockManagerImpl(
    private val appStateManager: AppStateManager,
    private val realtimeManager: RealtimeManager,
    private val settingsRepository: SettingsRepository,
    authDiskSource: AuthDiskSource,
    settingsDiskSource: SettingsDiskSource,
    dispatcherManager: DispatcherManager,
    context: Context,
) : AppLockManager {
    private val unconfinedScope: CoroutineScope = CoroutineScope(dispatcherManager.unconfined)

    /**
     * This tracks the timeout [Job] that is running and the associated data.
     */
    private var timeoutJobData: TimeoutJobData? = null

    init {
        if (authDiskSource.getUserBiometricUnlockKey() != null &&
            settingsDiskSource.appTimeoutInMinutes == null
        ) {
            // Ensures that users who already had biometrics enabled before we added the session
            // timeout feature will have the correct timeout state.
            // This check must occur before the internalHasUserUnlocked is initialized.
            settingsRepository.appTimeoutState = AppTimeout.OnAppRestart
        }
    }

    private val internalHasUserUnlocked: MutableStateFlow<Boolean> = MutableStateFlow(
        value = when (settingsRepository.appTimeoutState) {
            AppTimeout.Never -> true
            AppTimeout.OnAppRestart,
            AppTimeout.Immediately,
            AppTimeout.OneMinute,
            AppTimeout.FiveMinutes,
            AppTimeout.FifteenMinutes,
            AppTimeout.ThirtyMinutes,
            AppTimeout.OneHour,
            AppTimeout.FourHours,
                -> false
        },
    )

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

    init {
        observeAppForegroundChanges()
        context.registerReceiver(
            ScreenStateBroadcastReceiver(),
            IntentFilter(Intent.ACTION_SCREEN_ON),
        )
    }

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
            // The user has not yet manually unlocked, and we have a biometric key so we are locked.
            AppLockState.LOCKED
        } else {
            // The user has not yet setup biometrics, so we cannot be locked.
            AppLockState.UNLOCKED
        }

    private fun observeAppForegroundChanges() {
        appStateManager
            .appForegroundStateFlow
            .onEach { appForegroundState ->
                when (appForegroundState) {
                    AppForegroundState.BACKGROUNDED -> handleOnBackground()
                    AppForegroundState.FOREGROUNDED -> handleOnForeground()
                }
            }
            .launchIn(unconfinedScope)
    }

    private fun handleOnBackground() {
        when (val appTimeout = settingsRepository.appTimeoutState) {
            AppTimeout.Never -> {
                // Verify that there is no task to lock the app since we never lock.
                clearJob()
            }

            AppTimeout.OnAppRestart -> {
                // Verify that there is no task to lock the app while running, we will
                // naturally lock the app on restart.
                clearJob()
            }

            AppTimeout.Immediately,
            AppTimeout.OneMinute,
            AppTimeout.FiveMinutes,
            AppTimeout.FifteenMinutes,
            AppTimeout.ThirtyMinutes,
            AppTimeout.OneHour,
            AppTimeout.FourHours,
                -> {
                handleTimeoutActionWithDelay(
                    delayMs = appTimeout
                        .timeoutInMinutes
                        .minutes
                        .inWholeMilliseconds,
                )
            }
        }
    }

    private fun handleOnForeground() {
        clearJob()
    }

    private fun clearJob() {
        timeoutJobData?.job?.cancel()
        timeoutJobData = null
    }

    /**
     * Locks the app after the [delayMs] has passed.
     */
    private fun handleTimeoutActionWithDelay(delayMs: Long) {
        timeoutJobData?.job?.cancel()
        timeoutJobData = TimeoutJobData(
            job = unconfinedScope.launch {
                delay(timeMillis = delayMs)
                timeoutJobData = null
                internalHasUserUnlocked.update { false }
            },
            startTimeMs = realtimeManager.elapsedRealtimeMs,
            durationMs = delayMs,
        )
    }

    /**
     * A custom [BroadcastReceiver] that listens for when the screen is powered on and restarts the
     * timeout job to ensure they complete at the correct time.
     *
     * This is necessary because the [delay] function in a coroutine will not keep accurate time
     * when the screen is off. We do not cancel the job when the screen is off, this allows the
     * job to complete as-soon-as possible if the screen is powered off for an extended period.
     */
    private inner class ScreenStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            timeoutJobData?.let {
                val durationSoFarMs = (realtimeManager.elapsedRealtimeMs - it.startTimeMs)
                    .coerceAtLeast(minimumValue = 0L)
                handleTimeoutActionWithDelay(delayMs = it.durationMs - durationSoFarMs)
            }
        }
    }

    /**
     * A wrapper class containing all relevant data concerning a timeout action [Job].
     */
    private data class TimeoutJobData(
        val job: Job,
        val startTimeMs: Long,
        val durationMs: Long,
    )
}
