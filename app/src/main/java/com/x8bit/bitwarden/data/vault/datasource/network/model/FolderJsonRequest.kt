package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a folder request.
 *
 * @property name The name of the folder.
 */
@Serializable
data class FolderJsonRequest(
    @SerialName("name")
    val name: String?,
)
