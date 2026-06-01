package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an import ciphers request.
 *
 * @property folders A list of folders to import.
 * @property ciphers A list of ciphers to import.
 * @property folderRelationships A list of cipher-folder relationships to import. Each entry maps a
 * cipher index (Key) to a folder index (Value).
 */
@Serializable
data class ImportCiphersJsonRequest(
    @SerialName("folders")
    val folders: List<FolderWithIdJsonRequest>,
    @SerialName("ciphers")
    val ciphers: List<CipherJsonRequest>,
    @SerialName("folderRelationships")
    val folderRelationships: List<Int32Int32KeyValuePairJson>,
) {
    /**
     * Represents a folder request with an optional [id] if the folder already exists.
     *
     * @property name The name of the folder.
     * @property id The ID of the folder, if it already exists. Null otherwise.
     **/
    @Serializable
    data class FolderWithIdJsonRequest(
        @SerialName("name")
        val name: String?,
        @SerialName("id")
        val id: String?,
    )

    /**
     * A key-value pair of 32-bit integers, used for mapping relationships.
     *
     * @property key The key, typically an index.
     * @property value The value, typically an index.
     */
    @Serializable
    data class Int32Int32KeyValuePairJson(
        @SerialName("key")
        val key: Int,
        @SerialName("value")
        val value: Int,
    )
}
