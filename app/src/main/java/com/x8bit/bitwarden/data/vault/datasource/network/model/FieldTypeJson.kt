package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of fields.
 */
@Serializable(FieldTypeSerializer::class)
enum class FieldTypeJson {
    /**
     * The field stores freeform input.
     */
    @SerialName("0")
    TEXT,

    /**
     * The field stores freeform input that is hidden from view.
     */
    @SerialName("1")
    HIDDEN,

    /**
     * The field stores a boolean value.
     */
    @SerialName("2")
    BOOLEAN,

    /**
     * The field value is linked to the item's username or password.
     */
    @SerialName("3")
    LINKED,
}

@Keep
private class FieldTypeSerializer :
    BaseEnumeratedIntSerializer<FieldTypeJson>(FieldTypeJson.entries.toTypedArray())
