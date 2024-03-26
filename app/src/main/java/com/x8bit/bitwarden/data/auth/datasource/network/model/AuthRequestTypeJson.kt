package com.x8bit.bitwarden.data.auth.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the different types of auth requests.
 */
@Serializable(AuthRequestTypeSerializer::class)
enum class AuthRequestTypeJson {
    @SerialName("0")
    LOGIN_WITH_DEVICE,

    @SerialName("1")
    UNLOCK,

    @SerialName("2")
    ADMIN_APPROVAL,
}

@Keep
private class AuthRequestTypeSerializer :
    BaseEnumeratedIntSerializer<AuthRequestTypeJson>(AuthRequestTypeJson.entries.toTypedArray())
