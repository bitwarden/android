package com.x8bit.bitwarden.data.auth.datasource.disk.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Describes the current display status of the new device notice screen.
 */
@Serializable
enum class NewDeviceNoticeDisplayStatus {
    /**
     * The user has seen the screen and indicated they can access their email.
     */
    @SerialName("canAccessEmail")
    CAN_ACCESS_EMAIL,

    /**
     * The user has indicated they can access their email
     * as specified by the Permanent mode of the notice.
     */
    @SerialName("canAccessEmailPermanent")
    CAN_ACCESS_EMAIL_PERMANENT,

    /**
     * The user has not seen the screen.
     */
    @SerialName("hasNotSeen")
    HAS_NOT_SEEN,

    /**
     * The user has seen the screen and selected "remind me later".
     */
    @SerialName("hasSeen")
    HAS_SEEN,
}

/**
 * The state of the new device notice screen.
 */
@Suppress("MagicNumber")
@Serializable
data class NewDeviceNoticeState(
    @SerialName("displayStatus")
    val displayStatus: NewDeviceNoticeDisplayStatus,

    @SerialName("lastSeenDate")
    @Contextual
    val lastSeenDate: ZonedDateTime?,
) {
    /**
     * Whether the [lastSeenDate] is at least 7 days old.
     */
    val shouldDisplayNoticeIfSeen = lastSeenDate
        ?.isBefore(
            ZonedDateTime.now().minusDays(7),
        )
        ?: false
}
