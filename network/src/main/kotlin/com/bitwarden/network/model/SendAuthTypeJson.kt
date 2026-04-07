package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of send Authentication.
 */
@Serializable(SendAuthTypeSerializer::class)
enum class SendAuthTypeJson {
    /**
     * Email-based OTP authentication.
     */
    @SerialName("0")
    EMAIL,

    /**
     * Password-based authentication.
     */
    @SerialName("1")
    PASSWORD,

    /**
     * No authentication required.
     */
    @SerialName("2")
    NONE,
}

@Keep
private class SendAuthTypeSerializer : BaseEnumeratedIntSerializer<SendAuthTypeJson>(
    className = "SendAuthTypeJson",
    values = SendAuthTypeJson.entries.toTypedArray(),
)
