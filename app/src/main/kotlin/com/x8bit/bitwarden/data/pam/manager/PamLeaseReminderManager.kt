package com.x8bit.bitwarden.data.pam.manager

import java.time.Instant

/**
 * Schedules the local notification that warns the user shortly before a privileged access lease
 * ends.
 *
 * Timing is approximate (backed by WorkManager) so no exact-alarm permission is needed.
 */
interface PamLeaseReminderManager {

    /**
     * Schedules (or reschedules) a reminder for the lease with the given [leaseId] of the user
     * with the given [userId], shown 5 minutes before [leaseEndsAt]. Any previously scheduled
     * reminder for the lease is replaced. If the reminder time has already passed, nothing is
     * scheduled.
     *
     * Reminders are cancelled automatically when their user is logged out or removed, or when the
     * PAM feature flag is turned off.
     */
    fun scheduleReminder(userId: String, leaseId: String, leaseEndsAt: Instant)

    /**
     * Cancels the reminder for the lease with the given [leaseId], if any.
     */
    fun cancelReminder(leaseId: String)

    /**
     * Cancels every reminder scheduled for the user with the given [userId].
     */
    fun cancelRemindersForUser(userId: String)

    /**
     * Cancels every PAM lease reminder, for every user.
     */
    fun cancelAllReminders()
}
