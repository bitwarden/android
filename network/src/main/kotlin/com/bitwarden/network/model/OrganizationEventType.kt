package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representation of events used for organization tracking.
 */
@Serializable(OrganizationEventTypeSerializer::class)
enum class OrganizationEventType {
    @SerialName("1000")
    USER_LOGGED_IN,

    @SerialName("1001")
    USER_CHANGED_PASSWORD,

    @SerialName("1002")
    USER_UPDATED_2FA,

    @SerialName("1003")
    USER_DISABLED_2FA,

    @SerialName("1004")
    USER_RECOVERED_2FA,

    @SerialName("1005")
    USER_FAILED_LOGIN,

    @SerialName("1006")
    USER_FAILED_LOGIN_2FA,

    @SerialName("1007")
    USER_CLIENT_EXPORTED_VAULT,

    @SerialName("1100")
    CIPHER_CREATED,

    @SerialName("1101")
    CIPHER_UPDATED,

    @SerialName("1102")
    CIPHER_DELETED,

    @SerialName("1103")
    CIPHER_ATTACHMENT_CREATED,

    @SerialName("1104")
    CIPHER_ATTACHMENT_DELETED,

    @SerialName("1105")
    CIPHER_SHARED,

    @SerialName("1106")
    CIPHER_UPDATED_COLLECTIONS,

    @SerialName("1107")
    CIPHER_CLIENT_VIEWED,

    @SerialName("1108")
    CIPHER_CLIENT_TOGGLED_PASSWORD_VISIBLE,

    @SerialName("1109")
    CIPHER_CLIENT_TOGGLED_HIDDEN_FIELD_VISIBLE,

    @SerialName("1110")
    CIPHER_CLIENT_TOGGLED_CARD_CODE_VISIBLE,

    @SerialName("1111")
    CIPHER_CLIENT_COPIED_PASSWORD,

    @SerialName("1112")
    CIPHER_CLIENT_COPIED_HIDDEN_FIELD,

    @SerialName("1113")
    CIPHER_CLIENT_COPIED_CARD_CODE,

    @SerialName("1114")
    CIPHER_CLIENT_AUTO_FILLED,

    @SerialName("1115")
    CIPHER_SOFT_DELETED,

    @SerialName("1116")
    CIPHER_RESTORED,

    @SerialName("1117")
    CIPHER_CLIENT_TOGGLED_CARD_NUMBER_VISIBLE,

    @SerialName("1300")
    COLLECTION_CREATED,

    @SerialName("1301")
    COLLECTION_UPDATED,

    @SerialName("1302")
    COLLECTION_DELETED,

    @SerialName("1400")
    GROUP_CREATED,

    @SerialName("1401")
    GROUP_UPDATED,

    @SerialName("1402")
    GROUP_DELETED,

    @SerialName("1500")
    ORGANIZATION_USER_INVITED,

    @SerialName("1501")
    ORGANIZATION_USER_CONFIRMED,

    @SerialName("1502")
    ORGANIZATION_USER_UPDATED,

    @SerialName("1503")
    ORGANIZATION_USER_REMOVED,

    @SerialName("1504")
    ORGANIZATION_USER_UPDATED_GROUPS,

    @SerialName("1600")
    ORGANIZATION_UPDATED,

    @SerialName("1601")
    ORGANIZATION_PURGED_VAULT,

    @SerialName("1618")
    ORGANIZATION_ITEM_ORGANIZATION_ACCEPTED,

    @SerialName("1619")
    ORGANIZATION_ITEM_ORGANIZATION_DECLINED,
}

@Keep
private class OrganizationEventTypeSerializer : BaseEnumeratedIntSerializer<OrganizationEventType>(
    className = "OrganizationEventType",
    values = OrganizationEventType.entries.toTypedArray(),
)
