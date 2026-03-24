package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a collection request.
 *
 * @property name The encrypted name of the collection.
 * @property externalId The external ID of the collection.
 * @property groups The group access selections for this collection.
 * @property users The user access selections for this collection.
 */
@Serializable
data class CollectionJsonRequest(
    @SerialName("name")
    val name: String?,

    @SerialName("externalId")
    val externalId: String? = null,

    @SerialName("groups")
    val groups: List<CollectionAccessSelectionJson>? = null,

    @SerialName("users")
    val users: List<CollectionAccessSelectionJson>? = null,
)
