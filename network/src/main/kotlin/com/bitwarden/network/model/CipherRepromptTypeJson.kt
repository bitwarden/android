package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of cipher repromt.
 */
@Serializable(CipherRepromptTypeSerializer::class)
enum class CipherRepromptTypeJson {
    /**
     * No re-prompt is necessary.
     */
    @SerialName("0")
    NONE,

    /**
     * The user should be prompted for their master password prior to using the cipher password.
     */
    @SerialName("1")
    PASSWORD,
}

@Keep
private class CipherRepromptTypeSerializer : BaseEnumeratedIntSerializer<CipherRepromptTypeJson>(
    className = "CipherRepromptTypeJson",
    values = CipherRepromptTypeJson.entries.toTypedArray(),
)
