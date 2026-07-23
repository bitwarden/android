package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents who is allowed to view a Send under the `SendControls` policy.
 */
@Serializable(SendAccessTypeSerializer::class)
enum class SendAccessTypeJson {
    /**
     * Anyone with the link can view the Send.
     */
    @SerialName("0")
    ANY,

    /**
     * Only individuals with the password set on the Send can view it.
     */
    @SerialName("1")
    PASSWORD_PROTECTED,

    /**
     * Only specific people, identified by email, can view the Send.
     */
    @SerialName("2")
    SPECIFIC_PEOPLE,
}

@Keep
private class SendAccessTypeSerializer :
    BaseEnumeratedIntSerializer<SendAccessTypeJson>(
        className = "SendAccessTypeJson",
        values = SendAccessTypeJson.entries.toTypedArray(),
    )
