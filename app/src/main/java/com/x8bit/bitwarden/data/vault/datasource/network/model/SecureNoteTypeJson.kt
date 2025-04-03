package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of secure notes.
 */
@Serializable(SecureNoteTypeSerializer::class)
enum class SecureNoteTypeJson {
    /**
     * A generic note.
     */
    @SerialName("0")
    GENERIC,
}

@Keep
private class SecureNoteTypeSerializer : BaseEnumeratedIntSerializer<SecureNoteTypeJson>(
    className = "SecureNoteTypeJson",
    values = SecureNoteTypeJson.entries.toTypedArray(),
)
