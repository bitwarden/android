package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a collection request.
 *
 * @property name The encrypted name of the collection.
 */
@Serializable
data class CollectionJsonRequest(
    @SerialName("name")
    val name: String?,
)
