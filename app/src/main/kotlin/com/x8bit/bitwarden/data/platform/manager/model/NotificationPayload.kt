package com.x8bit.bitwarden.data.platform.manager.model

import com.x8bit.bitwarden.data.platform.manager.PushManager
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.ZonedDateTime

/**
 * The payload of a push notification.
 *
 * Note: The data we receive is not always reliable, so everything is nullable and we validate the
 * data in the [PushManager] as necessary.
 */
@OptIn(ExperimentalSerializationApi::class)
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
        @JsonNames("Id", "id") val cipherId: String?,
        @JsonNames("UserId", "userId") override val userId: String?,
        @JsonNames("OrganizationId", "organizationId") val organizationId: String?,
        @JsonNames("CollectionIds", "collectionIds") val collectionIds: List<String>?,
        @Contextual
        @JsonNames("RevisionDate", "revisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for sync folder operations.
     */
    @Serializable
    data class SyncFolderNotification(
        @JsonNames("Id", "id") val folderId: String?,
        @JsonNames("UserId", "userId") override val userId: String?,
        @Contextual
        @JsonNames("RevisionDate", "revisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for user-based operations.
     */
    @Serializable
    data class UserNotification(
        @JsonNames("UserId", "userId")
        override val userId: String?,

        @Contextual
        @JsonNames("Date", "date")
        val date: ZonedDateTime?,

        @JsonNames("Reason", "reason")
        val pushNotificationLogOutReason: PushNotificationLogOutReason?,
    ) : NotificationPayload()

    /**
     * A notification payload for sync send operations.
     */
    @Serializable
    data class SyncSendNotification(
        @JsonNames("Id", "id") val sendId: String?,
        @JsonNames("UserId", "userId") override val userId: String?,
        @Contextual
        @JsonNames("RevisionDate", "revisionDate") val revisionDate: ZonedDateTime?,
    ) : NotificationPayload()

    /**
     * A notification payload for passwordless requests.
     */
    @Serializable
    data class PasswordlessRequestNotification(
        @JsonNames("UserId", "userId") override val userId: String?,
        @JsonNames("Id", "id") val loginRequestId: String?,
    ) : NotificationPayload()

    /**
     * A notification payload for resynchronizing organization keys.
     */
    @Serializable
    data class SynchronizeOrganizationKeysNotifications(
        @JsonNames("UserId", "userId") override val userId: String?,
        @JsonNames("Id", "id") val loginRequestId: String?,
    ) : NotificationPayload()

    /**
     * A notification payload for syncing a users vault.
     */
    @Serializable
    data class SyncNotification(
        @JsonNames("UserId", "userId") override val userId: String?,
    ) : NotificationPayload()
}
