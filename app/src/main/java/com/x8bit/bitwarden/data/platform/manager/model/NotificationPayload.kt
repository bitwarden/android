package com.x8bit.bitwarden.data.platform.manager.model

import com.x8bit.bitwarden.data.platform.manager.PushManager
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * The payload of a push notification.
 *
 * Note: The data we receive is not always reliable, so everything is nullable and we validate the
 * data in the [PushManager] as necessary.
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
        @SerialName("Id") val cipherId: String?,
        @SerialName("UserId") override val userId: String?,
        @SerialName("OrganizationId") val organizationId: String?,
        @SerialName("CollectionIds") val collectionIds: List<String>?,
        @Contextual
        @SerialName("RevisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for sync folder operations.
     */
    @Serializable
    data class SyncFolderNotification(
        @SerialName("Id") val folderId: String?,
        @SerialName("UserId") override val userId: String?,
        @Contextual
        @SerialName("RevisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for user-based operations.
     */
    @Serializable
    data class UserNotification(
        @SerialName("UserId") override val userId: String?,
        @Contextual
        @SerialName("Date") val date: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for sync send operations.
     */
    @Serializable
    data class SyncSendNotification(
        @SerialName("Id") val sendId: String?,
        @SerialName("UserId") override val userId: String?,
        @Contextual
        @SerialName("RevisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for passwordless requests.
     */
    @Serializable
    data class PasswordlessRequestNotification(
        @SerialName("UserId") override val userId: String?,
        @SerialName("Id") val loginRequestId: String?,
    ) : NotificationPayload()
}
