package com.x8bit.bitwarden.data.platform.manager.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible notification types.
 */
@Serializable(NotificationTypeSerializer::class)
enum class NotificationType {
    @SerialName("0")
    SYNC_CIPHER_UPDATE,

    @SerialName("1")
    SYNC_CIPHER_CREATE,

    @SerialName("2")
    SYNC_LOGIN_DELETE,

    @SerialName("3")
    SYNC_FOLDER_DELETE,

    @SerialName("4")
    SYNC_CIPHERS,

    @SerialName("5")
    SYNC_VAULT,

    @SerialName("6")
    SYNC_ORG_KEYS,

    @SerialName("7")
    SYNC_FOLDER_CREATE,

    @SerialName("8")
    SYNC_FOLDER_UPDATE,

    @SerialName("9")
    SYNC_CIPHER_DELETE,

    @SerialName("10")
    SYNC_SETTINGS,

    @SerialName("11")
    LOG_OUT,

    @SerialName("12")
    SYNC_SEND_CREATE,

    @SerialName("13")
    SYNC_SEND_UPDATE,

    @SerialName("14")
    SYNC_SEND_DELETE,

    @SerialName("15")
    AUTH_REQUEST,

    @SerialName("16")
    AUTH_REQUEST_RESPONSE,
}

@Keep
private class NotificationTypeSerializer :
    BaseEnumeratedIntSerializer<NotificationType>(NotificationType.entries.toTypedArray())
