package com.x8bit.bitwarden.data.platform.manager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Bitwarden push notification.
 *
 * @property contextId The context ID. This is mainly used to check if the push notification
 * originated from this app.
 * @property notificationType The type of notification.
 * @property payload Data associated with the push notification.
 */
@Serializable
data class BitwardenNotification(
    @SerialName("contextId") val contextId: String?,
    @SerialName("type") val notificationType: NotificationType,
    @SerialName("payload") val payload: String,
)
