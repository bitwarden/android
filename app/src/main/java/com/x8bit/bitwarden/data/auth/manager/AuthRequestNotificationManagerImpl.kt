package com.x8bit.bitwarden.data.auth.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.util.createPasswordlessRequestDataIntent
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The default implementation of the [AuthRequestNotificationManager].
 */
@OmitFromCoverage
class AuthRequestNotificationManagerImpl(
    private val context: Context,
    private val authDiskSource: AuthDiskSource,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
) : AuthRequestNotificationManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)

    init {
        pushManager
            .passwordlessRequestFlow
            .onEach(::handlePasswordlessRequestData)
            .launchIn(ioScope)
    }

    @SuppressLint("MissingPermission")
    private fun handlePasswordlessRequestData(data: PasswordlessRequestData) {
        val notificationManager = NotificationManagerCompat.from(context)
        // Construct the channel, calling this more than once is safe
        notificationManager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(
                    NOTIFICATION_CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                )
                .setName(context.getString(R.string.pending_log_in_requests))
                .build(),
        )
        if (!notificationManager.areNotificationsEnabled(NOTIFICATION_CHANNEL_ID)) return
        // Create the notification
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(createContentIntent(data))
            .setContentTitle(context.getString(R.string.log_in_requested))
            .setContentText(
                authDiskSource
                    .userState
                    ?.accounts
                    ?.get(data.userId)
                    ?.profile
                    ?.email
                    ?.let { context.getString(R.string.confim_log_in_attemp_for_x, it) }
                    ?: context.getString(R.string.confirm_log_in),
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.White.value.toInt())
            .setAutoCancel(true)
            .setTimeoutAfter(NOTIFICATION_DEFAULT_TIMEOUT_MILLIS)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createContentIntent(data: PasswordlessRequestData): PendingIntent =
        PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE,
            createPasswordlessRequestDataIntent(context, data),
            PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
        )

    private fun NotificationManagerCompat.areNotificationsEnabled(
        channelId: String,
    ): Boolean = areNotificationsEnabled() && isChannelEnabled(channelId)

    private fun NotificationManagerCompat.isChannelEnabled(
        channelId: String,
    ): Boolean = getChannelImportance(channelId) != NotificationManagerCompat.IMPORTANCE_NONE

    private fun NotificationManagerCompat.getChannelImportance(
        channelId: String,
    ): Int = this
        .getNotificationChannelCompat(channelId)
        ?.importance
        ?: NotificationManagerCompat.IMPORTANCE_DEFAULT
}

private const val NOTIFICATION_CHANNEL_ID: String = "general_notification_channel"
private const val NOTIFICATION_ID: Int = 2_6072_022
private const val NOTIFICATION_REQUEST_CODE: Int = 20220801
private const val NOTIFICATION_DEFAULT_TIMEOUT_MILLIS: Long = 15L * 60L * 1_000L
