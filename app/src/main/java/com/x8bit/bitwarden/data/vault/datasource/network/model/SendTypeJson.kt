package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
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
private class SendTypeSerializer :
    BaseEnumeratedIntSerializer<SendTypeJson>(SendTypeJson.entries.toTypedArray())
