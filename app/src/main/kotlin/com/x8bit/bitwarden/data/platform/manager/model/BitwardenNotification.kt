package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Represents a Bitwarden push notification.
 *
 * @property contextId The context ID. This is mainly used to check if the push notification
 * originated from this app.
 * @property notificationType The type of notification.
 * @property payload Data associated with the push notification.
 */
data class BitwardenNotification(
    val contextId: String?,
    val notificationType: NotificationType,
    val payload: String,
)
