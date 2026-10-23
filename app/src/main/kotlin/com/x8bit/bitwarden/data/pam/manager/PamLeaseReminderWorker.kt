package com.x8bit.bitwarden.data.pam.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.util.toPendingIntentMutabilityFlag
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Gives the worker, which is created by WorkManager rather than Hilt, access to what it needs to
 * decide whether the reminder may still be shown.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PamLeaseReminderWorkerEntryPoint {
    /**
     * The feature flag manager.
     */
    fun featureFlagManager(): FeatureFlagManager

    /**
     * The auth disk source, used to see which accounts are still signed in.
     */
    fun authDiskSource(): AuthDiskSource
}

/**
 * Whether a reminder for [userId] may be shown: the feature is on and that account is still
 * signed in on this device.
 */
internal fun shouldShowPamLeaseReminder(
    isFeatureEnabled: Boolean,
    userId: String?,
    signedInUserIds: Set<String>,
): Boolean = isFeatureEnabled && userId != null && userId in signedInUserIds

/**
 * A worker that shows the "your privileged access ends soon" notification.
 */
@OmitFromCoverage
class PamLeaseReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val leaseId = inputData.getString(KEY_LEASE_ID) ?: return Result.failure()
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PamLeaseReminderWorkerEntryPoint::class.java,
        )
        val shouldShow = shouldShowPamLeaseReminder(
            isFeatureEnabled = entryPoint
                .featureFlagManager()
                .getFeatureFlag(FlagKey.PamMobilePrivilegedControls),
            userId = inputData.getString(KEY_USER_ID),
            signedInUserIds = entryPoint
                .authDiskSource()
                .userState
                ?.accounts
                ?.keys
                .orEmpty(),
        )
        if (!shouldShow) return Result.success()
        val notificationManager = NotificationManagerCompat.from(appContext)
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(appContext.getString(BitwardenString.pam_access_reminders_channel))
                .build(),
        )
        if (!notificationManager.areNotificationsEnabled()) return Result.success()
        val launchIntent = appContext
            .packageManager
            .getLaunchIntentForPackage(appContext.packageName)
        val notificationTag = "$NOTIFICATION_TAG_PREFIX$leaseId"
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                appContext,
                notificationTag.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
            )
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(appContext.getString(BitwardenString.pam_access_ending_soon_title))
            .setContentText(appContext.getString(BitwardenString.pam_access_ending_soon_text))
            .setSmallIcon(BitwardenDrawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(notificationTag, NOTIFICATION_ID, notification)
        return Result.success()
    }

    /**
     * Constants for the worker.
     */
    companion object {
        const val KEY_LEASE_ID: String = "lease_id"
        const val KEY_USER_ID: String = "user_id"
        private const val CHANNEL_ID: String = "pam_access_reminder_channel"
        private const val NOTIFICATION_TAG_PREFIX: String = "pam_lease_reminder:"
        private const val NOTIFICATION_ID: Int = 0
    }
}
