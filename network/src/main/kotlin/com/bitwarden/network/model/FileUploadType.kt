package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of file upload that should be used.
 */
@Serializable(FileUploadTypeSerializer::class)
enum class FileUploadType {
    @SerialName("0")
    DIRECT,

    @SerialName("1")
    AZURE,
}

@Keep
private class FileUploadTypeSerializer : BaseEnumeratedIntSerializer<FileUploadType>(
    className = "FileUploadType",
    values = FileUploadType.entries.toTypedArray(),
)
