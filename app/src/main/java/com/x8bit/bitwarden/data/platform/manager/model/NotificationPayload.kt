package com.x8bit.bitwarden.data.platform.manager.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * The payload of a push notification.
 */
@Serializable
sealed class NotificationPayload {
    /**
     * The user ID associated with the push notification.
     */
    abstract val userId: String?

    /**
     * A notification payload for sync cipher operations.
     */
    @Serializable
    data class SyncCipherNotification(
        @SerialName("id") val id: String,
        @SerialName("userId") override val userId: String?,
        @SerialName("organizationId") val organizationId: String?,
        @SerialName("collectionIds") val collectionIds: List<String>?,
        @Contextual
        @SerialName("revisionDate") val revisionDate: ZonedDateTime,
    ) : NotificationPayload()

    /**
     * A notification payload for sync folder operations.
     */
    @Serializable
    data class SyncFolderNotification(
        @SerialName("id") val id: String,
        @SerialName("userId") override val userId: String,
        @Contextual
        @SerialName("revisionDate") val revisionDate: ZonedDateTime,
    ) : NotificationPayload()

    /**
     * A notification payload for user-based operations.
     */
    @Serializable
    data class UserNotification(
        @SerialName("userId") override val userId: String,
        @Contextual
        @SerialName("date") val date: ZonedDateTime,
    ) : NotificationPayload()

    /**
     * A notification payload for sync send operations.
     */
    @Serializable
    data class SyncSendNotification(
        @SerialName("id") val id: String,
        @SerialName("userId") override val userId: String,
        @Contextual
        @SerialName("revisionDate") val revisionDate: ZonedDateTime,
    ) : NotificationPayload()

    /**
     * A notification payload for passwordless requests.
     */
    @Serializable
    data class PasswordlessRequestNotification(
        @SerialName("userId") override val userId: String,
        @SerialName("id") val id: String,
    ) : NotificationPayload()
}
