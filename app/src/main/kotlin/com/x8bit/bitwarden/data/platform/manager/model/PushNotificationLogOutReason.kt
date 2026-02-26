package com.x8bit.bitwarden.data.platform.manager.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enumerated values to represent the possible reasons for a log out push notification
 */
@Serializable(with = PushNotificationLogOutReasonSerializer::class)
enum class PushNotificationLogOutReason {
    @SerialName("0")
    KDF_CHANGE,
}

@Keep
private class PushNotificationLogOutReasonSerializer :
    BaseEnumeratedIntSerializer<PushNotificationLogOutReason>(
        className = "PushNotificationLogOutReason",
        values = PushNotificationLogOutReason.entries.toTypedArray(),
    )
