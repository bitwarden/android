package com.x8bit.bitwarden.data.pam.manager

import android.annotation.SuppressLint
import androidx.core.app.NotificationManagerCompat
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

internal const val PAM_ALERT_CHANNEL_ID: String = "pam_access_alert_channel"
private const val PAM_ALERT_TAG_PREFIX: String = "pam_alert:"
private const val PAM_ALERT_NOTIFICATION_ID: Int = 0

/**
 * The notification tag of the alert about the request with the given [requestId]. Tags (rather
 * than hashed ids) keep the alerts apart from each other and from the lease reminders.
 */
internal fun pamAlertNotificationTag(requestId: String): String = "$PAM_ALERT_TAG_PREFIX$requestId"

/**
 * The default implementation of the [PamNotificationManager].
 *
 * The push service only exists in the standard flavor, so nothing is ever emitted by the
 * [PushManager] in F-Droid builds and no notification is shown there.
 */
@Suppress("LongParameterList")
class PamNotificationManagerImpl(
    private val notificationManager: NotificationManagerCompat,
    private val notificationFactory: PamAlertNotificationFactory,
    private val featureFlagManager: FeatureFlagManager,
    private val pamRepository: PamRepository,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
) : PamNotificationManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)

    /**
     * Requests that have a "waiting for your decision" notification on screen.
     */
    private val shownRequestIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        pushManager
            .pamAlertFlow
            .onEach(::handlePamAlert)
            .launchIn(ioScope)
        merge(pamRepository.accessChangedFlow, pushManager.pamRefreshFlow)
            .onEach { cancelDecidedRequestAlerts() }
            .launchIn(ioScope)
    }

    @SuppressLint("MissingPermission")
    private fun handlePamAlert(data: PamAlertData) {
        if (!featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)) return
        notificationManager.createNotificationChannel(notificationFactory.createChannel())
        if (!notificationManager.areNotificationsEnabled()) return
        val channelImportance = notificationManager
            .getNotificationChannelCompat(PAM_ALERT_CHANNEL_ID)
            ?.importance
        if (channelImportance == NotificationManagerCompat.IMPORTANCE_NONE) return

        notificationManager.notify(
            pamAlertNotificationTag(data.requestId),
            PAM_ALERT_NOTIFICATION_ID,
            notificationFactory.createNotification(data),
        )
        if (data.type == PamAlertType.ACCESS_REQUESTED) shownRequestIds.add(data.requestId)
    }

    private suspend fun cancelDecidedRequestAlerts() {
        if (shownRequestIds.isEmpty()) return
        pamRepository
            .getApprovalInbox()
            .onSuccess { inbox ->
                val pendingIds = inbox.map { it.id }.toSet()
                shownRequestIds
                    .filterNot { it in pendingIds }
                    .forEach { requestId ->
                        notificationManager.cancel(
                            pamAlertNotificationTag(requestId),
                            PAM_ALERT_NOTIFICATION_ID,
                        )
                        shownRequestIds.remove(requestId)
                    }
            }
    }
}
