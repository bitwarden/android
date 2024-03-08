package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
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
private class SecureNoteTypeSerializer :
    BaseEnumeratedIntSerializer<SecureNoteTypeJson>(SecureNoteTypeJson.entries.toTypedArray())
