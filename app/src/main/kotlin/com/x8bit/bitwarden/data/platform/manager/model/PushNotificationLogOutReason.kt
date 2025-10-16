package com.x8bit.bitwarden.data.platform.manager.model

import kotlinx.serialization.SerialName

/**
 * Enumerated values to represent the possible reasons for a log out push notification
 */
enum class PushNotificationLogOutReason {
    @SerialName("0")
    KDF_CHANGE,
}
