package com.x8bit.bitwarden.data.pam.manager

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.toPendingIntentMutabilityFlag
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.pam.util.createPamAlertIntent
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType

/**
 * Builds the Android notification objects for the PAM alerts. Kept apart from
 * [PamNotificationManagerImpl] so that the decision logic can be tested without the Android
 * framework.
 */
interface PamAlertNotificationFactory {

    /**
     * Creates the channel the alerts are shown in.
     */
    fun createChannel(): NotificationChannelCompat

    /**
     * Creates the generic notification for the given alert. Tapping it opens the request.
     */
    fun createNotification(data: PamAlertData): Notification
}

/**
 * Default implementation of [PamAlertNotificationFactory].
 */
@OmitFromCoverage
class PamAlertNotificationFactoryImpl(
    private val context: Context,
) : PamAlertNotificationFactory {

    override fun createChannel(): NotificationChannelCompat =
        NotificationChannelCompat
            .Builder(PAM_ALERT_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(BitwardenString.pam_access_alerts_channel))
            .build()

    override fun createNotification(data: PamAlertData): Notification =
        NotificationCompat.Builder(context, PAM_ALERT_CHANNEL_ID)
            .setContentIntent(createContentIntent(data))
            .setContentTitle(context.getString(BitwardenString.pam_alert_title))
            .setContentText(context.getString(data.type.textRes()))
            .setSmallIcon(BitwardenDrawable.ic_notification)
            .setColor(Color.White.value.toInt())
            .setAutoCancel(true)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MILLIS)
            .build()

    private fun createContentIntent(data: PamAlertData): PendingIntent =
        PendingIntent.getActivity(
            context,
            pamAlertNotificationTag(data.requestId).hashCode(),
            createPamAlertIntent(context = context, data = data),
            PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )
}

private const val NOTIFICATION_TIMEOUT_MILLIS: Long = 24L * 60L * 60L * 1_000L

/**
 * Generic text for each alert type. Never contains names.
 */
internal fun PamAlertType.textRes(): Int = when (this) {
    PamAlertType.ACCESS_REQUESTED -> BitwardenString.pam_alert_requested
    PamAlertType.ACCESS_APPROVED -> BitwardenString.pam_alert_approved
    PamAlertType.ACCESS_DENIED -> BitwardenString.pam_alert_denied
    PamAlertType.ACCESS_REVOKED -> BitwardenString.pam_alert_revoked
}
