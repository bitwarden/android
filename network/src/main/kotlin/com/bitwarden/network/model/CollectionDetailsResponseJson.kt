package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for collection details including access permissions.
 *
 * Corresponds to the server's CollectionAccessDetailsResponseModel returned
 * from GET /organizations/{orgId}/collections/{id}/details endpoint.
 *
 * @property id The collection ID.
 * @property organizationId The organization ID.
 * @property name The encrypted collection name.
 * @property externalId The external ID of the collection.
 * @property groups The group access selections for this collection.
 * @property users The user access selections for this collection.
 */
@Serializable
data class CollectionDetailsResponseJson(
    @SerialName("id")
    val id: String,

    @SerialName("organizationId")
    val organizationId: String,

    @SerialName("name")
    val name: String,

    @SerialName("externalId")
    val externalId: String?,

    @SerialName("groups")
    val groups: List<CollectionAccessSelectionJson>?,

    @SerialName("users")
    val users: List<CollectionAccessSelectionJson>?,
)
