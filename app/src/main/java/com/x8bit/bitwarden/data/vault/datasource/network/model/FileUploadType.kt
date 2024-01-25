package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
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
    FileUploadType.entries.toTypedArray(),
)
