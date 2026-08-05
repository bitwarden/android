package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of send.
 */
@Serializable(SendTypeSerializer::class)
enum class SendTypeJson {
    /**
     * The send contains text data.
     */
    @SerialName("0")
    TEXT,

    /**
     * The send contains an attached file.
     */
    @SerialName("1")
    FILE,
}

@Keep
private class SendTypeSerializer : BaseEnumeratedIntSerializer<SendTypeJson>(
    className = "SendTypeJson",
    values = SendTypeJson.entries.toTypedArray(),
)
