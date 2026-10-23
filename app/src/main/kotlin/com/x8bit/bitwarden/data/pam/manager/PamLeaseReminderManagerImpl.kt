package com.x8bit.bitwarden.data.pam.manager

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val REMINDER_LEAD_MINUTES = 5L

/**
 * The amount of time before a lease ends that the reminder is shown.
 */
internal val PAM_LEASE_REMINDER_LEAD_TIME: Duration = Duration.ofMinutes(REMINDER_LEAD_MINUTES)

internal const val PAM_LEASE_REMINDER_TAG = "PamLeaseReminder"

internal fun pamLeaseReminderUserTag(userId: String): String = "PamLeaseReminder_user_$userId"

/**
 * Default implementation of [PamLeaseReminderManager] backed by [WorkManager].
 *
 * Every reminder is tagged with its user so it can be cancelled in bulk when the account goes away
 * or the feature flag is turned off.
 */
class PamLeaseReminderManagerImpl(
    private val workManager: WorkManager,
    private val clock: Clock,
    authDiskSource: AuthDiskSource,
    featureFlagManager: FeatureFlagManager,
    dispatcherManager: DispatcherManager,
) : PamLeaseReminderManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        var knownUserIds: Set<String>? = null
        authDiskSource
            .userStateFlow
            .map { it?.accounts?.keys.orEmpty() }
            .distinctUntilChanged()
            .onEach { currentUserIds ->
                knownUserIds
                    ?.minus(currentUserIds)
                    ?.forEach(::cancelRemindersForUser)
                knownUserIds = currentUserIds
            }
            .launchIn(unconfinedScope)
        featureFlagManager
            .getFeatureFlagFlow(key = FlagKey.PamMobilePrivilegedControls)
            .distinctUntilChanged()
            .onEach { isEnabled -> if (!isEnabled) cancelAllReminders() }
            .launchIn(unconfinedScope)
    }

    override fun scheduleReminder(userId: String, leaseId: String, leaseEndsAt: Instant) {
        val delayMillis = Duration
            .between(clock.instant(), leaseEndsAt.minus(PAM_LEASE_REMINDER_LEAD_TIME))
            .toMillis()
        if (delayMillis <= 0) {
            cancelReminder(leaseId = leaseId)
            return
        }
        val request = OneTimeWorkRequest
            .Builder(PamLeaseReminderWorker::class.java)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(PAM_LEASE_REMINDER_TAG)
            .addTag(pamLeaseReminderUserTag(userId))
            .setInputData(
                Data.Builder()
                    .putString(PamLeaseReminderWorker.KEY_USER_ID, userId)
                    .putString(PamLeaseReminderWorker.KEY_LEASE_ID, leaseId)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork(workName(leaseId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelReminder(leaseId: String) {
        workManager.cancelUniqueWork(workName(leaseId))
    }

    override fun cancelRemindersForUser(userId: String) {
        workManager.cancelAllWorkByTag(pamLeaseReminderUserTag(userId))
    }

    override fun cancelAllReminders() {
        workManager.cancelAllWorkByTag(PAM_LEASE_REMINDER_TAG)
    }

    private fun workName(leaseId: String): String = "PamLeaseReminder_$leaseId"
}
