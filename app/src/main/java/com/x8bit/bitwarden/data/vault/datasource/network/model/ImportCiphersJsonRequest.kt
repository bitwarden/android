package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an import ciphers request.
 *
 * @property folders A list of folders to import.
 * @property ciphers A list of ciphers to import.
 * @property folderRelationships A map of cipher folder relationships to import. Key correlates to
 * the index of the cipher in the ciphers list. Value correlates to the index of the folder in the
 * folders list.
 */
@Serializable
data class ImportCiphersJsonRequest(
    @SerialName("folders")
    val folders: List<FolderWithIdJsonRequest>,
    @SerialName("ciphers")
    val ciphers: List<CipherJsonRequest>,
    @SerialName("folderRelationships")
    val folderRelationships: Map<Int, Int>,
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
}
